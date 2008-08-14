package org.jezve.test;

import org.jezve.svg.*;
import org.jezve.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

public class SVGTest {

    private static ArrayList files = new ArrayList();
    private static int fco;
    private static final int W = 64; // 640;
    private static final int H = 64; // 480;
    private static int counter;
    private static long maxBaselineMemory;
    private static long maxLoadMemory;
    private static long maxRenderMemory;
    private static long maxLoadTime;
    private static long maxRenderTime;
    private static long sumLoadTime;
    private static long sumRenderTime;

    private static class SVGPane extends JComponent {

        private static BufferedImage render(int w, int h, SVG svg) {
            BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D g2d = (Graphics2D)bi.getGraphics();
            try {
                // http://www.w3.org/TR/SVG/coords.html#ViewBoxAttribute
                Root r = svg.getRoot();
                Rectangle2D viewBox = r.getViewBox();
                Rectangle2D bbox = r.getBoundingBox();
                float sx = r.getX();
                float sy = r.getY();
                float sw = r.getWidth();
                float sh = r.getHeight();
                if (sw == 0 || sh == 0) {
                    sx = (float)bbox.getX();
                    sw = (float)bbox.getWidth();
                    sy = (float)bbox.getX();
                    sh = (float)bbox.getHeight();
                }
                if (viewBox == null) {
                    viewBox = new Rectangle2D.Float(sx, sy, sw, sh);
                }
                double xscale = w / viewBox.getWidth();
                double yscale = h / viewBox.getHeight();
                AffineTransform sc;
                if (r.getParAlignX() == Root.PA_X_NONE || r.getParAlignY() == Root.PA_Y_NONE) {
                    assert r.getParAlignX() == Root.PA_X_NONE && r.getParAlignY() == Root.PA_Y_NONE;
                    sc = AffineTransform.getScaleInstance(xscale, yscale);
                } else {
                    double scale = r.getMeetOrSlice() == Root.PS_MEET ?
                            Math.min(xscale, yscale) : Math.max(xscale, yscale);
                    sc = AffineTransform.getScaleInstance(scale, scale);
                    double dx = w / scale - viewBox.getWidth();
                    double dy = h / scale - viewBox.getHeight();
                    if (r.getParAlignX() == Root.PA_X_MIN) {
                        dx = 0;
                    } else if (r.getParAlignX() == Root.PA_X_MID) {
                        dx = dx / 2;
                    } else {
                        // right aligned, keep dx
                    }
                    if (r.getParAlignY() == Root.PA_Y_MIN) {
                        dy = 0;
                    } else if (r.getParAlignY() == Root.PA_Y_MID) {
                        dy = dy / 2;
                    } else {
                        // right aligned, keep dx
                    }
                    sc.concatenate(AffineTransform.getTranslateInstance(dx - viewBox.getX(), dy - viewBox.getY()));
                }
/*
                System.out.println("boundingBox=" + r.getBoundingBox() + " viewBox=" + r.getViewBox() +
                        " box=" + new Rectangle2D.Float(sx, sy, sw, sh));
*/
                long time = Time.microseconds();
                g2d.setTransform(sc);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                svg.render(g2d);
                time = Time.microseconds() - time;
                long used3 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                maxRenderMemory = Math.max(used3, maxRenderMemory);
                System.out.println("rendered(" + fco + "): " + time + " usecs.");
                sumRenderTime += time;
                maxRenderTime = Math.max(maxRenderTime, time);
            } finally {
                g2d.dispose();
                g2d = null;
            }
            assert g2d == null;
            return bi;
        }

        private static BufferedImage getImage() {
            long used1 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            maxBaselineMemory = Math.max(used1, maxBaselineMemory);
            try {
                String fileName = null;
                if (fco < files.size()) {
                    fileName = ((File)files.get(fco)).getAbsolutePath();
                }
                if (fileName == null || !fileName.toLowerCase().endsWith(".svg") && !fileName.toLowerCase().endsWith(".svgz")) {
                    return null;
                }
                System.out.println("loading(" + fco + "): " + fileName);
                long time = Time.microseconds();
                InputStream is = new FileInputStream(new File(fileName));
                SVG svg = SVG.read(is);
                if (svg != null) {
                    time = Time.microseconds() - time;
                    System.out.println("loaded: " + time + " usecs.");
                    sumLoadTime += time;
                    maxLoadTime = Math.max(maxLoadTime, time);
                    counter++;
                    long used2 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                    maxLoadMemory = Math.max(used2, maxLoadMemory);
                    return render(W, H, svg);
                } else {
                    return null;
                }
            } catch (IOException e) {
                if (e.getCause() != null) {
                    e.getCause().printStackTrace();
                } else {
                    e.printStackTrace();
                }
                return null;
            }
        }

        public void paint(Graphics g) {
            Graphics2D g2d = (Graphics2D)g;
            BufferedImage img = getImage();
            if (img != null) {
                g2d.drawImage(img, 1, 1, null);
                g2d.setColor(Color.RED);
                g2d.draw(new Rectangle(0, 0, W + 2, H + 2));
            }
            System.gc();
            Misc.invokeLater(1, new Runnable() {
                public void run() {
                    fco++;
                    while (fco < files.size()) {
                        String fn = ((File)files.get(fco)).getName().toLowerCase();
                        if (fn.endsWith(".svgz") || fn.endsWith(".svg")) {
                            break;
                        }
                        fco++;
                    }
                    if (fco < files.size()) {
                        repaint();
                    } else {
                        System.out.println(counter + " files:\n" +
                                "\tmax load time " +
                                maxLoadTime / 1000 + " max render time " + maxRenderTime / 1000 +
                                " avg load: " + sumLoadTime / (1000 * counter) +
                                " avg render: " + sumRenderTime / (1000 * counter) +
                                " milliseconds. " +
                                "\n\tmemory used: baseline " + (maxBaselineMemory / 1024) +
                                " load " + (maxLoadMemory / 1024) +
                                " render " + (maxRenderMemory / 1024) +
                                " KB. "
                        );
                    }
                }
            });
        }

        public void update(Graphics g) {
            super.update(g);
        }

    }


    private static Collection getFiles(File dir) {
        assert dir.isDirectory();
        Collection files = new LinkedList();
        getFiles(dir, files);
        return files;
    }

    private static void getFiles(File dir, Collection files) {
        File[] ls = dir.listFiles();
        if (ls == null) {
            return;
        }
        for (int i = 0; i < ls.length; i++) {
            File f = ls[i];
            if (f.getName().startsWith(".")) {
                // do not go into hidden dirs
            } else if (f.isDirectory()) {
                getFiles(f, files);
            } else if (!skip(f.getAbsolutePath())) {
                String fname = f.getName().toLowerCase();
                if (!fname.startsWith(".") && (fname.endsWith(".svg") || fname.endsWith(".svgz"))) {
                    files.add(f);
                }
            }
        }
    }

    private final static String[] skip = new String[] {
            "liftarn/liftarn_children.svg", // bad UTF-8
            "recreation/religion/christianity/coat_of_arms_of_anglica_01.svg", // xml 1 versus 1.0 not supported
    };

    private static boolean skip(String fname) {
        for (int i = 0; i < skip.length; i++) {
            if (fname.toLowerCase().endsWith(skip[i].toLowerCase())) {
                return true;
            }
        }
        return false;
    }


    private static class Test extends JFrame {
        private Test() {
            getContentPane().add(new SVGPane());
            setSize(W + 32, H + 48);
            setVisible(true);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            File cd = new File(new File(".").getAbsolutePath());
            while (cd != null) {
                if (new File(cd, "test").isDirectory()) {
                    cd = new File(cd, "test");
                    break;
                }
                cd = cd.getParentFile();
            }
            if (cd == null) {
                cd = new File(System.getProperty("user.home"), "code/svg_test/test");
            }
            files.addAll(getFiles(cd));
            if (files.size() == 0) {
                cd = new File(System.getProperty("user.home"), "code/svg_test/test");
                files.addAll(getFiles(cd));
            }
/*
            files.addAll(getFiles(new File(System.getProperty("user.home"), "Desktop/Oxygen-pics-runtime-kdebase")));
            files.addAll(getFiles(new File(System.getProperty("user.home"), "Desktop/2008_May_daily_SVG_snapshot")));
            files.addAll(getFiles(new File(System.getProperty("user.home"), "Desktop/openclipart-0.18-svgonly")));
*/
            System.out.println(files.size() + " to go");
            fco = 0;
        }

        public void update(Graphics g) {
            super.update(g);
        }

    }

    public static void main(String[] args) {
        if (Platform.isMac()) {
            MacOSX.setSystemProperties("SVGText", true);
            System.setProperty("apple.awt.graphics.UseQuartz", "false"); // workaround for:
            // http://lists.apple.com/archives/Java-dev/2007/Jun/msg00067.html
            // http://lists.apple.com/archives/java-dev/2005/Nov/msg00222.html
        }
        // Parser.test();
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Test();
            }
        });
    }

}
