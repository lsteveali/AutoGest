package com.autogest.controller.components;

import com.autogest.controller.components.MeshKit.Builder;
import com.autogest.controller.components.MeshKit.Shading;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

/**
 * Vehículos importados de modelos 3D descargados (por ejemplo, el Mustang).
 * El modelo se convierte una sola vez con {@code tools/ModelConverter.java} al
 * formato {@code .agm}: piezas agrupadas por papel (pintura, vidrio, cromo,
 * caucho…), en metros, con el frente hacia +X y el piso en y = 0. Aquí se lee
 * ese archivo y cada pieza recibe el material de la vitrina; la pintura usa el
 * color elegido, igual que en los modelos hechos por código.
 *
 * <p>Lo leído se guarda en memoria, así volver a elegir el mismo carro no
 * vuelve a abrir el archivo.</p>
 *
 * <p>Principles: Pure Fabrication, Protected Variations.</p>
 */
final class ImportedModel {

    private static final String MAGIC = "AGM1";
    private static final Map<String, Data> CACHE = new ConcurrentHashMap<>();

    private ImportedModel() {
    }

    /**
     * Pieza del modelo: un papel y su malla.
     *
     * @param role      papel de la pieza (paint, glass, chrome, rubber…)
     * @param positions coordenadas x, y, z en metros
     * @param normals   normales x, y, z por vértice
     * @param triangles índices de vértices, de a tres
     */
    record Part(String role, float[] positions, float[] normals, int[] triangles) {
    }

    /**
     * Contenido de un archivo {@code .agm}.
     *
     * @param footprint huella para la sombra de contacto
     * @param parts     piezas en el orden del archivo
     */
    record Data(VehicleBuild.Footprint footprint, List<Part> parts) {

        /** Total de triángulos del modelo. */
        int triangleCount() {
            return parts.stream().mapToInt(part -> part.triangles().length / 3).sum();
        }

        /** Altura máxima de la pintura (techo), en metros. */
        float paintHeight() {
            return heights("paint")[1];
        }

        /**
         * Altura media de los vidrios: el reflejo de las ventanas se centra
         * ahí, igual que en los modelos hechos por código.
         */
        float glassMiddle() {
            float[] range = heights("glass");
            return range[0] > range[1] ? paintHeight() * 0.82f : (range[0] + range[1]) / 2;
        }

        /** Altura mínima y máxima de las piezas con ese papel. */
        private float[] heights(String role) {
            float low = Float.MAX_VALUE;
            float top = -Float.MAX_VALUE;
            for (Part part : parts) {
                if (part.role().equals(role)) {
                    for (int i = 1; i < part.positions().length; i += 3) {
                        low = Math.min(low, part.positions()[i]);
                        top = Math.max(top, part.positions()[i]);
                    }
                }
            }
            return top < low ? new float[]{1, 0} : new float[]{low, top};
        }
    }

    /**
     * Lee un modelo de los recursos de la aplicación.
     *
     * @param resource ruta del recurso, por ejemplo {@code /models/mustang.agm}
     * @return el modelo leído
     * @throws IllegalStateException si el recurso no existe o está dañado
     */
    static Data read(String resource) {
        return CACHE.computeIfAbsent(resource, path -> {
            try (InputStream in = ImportedModel.class.getResourceAsStream(path)) {
                if (in == null) {
                    throw new IllegalStateException("No está el modelo " + path);
                }
                return read(in);
            } catch (IOException error) {
                throw new IllegalStateException("No se pudo leer el modelo " + path, error);
            }
        });
    }

    /**
     * Lee un modelo {@code .agm}: posiciones en enteros de 16 bits dentro del
     * rango del encabezado y normales de 8 bits, comprimidos con GZIP.
     *
     * @param input contenido del archivo
     * @return el modelo leído
     * @throws IOException si el archivo no tiene el formato esperado
     */
    static Data read(InputStream input) throws IOException {
        DataInputStream data = new DataInputStream(new GZIPInputStream(input));
        byte[] magic = new byte[4];
        data.readFully(magic);
        if (!MAGIC.equals(new String(magic, java.nio.charset.StandardCharsets.US_ASCII))) {
            throw new IOException("No es un modelo de AutoGest.");
        }
        float range = data.readFloat();
        float[] f = new float[8];
        for (int i = 0; i < f.length; i++) {
            f[i] = data.readFloat();
        }
        VehicleBuild.Footprint footprint = new VehicleBuild.Footprint(f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7]);

        int count = data.readInt();
        List<Part> parts = new ArrayList<>(count);
        for (int p = 0; p < count; p++) {
            String role = data.readUTF();
            int vertices = data.readInt();
            float[] positions = new float[3 * vertices];
            float[] normals = new float[3 * vertices];
            for (int v = 0; v < vertices; v++) {
                for (int c = 0; c < 3; c++) {
                    positions[3 * v + c] = data.readShort() / 32767f * range;
                }
                for (int c = 0; c < 3; c++) {
                    normals[3 * v + c] = data.readByte() / 127f;
                }
            }
            int[] triangles = new int[3 * data.readInt()];
            for (int i = 0; i < triangles.length; i++) {
                triangles[i] = data.readInt();
                if (triangles[i] < 0 || triangles[i] >= vertices) {
                    throw new IOException("Índice fuera de rango en la pieza " + role);
                }
            }
            parts.add(new Part(role, positions, normals, triangles));
        }
        return new Data(footprint, List.copyOf(parts));
    }

    /**
     * Arma el vehículo para la vitrina.
     *
     * @param resource ruta del modelo en los recursos
     * @param color    color de la pintura
     * @return el vehículo, su malla de pintura y su huella
     */
    static VehicleBuild build(String resource, Color color) {
        Data data = read(resource);
        float height = data.paintHeight();
        double horizon = height * 0.72;
        double windowMiddle = data.glassMiddle();
        Group car = new Group();
        MeshView paintView = null;
        for (Part part : data.parts()) {
            Builder builder = new Builder(shading(part.role(), horizon, windowMiddle));
            float[] p = part.positions();
            float[] n = part.normals();
            for (int v = 0; v < p.length / 3; v++) {
                builder.vertex(p[3 * v], p[3 * v + 1], p[3 * v + 2], n[3 * v], n[3 * v + 1], n[3 * v + 2]);
            }
            int[] t = part.triangles();
            for (int i = 0; i < t.length; i += 3) {
                builder.triangle(t[i], t[i + 1], t[i + 2]);
            }
            boolean paint = part.role().equals("paint");
            if (builder.isEmpty()) {
                continue;
            }
            MeshView view = builder.view(paint ? VehiclePaint.material(color) : material(part.role()));
            car.getChildren().add(view);
            if (paint) {
                paintView = view;
            }
        }
        if (paintView == null) {
            throw new IllegalStateException("El modelo " + resource + " no tiene pintura.");
        }
        return new VehicleBuild(car, paintView, data.footprint());
    }

    private static Shading shading(String role, double horizon, double windowMiddle) {
        return switch (role) {
            case "paint" -> (x, y, z, nx, ny, nz) -> 0.5 - 0.42 * ny + 0.12 * (horizon - y);
            // Los vidrios importados suelen ser más inclinados que los hechos por código:
            // la normal pesa menos para que no tomen todo el reflejo claro del cielo.
            case "glass" -> (x, y, z, nx, ny, nz) -> 0.62 - 0.2 * ny + 0.3 * (windowMiddle - y);
            // Bandas de reflejo que se repiten con la altura, como el cromo de la camioneta.
            case "chrome", "rim", "rimDark" -> (x, y, z, nx, ny, nz) -> 0.5 - 0.4 * ny + 0.18 * Math.sin(y * 17);
            default -> Shading.flat();
        };
    }

    private static PhongMaterial material(String role) {
        return switch (role) {
            case "glass" -> VehicleMaterials.glass();
            case "chrome" -> VehicleMaterials.chrome();
            case "rim" -> VehicleMaterials.rim();
            case "rimDark" -> VehicleMaterials.darkRim();
            case "disc" -> VehicleMaterials.disc();
            case "rubber" -> VehicleMaterials.rubber();
            case "well" -> VehicleMaterials.well();
            case "grille" -> VehicleMaterials.grille();
            case "lens" -> VehicleMaterials.lens();
            case "tail" -> VehicleMaterials.tail();
            case "plate" -> VehicleMaterials.plate();
            default -> VehicleMaterials.trim();
        };
    }
}
