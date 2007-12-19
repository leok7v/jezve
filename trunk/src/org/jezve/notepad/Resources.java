package org.jezve.notepad;

import java.awt.*;
import java.io.InputStream;
import java.io.IOException;
import javax.swing.*;

final class Resources {

    public static ImageIcon getImageIcon(String name) {
        return new ImageIcon(getImage(name));
    }

    public static boolean hasImageIcon(String name) {
        return  Resources.class.getResource("resources/" + name + ".png") != null;
    }

    public static ImageIcon getImageIcon(String name, int size) {
        return new ImageIcon(getImage(name, size));
    }

    public static Image getImage(String name, int size) {
        Image image = getImage(name);
        return image.getScaledInstance(size, size, Image.SCALE_AREA_AVERAGING);
    }

    public static Image getImage(String name) {
        return getImage(name, "png");
    }

    public static Image getImage(String name, String ext) {
        String location = "resources/" + name + "." + ext;
        return Toolkit.getDefaultToolkit().createImage(readBytes(location));
    }

    public static String getUrl(String name) {
        String location = "./resources/" + name + ".png";
        java.net.URL url = Resources.class.getResource(location);
        assert  url != null : location;
        return url.toExternalForm();
    }

    public static byte[] getBytes(String name) {
        String location = "./resources/" + name + ".png";
        return readBytes(location);
    }

    public static byte[] readBytes(String location) {
        InputStream s = null;
        try {
            s = Resources.class.getResourceAsStream(location);
            byte[] b = new byte[s.available()];
            int n = s.read(b);
            assert n == b.length;
            return b;
        }
        catch (IOException e) {
            throw new Error(e);
        }
        finally {
            if (s != null) {
                try { s.close(); } catch (IOException e) { /*ignore*/ }
            }
        }
    }

}
