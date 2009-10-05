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
import java.util.*;

@SuppressWarnings({"unchecked"})
public class Content extends JComponent {

    private boolean popupTrigger;
    private boolean hadFocus;

    private MouseListener mouseListener = new MouseAdapter() {

        public void mouseClicked(MouseEvent e) {
            requestFocus();
            if (popupTrigger && e.getClickCount() == 1) {
                showContextMenu(e.getPoint());
            } else {
                click(e);
            }
            popupTrigger = false;
        }

        public void mousePressed(MouseEvent e) {
            popupTrigger = e.isPopupTrigger();
        }

        public void mouseReleased(MouseEvent e) {
            if (!popupTrigger) {
                popupTrigger = e.isPopupTrigger();
            }
        }

    };

    private FocusListener focusListener = new FocusAdapter() {

        public void focusGained(FocusEvent e) {
//          Debug.traceln("focusGained");
        }

        public void focusLost(FocusEvent e) {
            hadFocus = e.isTemporary();
//          Debug.traceln("focusLost temporary=" + hadFocus);
        }
    };

    public Content() {
        setFocusable(true);
        setRequestFocusEnabled(true);
    }

    public void addNotify() {
        super.addNotify();
        Events.addListener(this);
        addMouseListener(mouseListener);
        addFocusListener(focusListener);
    }

    public void removeNotify() {
        removeFocusListener(focusListener);
        removeMouseListener(mouseListener);
        Events.removeListener(this);
        super.removeNotify();
    }

    public void updateCommandState(Map m) {
//      Debug.traceln("hadFocus=" + hadFocus);
        if (hadFocus || hasFocus()) {
            m.put("commandEditCut", Boolean.TRUE);
            m.put("commandEditCopy", Boolean.TRUE);
            m.put("commandEditPaste", Boolean.TRUE);
            m.put("commandEditInsertLineBreak", Boolean.TRUE);
            m.put("commandEditInsertPageBreak", Boolean.TRUE);
            m.put("commandEditInsertParagraphBreak", Boolean.TRUE);
        }
    }

    public static void commandEditCut() {
        Debug.traceln("commandEditCut");
    }

    public static void commandEditCopy() {
        Debug.traceln("commandEditCopy");
    }

    public static void commandEditPaste() {
        Debug.traceln("commandEditPaste");
    }

    private void click(MouseEvent e) {
//      Debug.traceln("click: " + e);
    }

    private void showContextMenu(Point p) {
        String[] actions = new String[]{
            "Edit.Cut",
            "Edit.Copy",
            "Edit.Paste",
            "Edit.Insert"
        };
        Commands.showContextMenu(this, p, actions,
            new Commands.ContextMenuItemListener() {
                public void run(String cmd) {
                    Debug.traceln(cmd);
                    if ("commandEditCut".equalsIgnoreCase(cmd)) {
                        Debug.traceln("context menu: commandEditCut");
                    } else if ("commandEditCopy".equalsIgnoreCase(cmd)) {
                        Debug.traceln("context menu: commandEditCopy");
                    } else if ("commandEditPaste".equalsIgnoreCase(cmd)) {
                        Debug.traceln("context menu: commandEditPaste");
                    }
                }
            });
    }

    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D)g;
        g2d.setColor(Color.DARK_GRAY);
        g2d.fill(g2d.getClipBounds());
        // "view/document"
        g2d.setColor(Color.WHITE);
        g2d.fill(new Rectangle(20, 20, 120, 220));
        g2d.setColor(Color.BLACK);
        g2d.drawString("Hello World!", 30, 50);
    }

    public Dimension getPreferredSize() {
        return new Dimension(160, 240);
    }

}
