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

import org.jezve.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public final class StatusBar extends JComponent {

    private JLabel status;
    private final int height;

    public StatusBar() {
        setLayout(new BorderLayout());
        if (!Platform.isMac()) {
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLoweredBevelBorder(), this.getBorder()));
        } else {
            setBorder(null);
            setOpaque(true);
        }
        setFocusable(false);
        status = new JLabel("status bar", SwingConstants.LEADING);
        height = (int)(getFontMetrics(status.getFont()).getHeight() * (Platform.isMac() ? 1.5 : 1.7));
        status.setAlignmentY(BOTTOM_ALIGNMENT);
        status.setOpaque(false);
        add(status, BorderLayout.CENTER);
        add(new GrowBox(), BorderLayout.EAST);
        setDoubleBuffered(false);
    }

    public void addNotify() {
        super.addNotify();
        Events.addListener(this);
    }

    public void removeNotify() {
        Events.removeListener(this);
        super.removeNotify();
    }

    public Insets getInsets() {
        Insets i = super.getInsets();
        if (Platform.isMac()) {
            i.top += 1;
            i.bottom += 2;
            i.left += 6;
        }
        return i;
    }

    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, height);
    }

    /** set the text into status panel of status bar
     * Do not call directly from background threads.
     * Use Actions.postEvent("setInfo", "text") instead.
     * @param param message. plain text or html
     */

    public void setStatus(Object param) {
        assert EventQueue.isDispatchThread();
        String s = (String)param;
        if (!Misc.equals(s, status.getText())) {
            status.setText(s);
        }
    }

    private class GrowBox extends JComponent {

        private final Cursor SE_RESIZE_CURSOR = new Cursor(Cursor.SE_RESIZE_CURSOR);
        private final Color b = new JLabel().getBackground();
        private final Color[] lines = Platform.isWindows() ? new Color[]{b.brighter(), b.darker()} :
                new Color[]{Color.DARK_GRAY, Color.WHITE, Color.LIGHT_GRAY, Color.GRAY};

        private final Dragger dragger = new Dragger();

        private class Dragger extends MouseAdapter implements MouseMotionListener {

            private Point pt = null;

            public void mouseDragged(MouseEvent e) {
                if (pt != null) {
                    Point p = getScreenLocation(e);
                    JFrame frame = getJFrame();
                    Dimension size = frame.getSize();
                    frame.setSize(size.width + (p.x - pt.x), size.height + (p.y - pt.y));
                    pt = p;
                }
            }

            public void mousePressed(MouseEvent e) {
                pt = getScreenLocation(e);
            }

            public void mouseReleased(MouseEvent e) {
                pt = null;
            }

            private JFrame getJFrame() {
                Container c = getParent();
                while (!(c instanceof JFrame)) {
                    c = c.getParent();
                }
                return (JFrame)c;
            }

            private Point getScreenLocation(MouseEvent e) {
                Point c = e.getPoint();
                Point p = GrowBox.this.getLocationOnScreen();
                return new Point(p.x + c.x, p.y + c.y);
            }

            public void mouseMoved(MouseEvent e) { }
        }

        GrowBox() {
            if (Platform.isMac()) {
                setOpaque(true);
            }
            setDoubleBuffered(false);
        }

        public void addNotify() {
            super.addNotify();
            addMouseListener(dragger);
            addMouseMotionListener(dragger);
        }

        public void removeNotify() {
            removeMouseMotionListener(dragger);
            removeMouseListener(dragger);
            super.removeNotify();
        }

        public void paint(Graphics g) {
            super.paint(g);
            Graphics2D g2d = (Graphics2D)g;
            if (Platform.isMac()) {
                // system grow box was broken prio Mac OS X and java 1.5
                // in 10.5 value of System.setProperty("apple.awt.showGrowBox", "false");
                // is ignored and growbox always intrudes and is painted...
                if (Platform.getOsVersion() < 10.5 && MacOSX.isMetal()) {
                    g2d.setColor(Color.RED);
                    g2d.fill(getBounds());
                    int x = 0;
                    int y = height - 21;
                    for (int i = 0; i < 15; i++) {
                        Color c = lines[i % 4];
                        Color s = g2d.getColor();
                        g2d.setColor(c);
                        g2d.drawLine(x + i, y + 14, x + 14, y + i);
                        g2d.setColor(s);
                    }
                }
            } else {
                int n = getWidth() - 1;
                int y = 0;
                Color s = g2d.getColor();
                for (int dy = 0; dy < n; dy += 4) {
                    for (int dx = dy; dx > 0; dx -= 4) {
                        g2d.setColor(lines[0]);
                        g2d.fill(new Rectangle(n - dx + 1, y + dy + 1, 2, 2));
                        g2d.setColor(lines[1]);
                        g2d.fill(new Rectangle(n - dx, y + dy, 2, 2));
                    }
                }
                g2d.setColor(s);
            }
        }

        public Dimension getPreferredSize() {
            return new Dimension(16, height);
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        public Cursor getCursor() {
            return Platform.isWindows() ? SE_RESIZE_CURSOR : super.getCursor();
        }
    }

}
