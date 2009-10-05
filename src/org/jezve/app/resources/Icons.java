/**
* Copyright (c) 2007-2008, jezve.org and its Contributors
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*     * Redistributions of source code must retain the above copyright
*       notice, this list of conditions and the following disclaimer.
*     * Redistributions in binary form must reproduce the above copyright
*       notice, this list of conditions and the following disclaimer in the
*       documentation and/or other materials provided with the distribution.
*     * Neither the name of the jezve.org nor the
*       names of its contributors may be used to endorse or promote products
*       derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY jezve.org AND SOFTWARE CONTRIBUTORS ``AS IS'' AND ANY
* EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL jezve.org or CONTRIBUTORS BE LIABLE FOR ANY
* DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
* LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.jezve.app.resources;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;


public final class Icons {

    public static ImageIcon getImageIcon(String name) {
        return new ImageIcon(getImage(name));
    }

    public static boolean hasImageIcon(String name) {
        return  Icons.class.getResource(name + ".png") != null;
    }

    public static ImageIcon getImageIcon(String name, int size) {
        return new ImageIcon(getImage(name, size));
    }

    public static Image getImage(String name, int size) {
        Image image = getImage(name);
        return ensureImage(image.getScaledInstance(size, size, Image.SCALE_AREA_AVERAGING));
    }

    public static Image getImage(String name) {
        return getImage(name, "png");
    }

    public static Image getImage(String name, String ext) {
        String location = name + "." + ext;
        return ensureImage(Toolkit.getDefaultToolkit().createImage(Resources.readBytes(location)));
    }

    private static Image ensureImage(Image image) {
        MediaTracker mt = new MediaTracker(new JComponent() {});
        mt.addImage(image, 0);
        try {
            mt.waitForID(0);
        } catch (InterruptedException e) {
            /* ignore */
        } finally {
            mt.removeImage(image);
        }
        assert image.getWidth(null) > 0 : image.getWidth(null);
        assert image.getHeight(null) > 0 : image.getHeight(null);
        return image;
    }

    public static String getUrl(String name) {
        String location = "./" + name + ".png";
        java.net.URL url = Icons.class.getResource(location);
        assert  url != null : location;
        return url.toExternalForm();
    }

    public static byte[] getBytes(String name) {
        String location = name + ".png";
        return Resources.readBytes(location);
    }

    private Icons() { }

    public static BufferedImage asBufferedImage(Image img) {
        return asBufferedImage(img, 0, 0);
    }

    public static BufferedImage asBufferedImage(Image img, int dx, int dy) {
        assert img.getWidth(null) > 0 : img.getWidth(null);
        assert img.getHeight(null) > 0 : img.getHeight(null);
        int w = img.getWidth(null) + Math.max(dx, 0);
        int h = img.getHeight(null) + Math.max(dy, 0);
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics g = null;
        try {
            g = bi.getGraphics();
            g.drawImage(img, dx, dy, null);
        } finally {
            if (g != null) {
                g.dispose();
            }
        }
        return bi;
    }

    /** adjust Saturation (i == 1) or Brightness (i == 2)
     * @param bi image to adjust parameters of (must be ABGR
     * @param adjust value to adjust must be > 0.0f
     * @param i index of hsb to adjust
     */
    public static void adjustHSB(BufferedImage bi, float adjust, int i) {
        assert bi.getType() == BufferedImage.TYPE_INT_ARGB : "must be TYPE_INT_ARGB";
        int n = bi.getData().getNumBands();
        assert n == 4 : "must have alpha component";
        assert adjust > 0.0f;
        assert i > 0 : "adjusting hue is strange action with unpredictable color shift";
        int[] pixels = new int[bi.getWidth() * bi.getHeight() * n];
        float[] hsb = new float[3];
        bi.getData().getPixels(0, 0, bi.getWidth(), bi.getHeight(), pixels);
        int ix = 0;
        WritableRaster wr = bi.getRaster();
        for (int y = 0; y < bi.getHeight(); y++) {
            for (int x = 0; x < bi.getWidth(); x++) {
                int r = pixels[ix];
                int g = pixels[ix + 1];
                int b = pixels[ix + 2];
                int a = pixels[ix + 3];
                Color.RGBtoHSB(r, g, b, hsb);
                assert hsb[0] >= 0 && hsb[1] >= 0 && hsb[2] >= 0;
                hsb[i] = Math.max(0.0f, Math.min(hsb[i] * adjust, 1.0f));
                int c = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
                wr.getDataBuffer().setElem(ix / n, (c & 0xFFFFFF) | (a << 24));
                ix += n;
            }
        }
    }

}
