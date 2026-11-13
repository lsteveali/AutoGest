package com.autogest.controller.components;

import javafx.scene.shape.ArcTo;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Íconos lineales (estilo Feather/Lucide) definidos como datos de ruta SVG.
 *
 * <p>Decisión D-15: SVGPath propios, sin dependencias externas. JavaFX
 * rellena ({@link javafx.scene.shape.SVGPath}) pero el sistema de diseño
 * exige íconos lineales, por eso esta clase convierte los datos de ruta en
 * un {@link Path} con trazo (relleno transparente, clase CSS {@code icon}).</p>
 *
 * <p>Comandos soportados: M, L, C, A, Z (absolutos y relativos).</p>
 *
 * <p>Principles: High Cohesion, Pure Fabrication.</p>
 */
public final class IconSet {

    private IconSet() {
    }

    /** Íconos de navegación y comunes. */
    public static final String HOME = "M3 9 L12 2 L21 9 M3 9 L3 20 A1 1 0 0 0 4 21 L20 21 A1 1 0 0 0 21 20 L21 9 M9 21 L9 15 L15 15 L15 21";
    public static final String USERS = "M17 21 L17 19 A4 4 0 0 0 13 15 L5 15 A4 4 0 0 0 1 19 L1 21 M9 11 A4 4 0 1 1 9 3.2 A4 4 0 1 1 9 11 M23 21 L23 19 A4 4 0 0 0 20 15.4 M16 3.8 A4 4 0 1 1 15.6 10.4";
    /** Sedán de perfil (carrocería, cabina y dos ruedas); el anterior se leía como una patineta. */
    public static final String CAR = "M19 17 L21 17 C21.6 17 22 16.6 22 16 L22 13 C22 12.1 21.3 11.3 20.5 11.1 C18.7 10.6 16 10 16 10 C16 10 14.7 8.6 13.8 7.7 C13.3 7.3 12.7 7 12 7 L5 7 C4.4 7 3.9 7.4 3.6 7.9 L2.2 10.8 C2.1 11.2 2 11.6 2 12 L2 16 C2 16.6 2.4 17 3 17 L5 17 M9 17 L15 17 M7 19 A2 2 0 1 1 7 15 A2 2 0 1 1 7 19 M17 19 A2 2 0 1 1 17 15 A2 2 0 1 1 17 19";
    public static final String FILE_TEXT = "M14 2 L14 7 L19 7 M6 2 L14 2 L19 7 L19 20 A1 1 0 0 1 18 21 L6 21 A1 1 0 0 1 5 20 L5 3 A1 1 0 0 1 6 2 M9 12 L15 12 M9 16 L13 16";
    public static final String WRENCH = "M14.7 6.3 A1 1 0 0 0 14.7 7.7 L16.3 9.3 A1 1 0 0 0 17.7 9.3 L21.5 5.5 A6 6 0 0 1 13.6 13.4 L6.7 20.3 A2.1 2.1 0 0 1 3.7 17.3 L10.6 10.4 A6 6 0 0 1 18.5 2.5 Z";
    public static final String BOX = "M21 16 L21 8 A2 2 0 0 0 20 6.3 L13 2.3 A2 2 0 0 0 11 2.3 L4 6.3 A2 2 0 0 0 3 8 L3 16 A2 2 0 0 0 4 17.7 L11 21.7 A2 2 0 0 0 13 21.7 L20 17.7 A2 2 0 0 0 21 16 Z M3.3 7 L12 12 L20.7 7 M12 12 L12 21.7";
    public static final String CREDIT_CARD = "M2 6 A2 2 0 0 1 4 4 L20 4 A2 2 0 0 1 22 6 L22 18 A2 2 0 0 1 20 20 L4 20 A2 2 0 0 1 2 18 Z M2 10 L22 10";
    public static final String BELL = "M6 8 A6 6 0 0 1 18 8 C18 15 21 17 21 17 L3 17 C3 17 6 15 6 8 M9.8 21 A2.2 2.2 0 0 0 14.2 21";
    public static final String BAR_CHART = "M6 20 L6 14 M12 20 L12 6 M18 20 L18 10";
    public static final String SETTINGS = "M4 21 L4 14 M4 10 L4 3 M12 21 L12 12 M12 8 L12 3 M20 21 L20 16 M20 12 L20 3 M1 14 L7 14 M9 8 L15 8 M17 16 L23 16";
    public static final String SPARKLES = "M12 3 L13.8 8.8 L18 10 L13.8 11.2 L12 17 L10.2 11.2 L6 10 L10.2 8.8 Z";
    public static final String SEARCH = "M11 19 A8 8 0 1 1 11 3.2 A8 8 0 1 1 11 19 M21 21 L16.65 16.65";
    public static final String PLUS = "M12 5 L12 19 M5 12 L19 12";
    public static final String CHEVRON_DOWN = "M6 9 L12 15 L18 9";
    public static final String CHEVRON_RIGHT = "M9 6 L15 12 L9 18";
    public static final String EDIT = "M17 3 L21 7 L8 20 L4 21 L5 17 Z M14 6 L18 10";
    public static final String TRASH = "M3 6 L21 6 M10 6 L10 4 A2 2 0 0 1 12 2 M14 6 L14 4 A2 2 0 0 0 12 2 M19 6 L18 20 A2 2 0 0 1 16 22 L8 22 A2 2 0 0 1 6 20 L5 6";
    public static final String LOGOUT = "M9 21 L5 21 A2 2 0 0 1 3 19 L3 5 A2 2 0 0 1 5 3 L9 3 M16 17 L20 12 L16 7 M20 12 L10 12";
    public static final String X = "M6 6 L18 18 M18 6 L6 18";
    public static final String MORE = "M4 12 A1 1 0 1 1 6 12 A1 1 0 1 1 4 12 M11 12 A1 1 0 1 1 13 12 A1 1 0 1 1 11 12 M18 12 A1 1 0 1 1 20 12 A1 1 0 1 1 18 12";
    public static final String ARROW_LEFT = "M12 19 L5 12 L12 5 M19 12 L5 12";
    public static final String CHECK = "M20 6 L9 17 L4 12";
    /** Persona (perfil y cuenta). */
    public static final String USER = "M19 21 L19 19 A4 4 0 0 0 15 15 L9 15 A4 4 0 0 0 5 19 L5 21 M12 11 A4 4 0 1 1 12 3 A4 4 0 1 1 12 11";
    /** Taller: nave con portón (datos del taller). */
    public static final String WAREHOUSE = "M3 21 L3 9 L12 3 L21 9 L21 21 M7 21 L7 13 L17 13 L17 21 M7 17 L17 17";
    /** Escudo (seguridad). */
    public static final String SHIELD = "M12 22 C12 22 20 18 20 12 L20 5 L12 2 L4 5 L4 12 C4 18 12 22 12 22 Z M9 12 L11 14 L15 10";
    /** Nube con flecha hacia abajo (copias de seguridad). */
    public static final String CLOUD_DOWNLOAD = "M8 17 L12 21 L16 17 M12 12 L12 21 M20.9 18.1 A5 5 0 0 0 18 9 L16.7 9 A8 8 0 1 0 4 16.3";
    /** Flecha hacia la derecha (abrir una sección). */
    public static final String ARROW_RIGHT = "M5 12 L19 12 M13 6 L19 12 L13 18";
    /** Círculo con «i» (información del sistema). */
    public static final String INFO = "M12 22 A10 10 0 1 1 12 2 A10 10 0 1 1 12 22 M12 16 L12 12 M12 8 L12.01 8";
    public static final String MAIL = "M4 4 L20 4 A2 2 0 0 1 22 6 L22 18 A2 2 0 0 1 20 20 L4 20 A2 2 0 0 1 2 18 L2 6 A2 2 0 0 1 4 4 Z M22 6 L12 13 L2 6";
    public static final String LOCK = "M5 11 L19 11 A2 2 0 0 1 21 13 L21 20 A2 2 0 0 1 19 22 L5 22 A2 2 0 0 1 3 20 L3 13 A2 2 0 0 1 5 11 Z M7 11 L7 7 A5 5 0 0 1 17 7 L17 11";
    public static final String CALENDAR ="M3 5 A1 1 0 0 1 4 4 L20 4 A1 1 0 0 1 21 5 L21 19 A1 1 0 0 1 20 20 L4 20 A1 1 0 0 1 3 19 Z M3 9 L21 9 M8 2 L8 6 M16 2 L16 6";

    /**
     * Crea un ícono lineal de 24x24 dibujado con trazo.
     *
     * @param pathData datos de ruta del ícono (una constante de IconSet)
     * @return un nodo Path con la clase CSS {@code icon}, listo para escalar
     */
    public static Path icon(String pathData) {
        Path path = new Path();
        path.getElements().setAll(parsePathData(pathData));
        path.getStyleClass().add("icon");
        return path;
    }

    /**
     * Analiza datos de ruta SVG y los convierte en elementos de ruta de
     * JavaFX. Soporta M, L, C, A y Z (absolutos y relativos).
     *
     * @param data datos de ruta SVG
     * @return lista de elementos de ruta
     */
    static List<PathElement> parsePathData(String data) {
        List<PathElement> elements = new ArrayList<>();
        List<String> tokens = tokenize(data);
        double cx = 0;
        double cy = 0;
        int i = 0;
        while (i < tokens.size()) {
            String token = tokens.get(i);
            char cmd = token.charAt(0);
            boolean relative = Character.isLowerCase(cmd);
            char upper = Character.toUpperCase(cmd);
            i++;

            switch (upper) {
                case 'M' -> i = parseMove(elements, tokens, i, relative);
                case 'L' -> i = parseLine(elements, tokens, i, relative);
                case 'C' -> i = parseCubic(elements, tokens, i, relative);
                case 'A' -> i = parseArc(elements, tokens, i, relative);
                case 'Z' -> elements.add(new javafx.scene.shape.ClosePath());
                default -> throw new IllegalArgumentException("Comando SVG no soportado: " + token);
            }
        }
        return elements;
    }

    private static int parseMove(List<PathElement> elements, List<String> tokens, int i, boolean relative) {
        double x = number(tokens.get(i++));
        double y = number(tokens.get(i++));
        if (relative) {
            x = currentX(elements) + x;
            y = currentY(elements) + y;
        }
        elements.add(new MoveTo(x, y));
        // Los pares de coordenadas extra tras una M se tratan como L implícitos.
        while (i < tokens.size() && isNumber(tokens.get(i))) {
            double nx = number(tokens.get(i++));
            double ny = number(tokens.get(i++));
            if (relative) {
                nx = currentX(elements) + nx;
                ny = currentY(elements) + ny;
            }
            elements.add(new LineTo(nx, ny));
        }
        return i;
    }

    private static int parseLine(List<PathElement> elements, List<String> tokens, int i, boolean relative) {
        do {
            double x = number(tokens.get(i++));
            double y = number(tokens.get(i++));
            double px = currentX(elements);
            double py = currentY(elements);
            elements.add(new LineTo(relative ? px + x : x, relative ? py + y : y));
        } while (i < tokens.size() && isNumber(tokens.get(i)));
        return i;
    }

    private static int parseCubic(List<PathElement> elements, List<String> tokens, int i, boolean relative) {
        do {
            double x1 = number(tokens.get(i++));
            double y1 = number(tokens.get(i++));
            double x2 = number(tokens.get(i++));
            double y2 = number(tokens.get(i++));
            double x = number(tokens.get(i++));
            double y = number(tokens.get(i++));
            double px = currentX(elements);
            double py = currentY(elements);
            elements.add(new CubicCurveTo(
                    relative ? px + x1 : x1,
                    relative ? py + y1 : y1,
                    relative ? px + x2 : x2,
                    relative ? py + y2 : y2,
                    relative ? px + x : x,
                    relative ? py + y : y));
        } while (i < tokens.size() && isNumber(tokens.get(i)));
        return i;
    }

    private static int parseArc(List<PathElement> elements, List<String> tokens, int i, boolean relative) {
        do {
            double rx = number(tokens.get(i++));
            double ry = number(tokens.get(i++));
            double rot = number(tokens.get(i++));
            boolean large = number(tokens.get(i++)) != 0;
            boolean sweep = number(tokens.get(i++)) != 0;
            double x = number(tokens.get(i++));
            double y = number(tokens.get(i++));
            double px = currentX(elements);
            double py = currentY(elements);
            ArcTo arc = new ArcTo();
            arc.setRadiusX(rx);
            arc.setRadiusY(ry);
            arc.setXAxisRotation(rot);
            arc.setLargeArcFlag(large);
            arc.setSweepFlag(sweep);
            arc.setX(relative ? px + x : x);
            arc.setY(relative ? py + y : y);
            elements.add(arc);
        } while (i < tokens.size() && isNumber(tokens.get(i)));
        return i;
    }

    private static List<String> tokenize(String data) {
        List<String> tokens = new ArrayList<>();
        Pattern pattern = Pattern.compile("[A-Za-z]|[-+]?(\\d+\\.?\\d*|\\.\\d+)([eE][-+]?\\d+)?");
        Matcher matcher = pattern.matcher(data);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    private static double currentX(List<PathElement> elements) {
        return lastPoint(elements)[0];
    }

    private static double currentY(List<PathElement> elements) {
        return lastPoint(elements)[1];
    }

    private static double[] lastPoint(List<PathElement> elements) {
        if (elements.isEmpty()) {
            return new double[]{0, 0};
        }
        PathElement last = elements.get(elements.size() - 1);
        if (last instanceof MoveTo m) {
            return new double[]{m.getX(), m.getY()};
        }
        if (last instanceof LineTo l) {
            return new double[]{l.getX(), l.getY()};
        }
        if (last instanceof CubicCurveTo c) {
            return new double[]{c.getX(), c.getY()};
        }
        if (last instanceof ArcTo a) {
            return new double[]{a.getX(), a.getY()};
        }
        // ClosePath y otros: se conserva el punto previo al cierre.
        return new double[]{0, 0};
    }

    private static double number(String token) {
        return Double.parseDouble(token);
    }

    private static boolean isNumber(String token) {
        char c = token.charAt(0);
        return Character.isDigit(c) || c == '-' || c == '+';
    }
}