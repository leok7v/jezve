package org.jezve.svg;

import java.awt.geom.Rectangle2D;

public class Root extends SVG.Group {

    private Units x;
    private Units y;
    private Units width;
    private Units height;
    private Rectangle2D.Float viewBox;
    public static final int PA_X_NONE = 0;
    public static final int PA_X_MIN = 1;
    public static final int PA_X_MID = 2;
    public static final int PA_X_MAX = 3;
    public static final int PA_Y_NONE = 0;
    public static final int PA_Y_MIN = 1;
    public static final int PA_Y_MID = 2;
    public static final int PA_Y_MAX = 3;
    public static final int PS_MEET = 0;
    public static final int PS_SLICE = 1;
    private int parSpecifier = PS_MEET;
    private int parAlignX = PA_X_MID;
    private int parAlignY = PA_Y_MID;

    protected void loaderEndElement() {
        super.loaderEndElement();
        x = getUnits("x");
        y = getUnits("y");
        width = getUnits("width");
        height = getUnits("height");
        float[] vb = getFloats("viewBox");
        if (vb != null) {
            viewBox = new Rectangle2D.Float(vb[0], vb[1], vb[2], vb[3]);
        }
        String preserve = getString("preserveAspectRatio");
        if (preserve != null) {
            if (contains(preserve, "none")) {
                parAlignX = PA_X_NONE;
                parAlignY = PA_Y_NONE;
            } else if (contains(preserve, "xMinYMin")) {
                parAlignX = PA_X_MIN;
                parAlignY = PA_Y_MIN;
            } else if (contains(preserve, "xMidYMin")) {
                parAlignX = PA_X_MID;
                parAlignY = PA_Y_MIN;
            } else if (contains(preserve, "xMaxYMin")) {
                parAlignX = PA_X_MAX;
                parAlignY = PA_Y_MIN;
            } else if (contains(preserve, "xMinYMid")) {
                parAlignX = PA_X_MIN;
                parAlignY = PA_Y_MID;
            } else if (contains(preserve, "xMidYMid")) {
                parAlignX = PA_X_MID;
                parAlignY = PA_Y_MID;
            } else if (contains(preserve, "xMaxYMid")) {
                parAlignX = PA_X_MAX;
                parAlignY = PA_Y_MID;
            } else if (contains(preserve, "xMinYMax")) {
                parAlignX = PA_X_MIN;
                parAlignY = PA_Y_MAX;
            } else if (contains(preserve, "xMidYMax")) {
                parAlignX = PA_X_MID;
                parAlignY = PA_Y_MAX;
            } else if (contains(preserve, "xMaxYMax")) {
                parAlignX = PA_X_MAX;
                parAlignY = PA_Y_MAX;
            }
            if (contains(preserve, "meet")) {
                parSpecifier = PS_MEET;
            } else if (contains(preserve, "slice")) {
                parSpecifier = PS_SLICE;
            }
        }
    }

    private boolean contains(String text, String find) {
        return text.indexOf(find) >= 0;
    }

    public Rectangle2D.Float getViewBox() {
        return viewBox;
    }

    public float getWidth() {
        return width == null ? 0 : Units.convertUnitsToPixels(width.getKind(), width.getValue());
    }

    public float getHeight() {
        return height == null ? 0 : Units.convertUnitsToPixels(height.getKind(), height.getValue());
    }

    public float getX() {
        return x == null ? 0 : Units.convertUnitsToPixels(x.getKind(), x.getValue());
    }

    public float getY() {
        return y == null ? 0 : Units.convertUnitsToPixels(y.getKind(), y.getValue());
    }

    /**
     * @return PS_MEET or PS_SLICE
     * see http://www.w3.org/TR/SVG/coords.html#PreserveAspectRatioAttribute
     */
    public int getMeetOrSlice() {
        return parSpecifier;
    }

    /**
     * @return one of PA_X_*
     * see http://www.w3.org/TR/SVG/coords.html#PreserveAspectRatioAttribute
     */
    public int getParAlignX() {
        return parAlignX;
    }

    /**
     * @return one of PA_X_*
     * see http://www.w3.org/TR/SVG/coords.html#PreserveAspectRatioAttribute
     */
    public int getParAlignY() {
        return parAlignY;
    }
}
