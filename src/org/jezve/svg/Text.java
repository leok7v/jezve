package org.jezve.svg;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.LinkedList;

class Text extends SVG.ShapeElement {

    private float x = 0;
    private float y = 0;
    private String fontFamily;
    private float fontSize;
    // List of strings and tspans containing the content of this node
    private LinkedList content = new LinkedList();
    private Shape textShape;
    static final int TXAN_START = 0;
    static final int TXAN_MIDDLE = 1;
    static final int TXAN_END = 2;
    private int textAnchor = TXAN_START;
    static final int TXST_NORMAL = 0;
    static final int TXST_ITALIC = 1;
    static final int TXST_OBLIQUE = 2;
    private int fontStyle;
    static final int TXWE_NORMAL = 0;
    static final int TXWE_BOLD = 1;
    static final int TXWE_BOLDER = 2;
    static final int TXWE_LIGHTER = 3;
    static final int TXWE_100 = 4;
    static final int TXWE_200 = 5;
    static final int TXWE_300 = 6;
    static final int TXWE_400 = 7;
    static final int TXWE_500 = 8;
    static final int TXWE_600 = 9;
    static final int TXWE_700 = 10;
    static final int TXWE_800 = 11;
    static final int TXWE_900 = 12;
    private int fontWeight;

    void appendText(String text) {
        content.addLast(text);
    }

    void appendTspan(Tspan tspan) {
        super.loaderAddChild(tspan);
        content.addLast(tspan);
    }

    protected void loaderAddChild(Element child) {
        super.loaderAddChild(child);
        content.addLast(child);
    }

    void loaderAddText(SVG svg, String text) {
        String t = text.trim();
        if (t.length() > 0) {
            content.addLast(text);
        }
    }

    protected void build() {
        super.build();
        x = getFloatUnits("x", 0);
        y = getFloatUnits("y", 0);
        fontFamily = getStyleString("font-family", "Sans Serif");
        fontSize = getStyleFloatUnits("font-size", 12);
        String fs = getStyleString("font-style");
        if (fs != null) {
            if ("normal".equalsIgnoreCase(fs)) {
                fontStyle = TXST_NORMAL;
            } else if ("italic".equalsIgnoreCase(fs)) {
                fontStyle = TXST_ITALIC;
            } else if ("oblique".equalsIgnoreCase(fs)) {
                fontStyle = TXST_OBLIQUE;
            }
        } else {
            fontStyle = TXST_NORMAL;
        }
        String fw = getStyleString("font-weight");
        if (fw != null) {
            if ("normal".equalsIgnoreCase(fw)) {
                fontWeight = TXWE_NORMAL;
            } else if ("bold".equalsIgnoreCase(fw)) {
                fontWeight = TXWE_BOLD;
            }
        } else {
            fontWeight = TXWE_BOLD;
        }
        String ta = getStyleString("text-anchor");
        if (ta != null) {
            if ("middle".equalsIgnoreCase(ta)) {
                textAnchor = TXAN_MIDDLE;
            } else if ("end".equalsIgnoreCase(ta)) {
                textAnchor = TXAN_END;
            } else {
                textAnchor = TXAN_START;
            }
        } else {
            textAnchor = TXAN_START;
        }
        // text anchor
        // text-decoration
        // text-rendering
        buildFont();
    }

    protected void buildFont() {
        int style;
        switch (fontStyle) {
            case TXST_ITALIC:
                style = java.awt.Font.ITALIC;
                break;
            default:
                style = java.awt.Font.PLAIN;
                break;
        }
        int weight;
        switch (fontWeight) {
            case TXWE_BOLD:
            case TXWE_BOLDER:
                weight = java.awt.Font.BOLD;
                break;
            default:
                weight = java.awt.Font.PLAIN;
                break;
        }
        // Get font
        SVG.Font font = getRoot().getFont(fontFamily);
        if (font == null) {
            java.awt.Font sysFont = new java.awt.Font(fontFamily, style | weight, (int)fontSize);
            buildSysFont(sysFont);
        }
        font = getRoot().getFont(fontFamily);
        if (font == null) {
            return;
        }
        GeneralPath textPath = new GeneralPath();
        textShape = textPath;
        float cursorX = x, cursorY = y;
        SVG.FontFace fontFace = font.getFontFace();
        // int unitsPerEm = fontFace.getUnitsPerEm();
        int ascent = fontFace.getAscent();
        float fontScale = fontSize / (float)ascent;

        AffineTransform xform = new AffineTransform();
        for (Iterator i = content.iterator(); i.hasNext();) {
            Object obj = i.next();
            if (obj instanceof String) {
                String text = (String)obj;
                setStrokeWidthScalar(1f / fontScale);
                for (int k = 0; k < text.length(); k++) {
                    xform.setToIdentity();
                    xform.setToTranslation(cursorX, cursorY);
                    xform.scale(fontScale, fontScale);
                    String unicode = text.substring(k, k + 1);
                    SVG.MissingGlyph glyph = font.getGlyph(unicode);
                    Shape path = glyph.getPath();
                    if (path != null) {
                        path = xform.createTransformedShape(path);
                        textPath.append(path, false);
                    }
                    cursorX += fontScale * glyph.getHorizAdvX();
                }
                setStrokeWidthScalar(1f);
            } else if (obj instanceof Tspan) {
                Tspan tspan = (Tspan)obj;
                xform.setToIdentity();
                xform.setToTranslation(cursorX, cursorY);
                xform.scale(fontScale, fontScale);
                Shape tspanShape = tspan.getShape();
                tspanShape = xform.createTransformedShape(tspanShape);
                textPath.append(tspanShape, false);
            }
        }
        switch (textAnchor) {
            case TXAN_MIDDLE: {
                AffineTransform at = new AffineTransform();
                at.translate(-textPath.getBounds2D().getWidth() / 2, 0);
                textPath.transform(at);
                break;
            }
            case TXAN_END: {
                AffineTransform at = new AffineTransform();
                at.translate(-textPath.getBounds2D().getWidth(), 0);
                textPath.transform(at);
                break;
            }
        }
    }

    private void buildSysFont(java.awt.Font font) {
        GeneralPath textPath = new GeneralPath();
        textShape = textPath;
        float cursorX = x, cursorY = y;
        FontRenderContext frc = new FontRenderContext(null, true, true);
        for (Iterator i = content.iterator(); i.hasNext();) {
            Object obj = i.next();
            if (obj instanceof String) {
                String text = (String)obj;
                Shape textShape = font.createGlyphVector(frc, text).getOutline(cursorX, cursorY);
                textPath.append(textShape, false);
                Rectangle2D rect = font.getStringBounds(text, frc);
                cursorX += (float)rect.getWidth();
            } else if (obj instanceof Tspan) {
                Tspan tspan = (Tspan)obj;
                tspan.setCursorX(cursorX);
                tspan.setCursorY(cursorY);
                tspan.addShape(textPath);
                cursorX = tspan.getCursorX();
                cursorY = tspan.getCursorY();
            }
        }
        switch (textAnchor) {
            case TXAN_MIDDLE: {
                AffineTransform at = new AffineTransform();
                at.translate(-textPath.getBounds2D().getWidth() / 2, 0);
                textPath.transform(at);
                break;
            }
            case TXAN_END: {
                AffineTransform at = new AffineTransform();
                at.translate(-textPath.getBounds2D().getWidth(), 0);
                textPath.transform(at);
                break;
            }
        }
    }

    void render(Graphics2D g) {
        beginLayer(g);
        renderShape(g, textShape);
        finishLayer(g);
    }

    Shape getShape() {
        return shapeToParent(textShape);
    }

    Rectangle2D getBoundingBox() {
        return boundsToParent(includeStrokeInBounds(textShape.getBounds2D()));
    }

    static class Tspan extends SVG.ShapeElement {

        private float[] x;
        private float[] y;
        private float[] dx;
        private float[] dy;
        private float[] rotate;
        private StringBuffer text = new StringBuffer();
        private float cursorX;
        private float cursorY;

        float getCursorX() {
            return cursorX;
        }

        float getCursorY() {
            return cursorY;
        }

        void setCursorX(float cursorX) {
            this.cursorX = cursorX;
        }

        void setCursorY(float cursorY) {
            this.cursorY = cursorY;
        }

        void loaderAddText(SVG svg, String s) {
            text.append(s);
        }

        protected void build() {
            super.build();
            x = getFloats("x");
            y = getFloats("y");
            dx = getFloats("dx");
            dy = getFloats("dy");
            rotate = getFloats("rotate");
            for (int i = 0; rotate != null && i < this.rotate.length; i++) {
                rotate[i] = (float)Math.toRadians(this.rotate[i]);
            }
        }

        void addShape(GeneralPath addShape) {
            if (x != null) {
                cursorX = x[0];
                cursorY = y == null ? 0 : y[0];
            } else if (dx != null) {
                cursorX += dx[0];
                cursorY += dy == null ? 0 : dy[0];
            }
            String fontFamily = getStyleString("font-family");
            float fontSize = getStyleFloatUnits("font-size", 12f);
            SVG.Font font = getRoot().getFont(fontFamily);
            if (font == null) {
                addShapeSysFont(addShape, fontFamily, fontSize);
                return;
            }
            SVG.FontFace fontFace = font.getFontFace();
            int ascent = fontFace.getAscent();
            float fontScale = fontSize / (float)ascent;
            AffineTransform xform = new AffineTransform();
            setStrokeWidthScalar(1f / fontScale);
            int posPtr = 1;
            for (int i = 0; i < text.length(); i++) {
                xform.setToIdentity();
                xform.setToTranslation(cursorX, cursorY);
                xform.scale(fontScale, fontScale);
                if (rotate != null) {
                    xform.rotate(rotate[posPtr]);
                }
                String unicode = text.substring(i, i + 1);
                SVG.MissingGlyph glyph = font.getGlyph(unicode);
                Shape path = glyph.getPath();
                if (path != null) {
                    path = xform.createTransformedShape(path);
                    addShape.append(path, false);
                }
                if (x != null && posPtr < x.length) {
                    cursorX = x[posPtr];
                    cursorY = y[posPtr++];
                } else if (dx != null && posPtr < dx.length) {
                    cursorX += dx[posPtr];
                    cursorY += dy[posPtr++];
                }
                cursorX += fontScale * glyph.getHorizAdvX();
            }
            setStrokeWidthScalar(1f);
        }

        private void addShapeSysFont(GeneralPath addShape, String fontFamily, float fontSize) {
            java.awt.Font sysFont = new java.awt.Font(fontFamily, java.awt.Font.PLAIN, (int)fontSize);
            FontRenderContext frc = new FontRenderContext(null, true, true);
            GlyphVector textVector = sysFont.createGlyphVector(frc, text.toString());
            AffineTransform xform = new AffineTransform();
            int posPtr = 1;
            for (int i = 0; i < text.length(); i++) {
                xform.setToIdentity();
                xform.setToTranslation(cursorX, cursorY);
                if (rotate != null) {
                    xform.rotate(rotate[Math.min(i, rotate.length - 1)]);
                }
                Shape glyphOutline = textVector.getGlyphOutline(i);
                glyphOutline = xform.createTransformedShape(glyphOutline);
                addShape.append(glyphOutline, false);
                if (x != null && posPtr < x.length) {
                    cursorX = x[posPtr];
                    cursorY = y[posPtr++];
                } else if (dx != null && posPtr < dx.length) {
                    cursorX += dx[posPtr];
                    cursorY += dy[posPtr++];
                }
            }
        }

        void render(Graphics2D g) {
            if (x != null) {
                cursorX = x[0];
                cursorY = y[0];
            } else if (dx != null) {
                cursorX += dx[0];
                cursorY += dy[0];
            }
            String fontFamily = getString("font-family");
            float fontSize = getFloatUnits("font-size", 12f);
            SVG.Font font = getRoot().getFont(fontFamily);
            if (font == null) {
//              System.err.println("Failed to load font: " + fontFamily);
                java.awt.Font sysFont = new java.awt.Font(fontFamily, java.awt.Font.PLAIN, (int)fontSize);
                renderSysFont(g, sysFont);
                return;
            }
            SVG.FontFace fontFace = font.getFontFace();
            int ascent = fontFace.getAscent();
            float fontScale = fontSize / (float)ascent;
            AffineTransform savedXform = g.getTransform();
            AffineTransform xform = new AffineTransform();
            setStrokeWidthScalar(1f / fontScale);
            int posPtr = 1;
            for (int i = 0; i < text.length(); i++) {
                xform.setToTranslation(cursorX, cursorY);
                xform.scale(fontScale, fontScale);
                g.transform(xform);
                String unicode = text.substring(i, i + 1);
                SVG.MissingGlyph glyph = font.getGlyph(unicode);
                Shape path = glyph.getPath();
                if (path != null) {
                    renderShape(g, path);
                } else {
                    glyph.render(g);
                }
                if (x != null && posPtr < x.length) {
                    cursorX = x[posPtr];
                    cursorY = y[posPtr++];
                } else if (dx != null && posPtr < dx.length) {
                    cursorX += dx[posPtr];
                    cursorY += dy[posPtr++];
                }
                cursorX += fontScale * glyph.getHorizAdvX();
                g.setTransform(savedXform);
            }
            setStrokeWidthScalar(1f);
        }

        protected void renderSysFont(Graphics2D g, java.awt.Font font) {
            String s = text.toString();
            FontRenderContext frc = g.getFontRenderContext();
            Shape textShape = font.createGlyphVector(frc, s).getOutline(cursorX, cursorY);
            renderShape(g, textShape);
            Rectangle2D rect = font.getStringBounds(s, frc);
            cursorX += (float)rect.getWidth();
        }

        Shape getShape() {
            return null;
        }

        Rectangle2D getBoundingBox() {
            return null;
        }

        String getText() {
            return text.toString();
        }

    }

}
