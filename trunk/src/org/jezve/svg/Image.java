package org.jezve.svg;

import org.jezve.svg.batik.GraphicsUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;

public class Image extends SVG.RenderableElement {

    private float x;
    private float y;
    private float width;
    private float height;
    private AffineTransform xform = GraphicsUtil.IDENTITY;
    private Rectangle2D bounds = GraphicsUtil.EMPTY_BOX;
    private BufferedImage img;
    private boolean isVisible;
    private float opacity = 1f;

    protected void build() {
        super.build();
        x = getFloatUnits("x", 0);
        y = getFloatUnits("y", 0);
        width = getFloatUnits("width", 0);
        height = getFloatUnits("height", 0);
        try {
            String uri = getStyleHref();
            if (uri != null) {
                String prefix1 = "data:image/png;base64,";
                String prefix2 = "data:image/jpeg;base64,";
//              String prefix3 = "data:;base64,"; // rarely seen and usually results in corrupted images
                int len1 = Math.min(uri.length(), prefix1.length());
                int len2 = Math.min(uri.length(), prefix2.length());
                String s1 = uri.substring(0, len1).toLowerCase();
                String s2 = uri.substring(0, len2).toLowerCase();
                if (s1.startsWith(prefix1) || s2.startsWith(prefix2)) {
                    String data = s1.startsWith(prefix1) ?
                            uri.substring(prefix1.length()) : uri.substring(prefix2.length());
                    InputStream si = new ByteArrayInputStream(data.getBytes());
                    InputStream is = new B64InputStream(si);
                    if (s1.startsWith(prefix1)) {
                        img = ImageIO.read(is);
                    } else {
                        img = null; // image reads OK but paints really wierd
                    }
                    is.close();
                } else {
//                  System.err.println("WARNING: external references not implemented: " + uri);
                    return;
                }
            }
        } catch (IOException e) {
            throw new Error(e);
        }
        if (img == null) {
            return;
        }
        if (width == 0) {
            width = img.getWidth();
        }
        if (height == 0) {
            height = img.getHeight();
        }
        // Determine image xform
        xform = AffineTransform.getTranslateInstance(this.x, this.y);
        xform.scale(this.width / img.getWidth(), this.height / img.getHeight());
        bounds = new Rectangle2D.Float(this.x, this.y, this.width, this.height);
        String v = getString("visibility");
        isVisible = v == null || !"visible".equalsIgnoreCase(v);
        opacity = getStyleRatioValue("opacity", 1f);
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public void render(Graphics2D g) {
        if (!isVisible) {
            return;
        }
        if (opacity <= 0) {
            return;
        }
        beginLayer(g);
        Composite saveComp = null;
        if (opacity < 1) {
            saveComp = g.getComposite();
            Composite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
            g.setComposite(comp);
        }
        if (img == null) {
            return;
        }
        AffineTransform curXform = g.getTransform();
        g.transform(xform);
        g.drawImage(img, 0, 0, null);
        g.setTransform(curXform);
        if (saveComp != null) {
            g.setComposite(saveComp);
        }
        finishLayer(g);
    }

    public Rectangle2D getBoundingBox() {
        return boundsToParent(bounds);
    }

    public static class B64InputStream extends InputStream {

        private static final String B64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        private InputStream is;
        private int state;
        private int buffer;

        public B64InputStream(InputStream input) {
            is = input;
            state = 0;
            buffer = 0;
        }

        public int available() {
            throw new UnsupportedOperationException();
        }

        public int read() throws IOException {
            throw new UnsupportedOperationException();
        }

        private static boolean isWhitespace(int ch) { // faster than Character.isWhitespace
            return ' ' == ch || '\n' == ch || '\r' == ch || '\t' == ch;
        }

        private int skipWhitespace() throws IOException {
            int i;
            while (isWhitespace(i = is.read())) { }
            return i;
        }

        public int read(byte[] bytes, int offset, int length) throws IOException {
            if (state == 5) { // eof
                return -1;
            }
            int count = 0;
            while (count < length) {
                int i = skipWhitespace();
                int ix = B64.indexOf((char)i);
                if (ix >= 0) {
                    switch (state) {
                        case 0:
                            buffer = ix << 2;
                            state = 1;
                            break;
                        case 1:
                            bytes[count++] = (byte)(buffer | (ix >>> 4));
                            buffer = (ix & 0x0F) << 4;
                            state = 2;
                            break;
                        case 2:
                            bytes[count++] = (byte)(buffer | (ix >>> 2));
                            buffer = (ix & 0x03) << 6;
                            state = 3;
                            break;
                        case 3:
                            bytes[count++] = (byte)(buffer | ix);
                            state = 0;
                            break;
                    }
                } else if (i == '=') {
                    switch (state) {
                        case 0:
                        case 1: throw new IOException("base64");
                        case 2:
                            if (skipWhitespace() != '=') {
                                throw new IOException("base64");
                            }
                        case 3:
                            skipWhitespace();
                    }
                    state = 5; // eof
                    break;
                } else {
                    if (state != 0) {
                        throw new IOException("base64");
                    }
                    state = 5; // eof
                    break;
                }
            }
            return count;
        }

        public boolean markSupported() {
            throw new UnsupportedOperationException();
        }

        public void mark(int markLimit) {
            throw new UnsupportedOperationException();
        }

        public void reset() throws IOException {
            throw new UnsupportedOperationException();
        }

        public long skip(long n) throws IOException {
            throw new UnsupportedOperationException();
        }

    }


}
