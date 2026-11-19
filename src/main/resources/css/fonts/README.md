# Fuentes — Manrope (interfaz) y Roboto (PDF)

Tipografías incluidas en el proyecto (decisión D-18) para que se vean igual
aunque no estén instaladas en el sistema.

## Manrope — interfaz (desde el 19/11/2026)

| Archivo | Peso |
|---|---|
| `Manrope-Regular.ttf` | 400 (normal) |
| `Manrope-Medium.ttf` | 500 |
| `Manrope-Bold.ttf` | 700 (negrita) |

- **Origen:** <https://github.com/googlefonts/manrope> (`fonts/ttf`).
- **Licencia:** SIL Open Font License 1.1 (`OFL-Manrope.txt`).
- **Uso:** `AutoGestApplication` las carga con `Font.loadFont`; el CSS las
  referencia en `.root` con `-fx-font-family: "Manrope", "Segoe UI", sans-serif;`.

## Roboto — PDF de factura y reporte

| Archivo | Peso |
|---|---|
| `Roboto-Regular.ttf` | 400 (normal) |
| `Roboto-Medium.ttf` | 500 |
| `Roboto-Bold.ttf` | 700 (negrita) |

- **Origen:** <https://github.com/googlefonts/roboto-2> (`src/hinted`).
- **Licencia:** Apache License 2.0 (<https://www.apache.org/licenses/LICENSE-2.0>).
- **Uso:** `InvoicePdfService` y `ReportPdfService` las incrustan en los PDF.
