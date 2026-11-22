package com.autogest.controller.components;

import javafx.scene.Group;
import javafx.scene.shape.MeshView;

/**
 * Vehículo armado para la vitrina: el grupo 3D completo, la malla de la
 * pintura (para cambiar el color sin rearmarlo) y su huella en el piso.
 *
 * @param group     piezas del vehículo con el piso en y = 0 y el frente hacia +X
 * @param paint     malla de la carrocería pintada
 * @param footprint medidas para dibujar la sombra de contacto
 */
record VehicleBuild(Group group, MeshView paint, Footprint footprint) {

    /**
     * Huella del vehículo en metros, vista desde arriba: el rectángulo de la
     * carrocería y la posición de los cauchos.
     *
     * @param centerX    centro de la carrocería en X
     * @param halfLength media longitud de la carrocería
     * @param halfWidth  medio ancho de la carrocería
     * @param track      distancia del centro a cada caucho en Z
     * @param frontAxle  eje delantero en X
     * @param rearAxle   eje trasero en X
     * @param tireLength medio largo de la mancha del caucho en X
     * @param tireWidth  medio ancho de la mancha del caucho en Z
     */
    record Footprint(double centerX, double halfLength, double halfWidth, double track,
                     double frontAxle, double rearAxle, double tireLength, double tireWidth) {
    }
}
