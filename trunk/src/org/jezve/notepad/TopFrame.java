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

package org.jezve.notepad;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

class TopFrame extends JFrame {

    private static final Dimension SMALLEST = new Dimension(340, 240);

    private ComponentListener componentListenner;
    private WindowAdapter frameListener;

    private TextView view;
    private String title;

    TopFrame(String title) {
        super(title);
        this.title = title;

        setJMenuBar(Actions.createMenuBar());

        JPanel content = new JPanel(new BorderLayout());
        view = new TextView();
        JScrollPane sp = new JScrollPane(view);
        sp.getViewport().setBackground(view.getBackground());
        sp.setBorder(null);
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        // BLT scroll mode will generally leaves garbage behind... and waste performance and memory
        sp.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        content.add(sp, BorderLayout.CENTER);

        setContentPane(content);
        Color transparent = new Color(255, 255, 255, 255);
        setBackground(transparent);
        content.setBackground(transparent);
        content.setOpaque(false);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setIconImage(Resources.getImage("notepad"));

        componentListenner = new ComponentAdapter() {
            public void componentResized(ComponentEvent e) { resized(); }
            public void componentMoved(ComponentEvent e) { resized(); }
        };

        frameListener = new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                Actions.postEvent("commandFileExit");
                dispose();
            }
        };

        int x = Notepad.user.getInt("frame.x", -1);
        int y = Notepad.user.getInt("frame.y", -1);
        int w = Notepad.user.getInt("frame.width", -1);
        int h = Notepad.user.getInt("frame.height", -1);
        if (x >= 0 && y >= 0 && w >= 0 && h >= 0) {
            // TODO: fit the bounds into monitor(s) shape
            setBounds(x, y, w, h);
        }
        else {
            super.setSize(SMALLEST.width * 2, SMALLEST.height * 2);
        }
    }

    private void resized() {
        if (!isVisible() || (getExtendedState() & ICONIFIED) != 0) {
            return;
        }
        Rectangle b = getBounds();
        Notepad.user.putInt("frame.x", b.x);
        Notepad.user.putInt("frame.y", b.y);
        Notepad.user.putInt("frame.width", b.width);
        Notepad.user.putInt("frame.height", b.height);
    }

    public void addNotify() {
        super.addNotify();
        Actions.addListener(this);
        addComponentListener(componentListenner);
        addWindowListener(frameListener);
    }

    public void removeNotify() {
        removeWindowListener(frameListener);
        removeComponentListener(componentListenner);
        Actions.removeListener(this);
        super.removeNotify();
    }

    public void updateCommandState(Map m) {
        m.put("commandViewZoomIn", Boolean.TRUE);
        m.put("commandViewZoomOut", Boolean.TRUE);
    }

    public void commandViewZoomIn() {
        view.setZoom(view.getZoom() * 1.1);
        validate();
        repaint();
    }

    public void commandViewZoomOut() {
        view.setZoom(view.getZoom() / 1.1);
        validate();
        repaint();
    }

    void updateTitle(String extra) {
        setTitle(title + " - " + extra);
    }

}
