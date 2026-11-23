package com.autogest.controller.components;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

/**
 * Materiales comunes de los automóviles de la vitrina (vidrio, cromo, rines,
 * caucho, luces…). Los usan los modelos hechos por código ({@link CarModel})
 * y los importados ({@link ImportedModel}); la pintura sale de
 * {@link VehiclePaint}.
 *
 * <p>Principles: Pure Fabrication, High Cohesion.</p>
 */
final class VehicleMaterials {

    private VehicleMaterials() {
        // Clase de utilidad: no se instancia.
    }

    static PhongMaterial glass() {
        return MeshKit.material(MeshKit.gradient(
                0.0, Color.web("#8C99A6"), 0.35, Color.web("#3A4552"), 0.5, Color.web("#1A2029"),
                1.0, Color.web("#0B0E12")), 0.9, 90);
    }

    static PhongMaterial chrome() {
        return MeshKit.material(MeshKit.gradient(
                0.0, Color.WHITE, 0.30, Color.web("#E9EDF1"), 0.46, Color.web("#A9B0B8"),
                0.50, Color.web("#2F353C"), 0.58, Color.web("#5F666E"), 0.80, Color.web("#D3D8DD"),
                1.0, Color.web("#F2F4F6")), 1.0, 80);
    }

    /** Rin plateado. */
    static PhongMaterial rim() {
        return MeshKit.material(MeshKit.gradient(0.0, Color.web("#EEF1F4"), 0.45, Color.web("#C2C8CF"),
                0.5, Color.web("#7E858D"), 1.0, Color.web("#C9CED4")), 0.8, 50);
    }

    /** Rin oscuro, color grafito. */
    static PhongMaterial darkRim() {
        return MeshKit.material(MeshKit.gradient(0.0, Color.web("#7A828C"), 0.45, Color.web("#4A5058"),
                0.5, Color.web("#2B2F35"), 1.0, Color.web("#555B63")), 0.7, 40);
    }

    static PhongMaterial rimBack() {
        return MeshKit.material(Color.web("#2C3036"), 0.3, 16);
    }

    static PhongMaterial disc() {
        return MeshKit.material(Color.web("#8A9099"), 0.5, 30);
    }

    static PhongMaterial trim() {
        return MeshKit.material(Color.web("#202327"), 0.2, 12);
    }

    static PhongMaterial rubber() {
        return MeshKit.material(Color.web("#1B1D20"), 0.12, 6);
    }

    static PhongMaterial well() {
        return MeshKit.material(Color.web("#0D0F11"), 0.0, 1);
    }

    static PhongMaterial grille() {
        return MeshKit.material(Color.web("#121417"), 0.3, 20);
    }

    static PhongMaterial lens() {
        return MeshKit.material(Color.web("#E6ECF1"), 1.0, 96);
    }

    static PhongMaterial tail() {
        return MeshKit.material(Color.web("#B42F2F"), 0.7, 60);
    }

    static PhongMaterial plate() {
        return MeshKit.material(Color.web("#F1F3F5"), 0.3, 16);
    }
}
