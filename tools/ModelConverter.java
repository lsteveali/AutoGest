import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * Convierte un modelo OBJ descargado en el formato compacto {@code .agm} que
 * carga la vitrina del login ({@code ImportedModel}). Herramienta del equipo:
 * no forma parte de la aplicación.
 *
 * <p>Uso, desde la raíz del repositorio:</p>
 * <pre>java tools/ModelConverter.java &lt;modelo.obj&gt; tools/models/&lt;carro&gt;.rules src/main/resources/models/&lt;carro&gt;.agm</pre>
 *
 * <p>Las reglas ({@code .rules}) dicen el largo real del carro, hacia dónde
 * mira, qué piezas se descartan (pernos, fondos, luces de la escena) y qué
 * papel cumple cada pieza según su objeto y su material del OBJ: la pintura
 * (la que se recolorea), vidrio, cromo, caucho, etc. La primera regla que
 * coincide gana; {@code *} vale por cualquier texto. Con {@code mirror} se
 * reflejan al otro costado las piezas que el modelo solo trae de un lado, y
 * con {@code tires} se dicen cuáles objetos son los cuatro cauchos (para la
 * sombra), si hay más caucho en el carro. Con {@code twosided} un papel se
 * dibuja por las dos caras, para modelos con caras volteadas. El frente puede ser un eje o una
 * dirección {@code dx dz} para modelos girados.</p>
 *
 * <p>El resultado queda en metros, con el frente hacia +X, Y hacia arriba,
 * el piso en y = 0 y el carro centrado. Las posiciones se guardan como
 * enteros de 16 bits y las normales de 8 bits, comprimidos con GZIP.</p>
 */
public final class ModelConverter {

    /** Medio rango de las posiciones guardadas, en metros. */
    static final float RANGE = 4f;

    private record Rule(Pattern object, Pattern material, String role) {
    }

    /** Pieza en construcción: vértices únicos (posición + normal) y triángulos. */
    private static final class Part {
        final Map<Long, Integer> index = new HashMap<>();
        final List<float[]> vertices = new ArrayList<>();
        final List<int[]> triangles = new ArrayList<>();
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Uso: java tools/ModelConverter.java <modelo.obj> <reglas.rules> <salida.agm>");
            System.exit(2);
        }
        Path obj = Path.of(args[0]);
        Path rulesFile = Path.of(args[1]);
        Path out = Path.of(args[2]);

        double length = 0;
        double[] front = {1, 0};
        List<Rule> rules = new ArrayList<>();
        List<Pattern> mirrors = new ArrayList<>();
        List<Pattern> tires = new ArrayList<>();
        List<String> twoSided = new ArrayList<>();
        for (String raw : Files.readAllLines(rulesFile, StandardCharsets.UTF_8)) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] t = line.split("\\s+");
            switch (t[0]) {
                case "length" -> length = Double.parseDouble(t[1]);
                case "front" -> front = direction(t);
                case "part" -> rules.add(new Rule(glob(t[1]), glob(t[2]), t[3]));
                case "mirror" -> mirrors.add(glob(t[1]));
                case "tires" -> {
                    for (int i = 1; i < t.length; i++) {
                        tires.add(glob(t[i]));
                    }
                }
                case "twosided" -> twoSided.addAll(List.of(t).subList(1, t.length));
                default -> throw new IllegalArgumentException("Regla desconocida: " + line);
            }
        }
        if (length <= 0) {
            throw new IllegalArgumentException("Falta «length» en las reglas.");
        }

        List<float[]> positions = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        Map<String, Part> parts = new LinkedHashMap<>();
        Map<String, Integer> dropped = new LinkedHashMap<>();
        // Eje del ancho en las coordenadas del OBJ: el que no es el del frente ni Y.
        int widthAxis = Math.abs(front[1]) > 0.999 ? 0 : 2;
        if (!mirrors.isEmpty() && Math.abs(front[0]) < 0.999 && Math.abs(front[1]) < 0.999) {
            throw new IllegalArgumentException("«mirror» solo funciona con un frente alineado a X o a Z.");
        }
        List<float[]> tirePoints = new ArrayList<>();
        String object = "";
        String material = "";
        String role = "drop";
        boolean mirrored = false;
        boolean tire = false;
        try (BufferedReader reader = Files.newBufferedReader(obj, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("v ")) {
                    positions.add(floats(line));
                } else if (line.startsWith("vn ")) {
                    normals.add(floats(line));
                } else if (line.startsWith("o ") || line.startsWith("g ")) {
                    object = line.substring(2).strip();
                    role = roleFor(rules, object, material);
                    String current = object;
                    mirrored = mirrors.stream().anyMatch(p -> p.matcher(current).matches());
                    tire = tires.stream().anyMatch(p -> p.matcher(current).matches());
                } else if (line.startsWith("usemtl ")) {
                    material = line.substring(7).strip();
                    role = roleFor(rules, object, material);
                } else if (line.startsWith("f ")) {
                    String[] corners = line.substring(2).strip().split("\\s+");
                    if (role.equals("drop")) {
                        dropped.merge(object + " | " + material, corners.length - 2, Integer::sum);
                        continue;
                    }
                    Part part = parts.computeIfAbsent(role, r -> new Part());
                    addFace(part, corners, positions, normals, -1);
                    if (mirrored) {
                        addFace(part, corners, positions, normals, widthAxis);
                    }
                    if (tire) {
                        for (String corner : corners) {
                            float[] p = positions.get(resolve(corner.split("/")[0], positions.size()));
                            tirePoints.add(new float[]{p[0], p[1], p[2]});
                        }
                    }
                }
            }
        }

        // Piezas de dos caras: cada triángulo se repite con la normal al revés, para que
        // se vean aunque el modelo traiga caras volteadas (JavaFX no dibuja la cara de atrás).
        for (String name : twoSided) {
            Part part = parts.get(name);
            if (part == null) {
                throw new IllegalArgumentException("«twosided» nombra un papel sin piezas: " + name);
            }
            int count = part.vertices.size();
            for (int i = 0; i < count; i++) {
                float[] v = part.vertices.get(i);
                part.vertices.add(new float[]{v[0], v[1], v[2], -v[3], -v[4], -v[5]});
            }
            int triangles = part.triangles.size();
            for (int i = 0; i < triangles; i++) {
                int[] t = part.triangles.get(i);
                part.triangles.add(new int[]{t[0] + count, t[2] + count, t[1] + count});
            }
        }

        // Orientación: el frente pasa a +X con un giro alrededor de Y (se conserva la mano derecha).
        for (Part part : parts.values()) {
            for (float[] v : part.vertices) {
                orient(v, 0, front);
                orient(v, 3, front);
            }
        }
        for (float[] p : tirePoints) {
            orient(p, 0, front);
        }
        float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (Part part : parts.values()) {
            for (float[] v : part.vertices) {
                for (int c = 0; c < 3; c++) {
                    min[c] = Math.min(min[c], v[c]);
                    max[c] = Math.max(max[c], v[c]);
                }
            }
        }
        double scale = length / (max[0] - min[0]);
        float cx = (min[0] + max[0]) / 2;
        float cz = (min[2] + max[2]) / 2;
        for (Part part : parts.values()) {
            for (float[] v : part.vertices) {
                v[0] = (float) ((v[0] - cx) * scale);
                v[1] = (float) ((v[1] - min[1]) * scale);
                v[2] = (float) ((v[2] - cz) * scale);
            }
        }
        for (float[] p : tirePoints) {
            p[0] = (float) ((p[0] - cx) * scale);
            p[1] = (float) ((p[1] - min[1]) * scale);
            p[2] = (float) ((p[2] - cz) * scale);
        }

        float[] footprint = footprint(parts, tirePoints);
        int triangles = 0;
        Files.createDirectories(out.toAbsolutePath().getParent());
        try (OutputStream file = Files.newOutputStream(out);
             DataOutputStream data = new DataOutputStream(new GZIPOutputStream(file))) {
            data.writeBytes("AGM1");
            data.writeFloat(RANGE);
            for (float value : footprint) {
                data.writeFloat(value);
            }
            data.writeInt(parts.size());
            for (Map.Entry<String, Part> entry : parts.entrySet()) {
                Part part = entry.getValue();
                data.writeUTF(entry.getKey());
                data.writeInt(part.vertices.size());
                for (float[] v : part.vertices) {
                    for (int c = 0; c < 3; c++) {
                        data.writeShort(Math.round(Math.max(-1, Math.min(1, v[c] / RANGE)) * 32767));
                    }
                    double len = Math.sqrt(v[3] * v[3] + v[4] * v[4] + v[5] * v[5]);
                    for (int c = 3; c < 6; c++) {
                        data.writeByte((int) Math.round(len > 0 ? v[c] / len * 127 : 0));
                    }
                }
                data.writeInt(part.triangles.size());
                for (int[] t : part.triangles) {
                    for (int i : t) {
                        data.writeInt(i);
                    }
                }
                triangles += part.triangles.size();
                System.out.printf(Locale.ROOT, "  %-10s %8d vértices %8d triángulos%n", entry.getKey(),
                        part.vertices.size(), part.triangles.size());
            }
        }
        System.out.printf(Locale.ROOT, "Total: %d triángulos; tamaño %.2f m × %.2f m × %.2f m; archivo %.2f MB%n",
                triangles, (max[0] - min[0]) * scale, (max[1] - min[1]) * scale, (max[2] - min[2]) * scale,
                Files.size(out) / 1048576.0);
        System.out.println("Descartado:");
        dropped.forEach((k, v) -> System.out.printf(Locale.ROOT, "  %-50s %8d%n", k, v));
        System.out.printf(Locale.ROOT, "Huella: ejes %.2f / %.2f, trocha %.2f, medio largo %.2f, medio ancho %.2f%n",
                footprint[4], footprint[5], footprint[3], footprint[1], footprint[2]);
    }

    /**
     * Agrega una cara (en abanico de triángulos) a la pieza. Con
     * {@code mirrorAxis} en 0 o 2 agrega su copia reflejada al otro lado del
     * carro, para piezas que el modelo solo trae de un costado.
     */
    private static void addFace(Part part, String[] corners, List<float[]> positions, List<float[]> normals,
                                int mirrorAxis) {
        int[] ids = new int[corners.length];
        for (int i = 0; i < corners.length; i++) {
            String[] refs = corners[i].split("/");
            int v = resolve(refs[0], positions.size());
            int n = refs.length > 2 && !refs[2].isEmpty() ? resolve(refs[2], normals.size()) : -1;
            long key = ((long) v << 32) | (n & 0xffffffffL);
            if (mirrorAxis >= 0) {
                key ^= 1L << 63;
            }
            Integer id = part.index.get(key);
            if (id == null) {
                float[] p = positions.get(v);
                float[] nn = n >= 0 ? normals.get(n) : new float[]{0, 1, 0};
                float[] vertex = {p[0], p[1], p[2], nn[0], nn[1], nn[2]};
                if (mirrorAxis >= 0) {
                    vertex[mirrorAxis] = -vertex[mirrorAxis];
                    vertex[3 + mirrorAxis] = -vertex[3 + mirrorAxis];
                }
                id = part.vertices.size();
                part.vertices.add(vertex);
                part.index.put(key, id);
            }
            ids[i] = id;
        }
        for (int i = 1; i + 1 < ids.length; i++) {
            part.triangles.add(new int[]{ids[0], ids[i], ids[i + 1]});
        }
    }

    /**
     * Huella para la sombra: carrocería (pintura) y cauchos. Devuelve centro X,
     * medio largo, medio ancho, trocha, eje delantero, eje trasero y medio
     * largo y medio ancho de la mancha de cada caucho. Los cauchos son las
     * piezas de la regla {@code tires} o, si no hay, todo lo marcado «rubber».
     */
    private static float[] footprint(Map<String, Part> parts, List<float[]> tirePoints) {
        float[] body = bounds(parts.get("paint"));
        List<float[]> points = tirePoints;
        if (points.isEmpty()) {
            Part rubber = parts.get("rubber");
            if (rubber == null) {
                throw new IllegalArgumentException("Las reglas deben marcar los cauchos como «rubber» o con «tires».");
            }
            points = rubber.vertices;
        }
        double frontSum = 0;
        double rearSum = 0;
        double zSum = 0;
        double top = 0;
        int frontCount = 0;
        int rearCount = 0;
        for (float[] v : points) {
            if (v[0] > 0) {
                frontSum += v[0];
                frontCount++;
            } else {
                rearSum += v[0];
                rearCount++;
            }
            zSum += Math.abs(v[2]);
            top = Math.max(top, v[1]);
        }
        float radius = (float) (top / 2);
        return new float[]{(body[0] + body[1]) / 2, (body[1] - body[0]) / 2, (body[5] - body[4]) / 2,
                (float) (zSum / points.size()), (float) (frontSum / frontCount),
                (float) (rearSum / rearCount), radius * 0.65f, radius * 0.45f};
    }

    private static float[] bounds(Part part) {
        float[] b = {Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE,
                -Float.MAX_VALUE};
        for (float[] v : part.vertices) {
            for (int c = 0; c < 3; c++) {
                b[2 * c] = Math.min(b[2 * c], v[c]);
                b[2 * c + 1] = Math.max(b[2 * c + 1], v[c]);
            }
        }
        return b;
    }

    /**
     * Gira un punto o una normal alrededor de Y para que la dirección del
     * frente {@code (dx, dz)} quede en +X.
     */
    private static void orient(float[] v, int at, double[] front) {
        double x = v[at];
        double z = v[at + 2];
        v[at] = (float) (x * front[0] + z * front[1]);
        v[at + 2] = (float) (-x * front[1] + z * front[0]);
    }

    /**
     * Dirección del frente en el plano X-Z: {@code +x}, {@code -x}, {@code +z},
     * {@code -z} o dos números (para modelos girados en diagonal).
     */
    private static double[] direction(String[] t) {
        double[] d = switch (t[1]) {
            case "+x" -> new double[]{1, 0};
            case "-x" -> new double[]{-1, 0};
            case "+z" -> new double[]{0, 1};
            case "-z" -> new double[]{0, -1};
            default -> {
                if (t.length < 3) {
                    throw new IllegalArgumentException("«front» debe ser +x, -x, +z, -z o dos números dx dz.");
                }
                yield new double[]{Double.parseDouble(t[1]), Double.parseDouble(t[2])};
            }
        };
        double length = Math.hypot(d[0], d[1]);
        return new double[]{d[0] / length, d[1] / length};
    }

    private static String roleFor(List<Rule> rules, String object, String material) {
        for (Rule rule : rules) {
            if (rule.object().matcher(object).matches() && rule.material().matcher(material).matches()) {
                return rule.role();
            }
        }
        return "drop";
    }

    private static Pattern glob(String text) {
        StringBuilder regex = new StringBuilder();
        for (char c : text.toCharArray()) {
            regex.append(c == '*' ? ".*" : Pattern.quote(String.valueOf(c)));
        }
        return Pattern.compile(regex.toString());
    }

    private static int resolve(String ref, int size) {
        int i = Integer.parseInt(ref);
        return i < 0 ? size + i : i - 1;
    }

    private static float[] floats(String line) {
        String[] t = line.strip().split("\\s+");
        return new float[]{Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3])};
    }
}
