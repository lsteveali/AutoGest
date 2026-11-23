package com.autogest.controller.components;

import com.autogest.controller.components.MeshKit.Builder;
import com.autogest.controller.components.MeshKit.Shading;
import com.autogest.controller.components.MeshKit.Warp;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;

/**
 * Modelos 3D de automóviles para la vitrina del login: un sedán de cuatro
 * puertas y un deportivo de dos puertas con alerón. Los dos salen de la misma
 * receta ({@link Spec}): una carrocería baja de aristas redondeadas que baja
 * hacia el frente, una cabina cuyo parabrisas y vidrio trasero se inclinan con
 * una deformación, pasos de rueda oscuros y rines de rayos.
 *
 * <p>Las medidas están en metros con el eje Y hacia arriba: X apunta al
 * frente, Z hacia el lado derecho y el piso está en y = 0, igual que en
 * {@link TruckModel}.</p>
 *
 * <p>Principles: Pure Fabrication, High Cohesion.</p>
 */
final class CarModel {

    /**
     * Proporciones de un automóvil.
     *
     * @param frontAxle     eje delantero en X
     * @param rearAxle      eje trasero en X
     * @param wheelRadius   radio del caucho (también la altura del eje)
     * @param tireHalfWidth medio ancho del caucho
     * @param track         distancia del centro a cada caucho en Z
     * @param noseX         punta delantera
     * @param tailX         punta trasera
     * @param bottom        altura del borde inferior de la carrocería
     * @param beltY         altura de la línea de cintura (base de los vidrios)
     * @param halfWidth     medio ancho de la carrocería
     * @param noseDrop      cuánto baja el capó hacia la punta (0 a 1)
     * @param tailDrop      cuánto baja la cola hacia atrás (0 a 1)
     * @param cabinFront    base del parabrisas en X
     * @param cabinRear     base del vidrio trasero en X
     * @param roofY         altura del techo
     * @param rakeFront     inclinación del parabrisas (0 a 1)
     * @param rakeRear      inclinación del vidrio trasero (0 a 1)
     * @param roofNarrow    cuánto se angosta el techo (0 a 1)
     * @param spokes        rayos del rin
     * @param darkRims      rines oscuros en vez de plateados
     * @param fourDoors     cuatro puertas (si no, dos)
     * @param sporty        alerón, tomas laterales y frenos rojos
     */
    record Spec(double frontAxle, double rearAxle, double wheelRadius, double tireHalfWidth, double track,
                double noseX, double tailX, double bottom, double beltY, double halfWidth,
                double noseDrop, double tailDrop,
                double cabinFront, double cabinRear, double roofY,
                double rakeFront, double rakeRear, double roofNarrow,
                int spokes, boolean darkRims, boolean fourDoors, boolean sporty) {
    }

    /** Sedán familiar de cuatro puertas. */
    static final Spec SEDAN = new Spec(1.36, -1.34, 0.33, 0.11, 0.80,
            2.30, -2.30, 0.24, 0.84, 0.88,
            0.30, 0.10,
            1.05, -1.35, 1.42, 0.55, 0.42, 0.14,
            5, false, true, false);

    /** Deportivo bajo de dos puertas, con alerón. */
    static final Spec SPORTS = new Spec(1.40, -1.30, 0.33, 0.13, 0.84,
            2.25, -2.20, 0.20, 0.80, 0.95,
            0.40, 0.06,
            0.78, -1.20, 1.22, 0.70, 0.62, 0.18,
            10, true, false, true);

    private final Spec spec;
    private final Warp body;
    private final Warp cabin;
    private final double cabinCenter;

    private final Builder paint;
    private final Builder glass;
    private final Builder chrome = new Builder(TruckLikeShading.CHROME);
    private final Builder rim = new Builder(TruckLikeShading.CHROME);
    private final Builder rimDark = new Builder(Shading.flat());
    private final Builder disc = new Builder(Shading.flat());
    private final Builder trim = new Builder(Shading.flat());
    private final Builder rubber = new Builder(Shading.flat());
    private final Builder well = new Builder(Shading.flat());
    private final Builder grille = new Builder(Shading.flat());
    private final Builder lens = new Builder(Shading.flat());
    private final Builder tail = new Builder(Shading.flat());
    private final Builder plate = new Builder(Shading.flat());

    private CarModel(Spec spec) {
        this.spec = spec;
        double horizon = spec.beltY() + 0.2;
        double windowMiddle = (spec.beltY() + spec.roofY()) / 2;
        paint = new Builder((x, y, z, nx, ny, nz) -> 0.5 - 0.42 * ny + 0.12 * (horizon - y));
        glass = new Builder((x, y, z, nx, ny, nz) -> 0.5 - 0.45 * ny + 0.35 * (windowMiddle - y));
        cabinCenter = (spec.cabinFront() + spec.cabinRear()) / 2;
        body = bodyWarp(spec);
        cabin = cabinWarp(spec, cabinCenter);
    }

    /**
     * Arma el automóvil con el piso en y = 0 y el frente hacia +X.
     *
     * @param spec  proporciones ({@link #SEDAN} o {@link #SPORTS})
     * @param color color de la pintura
     * @return el automóvil, su malla de pintura y su huella
     */
    static VehicleBuild build(Spec spec, Color color) {
        CarModel model = new CarModel(spec);
        model.lowerBody();
        model.cabin();
        model.front();
        model.rear();
        for (double side : new double[]{-1, 1}) {
            model.side(side);
            for (double axle : new double[]{spec.frontAxle(), spec.rearAxle()}) {
                Warp place = (side < 0 ? Warp.rotateY(180) : Warp.identity())
                        .then(Warp.translate(axle, spec.wheelRadius(), side * spec.track()));
                model.wheel(place);
            }
        }
        if (spec.sporty()) {
            model.spoiler();
        }
        return model.group(color);
    }

    /** Huella para la sombra de contacto de la plataforma. */
    static VehicleBuild.Footprint footprint(Spec spec) {
        return new VehicleBuild.Footprint((spec.noseX() + spec.tailX()) / 2,
                (spec.noseX() - spec.tailX()) / 2 - 0.06, spec.halfWidth() + 0.04, spec.track(),
                spec.frontAxle(), spec.rearAxle(), spec.wheelRadius() * 0.65, spec.tireHalfWidth() * 1.6);
    }

    // ------------------------------------------------------------------ partes

    /** Carrocería baja con el capó que cae hacia la punta, más el piso oscuro. */
    private void lowerBody() {
        Spec s = spec;
        MeshKit.roundedBox(paint, (s.noseX() + s.tailX()) / 2, (s.bottom() + s.beltY()) / 2, 0,
                (s.noseX() - s.tailX()) / 2, (s.beltY() - s.bottom()) / 2, s.halfWidth(), 0.10, body);
        MeshKit.box(trim, s.tailX() + 0.35, s.noseX() - 0.35, 0.13, s.bottom() + 0.06,
                -(s.halfWidth() - 0.12), s.halfWidth() - 0.12, Warp.identity());

        // Juntas del capó sobre la cara de arriba.
        double[] up = {0, 1, 0};
        for (double side : new double[]{-1, 1}) {
            seam(onPlaneY(s.beltY(), rect(s.cabinFront() + 0.08, s.noseX() - 0.32,
                    side * 0.56, side * 0.566)), up, body);
        }
        seam(onPlaneY(s.beltY(), rect(s.noseX() - 0.33, s.noseX() - 0.32, -0.56, 0.56)), up, body);
    }

    /** Cabina con parabrisas, vidrio trasero y moldura cromada en la cintura. */
    private void cabin() {
        Spec s = spec;
        double base = s.beltY() - 0.06;
        double halfZ = s.halfWidth() - 0.05;
        MeshKit.roundedBox(paint, cabinCenter, (base + s.roofY()) / 2, 0,
                (s.cabinFront() - s.cabinRear()) / 2, (s.roofY() - base) / 2, halfZ, 0.10, cabin);

        double low = s.beltY() + 0.06;
        double high = s.roofY() - 0.12;
        double glassZ = halfZ - 0.16;
        window(onPlaneX(s.cabinFront(), MeshKit.roundedRect(-glassZ - 0.02, glassZ + 0.02, low - 0.02, high + 0.02, 0.06)),
                onPlaneX(s.cabinFront(), MeshKit.roundedRect(-glassZ, glassZ, low, high, 0.05)), new double[]{1, 0, 0});
        window(onPlaneX(s.cabinRear(), MeshKit.roundedRect(-glassZ - 0.02, glassZ + 0.02, low - 0.02, high + 0.02, 0.06)),
                onPlaneX(s.cabinRear(), MeshKit.roundedRect(-glassZ, glassZ, low, high, 0.05)), new double[]{-1, 0, 0});

        for (double side : new double[]{-1, 1}) {
            MeshKit.roundedBox(chrome, cabinCenter, s.beltY() - 0.005, side * halfZ,
                    (s.cabinFront() - s.cabinRear()) / 2 - 0.08, 0.008, 0.012, 0.005, Warp.identity());
        }
    }

    /** Vidrios laterales, costuras, manillas, espejos y pasos de rueda de un costado. */
    private void side(double side) {
        Spec s = spec;
        double[] out = {0, 0, side};
        double halfZ = s.halfWidth() - 0.05;
        double low = s.beltY() + 0.06;
        double high = s.roofY() - 0.12;
        double front = s.cabinFront() - 0.14;
        double rearEdge = s.cabinRear() + 0.14;
        double pillar = s.fourDoors() ? cabinCenter - 0.05 : cabinCenter - 0.30;
        sideWindow(side, halfZ, pillar + 0.05, front, low, high);
        sideWindow(side, halfZ, rearEdge, pillar - 0.05, low, high);

        // Costuras de las puertas y manillas.
        double seamLow = s.bottom() + 0.16;
        double seamHigh = s.beltY() - 0.12;
        double frontSeam = s.cabinFront() - 0.02;
        double[] seams = s.fourDoors()
                ? new double[]{frontSeam, cabinCenter - 0.05, s.cabinRear() + 0.18}
                : new double[]{frontSeam, s.cabinRear() + 0.30};
        double z = side * s.halfWidth();
        for (int i = 0; i < seams.length; i++) {
            seam(onPlaneZ(z, rect(seams[i] - 0.005, seams[i] + 0.005, seamLow, seamHigh)), out, Warp.identity());
            if (i + 1 < seams.length) {
                MeshKit.roundedBox(trim, seams[i + 1] + 0.18, s.beltY() - 0.16, side * (s.halfWidth() + 0.008),
                        0.07, 0.014, 0.01, 0.006, Warp.identity());
            }
        }

        // Espejo con su brazo.
        double mirrorX = s.cabinFront() - 0.12;
        MeshKit.box(trim, mirrorX - 0.04, mirrorX + 0.04, s.beltY() + 0.02, s.beltY() + 0.08,
                side * (halfZ - 0.02), side * (s.halfWidth() + 0.02), Warp.identity());
        MeshKit.roundedBox(paint, mirrorX, s.beltY() + 0.13, side * (s.halfWidth() + 0.07),
                0.07, 0.05, 0.08, 0.03, Warp.identity());

        if (s.sporty()) {
            MeshKit.slab(well, onPlaneZ(side * (s.halfWidth() - 0.02),
                    rect(s.cabinRear() + 0.02, s.cabinRear() + 0.26, s.bottom() + 0.22, s.beltY() - 0.16)),
                    out, 0, 0.024, Warp.identity());
        }

        for (double axle : new double[]{s.frontAxle(), s.rearAxle()}) {
            MeshKit.slab(well, onPlaneZ(side * (s.halfWidth() - 0.15), wheelArch(axle)), out, 0, 0.154,
                    Warp.identity());
        }
    }

    /** Frente: toma inferior, placa, parrilla y faros. */
    private void front() {
        Spec s = spec;
        double height = (s.beltY() - s.bottom()) * (1 - s.noseDrop());
        double x = s.noseX();
        MeshKit.box(grille, x - 0.02, x + 0.006, at(height, 0.10), at(height, 0.24),
                -(s.halfWidth() - 0.32), s.halfWidth() - 0.32, Warp.identity());
        MeshKit.box(plate, x - 0.02, x + 0.012, at(height, 0.27), at(height, 0.45), -0.17, 0.17, Warp.identity());
        MeshKit.box(grille, x - 0.02, x + 0.006, at(height, 0.48), at(height, 0.64), -0.42, 0.42, Warp.identity());
        for (double side : new double[]{-1, 1}) {
            MeshKit.roundedBox(trim, x - 0.03, at(height, 0.72), side * (s.halfWidth() - 0.30),
                    0.04, height * 0.10, 0.17, 0.02, Warp.identity());
            MeshKit.roundedBox(lens, x - 0.02, at(height, 0.72), side * (s.halfWidth() - 0.30),
                    0.04, height * 0.07, 0.15, 0.02, Warp.identity());
        }
    }

    /** Parte trasera: difusor, placa y stops. */
    private void rear() {
        Spec s = spec;
        double height = (s.beltY() - s.bottom()) * (1 - s.tailDrop());
        double x = s.tailX();
        MeshKit.box(trim, x - 0.006, x + 0.03, at(height, 0.04), at(height, 0.20),
                -(s.halfWidth() - 0.25), s.halfWidth() - 0.25, Warp.identity());
        MeshKit.box(plate, x - 0.012, x + 0.02, at(height, 0.36), at(height, 0.56), -0.17, 0.17, Warp.identity());
        for (double side : new double[]{-1, 1}) {
            MeshKit.roundedBox(tail, x + 0.02, at(height, 0.78), side * (s.halfWidth() - 0.32),
                    0.04, height * 0.08, 0.24, 0.02, Warp.identity());
        }
    }

    /** Alerón del deportivo sobre dos soportes. */
    private void spoiler() {
        Spec s = spec;
        double x = s.tailX() + 0.20;
        double top = s.beltY() + 0.14;
        for (double side : new double[]{-1, 1}) {
            MeshKit.box(trim, x - 0.04, x + 0.04, s.beltY() - 0.06, top, side * 0.52, side * 0.58, Warp.identity());
        }
        MeshKit.roundedBox(paint, x, top + 0.02, 0, 0.17, 0.022, s.halfWidth() - 0.08, 0.02, Warp.identity());
    }

    /**
     * Rueda con el eje en Z y el rin hacia +Z: caucho de calle, disco de
     * freno, rin de rayos, cubo y tuercas.
     */
    private void wheel(Warp place) {
        double r = spec.wheelRadius();
        double w = spec.tireHalfWidth();
        double inner = r * 0.66;
        MeshKit.lathe(rubber, new double[]{
                inner, w * 0.78, r * 0.80, w * 0.95, r * 0.90, w, r * 0.965, w * 0.93, r * 0.99, w * 0.72,
                r, w * 0.35, r, -w * 0.35, r * 0.99, -w * 0.72, r * 0.965, -w * 0.93, r * 0.90, -w,
                r * 0.80, -w * 0.95, inner, -w * 0.78}, 64, place);

        MeshKit.lathe(rimDark, new double[]{0, w * 0.30, inner - 0.004, w * 0.30, inner - 0.004, w * 0.30,
                inner - 0.002, w * 0.78}, 48, place);
        MeshKit.lathe(disc, new double[]{0, w * 0.40, inner * 0.80, w * 0.40}, 40, place);
        if (spec.sporty()) {
            double angle = Math.toRadians(160);
            MeshKit.roundedBox(tail, inner * 0.70 * Math.cos(angle), inner * 0.70 * Math.sin(angle), w * 0.46,
                    0.05, 0.035, 0.03, 0.012, place);
        }
        MeshKit.lathe(rim, new double[]{inner - 0.008, w * 0.76, inner + 0.004, w * 0.88, inner + 0.014, w * 0.90,
                inner + 0.02, w * 0.84, inner + 0.022, w * 0.70}, 48, place);

        int count = spec.spokes();
        double width = count > 6 ? 0.012 : 0.026;
        for (int i = 0; i < count; i++) {
            Warp spoke = Warp.rotateZ(i * 360.0 / count).then(place);
            MeshKit.hexa(rim,
                    spoke.apply(-width * 1.3, 0.055, w * 0.48), spoke.apply(width * 1.3, 0.055, w * 0.48),
                    spoke.apply(width, inner - 0.012, w * 0.56), spoke.apply(-width, inner - 0.012, w * 0.56),
                    spoke.apply(-width * 1.2, 0.055, w * 0.86), spoke.apply(width * 1.2, 0.055, w * 0.86),
                    spoke.apply(width * 0.9, inner - 0.012, w * 0.80), spoke.apply(-width * 0.9, inner - 0.012, w * 0.80));
        }
        MeshKit.lathe(rim, new double[]{0, w * 0.88, 0.05, w * 0.88, 0.064, w * 0.80, 0.066, w * 0.55}, 32, place);
        MeshKit.lathe(chrome, new double[]{0, w * 0.93, 0.022, w * 0.92, 0.028, w * 0.86}, 24, place);
        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(i * 72 + 36);
            MeshKit.lathe(chrome, new double[]{0, w * 0.91, 0.008, w * 0.91, 0.009, w * 0.85}, 10,
                    Warp.translate(0.04 * Math.cos(angle), 0.04 * Math.sin(angle), 0).then(place));
        }
    }

    // --------------------------------------------------------------- geometría

    /** Altura absoluta a una fracción de la altura de la punta. */
    private double at(double height, double fraction) {
        return spec.bottom() + height * fraction;
    }

    private void sideWindow(double side, double halfZ, double x0, double x1, double low, double high) {
        double z = side * halfZ;
        window(onPlaneZ(z, MeshKit.roundedRect(x0 - 0.02, x1 + 0.02, low - 0.02, high + 0.02, 0.06)),
                onPlaneZ(z, MeshKit.roundedRect(x0, x1, low, high, 0.05)), new double[]{0, 0, side});
    }

    /** Goma negra y vidrio encima, ambos siguiendo la forma de la cabina. */
    private void window(double[][] seal, double[][] pane, double[] normal) {
        MeshKit.slab(trim, seal, normal, 0.0, 0.004, cabin);
        MeshKit.slab(glass, pane, normal, 0.002, 0.008, cabin);
    }

    /** Línea oscura de junta sobre una superficie. */
    private void seam(double[][] outline, double[] normal, Warp warp) {
        MeshKit.slab(trim, outline, normal, 0.0, 0.003, warp);
    }

    /** Paso de rueda: arco alrededor del caucho cerrado por el borde inferior de la carrocería. */
    private double[][] wheelArch(double axle) {
        double radius = spec.wheelRadius() + 0.04;
        double start = -Math.toDegrees(Math.asin(Math.min(1, (spec.wheelRadius() - spec.bottom()) / radius)));
        int steps = 8;
        double[][] points = new double[steps + 1][];
        for (int i = 0; i <= steps; i++) {
            double angle = Math.toRadians(start + (180 - 2 * start) * i / steps);
            points[i] = new double[]{axle + radius * Math.cos(angle), spec.wheelRadius() + radius * Math.sin(angle)};
        }
        return points;
    }

    /**
     * Baja la cara de arriba hacia la punta y la cola, y angosta un poco los
     * extremos, para que la carrocería no parezca una caja.
     */
    private static Warp bodyWarp(Spec s) {
        double frontStart = s.frontAxle() - 0.3;
        double rearStart = s.rearAxle() + 0.3;
        return (x, y, z) -> {
            double f = clamp((x - frontStart) / (s.noseX() - frontStart));
            double r = clamp((rearStart - x) / (rearStart - s.tailX()));
            double drop = s.noseDrop() * f * f + s.tailDrop() * r * r;
            return new double[]{x, s.bottom() + (y - s.bottom()) * (1 - drop), z * (1 - 0.03 * (f * f + r * r))};
        };
    }

    /** Inclina el parabrisas y el vidrio trasero y angosta el techo según la altura. */
    private static Warp cabinWarp(Spec s, double center) {
        return (x, y, z) -> {
            double t = clamp((y - s.beltY()) / (s.roofY() - s.beltY()));
            double rake = x > center ? s.rakeFront() : s.rakeRear();
            return new double[]{center + (x - center) * (1 - rake * t), y, z * (1 - s.roofNarrow() * t)};
        };
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
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

    /** Lleva un contorno 2D (x, z) al plano y dado. */
    private static double[][] onPlaneY(double y, double[][] outline) {
        double[][] points = new double[outline.length][];
        for (int i = 0; i < outline.length; i++) {
            points[i] = new double[]{outline[i][0], y, outline[i][1]};
        }
        return points;
    }

    // ------------------------------------------------------------- materiales

    private VehicleBuild group(Color color) {
        Group car = new Group();
        MeshView paintView = add(car, paint, VehiclePaint.material(color));
        add(car, glass, VehicleMaterials.glass());
        add(car, chrome, VehicleMaterials.chrome());
        add(car, rim, spec.darkRims() ? VehicleMaterials.darkRim() : VehicleMaterials.rim());
        add(car, rimDark, VehicleMaterials.rimBack());
        add(car, disc, VehicleMaterials.disc());
        add(car, trim, VehicleMaterials.trim());
        add(car, rubber, VehicleMaterials.rubber());
        add(car, well, VehicleMaterials.well());
        add(car, grille, VehicleMaterials.grille());
        add(car, lens, VehicleMaterials.lens());
        add(car, tail, VehicleMaterials.tail());
        add(car, plate, VehicleMaterials.plate());
        return new VehicleBuild(car, paintView, footprint(spec));
    }

    static MeshView add(Group group, Builder builder, PhongMaterial material) {
        if (builder.isEmpty()) {
            return null;
        }
        MeshView view = builder.view(material);
        group.getChildren().add(view);
        return view;
    }

    /** Reflejo del cromo, el mismo de la camioneta. */
    static final class TruckLikeShading {
        static final Shading CHROME = (x, y, z, nx, ny, nz) -> 0.5 - 0.4 * ny + 0.18 * Math.sin(y * 17);

        private TruckLikeShading() {
        }
    }
}
