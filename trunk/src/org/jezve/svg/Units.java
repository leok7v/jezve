package org.jezve.svg;


import java.util.Map;
import java.util.HashMap;


class Units {

    static final int KIND_UNITLESS = 0;
    static final int KIND_PX = 1;  // Pixels
    static final int KIND_CM = 2;  // Centimeters
    static final int KIND_MM = 3;  // Millimeters
    static final int KIND_IN = 4;  // Inches
    static final int KIND_EM = 5;  // Default font height
    static final int KIND_EX = 6;  // Height of character 'x' in default font
    static final int KIND_PT = 7;  // Points - 1/72 of an inch
    static final int KIND_PC = 8;  // Picas - 1/6 of an inch
    static final int KIND_PERCENT = 9;  // Percent - relative width
    private float value;
    private int kind = KIND_UNITLESS;
    private static final float PPI = 72; // pixels per inch

    Units(String s) {
        Parser.Double parser = new Parser.Double(s, null);
        this.value = parser.nextFloat();
        int k = parser.getPosition();
        if (k < s.length()) {
            String suffix = s.substring(k).toLowerCase();
            kind = getType(suffix);
        }
    }

    Units(float value, int unitType) {
        this.value = value;
        this.kind = unitType;
    }

    float getValue() {
        return value;
    }

    int getKind() {
        return kind;
    }

    private static final Map type = new HashMap(15) {
        {
            putType("px", KIND_PX);
            putType("cm", KIND_CM);
            putType("mm", KIND_MM);
            putType("in", KIND_IN);
            putType("em", KIND_EM);
            putType("ex", KIND_EX);
            putType("pt", KIND_PT);
            putType("pc", KIND_PC);
            putType("px", KIND_PX);
            putType("%", KIND_PERCENT);
        }

        private void putType(String k, int v) {
            put(k, new Integer(v));
        }
    };

    private static int getType(String k) {
        return ((Integer)type.get(k)).intValue();
    }

    static float convertUnitsToPixels(int unitType, float value) {
        if (unitType == KIND_UNITLESS || unitType == KIND_PERCENT) {
            return value;
        }
        final float inchesPerCm = .3936f;
        switch (unitType) {
            case KIND_IN:
                return value * PPI;
            case KIND_CM:
                return value * inchesPerCm * PPI;
            case KIND_MM:
                return value * .1f * inchesPerCm * PPI;
            case KIND_PT:
                return value * (1f / 72f) * PPI;
            case KIND_PC:
                return value * (1f / 6f) * PPI;
        }
        return value;
    }

}
