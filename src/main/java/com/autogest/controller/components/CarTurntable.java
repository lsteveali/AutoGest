package com.autogest.controller.components;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.DirectionalLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

/**
 * Vitrina 3D de la pantalla de inicio de sesión: una camioneta todoterreno
 * ({@link TruckModel}) sobre una plataforma giratoria, ambas girando juntas de
 * izquierda a derecha. Todo se construye con figuras de JavaFX 3D, así que no
 * depende de imágenes ni de modelos externos.
 *
 * <p>Las medidas están en unidades de escena con el eje Y hacia abajo, como en
 * JavaFX; la camioneta se modela en metros y se escala para llenar la
 * plataforma. Si el equipo no soporta 3D se muestra la ilustración fija del
 * carro. La animación se detiene cuando el componente sale de la escena.</p>
 *
 * <p>Principles: Pure Fabrication, High Cohesion.</p>
 */
public final class CarTurntable extends StackPane {

    private static final String FALLBACK_IMAGE = "/images/login-car.png";

    /** Una vuelta completa, en segundos. */
    private static final double SECONDS_PER_TURN = 16;
    /** Escala de metros a unidades de escena para la camioneta. */
    private static final double TRUCK_SCALE = 1.04;
    /** Altura de la cara superior de la plataforma, donde se apoyan las ruedas. */
    private static final double GROUND = 0.034;
    /** Radio del disco central con la sombra de contacto. */
    private static final double DISC_RADIUS = 2.56;

    private static final Color PLATFORM_TOP = Color.web("#FFFFFF");
    private static final Color PLATFORM_EDGE = Color.web("#DCE3EA");
    private static final Color PLATFORM_BASE = Color.web("#B9C7D8");
    private static final Color ACCENT = Color.web("#2F6DB5");
    private static final Color HALO = Color.web("#DCE6F2");

    private Timeline timeline;

    /**
     * Crea la vitrina con el tamaño indicado.
     *
     * @param width  ancho en px
     * @param height alto en px
     */
    public CarTurntable(double width, double height) {
        setPrefSize(width, height);
        setMaxSize(width, height);
        setMouseTransparent(true);

        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            ImageView fallback = new ImageView(new Image(
                    CarTurntable.class.getResourceAsStream(FALLBACK_IMAGE)));
            fallback.setFitWidth(Math.min(width, height));
            fallback.setPreserveRatio(true);
            fallback.setSmooth(true);
            getChildren().add(fallback);
            return;
        }

        // Halo de fondo, como la mancha suave de la ilustración original.
        Circle halo = new Circle(Math.min(width, height) * 0.36, HALO);
        halo.setTranslateY(-height * 0.06);
        getChildren().addAll(halo, buildScene(width, height));
    }

    private SubScene buildScene(double width, double height) {
        Group truck = TruckModel.build();
        truck.getTransforms().addAll(
                new Translate(0, -GROUND, 0),
                new Scale(TRUCK_SCALE, TRUCK_SCALE, TRUCK_SCALE));

        Group turntable = new Group(buildPlatform(), truck);
        Rotate spin = new Rotate(0, Rotate.Y_AXIS);
        turntable.getTransforms().add(spin);

        // Luz de estudio: sol desde arriba a la izquierda, relleno suave desde la
        // derecha y contraluz que marca los bordes.
        AmbientLight ambient = new AmbientLight(Color.gray(0.30));
        DirectionalLight key = new DirectionalLight(Color.gray(0.60));
        key.setDirection(new Point3D(0.55, 1.0, 0.65));
        DirectionalLight fill = new DirectionalLight(Color.gray(0.24));
        fill.setDirection(new Point3D(-1.0, 0.35, 0.25));
        DirectionalLight rim = new DirectionalLight(Color.gray(0.26));
        rim.setDirection(new Point3D(0.15, 0.45, -1.0));

        Group root = new Group(turntable, ambient, key, fill, rim);

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFieldOfView(23);
        camera.setNearClip(2);
        camera.setFarClip(200);
        camera.getTransforms().addAll(
                new Translate(0, -1.0, 0),
                new Rotate(-15, Rotate.X_AXIS),
                new Translate(0, 0, -17.5));

        SubScene scene = new SubScene(root, width, height, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.TRANSPARENT);
        scene.setCamera(camera);

        // Arranca en vista de tres cuartos delantera; por el borde delantero
        // el giro va de izquierda a derecha.
        double start = 154;
        timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(spin.angleProperty(), start, Interpolator.LINEAR)),
                new KeyFrame(Duration.seconds(SECONDS_PER_TURN),
                        new KeyValue(spin.angleProperty(), start - 360, Interpolator.LINEAR)));
        timeline.setCycleCount(Animation.INDEFINITE);
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                timeline.stop();
            } else {
                timeline.play();
            }
        });
        return scene;
    }

    /** Plataforma: base, canto, cara superior, aro de acento y marcas del giro. */
    private static Group buildPlatform() {
        Group platform = new Group();
        platform.getChildren().add(cylinder(3.55, 0.16, PLATFORM_BASE, 0.30));
        platform.getChildren().add(cylinder(3.35, 0.30, PLATFORM_EDGE, 0.15));
        platform.getChildren().add(cylinder(3.18, 0.04, ACCENT, 0.0));
        platform.getChildren().add(cylinder(3.08, 0.06, PLATFORM_TOP, -0.01));

        // Aro interior tenue y cuatro marcas discretas que delatan el giro.
        platform.getChildren().add(cylinder(2.62, 0.075, PLATFORM_EDGE, -0.025));
        platform.getChildren().add(cylinder(DISC_RADIUS, 0.08, PLATFORM_TOP, -0.03));
        platform.getChildren().add(shadowDisc());
        for (int i = 0; i < 4; i++) {
            Box mark = box(0.10, 0.03, 0.30, ACCENT);
            mark.getTransforms().addAll(
                    new Rotate(i * 90 + 45, Rotate.Y_AXIS),
                    new Translate(0, -0.035, 2.85));
            platform.getChildren().add(mark);
        }

        return platform;
    }

    /**
     * Disco central de la plataforma con la sombra de contacto ya pintada: un
     * halo suave bajo la carrocería y manchas más oscuras donde apoyan los
     * cauchos. Gira con la camioneta, así que siempre queda debajo de ella.
     */
    private static MeshView shadowDisc() {
        int size = 512;
        double scale = TRUCK_SCALE;
        double[] axles = {1.18 * scale, -1.13 * scale};
        WritableImage texture = new WritableImage(size, size);
        PixelWriter writer = texture.getPixelWriter();
        Color shade = Color.web("#76818F");
        for (int j = 0; j < size; j++) {
            for (int i = 0; i < size; i++) {
                double x = (i / (size - 1.0) * 2 - 1) * DISC_RADIUS;
                double z = (j / (size - 1.0) * 2 - 1) * DISC_RADIUS;
                // Distancia a un rectángulo redondeado con la huella de la carrocería.
                double corner = 0.5;
                double qx = Math.abs(x - 0.03 * scale) - 2.13 * scale + corner;
                double qz = Math.abs(z) - 0.97 * scale + corner;
                double outside = Math.hypot(Math.max(qx, 0), Math.max(qz, 0));
                double distance = outside + Math.min(Math.max(qx, qz), 0) - corner;
                double body = 0.30 * (distance < 0 ? 1 : Math.exp(-Math.pow(distance / 0.35, 2)));
                double tires = 0;
                for (double axle : axles) {
                    for (double side : new double[]{-1, 1}) {
                        double dx = (x - axle) / 0.30;
                        double dz = (z - side * 0.80 * scale) / 0.20;
                        tires = Math.max(tires, 0.42 * Math.exp(-(dx * dx + dz * dz)));
                    }
                }
                double amount = 1 - (1 - body) * (1 - tires);
                writer.setColor(i, j, PLATFORM_TOP.interpolate(shade, amount));
            }
        }

        MeshKit.Builder disc = new MeshKit.Builder(MeshKit.Shading.flat());
        int segments = 96;
        double y = GROUND - 0.001;
        int center = disc.vertex(0, y, 0, 0, 1, 0, 0.5, 0.5);
        int[] ring = new int[segments];
        for (int s = 0; s < segments; s++) {
            double angle = 2 * Math.PI * s / segments;
            double x = DISC_RADIUS * Math.cos(angle);
            double z = DISC_RADIUS * Math.sin(angle);
            ring[s] = disc.vertex(x, y, z, 0, 1, 0, (x / DISC_RADIUS + 1) / 2, (z / DISC_RADIUS + 1) / 2);
        }
        for (int s = 0; s < segments; s++) {
            disc.triangle(center, ring[s], ring[(s + 1) % segments]);
        }
        return disc.view(MeshKit.material(texture, 0.1, 8));
    }

    /**
     * Cilindro vertical apoyado bajo el plano y = 0 (la superficie de la
     * plataforma), desplazado hacia abajo {@code drop} unidades.
     */
    private static Cylinder cylinder(double radius, double height, Color color, double drop) {
        Cylinder cylinder = new Cylinder(radius, height, 96);
        cylinder.setMaterial(material(color));
        cylinder.setTranslateY(height / 2 + drop);
        return cylinder;
    }

    private static Box box(double width, double height, double depth, Color color) {
        Box box = new Box(width, height, depth);
        box.setMaterial(material(color));
        return box;
    }

    private static PhongMaterial material(Color color) {
        PhongMaterial material = new PhongMaterial(color);
        material.setSpecularColor(Color.gray(0.35));
        material.setSpecularPower(24);
        return material;
    }
}
