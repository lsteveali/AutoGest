package com.autogest.controller.components;

import com.autogest.controller.components.MeshKit.Builder;
import com.autogest.controller.components.MeshKit.Shading;
import com.autogest.controller.components.MeshKit.Warp;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

/**
 * Modelo 3D de una camioneta todoterreno corta de dos puertas (estilo serie 70
 * plateada, levantada y con cauchos de barro) que usa {@link CarTurntable}.
 *
 * <p>Las medidas están en metros con el eje Y hacia arriba: X apunta al
 * frente, Z hacia el lado derecho y el piso está en y = 0. La carrocería se
 * arma con cajas de aristas redondeadas y las ruedas son piezas torneadas
 * ({@link MeshKit}). La pintura, los vidrios y el cromo usan texturas de
 * degradado que imitan el reflejo del cielo, el horizonte y el piso.</p>
 *
 * <p>Principles: Pure Fabrication, High Cohesion.</p>
 */
final class TruckModel {

    /** Ejes delantero y trasero (distancia entre ejes de 2.31 m). */
    private static final double FRONT_AXLE = 1.18;
    private static final double REAR_AXLE = -1.13;
    /** Altura del centro de las ruedas: caucho de 33" con tacos. */
    private static final double WHEEL_Y = 0.447;
    private static final double TRACK = 0.80;
    private static final double BODY_SIDE = 0.85;
    private static final double CABIN_SIDE = 0.84;

    /** Reflejo según la normal y la altura: cielo arriba, horizonte a media puerta, piso abajo. */
    private static final Shading PAINT_SHADE = (x, y, z, nx, ny, nz) -> 0.5 - 0.42 * ny + 0.12 * (1.05 - y);
    private static final Shading GLASS_SHADE = (x, y, z, nx, ny, nz) -> 0.5 - 0.45 * ny + 0.35 * (1.62 - y);
    /** El cromo repite bandas de reflejo con la altura para que cualquier pieza las muestre. */
    private static final Shading CHROME_SHADE = (x, y, z, nx, ny, nz) -> 0.5 - 0.4 * ny + 0.18 * Math.sin(y * 17);

    /**
     * La cabina: los costados se cierran hacia el techo y la parte delantera
     * se inclina hacia atrás para formar el parabrisas, sin mover el parante B.
     */
    private static final Warp CABIN = (x, y, z) -> {
        double t = Math.max(0, y - 1.30) / 0.64;
        double w = Math.max(0, Math.min(1, (x - 0.15) / 0.45));
        return new double[]{x - 0.29 * t * w, y, z * (1 - 0.07 * t)};
    };

    private final Builder paint = new Builder(PAINT_SHADE);
    private final Builder glass = new Builder(GLASS_SHADE);
    private final Builder chrome = new Builder(CHROME_SHADE);
    private final Builder rim = new Builder(CHROME_SHADE);
    private final Builder rimDark = new Builder(Shading.flat());
    private final Builder trim = new Builder(Shading.flat());
    private final Builder rubber = new Builder(Shading.flat());
    private final Builder well = new Builder(Shading.flat());
    private final Builder grille = new Builder(Shading.flat());
    private final Builder lens = new Builder(Shading.flat());
    private final Builder amber = new Builder(Shading.flat());
    private final Builder tail = new Builder(Shading.flat());
    private final Builder plate = new Builder(Shading.flat());
    private final Builder plateText = new Builder(Shading.flat());

    private TruckModel() {
    }

    /** Arma la camioneta completa con el piso en y = 0 y el frente hacia +X. */
    static Group build() {
        TruckModel model = new TruckModel();
        model.chassis();
        model.body();
        model.cabin();
        model.front();
        model.rear();
        for (double side : new double[]{-1, 1}) {
            model.side(side);
            for (double axle : new double[]{FRONT_AXLE, REAR_AXLE}) {
                Warp place = (side < 0 ? Warp.rotateY(180) : Warp.identity())
                        .then(Warp.translate(axle, WHEEL_Y, side * TRACK));
                model.wheel(place);
            }
        }
        model.wheel(Warp.rotateY(-90).then(Warp.translate(-2.14, 1.08, 0.24)));
        return model.group();
    }

    private Group group() {
        PhongMaterial paintMaterial = MeshKit.material(MeshKit.gradient(
                0.0, Color.web("#D5DADF"), 0.25, Color.web("#CFD4DA"), 0.47, Color.web("#C2C8CF"),
                0.50, Color.web("#8B929B"), 0.53, Color.web("#9EA5AD"), 0.75, Color.web("#B0B6BD"),
                1.0, Color.web("#5E646B")), 0.55, 36);
        PhongMaterial glassMaterial = MeshKit.material(MeshKit.gradient(
                0.0, Color.web("#8C99A6"), 0.35, Color.web("#3A4552"), 0.5, Color.web("#1A2029"),
                1.0, Color.web("#0B0E12")), 0.9, 90);
        PhongMaterial chromeMaterial = MeshKit.material(MeshKit.gradient(
                0.0, Color.WHITE, 0.30, Color.web("#E9EDF1"), 0.46, Color.web("#A9B0B8"),
                0.50, Color.web("#2F353C"), 0.58, Color.web("#5F666E"), 0.80, Color.web("#D3D8DD"),
                1.0, Color.web("#F2F4F6")), 1.0, 80);
        PhongMaterial rimMaterial = MeshKit.material(MeshKit.gradient(
                0.0, Color.web("#EEF1F4"), 0.45, Color.web("#C2C8CF"), 0.5, Color.web("#7E858D"),
                1.0, Color.web("#C9CED4")), 0.8, 50);

        Group truck = new Group();
        add(truck, paint, paintMaterial);
        add(truck, glass, glassMaterial);
        add(truck, chrome, chromeMaterial);
        add(truck, rim, rimMaterial);
        add(truck, rimDark, MeshKit.material(Color.web("#3B4047"), 0.3, 16));
        add(truck, trim, MeshKit.material(Color.web("#26292E"), 0.2, 12));
        add(truck, rubber, MeshKit.material(Color.web("#1B1D20"), 0.12, 6));
        add(truck, well, MeshKit.material(Color.web("#0D0F11"), 0.0, 1));
        add(truck, grille, MeshKit.material(Color.web("#121417"), 0.3, 20));
        add(truck, lens, MeshKit.material(Color.web("#E6ECF1"), 1.0, 96));
        add(truck, amber, MeshKit.material(Color.web("#E27F1C"), 0.7, 60));
        add(truck, tail, MeshKit.material(Color.web("#B42F2F"), 0.7, 60));
        add(truck, plate, MeshKit.material(Color.web("#F1F3F5"), 0.3, 16));
        add(truck, plateText, MeshKit.material(Color.web("#1F2A44"), 0.1, 8));
        return truck;
    }

    private static void add(Group group, Builder builder, PhongMaterial material) {
        if (!builder.isEmpty()) {
            group.getChildren().add(builder.view(material));
        }
    }

    // ------------------------------------------------------------------ partes

    /** Chasis, ejes con su diferencial y estribo, visibles bajo la carrocería. */
    private void chassis() {
        MeshKit.box(trim, -1.75, 1.85, 0.36, 0.64, -0.50, 0.50, Warp.identity());
        MeshKit.box(trim, -0.58, 0.64, 0.58, 0.66, -0.80, 0.80, Warp.identity());
        for (double axle : new double[]{FRONT_AXLE, REAR_AXLE}) {
            MeshKit.lathe(trim, new double[]{0, 0.7, 0.065, 0.7, 0.065, 0.7, 0.065, -0.7, 0.065, -0.7, 0, -0.7},
                    16, Warp.translate(axle, WHEEL_Y, 0));
            MeshKit.lathe(trim, sphere(0.15, 12), 20, Warp.translate(axle, WHEEL_Y, 0.12));
        }
    }

    /** Carrocería baja (capó, guardafangos y caja) con la joroba y las juntas del capó. */
    private void body() {
        MeshKit.roundedBox(paint, 0.025, 0.97, 0, 1.975, 0.33, BODY_SIDE, 0.07, Warp.identity());
        MeshKit.roundedBox(paint, 1.27, 1.29, 0, 0.62, 0.045, 0.40, 0.04, Warp.identity());
        for (double side : new double[]{-1, 1}) {
            MeshKit.box(trim, 0.66, 1.92, 1.296, 1.303, side * 0.696, side * 0.704, Warp.identity());
        }
        // Rejilla del cowl y limpiaparabrisas.
        MeshKit.box(trim, 0.585, 0.66, 1.29, 1.305, -0.78, 0.78, Warp.identity());
        MeshKit.box(trim, 0.585, 0.605, 1.305, 1.318, -0.64, -0.10, Warp.identity());
        MeshKit.box(trim, 0.585, 0.605, 1.305, 1.318, 0.06, 0.60, Warp.identity());
    }

    /** Cabina de techo duro, canales de lluvia, parabrisas y vidrios traseros con su goma. */
    private void cabin() {
        MeshKit.roundedBox(paint, -0.675, 1.58, 0, 1.275, 0.36, CABIN_SIDE, 0.08, CABIN);
        for (double side : new double[]{-1, 1}) {
            MeshKit.roundedBox(paint, -0.81, 1.90, side * 0.852, 1.11, 0.012, 0.012, 0.008, CABIN);
        }

        // Parabrisas sobre la cara delantera (x = 0.60 antes de inclinarla).
        double[] forward = {1, 0, 0};
        window(onPlaneX(0.60, MeshKit.roundedRect(-0.745, 0.745, 1.35, 1.87, 0.06)),
                onPlaneX(0.60, MeshKit.roundedRect(-0.72, 0.72, 1.37, 1.85, 0.05)), forward);

        // Vidrios de las puertas de batea (división asimétrica).
        double[] backward = {-1, 0, 0};
        window(onPlaneX(-1.95, MeshKit.roundedRect(-0.68, -0.15, 1.38, 1.86, 0.05)),
                onPlaneX(-1.95, MeshKit.roundedRect(-0.66, -0.17, 1.40, 1.84, 0.04)), backward);
        window(onPlaneX(-1.95, MeshKit.roundedRect(-0.10, 0.68, 1.38, 1.86, 0.05)),
                onPlaneX(-1.95, MeshKit.roundedRect(-0.08, 0.66, 1.40, 1.84, 0.04)), backward);
    }

    /** Vidrios laterales, costuras de puerta, manilla, espejo, pasos de rueda y guardafangos. */
    private void side(double side) {
        double[] out = {0, 0, side};
        double z = side * CABIN_SIDE;
        window(onPlaneZ(z, MeshKit.roundedRect(-0.44, 0.52, 1.36, 1.86, 0.05)),
                onPlaneZ(z, MeshKit.roundedRect(-0.42, 0.50, 1.38, 1.84, 0.04)), out);
        window(onPlaneZ(z, MeshKit.roundedRect(-1.84, -0.54, 1.36, 1.86, 0.05)),
                onPlaneZ(z, MeshKit.roundedRect(-1.82, -0.56, 1.38, 1.84, 0.04)), out);
        MeshKit.slab(trim, onPlaneZ(z, MeshKit.roundedRect(-1.03, -1.00, 1.38, 1.84, 0.005)),
                out, 0.006, 0.012, CABIN);

        // Costuras de la puerta en la carrocería baja y en la cabina.
        double bodyZ = side * BODY_SIDE;
        seam(onPlaneZ(bodyZ, rect(0.565, 0.575, 0.70, 1.24)), out, Warp.identity());
        seam(onPlaneZ(bodyZ, rect(-0.505, -0.495, 0.70, 1.24)), out, Warp.identity());
        seam(onPlaneZ(bodyZ, rect(-0.50, 0.57, 0.70, 0.71)), out, Warp.identity());
        seam(onPlaneZ(z, rect(-0.505, -0.495, 1.31, 1.89)), out, CABIN);
        seam(onPlaneZ(z, rect(0.565, 0.575, 1.31, 1.86)), out, CABIN);

        // Manilla, emblema del guardafango y luz lateral.
        MeshKit.roundedBox(trim, -0.36, 1.19, side * 0.862, 0.065, 0.018, 0.012, 0.008, Warp.identity());
        MeshKit.roundedBox(chrome, 1.40, 1.16, side * 0.852, 0.15, 0.008, 0.004, 0.003, Warp.identity());
        MeshKit.roundedBox(amber, 1.90, 1.09, side * 0.852, 0.06, 0.04, 0.006, 0.004, Warp.identity());

        // Espejo cromado con su luna hacia atrás.
        MeshKit.roundedBox(chrome, 0.47, 1.41, side * 0.90, 0.018, 0.014, 0.07, 0.008, Warp.identity());
        MeshKit.roundedBox(chrome, 0.44, 1.48, side * 0.995, 0.035, 0.085, 0.065, 0.025, Warp.identity());
        MeshKit.box(glass, 0.400, 0.405, 1.405, 1.555, side * 0.945, side * 1.045, Warp.identity());

        // Pasos de rueda oscuros y guardafangos trapezoidales de la serie 70.
        for (double axle : new double[]{FRONT_AXLE, REAR_AXLE}) {
            MeshKit.slab(well, onPlaneZ(side * 0.70, wheelWell(axle)), out, 0, 0.158, Warp.identity());
            flare(axle, side);
        }
    }

    /** Frente: parrilla cromada de barras, faros redondos, cruces, faldón y parachoques. */
    private void front() {
        MeshKit.box(grille, 1.985, 2.006, 0.94, 1.22, -0.76, 0.76, Warp.identity());
        MeshKit.roundedBox(chrome, 2.008, 1.225, 0, 0.012, 0.012, 0.775, 0.008, Warp.identity());
        MeshKit.roundedBox(chrome, 2.008, 0.935, 0, 0.012, 0.012, 0.775, 0.008, Warp.identity());
        for (double y = 0.975; y < 1.2; y += 0.05) {
            MeshKit.roundedBox(chrome, 2.010, y, 0, 0.012, 0.011, 0.44, 0.008, Warp.identity());
        }
        MeshKit.lathe(chrome, new double[]{0, 0.012, 0.035, 0.011, 0.05, 0.004, 0.052, -0.004}, 32,
                Warp.scale(1, 0.7, 1).then(Warp.rotateY(90)).then(Warp.translate(2.026, 1.075, 0)));

        for (double side : new double[]{-1, 1}) {
            MeshKit.roundedBox(chrome, 2.008, 1.08, side * 0.77, 0.012, 0.157, 0.012, 0.008, Warp.identity());
            MeshKit.roundedBox(chrome, 2.008, 1.08, side * 0.455, 0.010, 0.145, 0.010, 0.007, Warp.identity());
            Warp light = Warp.rotateY(90).then(Warp.translate(2.006, 1.08, side * 0.615));
            MeshKit.lathe(chrome, new double[]{0.098, 0.012, 0.108, 0.024, 0.122, 0.026, 0.132, 0.016,
                    0.134, -0.01}, 48, light);
            // Faro sellado: reflector cromado cóncavo y bombillo al centro.
            MeshKit.lathe(chrome, new double[]{0, 0.004, 0.03, 0.006, 0.07, 0.014, 0.1, 0.024}, 48, light);
            MeshKit.lathe(lens, new double[]{0, 0.03, 0.015, 0.029, 0.022, 0.02, 0.024, 0.008}, 24, light);
            MeshKit.roundedBox(amber, 1.995, 1.085, side * 0.815, 0.015, 0.075, 0.03, 0.01, Warp.identity());
        }
        for (double z = -0.5; z <= 0.51; z += 0.2) {
            MeshKit.box(grille, 1.99, 2.004, 0.80, 0.84, z - 0.07, z + 0.07, Warp.identity());
        }

        // Parachoques cromado y placa.
        MeshKit.roundedBox(chrome, 2.09, 0.595, 0, 0.08, 0.095, 0.92, 0.045, Warp.identity());
        MeshKit.box(plate, 2.168, 2.176, 0.54, 0.65, -0.16, 0.16, Warp.identity());
        for (int i = 0; i < 6; i++) {
            double z = -0.12 + i * 0.048;
            MeshKit.box(plateText, 2.176, 2.178, 0.565, 0.615, z - 0.016, z + 0.016, Warp.identity());
        }
    }

    /** Parte trasera: costura de las puertas, stops, manilla, parachoques y soporte del repuesto. */
    private void rear() {
        MeshKit.box(trim, -1.956, -1.949, 0.70, 1.90, -0.128, -0.120, Warp.identity());
        MeshKit.roundedBox(chrome, -1.962, 1.135, -0.26, 0.008, 0.014, 0.06, 0.006, Warp.identity());
        for (double side : new double[]{-1, 1}) {
            MeshKit.roundedBox(tail, -1.958, 0.92, side * 0.775, 0.012, 0.12, 0.045, 0.01, Warp.identity());
            MeshKit.roundedBox(amber, -1.958, 1.085, side * 0.775, 0.012, 0.045, 0.045, 0.01, Warp.identity());
        }
        MeshKit.roundedBox(trim, -2.02, 0.58, 0, 0.08, 0.085, 0.90, 0.03, Warp.identity());
        MeshKit.box(trim, -2.0, -1.95, 0.85, 1.30, 0.19, 0.29, Warp.identity());
    }

    /**
     * Rueda con el eje en Z y el rin hacia +Z: caucho de barro con tacos
     * alternados que cubren el hombro y rin de seis rayos.
     */
    private void wheel(Warp place) {
        MeshKit.lathe(rubber, new double[]{
                0.215, 0.118, 0.26, 0.140, 0.31, 0.155, 0.36, 0.160, 0.39, 0.155, 0.408, 0.143,
                0.418, 0.125, 0.423, 0.10, 0.425, 0.0, 0.423, -0.10, 0.418, -0.125, 0.408, -0.143,
                0.39, -0.155, 0.36, -0.160, 0.31, -0.155, 0.26, -0.140, 0.215, -0.118}, 64, place);

        int lugs = 26;
        double pitch = 360.0 / lugs;
        for (int i = 0; i < lugs; i++) {
            for (int row = -1; row <= 1; row += 2) {
                double start = i * pitch + (row > 0 ? 0 : pitch / 2);
                double end = start + pitch * 0.55;
                double z0 = row * 0.008;
                double z1 = row * 0.118;
                MeshKit.hexa(rubber,
                        polar(0.423, start, z0, place), polar(0.423, end, z0, place),
                        polar(0.423, end, z1, place), polar(0.423, start, z1, place),
                        polar(0.447, start, z0, place), polar(0.447, end, z0, place),
                        polar(0.447, end, z1, place), polar(0.447, start, z1, place));
                // Taco del hombro, que baja por el costado del caucho.
                MeshKit.hexa(rubber,
                        polar(0.428, start, row * 0.095, place), polar(0.447, start, row * 0.100, place),
                        polar(0.420, start, row * 0.158, place), polar(0.402, start, row * 0.150, place),
                        polar(0.428, end, row * 0.095, place), polar(0.447, end, row * 0.100, place),
                        polar(0.420, end, row * 0.158, place), polar(0.402, end, row * 0.150, place));
            }
        }

        // Rin: fondo oscuro, ceja, rayos, cubo, tuercas y tapa.
        MeshKit.lathe(rimDark, new double[]{0, 0.055, 0.198, 0.055, 0.198, 0.055, 0.203, 0.128}, 48, place);
        MeshKit.lathe(rim, new double[]{0.196, 0.128, 0.212, 0.142, 0.228, 0.143, 0.237, 0.135,
                0.239, 0.115}, 48, place);
        for (int i = 0; i < 6; i++) {
            Warp spoke = Warp.rotateZ(i * 60).then(place);
            MeshKit.hexa(rim,
                    spoke.apply(-0.032, 0.06, 0.075), spoke.apply(0.032, 0.06, 0.075),
                    spoke.apply(0.022, 0.20, 0.075), spoke.apply(-0.022, 0.20, 0.075),
                    spoke.apply(-0.028, 0.06, 0.122), spoke.apply(0.028, 0.06, 0.122),
                    spoke.apply(0.019, 0.20, 0.130), spoke.apply(-0.019, 0.20, 0.130));
            double angle = Math.toRadians(i * 60 + 30);
            MeshKit.lathe(chrome, new double[]{0, 0.127, 0.009, 0.127, 0.010, 0.118}, 12,
                    Warp.translate(0.05 * Math.cos(angle), 0.05 * Math.sin(angle), 0).then(place));
        }
        MeshKit.lathe(rim, new double[]{0, 0.118, 0.045, 0.118, 0.066, 0.108, 0.072, 0.085, 0.072, 0.06},
                32, place);
        MeshKit.lathe(chrome, new double[]{0, 0.132, 0.022, 0.131, 0.032, 0.124, 0.035, 0.116}, 32, place);
    }

    /** Guardafango: cuatro tramos redondeados alrededor del paso de rueda. */
    private void flare(double axle, double side) {
        double[] angles = {8, 49, 90, 131, 172};
        double radius = 0.575;
        for (int i = 0; i + 1 < angles.length; i++) {
            double ax = axle + radius * Math.cos(Math.toRadians(angles[i]));
            double ay = WHEEL_Y + radius * Math.sin(Math.toRadians(angles[i]));
            double bx = axle + radius * Math.cos(Math.toRadians(angles[i + 1]));
            double by = WHEEL_Y + radius * Math.sin(Math.toRadians(angles[i + 1]));
            double length = Math.hypot(bx - ax, by - ay);
            double tilt = Math.toDegrees(Math.atan2(by - ay, bx - ax));
            MeshKit.roundedBox(paint, 0, 0, 0, length / 2 + 0.03, 0.042, 0.07, 0.025,
                    Warp.rotateZ(tilt).then(Warp.translate((ax + bx) / 2, (ay + by) / 2, side * 0.885)));
        }
    }

    // --------------------------------------------------------------- geometría

    /** Goma negra y vidrio encima, ambos siguiendo la forma de la cabina. */
    private void window(double[][] seal, double[][] pane, double[] normal) {
        MeshKit.slab(trim, seal, normal, 0.0, 0.004, CABIN);
        MeshKit.slab(glass, pane, normal, 0.002, 0.008, CABIN);
    }

    /** Línea oscura de junta sobre una superficie. */
    private void seam(double[][] outline, double[] normal, Warp warp) {
        MeshKit.slab(trim, outline, normal, 0.0, 0.003, warp);
    }

    /** Paso de rueda: la parte del contorno del guardafango sobre el borde inferior de la carrocería. */
    private static double[][] wheelWell(double axle) {
        double[] angles = {8, 49, 90, 131, 172};
        double[] xs = new double[angles.length];
        double[] ys = new double[angles.length];
        for (int i = 0; i < angles.length; i++) {
            xs[i] = axle + 0.52 * Math.cos(Math.toRadians(angles[i]));
            ys[i] = WHEEL_Y + 0.52 * Math.sin(Math.toRadians(angles[i]));
        }
        double bottom = 0.64;
        double t0 = (bottom - ys[0]) / (ys[1] - ys[0]);
        double t1 = (bottom - ys[4]) / (ys[3] - ys[4]);
        return new double[][]{
                {xs[0] + (xs[1] - xs[0]) * t0, bottom},
                {xs[1], ys[1]}, {xs[2], ys[2]}, {xs[3], ys[3]},
                {xs[4] + (xs[3] - xs[4]) * t1, bottom}};
    }

    private static double[][] rect(double u0, double u1, double v0, double v1) {
        return new double[][]{{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};
    }

    /** Lleva un contorno 2D (x, y) al plano z dado. */
    private static double[][] onPlaneZ(double z, double[][] outline) {
        double[][] points = new double[outline.length][];
        for (int i = 0; i < outline.length; i++) {
            points[i] = new double[]{outline[i][0], outline[i][1], z};
        }
        return points;
    }

    /** Lleva un contorno 2D (z, y) al plano x dado. */
    private static double[][] onPlaneX(double x, double[][] outline) {
        double[][] points = new double[outline.length][];
        for (int i = 0; i < outline.length; i++) {
            points[i] = new double[]{x, outline[i][1], outline[i][0]};
        }
        return points;
    }

    /** Punto en coordenadas de la rueda (radio, ángulo en grados, z) ya colocado. */
    private static double[] polar(double radius, double degrees, double z, Warp place) {
        double angle = Math.toRadians(degrees);
        return place.apply(radius * Math.cos(angle), radius * Math.sin(angle), z);
    }

    /** Perfil (radio, z) de una esfera, del polo +Z al polo -Z. */
    private static double[] sphere(double radius, int steps) {
        double[] profile = new double[2 * (steps + 1)];
        for (int i = 0; i <= steps; i++) {
            double angle = Math.PI * i / steps;
            profile[2 * i] = radius * Math.sin(angle);
            profile[2 * i + 1] = radius * Math.cos(angle);
        }
        return profile;
    }
}
