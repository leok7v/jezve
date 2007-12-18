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

package org.jezve.util;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

public final class Misc {

    /** Compares two objects. Why don't Sun implemented it in the Object class?
     * @param o1 object1
     * @param o2 object2
     * @return true if equals
     */
    public static boolean equals(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    /** Do not use Thread.sleep() directly.
     * This is more controllable way of doing the same.
     * @param milliseconds to sleep
     */
    public static void sleep(int milliseconds) {
        try { Thread.sleep(milliseconds); } catch (InterruptedException e) { /* ignore */ }
    }

    /** invoke runnable after at least number milliseconds passed.
     * @param milliseconds to invoke after
     * @param r runnable to invoke
     */
    public static void invokeLater(int milliseconds, final Runnable r) {
        Timer timer = new Timer(milliseconds, new ActionListener(){
            public void actionPerformed(ActionEvent actionEvent) {
                r.run();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }


    /** Create simple scroll mode JScrollPane wrapper with matching background collor.
     * @param c component to wrap
     * @return JScrollPane wrapper
     */
    public static JScrollPane createScrollPane(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.getViewport().setBackground(c.getBackground());
        sp.setBorder(null);
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        return sp;
    }

    /**
     * creates document modal dialog and centers it in the owner window
     * @param parent window
     * @return modal JDialog
     */
    public static JDialog createDocumentModalDialog(JFrame parent) {
        Throwable x = null;
        JDialog d = null;
        boolean macModal = Platform.isMac() && Platform.getJavaVersion() >= 1.6;
        try {
            if (macModal) {
                Class mt = Class.forName("java.awt.Dialog$ModalityType");
                Field dm = mt.getField("DOCUMENT_MODAL");
                Class[] sig = new Class[]{Window.class, mt};
                Constructor c = JDialog.class.getConstructor(sig);
                Object[] params = new Object[]{parent, dm.get(null)};
                d = (JDialog)c.newInstance(params);
            }
        } catch (ClassNotFoundException e) {
            // ignore
        } catch (NoSuchFieldException e) { x = e;
        } catch (NoSuchMethodException e) { x = e;
        } catch (IllegalAccessException e) { x = e;
        } catch (InvocationTargetException e) { x = e;
        } catch (InstantiationException e) { x = e;
        }
        if (x != null) {
            throw new Error(x);
        }
        if (d == null) {
            d = new JDialog(parent) {
                public void pack() {
                    super.pack();
                    try { // the line below not guaranteed to work on all java variations
                        setLocationRelativeTo(getOwner());
                    } catch (Throwable ignore) {
                        // ignore
                    }
                }
            };
            d.setModal(true);
        }
        final JDialog dlg = d;
        // see SwingUtilities.getSharedOwnerFrame()
        if (macModal) {
            if (parent != null) {
                parent.getRootPane().putClientProperty("apple.awt.documentModalSheet", "true");
            }
            dlg.getRootPane().putClientProperty("apple.awt.documentModalSheet", "true");
        } else {
            dlg.addComponentListener(new ComponentAdapter() {
                public void componentShown(ComponentEvent e) {
                    if (dlg.getOwner() != null) {
                        dlg.setLocationRelativeTo(dlg.getOwner());
                        dlg.removeComponentListener(this);
                    }
                }
            });
        }
        KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        Action escapeAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                dlg.dispose();
            }
        };
        dlg.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escape, "ESCAPE");
        dlg.getRootPane().getActionMap().put("ESCAPE", escapeAction);
        return dlg;
    }

    /** Create readonly label filled with html with hyperlink listenner attached.
     * @param html html content of the label
     * @return JLabel
     */
    public static JEditorPane createReadOnlyLabel(String html) {
        JEditorPane pane = new JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.setText(html);
        pane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        Web.openUrl(e.getURL().toString());
                    } catch (IOException e1) {
                        // ignore
                    }
                }
            }
        });
        return pane;
    }

    private Misc() { /* no instantiation */ }

}
