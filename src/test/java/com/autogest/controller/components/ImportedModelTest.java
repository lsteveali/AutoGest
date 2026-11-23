package com.autogest.controller.components;

import com.autogest.util.LoginAppearance.Vehicle;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lee los modelos importados sin JavaFX (solo los datos del archivo). */
class ImportedModelTest {

    @Test
    void mustangHasPaintTiresAndAReasonableSize() {
        ImportedModel.Data data = ImportedModel.read(Vehicle.MUSTANG.model());

        Set<String> roles = data.parts().stream().map(ImportedModel.Part::role).collect(Collectors.toSet());
        assertTrue(roles.containsAll(Set.of("paint", "glass", "rubber")), roles.toString());
        assertTrue(data.triangleCount() < 200_000, "Demasiados triángulos: " + data.triangleCount());

        VehicleBuild.Footprint footprint = data.footprint();
        assertEquals(4.79 / 2, footprint.halfLength(), 0.15);
        assertTrue(footprint.frontAxle() > 1 && footprint.rearAxle() < -1, footprint.toString());
        float height = data.paintHeight();
        assertTrue(height > 1.2 && height < 1.8, "Altura: " + height);
    }

    @Test
    void sportsCarIsImportedWithPaintAndTires() {
        ImportedModel.Data data = ImportedModel.read(Vehicle.SPORTS.model());

        Set<String> roles = data.parts().stream().map(ImportedModel.Part::role).collect(Collectors.toSet());
        assertTrue(roles.containsAll(Set.of("paint", "glass", "rubber", "rim")), roles.toString());
        assertTrue(data.footprint().frontAxle() > 0 && data.footprint().rearAxle() < 0);
        assertTrue(data.paintHeight() < 1.5, "Un deportivo es bajo: " + data.paintHeight());
    }

    @Test
    void classicSitsOnItsFourTiresAndNotOnTheSpare() {
        ImportedModel.Data data = ImportedModel.read(Vehicle.CLASSIC.model());

        Set<String> roles = data.parts().stream().map(ImportedModel.Part::role).collect(Collectors.toSet());
        assertTrue(roles.containsAll(Set.of("paint", "rubber", "chrome", "tail")), roles.toString());
        // Las ruedas quedan a lado y lado (la de repuesto, atrás y en el centro, no cuenta).
        VehicleBuild.Footprint footprint = data.footprint();
        assertTrue(footprint.track() > 0.6, footprint.toString());
        assertTrue(footprint.frontAxle() > 1.5 && footprint.rearAxle() < -1, footprint.toString());
    }

    @Test
    void modelIsReadOnceAndKeptInMemory() {
        assertSame(ImportedModel.read(Vehicle.MUSTANG.model()), ImportedModel.read(Vehicle.MUSTANG.model()));
    }

    @Test
    void onlyImportedVehiclesHaveAModelFile() {
        assertEquals(null, Vehicle.TRUCK.model());
        assertTrue(Vehicle.CLASSIC.model().endsWith(".agm"));
        assertTrue(Vehicle.MUSTANG.model().endsWith(".agm"));
        assertTrue(Vehicle.SPORTS.model().endsWith(".agm"));
    }

    @Test
    void wrongFilesAreRejected() throws IOException {
        assertThrows(IllegalStateException.class, () -> ImportedModel.read("/models/no-existe.agm"));
        assertThrows(IOException.class, () -> ImportedModel.read(gzip("HOLA".getBytes())));

        // Triángulo que apunta a un vértice que no existe.
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bytes))) {
            out.writeBytes("AGM1");
            out.writeFloat(4f);
            for (int i = 0; i < 8; i++) {
                out.writeFloat(1f);
            }
            out.writeInt(1);
            out.writeUTF("paint");
            out.writeInt(1);
            out.writeShort(0);
            out.writeShort(0);
            out.writeShort(0);
            out.writeByte(0);
            out.writeByte(127);
            out.writeByte(0);
            out.writeInt(1);
            out.writeInt(0);
            out.writeInt(0);
            out.writeInt(5);
        }
        assertThrows(IOException.class, () -> ImportedModel.read(new ByteArrayInputStream(bytes.toByteArray())));
    }

    private static ByteArrayInputStream gzip(byte[] content) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(bytes)) {
            out.write(content);
        }
        return new ByteArrayInputStream(bytes.toByteArray());
    }
}
