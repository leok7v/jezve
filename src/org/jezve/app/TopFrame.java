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

package org.jezve.app;

import org.jezve.util.Platform;
import org.jezve.util.Misc;
import org.jezve.util.MacOSX;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.io.File;

public final class TopFrame extends JFrame {

    private static final Dimension SMALLEST = new Dimension(340, 240);
    private static ArrayList frames = new ArrayList();
    private static JFrame offscreen; // menu holder for Mac. see setVisible
    private static TopFrame active;
    private static Robot robot;
    private TopFrameAdapter frameListenner;
    private ComponentListener componentListenner;
    private MinimumSizeLimiter limiter;

    static {
        try { robot = new Robot(); } catch (AWTException e) { robot = null; }
    }

    TopFrame() {
        if (Platform.isMac() && Platform.getOsVersion() >= 10.5) {
            // http://developer.apple.com/technotes/tn2007/tn2196.html
            JRootPane rp = new JRootPane();
            rp.putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
            rp.putClientProperty("apple.awt.brushMetalRounded", Boolean.TRUE);
            setRootPaneCheckingEnabled(true);
            setRootPane(rp);
        }
        setJMenuBar(Commands.createMenuBar());
        frames.add(this);
        String key = "frame." + getFrameIndex() + ".";
        int x = UserSettings.getInt(key + "x", -1);
        int y = UserSettings.getInt(key + "y", -1);
        int w = UserSettings.getInt(key + "width", -1);
        int h = UserSettings.getInt(key + "height", -1);
        if (x >= 0 && y >= 0 && w >= 0 && h >= 0) {
            setBounds(x, y, w, h);
        } else {
            super.setSize(SMALLEST.width * 2, SMALLEST.height * 2);
            if (active != null) {
                if ((active.getExtendedState() & ICONIFIED) == 0) {
                    Point p = active.getLocation();
                    setLocation(p.x + 32, p.y + 32);
                }
            }
        }
        ContentPane cp = new ContentPane();
        setContentPane(cp);
        Color transparent = new Color(255, 255, 255, 255);
        setBackground(transparent);
        cp.setBackground(transparent);
        cp.setOpaque(false);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setIconImage(Icons.getImage("jezve"));
        super.setTitle(Main.APPLICATION);
        limiter = new MinimumSizeLimiter();
        frameListenner = new TopFrameAdapter();
        componentListenner = new ComponentAdapter() {
            public void componentResized(ComponentEvent e) { resized(); }
            public void componentMoved(ComponentEvent e) { resized(); }
        };
        applyComponentOrientation(this,
                ComponentOrientation.getOrientation(Locale.getDefault()));
    }

    public void setTitle(String s) {
        if (s == null || "".equals(s)) {
            s = Main.APPLICATION;
        } else {
            s = Main.APPLICATION + ": " + s;
        }
        if (!s.equals(super.getTitle())) {
            super.setTitle(s);
        }
    }

    public void setDocument(File file) {
        // http://developer.apple.com/technotes/tn2007/tn2196.html
        if (Platform.isMac()) {
            getRootPane().putClientProperty("Window.documentFile", file);
        }
    }

    public void setModified(boolean b) {
        // http://developer.apple.com/technotes/tn2007/tn2196.html
        if (Platform.isMac()) {
            getRootPane().putClientProperty("Window.documentModified", Boolean.valueOf(b));
        }
    }

    public static TopFrame getActiveFrame() {
        return active;
    }

    public int getFrameIndex() {
        int n = frames.indexOf(this);
        assert n >= 0 : "frame is not in frames: " + this;
        return n;
    }

    public static void repaintMenuBars() {
        for (Iterator i = frames.iterator(); i.hasNext(); ) {
            TopFrame f = (TopFrame)i.next();
            f.getJMenuBar().repaint();
        }
        if (offscreen != null) {
            offscreen.getJMenuBar().repaint();
        }
    }

    public static void setEnabledAll(boolean b) {
        for (Iterator i = frames.iterator(); i.hasNext(); ) {
            TopFrame f = (TopFrame)i.next();
            f.setEnabled(b);
        }
        if (offscreen != null) {
            offscreen.setEnabled(b);
        }
    }

    public static void setNewLocale(Locale loc) {
        if (!Locale.getDefault().equals(loc)) {
            Locale.setDefault(loc);
            JComponent.setDefaultLocale(loc);
            Commands.reset();
            for (Iterator i = frames.iterator(); i.hasNext(); ) {
                TopFrame f = (TopFrame)i.next();
                if (!f.getLocale().equals(loc)) {
                    f.setLocale(loc);
                    f.setJMenuBar(Commands.createMenuBar());
                    ContentPane cp = (ContentPane)f.getContentPane();
                    cp.createToolbar();
                }
            }
            adjustComponentOrientation();
            repaintMenuBars();
        }
    }

    private static void adjustComponentOrientation() {
        for (Iterator i = frames.iterator(); i.hasNext(); ) {
            TopFrame f = (TopFrame)i.next();
            applyComponentOrientation(f, ComponentOrientation.getOrientation(Locale.getDefault()));
            f.validate();
        }
    }

    private void resized() {
        if (!isVisible() || (getExtendedState() & ICONIFIED) != 0) {
            return;
        }
        if (Platform.isMac()) {
            getRootPane().putClientProperty("apple.awt.windowShadow.revalidateNow",
                                            new Double(Math.random()));
        }
        String key = "frame." + getFrameIndex() + ".";
        Rectangle b = getBounds();
        UserSettings.putInt(key + "x", b.x);
        UserSettings.putInt(key + "y", b.y);
        UserSettings.putInt(key + "width", b.width);
        UserSettings.putInt(key + "height", b.height);
        UserSettings.flush();
    }

    public void addNotify() {
        super.addNotify();
        Events.addListener(this);
        addWindowListener(frameListenner);
        addWindowFocusListener(frameListenner);
        addWindowStateListener(frameListenner);
        addComponentListener(componentListenner);
        getContentPane().addComponentListener(limiter);
        if (offscreen != null) {
            offscreen.setVisible(false);
        }
    }

    public void removeNotify() {
        getContentPane().removeComponentListener(limiter);
        removeComponentListener(componentListenner);
        removeWindowStateListener(frameListenner);
        removeWindowFocusListener(frameListenner);
        removeWindowListener(frameListenner);
        Events.removeListener(this);
        super.removeNotify();
    }

    public Insets getInsets() {
        Insets i = super.getInsets();
        if (Platform.isMac() && Platform.getOsVersion() < 10.5 && MacOSX.isMetal()) {
            i.left += 6;
            i.right += 6;
        }
        return i;
    }

    private void hostMenuOffscreen() {
        if (offscreen == null) {
            offscreen = new JFrame();
            offscreen.setUndecorated(true);
            offscreen.setLocation(Integer.MIN_VALUE, Integer.MIN_VALUE);
            offscreen.setSize(0, 0);
            offscreen.setEnabled(false);
            offscreen.setJMenuBar(Commands.createMenuBar());
        }
        offscreen.setVisible(true);
    }

    private static void applyComponentOrientation(Component c, ComponentOrientation o) {
        c.setComponentOrientation(o);
        if (c instanceof JMenu) {
            JMenu menu = (JMenu)c;
            int ncomponents = menu.getMenuComponentCount();
            for (int i = 0; i < ncomponents; ++i) {
                applyComponentOrientation(menu.getMenuComponent(i), o);
            }
        } else if (c instanceof Container) {
            Container container = (Container)c;
            int ncomponents = container.getComponentCount();
            for (int i = 0; i < ncomponents; ++i) {
                applyComponentOrientation(container.getComponent(i), o);
            }
        }
    }

    private final class ContentPane extends JPanel {

        JToolBar tb;

        ContentPane() {
            super(new BorderLayout());
            createToolbar();
            add(new StatusBar(), BorderLayout.SOUTH);
            add(Misc.createScrollPane(new Content()), BorderLayout.CENTER);
        }

        public void addNotify() {
            super.addNotify();
            Events.addListener(this);
        }

        public void removeNotify() {
            Events.removeListener(this);
            super.removeNotify();
        }

        public void createToolbar() {
            if (tb != null) {
                remove(tb);
            }
            tb = Commands.createToolBar(22);
            add(tb, BorderLayout.NORTH);
        }

    }

    private class TopFrameAdapter extends WindowAdapter implements WindowFocusListener, WindowStateListener {

        public void windowClosing(WindowEvent e) {
            frames.remove(TopFrame.this);
            Events.postEvent("commandWindowClose");
            if (Platform.isWindows() && frames.size() == 0) {
                Events.postEvent("commandFileExit");
            } else if (Platform.isMac() && frames.size() == 0) {
                hostMenuOffscreen();
            }
            if (active == TopFrame.this) {
                active = null;
            }
            dispose();
        }

        public void windowGainedFocus(WindowEvent e) {
            active = TopFrame.this;
//          Debug.traceln("gained focus " + e);
        }

        public void windowLostFocus(WindowEvent e) {
//          Debug.traceln("lost focus " + e);
        }

        public void windowStateChanged(WindowEvent e) {
//          Debug.traceln("state changed " + e);
        }

    }

    private class MinimumSizeLimiter extends ComponentAdapter {

        public void componentResized(ComponentEvent e) {
            Dimension size = getSize();
            if (size.width < SMALLEST.width || size.height < SMALLEST.height) {
                boolean limit = false;
                if (size.width < SMALLEST.width) {
                    size.width = SMALLEST.width;
                    limit = true;
                }
                if (size.height < SMALLEST.height) {
                    size.height = SMALLEST.height;
                    limit = true;
                }
                if (!limit || size.equals(getSize())) {
                    return;
                }
                setResizable(false);
                if (robot != null) {
                    robot.mouseRelease(InputEvent.BUTTON1_MASK |
                            InputEvent.BUTTON2_MASK |
                            InputEvent.BUTTON3_MASK);
                }
                final Dimension s = size;
                Misc.invokeLater(100, new Runnable() {
                    public void run() {
                        setResizable(true);
                        setSize(s);
                    }
                });
            }
        }
    }

}
