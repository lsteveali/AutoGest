package com.autogest.controller.components;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;

import java.util.Arrays;

/**
 * Pequeño motor de mallas para los modelos 3D de la vitrina del login. Arma
 * superficies con normales propias (cajas con aristas redondeadas, piezas
 * torneadas y sólidos planos) y las acumula en un {@link Builder} por
 * material, de modo que cada material termina siendo una sola malla.
 *
 * <p>Las coordenadas van en metros con el eje Y hacia arriba; el
 * {@link Builder} las convierte a JavaFX (Y hacia abajo) al final. Las piezas
 * se pueden deformar con un {@link Warp}: las normales se corrigen con la
 * jacobiana de la deformación, así que las curvas siguen viéndose suaves.</p>
 *
 * <p>Principles: Pure Fabrication, High Cohesion.</p>
 */
final class MeshKit {

    private MeshKit() {
        // Clase de utilidad: no se instancia.
    }

    /** Deformación de puntos (Y hacia arriba). */
    @FunctionalInterface
    interface Warp {
        double[] apply(double x, double y, double z);

        default Warp then(Warp next) {
            return (x, y, z) -> {
                double[] p = apply(x, y, z);
                return next.apply(p[0], p[1], p[2]);
            };
        }

        static Warp identity() {
            return (x, y, z) -> new double[]{x, y, z};
        }

        static Warp translate(double dx, double dy, double dz) {
            return (x, y, z) -> new double[]{x + dx, y + dy, z + dz};
        }

        static Warp scale(double sx, double sy, double sz) {
            return (x, y, z) -> new double[]{x * sx, y * sy, z * sz};
        }

        /** Giro alrededor de Y; con 90° el eje +Z pasa a apuntar a +X. */
        static Warp rotateY(double degrees) {
            double c = Math.cos(Math.toRadians(degrees));
            double s = Math.sin(Math.toRadians(degrees));
            return (x, y, z) -> new double[]{x * c + z * s, y, -x * s + z * c};
        }

        /** Giro alrededor de Z, de +X hacia +Y. */
        static Warp rotateZ(double degrees) {
            double c = Math.cos(Math.toRadians(degrees));
            double s = Math.sin(Math.toRadians(degrees));
            return (x, y, z) -> new double[]{x * c - y * s, x * s + y * c, z};
        }
    }

    /** Coordenada V de la textura según posición y normal (para reflejos falsos). */
    @FunctionalInterface
    interface Shading {
        double v(double x, double y, double z, double nx, double ny, double nz);

        /** Sin textura: siempre el centro. */
        static Shading flat() {
            return (x, y, z, nx, ny, nz) -> 0.5;
        }
    }

    /** Acumula triángulos con normales y coordenadas de textura propias. */
    static final class Builder {
        private float[] points = new float[3 * 512];
        private float[] normals = new float[3 * 512];
        private float[] uvs = new float[2 * 512];
        private int[] faces = new int[9 * 512];
        private int vertices;
        private int triangles;
        private final Shading shading;

        Builder(Shading shading) {
            this.shading = shading;
        }

        /** Agrega un vértice; la normal se normaliza aquí. */
        int vertex(double x, double y, double z, double nx, double ny, double nz) {
            double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length > 1e-12) {
                nx /= length;
                ny /= length;
                nz /= length;
            }
            return vertex(x, y, z, nx, ny, nz, 0.5, shading.v(x, y, z, nx, ny, nz));
        }

        int vertex(double x, double y, double z, double nx, double ny, double nz, double u, double v) {
            if (vertices * 3 + 3 > points.length) {
                points = Arrays.copyOf(points, points.length * 2);
                normals = Arrays.copyOf(normals, normals.length * 2);
                uvs = Arrays.copyOf(uvs, uvs.length * 2);
            }
            points[3 * vertices] = (float) x;
            points[3 * vertices + 1] = (float) -y;
            points[3 * vertices + 2] = (float) z;
            normals[3 * vertices] = (float) nx;
            normals[3 * vertices + 1] = (float) -ny;
            normals[3 * vertices + 2] = (float) nz;
            uvs[2 * vertices] = (float) u;
            uvs[2 * vertices + 1] = (float) Math.max(0.002, Math.min(0.998, v));
            return vertices++;
        }

        /**
         * Agrega un triángulo y lo orienta según las normales de sus vértices,
         * para que su cara visible quede hacia afuera.
         */
        void triangle(int a, int b, int c) {
            double[] ab = delta(points, b, a);
            double[] ac = delta(points, c, a);
            double gx = ab[1] * ac[2] - ab[2] * ac[1];
            double gy = ab[2] * ac[0] - ab[0] * ac[2];
            double gz = ab[0] * ac[1] - ab[1] * ac[0];
            double nx = normals[3 * a] + normals[3 * b] + normals[3 * c];
            double ny = normals[3 * a + 1] + normals[3 * b + 1] + normals[3 * c + 1];
            double nz = normals[3 * a + 2] + normals[3 * b + 2] + normals[3 * c + 2];
            if (gx * nx + gy * ny + gz * nz < 0) {
                int swap = b;
                b = c;
                c = swap;
            }
            if (triangles * 9 + 9 > faces.length) {
                faces = Arrays.copyOf(faces, faces.length * 2);
            }
            int f = triangles * 9;
            for (int index : new int[]{a, b, c}) {
                faces[f++] = index;
                faces[f++] = index;
                faces[f++] = index;
            }
            triangles++;
        }

        void quad(int a, int b, int c, int d) {
            triangle(a, b, c);
            triangle(a, c, d);
        }

        boolean isEmpty() {
            return triangles == 0;
        }

        MeshView view(PhongMaterial material) {
            TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
            mesh.getPoints().setAll(points, 0, vertices * 3);
            mesh.getNormals().setAll(normals, 0, vertices * 3);
            mesh.getTexCoords().setAll(uvs, 0, vertices * 2);
            mesh.getFaces().setAll(faces, 0, triangles * 9);
            MeshView view = new MeshView(mesh);
            view.setMaterial(material);
            return view;
        }

        private static double[] delta(float[] p, int to, int from) {
            return new double[]{p[3 * to] - p[3 * from], p[3 * to + 1] - p[3 * from + 1],
                    p[3 * to + 2] - p[3 * from + 2]};
        }
    }

    // ------------------------------------------------------------ superficies

    /**
     * Caja centrada en (cx, cy, cz) con medias medidas hx, hy, hz y todas sus
     * aristas redondeadas con radio r; luego se aplica la deformación.
     */
    static void roundedBox(Builder builder, double cx, double cy, double cz,
                           double hx, double hy, double hz, double r, Warp warp) {
        double[] half = {hx, hy, hz};
        double[][] axes = {axis(hx, r), axis(hy, r), axis(hz, r)};
        double[] inner = {hx - r, hy - r, hz - r};
        for (int k = 0; k < 3; k++) {
            int i = (k + 1) % 3;
            int j = (k + 2) % 3;
            for (int sign = -1; sign <= 1; sign += 2) {
                double[] li = axes[i];
                double[] lj = axes[j];
                int[][] index = new int[li.length][lj.length];
                for (int a = 0; a < li.length; a++) {
                    for (int b = 0; b < lj.length; b++) {
                        double[] p = new double[3];
                        p[k] = sign * half[k];
                        p[i] = li[a];
                        p[j] = lj[b];
                        double[] q = new double[3];
                        double[] n = new double[3];
                        for (int c = 0; c < 3; c++) {
                            q[c] = Math.max(-inner[c], Math.min(inner[c], p[c]));
                            n[c] = p[c] - q[c];
                        }
                        double length = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
                        for (int c = 0; c < 3; c++) {
                            n[c] /= length;
                        }
                        index[a][b] = warped(builder, warp,
                                cx + q[0] + r * n[0], cy + q[1] + r * n[1], cz + q[2] + r * n[2], n);
                    }
                }
                for (int a = 0; a + 1 < li.length; a++) {
                    for (int b = 0; b + 1 < lj.length; b++) {
                        builder.quad(index[a][b], index[a + 1][b], index[a + 1][b + 1], index[a][b + 1]);
                    }
                }
            }
        }
    }

    /** Valores de la rejilla de una cara: tramos curvos en los bordes y planos en medio. */
    private static double[] axis(double half, double r) {
        int arc = 4;
        int middle = 6;
        double inner = half - r;
        double edge = Math.min(0.006, inner * 0.2);
        double[] values = new double[2 * arc + middle + 4];
        int n = 0;
        for (int k = arc; k >= 1; k--) {
            values[n++] = -(inner + r * Math.tan(Math.toRadians(45.0 * k / arc)));
        }
        values[n++] = -inner;
        values[n++] = -inner + edge;
        for (int k = 1; k < middle; k++) {
            values[n++] = -inner + edge + (2 * (inner - edge)) * k / middle;
        }
        values[n++] = inner - edge;
        values[n++] = inner;
        for (int k = 1; k <= arc; k++) {
            values[n++] = inner + r * Math.tan(Math.toRadians(45.0 * k / arc));
        }
        return Arrays.copyOf(values, n);
    }

    /**
     * Pieza torneada alrededor del eje Z. El perfil son pares (radio, z)
     * recorridos de modo que, en la cara que mira a +Z, el radio crece; un
     * punto repetido marca una arista viva.
     */
    static void lathe(Builder builder, double[] profile, int segments, Warp warp) {
        int count = profile.length / 2;
        int[] previous = null;
        for (int i = 0; i < count; i++) {
            boolean creaseAfter = i + 1 < count && samePoint(profile, i, i + 1);
            boolean creaseBefore = i > 0 && samePoint(profile, i, i - 1);
            double nr = 0;
            double nz = 0;
            if (i > 0 && !creaseBefore) {
                double[] n = segmentNormal(profile, i - 1, i);
                nr += n[0];
                nz += n[1];
            }
            if (i + 1 < count && !creaseAfter) {
                double[] n = segmentNormal(profile, i, i + 1);
                nr += n[0];
                nz += n[1];
            }
            double radius = profile[2 * i];
            double z = profile[2 * i + 1];
            int[] ring = new int[segments + 1];
            for (int s = 0; s <= segments; s++) {
                double angle = 2 * Math.PI * s / segments;
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                ring[s] = warped(builder, warp, radius * cos, radius * sin, z,
                        new double[]{nr * cos, nr * sin, nz});
            }
            if (previous != null && !creaseBefore) {
                for (int s = 0; s < segments; s++) {
                    builder.quad(previous[s], ring[s], ring[s + 1], previous[s + 1]);
                }
            }
            previous = ring;
        }
    }

    private static boolean samePoint(double[] profile, int a, int b) {
        return profile[2 * a] == profile[2 * b] && profile[2 * a + 1] == profile[2 * b + 1];
    }

    private static double[] segmentNormal(double[] profile, int a, int b) {
        double dr = profile[2 * b] - profile[2 * a];
        double dz = profile[2 * b + 1] - profile[2 * a + 1];
        double length = Math.sqrt(dr * dr + dz * dz);
        return length < 1e-12 ? new double[]{0, 0} : new double[]{-dz / length, dr / length};
    }

    /**
     * Sólido convexo de caras planas: puntos (ya en su lugar) y polígonos con
     * sus índices en orden. Cada cara se orienta hacia afuera del centro.
     */
    static void convex(Builder builder, double[][] points, int[][] polygons) {
        double cx = 0;
        double cy = 0;
        double cz = 0;
        for (double[] p : points) {
            cx += p[0];
            cy += p[1];
            cz += p[2];
        }
        cx /= points.length;
        cy /= points.length;
        cz /= points.length;
        for (int[] polygon : polygons) {
            // Normal de Newell y centro de la cara.
            double nx = 0;
            double ny = 0;
            double nz = 0;
            double fx = 0;
            double fy = 0;
            double fz = 0;
            for (int i = 0; i < polygon.length; i++) {
                double[] a = points[polygon[i]];
                double[] b = points[polygon[(i + 1) % polygon.length]];
                nx += (a[1] - b[1]) * (a[2] + b[2]);
                ny += (a[2] - b[2]) * (a[0] + b[0]);
                nz += (a[0] - b[0]) * (a[1] + b[1]);
                fx += a[0];
                fy += a[1];
                fz += a[2];
            }
            fx = fx / polygon.length - cx;
            fy = fy / polygon.length - cy;
            fz = fz / polygon.length - cz;
            if (nx * fx + ny * fy + nz * fz < 0) {
                nx = -nx;
                ny = -ny;
                nz = -nz;
            }
            int[] index = new int[polygon.length];
            for (int i = 0; i < polygon.length; i++) {
                double[] p = points[polygon[i]];
                index[i] = builder.vertex(p[0], p[1], p[2], nx, ny, nz);
            }
            for (int i = 1; i + 1 < index.length; i++) {
                builder.triangle(index[0], index[i], index[i + 1]);
            }
        }
    }

    /** Hexaedro convexo: una cara de cuatro puntos y luego la opuesta, en el mismo orden. */
    static void hexa(Builder builder, double[]... points) {
        convex(builder, points, new int[][]{
                {0, 1, 2, 3}, {4, 5, 6, 7},
                {0, 1, 5, 4}, {1, 2, 6, 5}, {2, 3, 7, 6}, {3, 0, 4, 7}});
    }

    /** Caja de caras planas alineada con los ejes y luego deformada. */
    static void box(Builder builder, double x0, double x1, double y0, double y1, double z0, double z1,
                    Warp warp) {
        double[][] corners = {
                {x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1},
                {x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}};
        for (int i = 0; i < corners.length; i++) {
            corners[i] = warp.apply(corners[i][0], corners[i][1], corners[i][2]);
        }
        hexa(builder, corners);
    }

    /**
     * Lámina gruesa: un contorno convexo (puntos 3D sobre una superficie)
     * desplazado entre d0 y d1 a lo largo de la normal y luego deformado.
     */
    static void slab(Builder builder, double[][] outline, double[] normal, double d0, double d1, Warp warp) {
        int n = outline.length;
        double[][] points = new double[2 * n][];
        int[][] polygons = new int[n + 2][];
        polygons[0] = new int[n];
        polygons[1] = new int[n];
        for (int i = 0; i < n; i++) {
            double[] p = outline[i];
            points[i] = warp.apply(p[0] + normal[0] * d0, p[1] + normal[1] * d0, p[2] + normal[2] * d0);
            points[n + i] = warp.apply(p[0] + normal[0] * d1, p[1] + normal[1] * d1, p[2] + normal[2] * d1);
            polygons[0][i] = i;
            polygons[1][i] = n + i;
            int next = (i + 1) % n;
            polygons[i + 2] = new int[]{i, next, n + next, n + i};
        }
        convex(builder, points, polygons);
    }

    /** Rectángulo de esquinas redondeadas en 2D (u, v), en sentido antihorario. */
    static double[][] roundedRect(double u0, double u1, double v0, double v1, double radius) {
        int steps = 4;
        double[][] centers = {{u1 - radius, v0 + radius}, {u1 - radius, v1 - radius},
                {u0 + radius, v1 - radius}, {u0 + radius, v0 + radius}};
        double[] startAngles = {-90, 0, 90, 180};
        double[][] points = new double[4 * (steps + 1)][];
        int n = 0;
        for (int corner = 0; corner < 4; corner++) {
            for (int s = 0; s <= steps; s++) {
                double angle = Math.toRadians(startAngles[corner] + 90.0 * s / steps);
                points[n++] = new double[]{centers[corner][0] + radius * Math.cos(angle),
                        centers[corner][1] + radius * Math.sin(angle)};
            }
        }
        return points;
    }

    /** Agrega un vértice deformado, con la normal corregida por la jacobiana. */
    private static int warped(Builder builder, Warp warp, double x, double y, double z, double[] normal) {
        double[] p = warp.apply(x, y, z);
        double e = 1e-4;
        double[][] columns = new double[3][];
        for (int c = 0; c < 3; c++) {
            double[] plus = warp.apply(x + (c == 0 ? e : 0), y + (c == 1 ? e : 0), z + (c == 2 ? e : 0));
            double[] minus = warp.apply(x - (c == 0 ? e : 0), y - (c == 1 ? e : 0), z - (c == 2 ? e : 0));
            columns[c] = new double[]{(plus[0] - minus[0]) / (2 * e), (plus[1] - minus[1]) / (2 * e),
                    (plus[2] - minus[2]) / (2 * e)};
        }
        double[] c12 = cross(columns[1], columns[2]);
        double[] c20 = cross(columns[2], columns[0]);
        double[] c01 = cross(columns[0], columns[1]);
        double nx = normal[0] * c12[0] + normal[1] * c20[0] + normal[2] * c01[0];
        double ny = normal[0] * c12[1] + normal[1] * c20[1] + normal[2] * c01[1];
        double nz = normal[0] * c12[2] + normal[1] * c20[2] + normal[2] * c01[2];
        return builder.vertex(p[0], p[1], p[2], nx, ny, nz);
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    // ------------------------------------------------------------- materiales

    /**
     * Textura vertical de 4 × 256 px: pares (posición 0..1, color) que se
     * interpolan, para simular el reflejo del entorno según la coordenada V.
     */
    static Image gradient(Object... stops) {
        int height = 256;
        WritableImage image = new WritableImage(4, height);
        PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < height; y++) {
            double t = y / (height - 1.0);
            Color color = (Color) stops[1];
            for (int s = 0; s + 2 < stops.length; s += 2) {
                double t0 = ((Number) stops[s]).doubleValue();
                double t1 = ((Number) stops[s + 2]).doubleValue();
                if (t >= t0 && t <= t1) {
                    double f = t1 > t0 ? (t - t0) / (t1 - t0) : 0;
                    color = ((Color) stops[s + 1]).interpolate((Color) stops[s + 3], f);
                    break;
                }
                if (t > t1) {
                    color = (Color) stops[s + 3];
                }
            }
            for (int x = 0; x < 4; x++) {
                writer.setColor(x, y, color);
            }
        }
        return image;
    }

    static PhongMaterial material(Color color, double specular, double power) {
        PhongMaterial material = new PhongMaterial(color);
        material.setSpecularColor(Color.gray(specular));
        material.setSpecularPower(power);
        return material;
    }

    static PhongMaterial material(Image diffuse, double specular, double power) {
        PhongMaterial material = material(Color.WHITE, specular, power);
        material.setDiffuseMap(diffuse);
        return material;
    }
}
