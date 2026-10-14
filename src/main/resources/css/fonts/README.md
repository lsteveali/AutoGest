# Fuentes — Roboto

Tipografía incluida en el proyecto (decisión D-18) para que la interfaz use
Roboto aunque no esté instalada en el sistema.

| Archivo | Peso |
|---|---|
| `Roboto-Regular.ttf` | 400 (normal) |
| `Roboto-Medium.ttf` | 500 |
| `Roboto-Bold.ttf` | 700 (negrita) |

- **Origen:** <https://github.com/googlefonts/roboto-2> (`src/hinted`).
- **Licencia:** Apache License 2.0 (<https://www.apache.org/licenses/LICENSE-2.0>).
- **Uso:** `AutoGestApplication` las carga con `Font.loadFont`; el CSS las
  referencia en `.root` con `-fx-font-family: "Roboto", "Segoe UI", sans-serif;`.
