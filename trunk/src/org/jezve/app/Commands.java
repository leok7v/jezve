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
import org.jezve.app.resources.Strings;
import org.jezve.app.resources.Icons;

import java.util.*;
import java.awt.event.*;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.accessibility.*;

@SuppressWarnings({"ForLoopReplaceableByForEach", "unchecked"})
public final class Commands {

    private static boolean enabled = true;
    private static final Map commands = new HashMap();
    private static final Set menus = new HashSet();

    static {
        Events.addListener(new Commands());
        Misc.invokeLater(100, new Runnable() {
            public void run() { updateCommandState(); }
        });
    }

    public static void reset() {
        commands.clear();
        menus.clear();
    }

    /** create application menubar from resources/app.properties
     * @return menubar
     */
    public static JMenuBar createMenuBar() {
        assert EventQueue.isDispatchThread();
        final JMenuBar mb = new JMenuBar();
        String menu = Strings.getString("menu");
        String[] items = menu.split("\\|");
        processMenu("menu", items, null, mb);
        mb.setBorder(null);
        return mb;
    }

    /** create application toolbar from resources/app.properties
     * @return toolbar
     * @param size buttons size (e.g. 16, 22, 32)
     */
    public static JToolBar createToolBar(int size) {
        assert EventQueue.isDispatchThread();
        JToolBar tb = new JToolBar();
        tb.setRollover(true);
        tb.setFloatable(false);
        // surround default LnF border with etched frame
        if (!Platform.isMac()) {
            Border border = BorderFactory.createEtchedBorder();
            border = BorderFactory.createCompoundBorder(border, tb.getBorder());
            tb.setBorder(border);
        }
        tb.setFocusable(true);
        tb.setRequestFocusEnabled(true);
        String toolbar = Strings.getString("toolbar");
        String[] items = toolbar.split("\\|");
        for (int i = 0; i < items.length; i++) {
            String item = items[i].trim();
            if ("---".equals(item)) {
                tb.addSeparator(new Dimension(2, 1));
            } else if (item.length() == 0) {
                addSpacer(tb, size / 2, size);
            } else {
                addButton(tb, item, size);
            }
        }
        return tb;
    }

    /** allow application to disable/enable all commands
     * including menu items
     * @param b set to true to enable
     */
    public static void setEnabled(boolean b) {
        assert EventQueue.isDispatchThread();
        enabled = b;
        updateCommandState();
        if (Platform.isMac()) {
            MacOSX.setEnabledAboutMenu(b);
            MacOSX.setEnabledPreferencesMenu(b);
        }
        for (Iterator i = menus.iterator(); i.hasNext(); ) {
            AbstractAction aa = (AbstractAction)i.next();
            aa.setEnabled(b);
        }
        TopFrame.setEnabledAll(b);
        updateCommandState();
    }

    /** enable/disable commands state
     * @param map command ids (like "commandFileOpen" to Boolean.TRUE/FALSE
     * @noinspection UnusedDeclaration
     */
    public static void updateCommandState(Map map) {
    }

    public interface ContextMenuItemListener {
        void run(String command);
    }

    public static void showContextMenu(Component c, Point pt, String[] spec, ContextMenuItemListener listener) {
        final JPopupMenu pm = new JPopupMenu();
        processMenu("menu", spec, listener, pm);
        pm.show(c, pt.x, pt.y);
    }

    private static void addAction(String command, AbstractAction aa) {
        assert EventQueue.isDispatchThread();
        AbstractAction a = getAction(command);
        if (a == null) {
            commands.put(command, aa);
        } else {
            assert a == aa;
        }
    }

    private static AbstractAction getAction(String command) {
        assert EventQueue.isDispatchThread();
        return (AbstractAction)commands.get(command);
    }

    private static AbstractAction createAction(String item, final ContextMenuItemListener listener) {
        String label = removeAccelerator(getLabel(item).replaceAll("\\&", ""));
        String c = item + ".command";
        final String command = Strings.hasString(c) ? Strings.getString(c) : null;
        AbstractAction aa = command == null || listener != null ? null : getAction(command);
        if (aa == null) {
//          Debug.traceln("AbstractAction: " + label);
            aa = new AbstractAction(label) {
                public void actionPerformed(ActionEvent actionEvent) {
                    if (listener != null) {
                        listener.run(command);
                    } else if (command != null) {
                        Events.postEvent(command);
                    }
                }
            };
            aa.putValue(Action.LONG_DESCRIPTION,  getLongDescription(item));
            aa.putValue(Action.SHORT_DESCRIPTION, Strings.getString(item + ".shortDescription"));
            if (listener != null || command == null) {
                // all context menu actions are always enabled;
                // context menu actions are not poll updated;
                // simply filter out disabled actions from context menus
                // prio to showing them.
                aa.setEnabled(true);
            }
            if (listener == null) {
                if (command != null) {
                    addAction(command, aa);
                } else {
                    menus.add(aa);
                    menus.add(aa);
                }
            }
        }
        return aa;
    }

    private static String getLabel(String item) {
        return Strings.getString(item + ".label");
    }

    private static String getLongDescription(String item) {
        return Strings.getString(item + ".longDescription");
    }

    private static void setAccessibleContext(AccessibleContext ac, String item) {
        ac.setAccessibleDescription(Strings.getString(item + ".accessibleDescription"));
        ac.setAccessibleName(Strings.getString(item + ".accessibleName"));
    }

    private static void setMnemonic(JMenuItem mi, String item) {
        String label = getLabel(item);
        int ix = label.indexOf('&');
        if (ix >= 0 && ix < label.length() - 1) {
            mi.setMnemonic(label.charAt(ix + 1));
            mi.setDisplayedMnemonicIndex(ix);
        }
    }

    private static void setMnemonic(JMenu m, String item) {
        String label = getLabel(item);
        int ix = label.indexOf('&');
        if (ix >= 0 && ix < label.length() - 1) {
            m.setMnemonic(label.charAt(ix + 1));
            m.setDisplayedMnemonicIndex(ix);
        }
    }

    private static void processMenu(final JMenu jmenu, String menu) {
        String sub = Strings.getString(menu);
        String[] items = sub.split("\\|");
        processMenu(menu, items, null, jmenu);
    }

    private static void processMenu(String parent, String[] items,
            ContextMenuItemListener listener, JComponent menu) {
        for (int i = 0; i < items.length; i++) {
            String it = items[i].trim();
            if ("---".equals(it)) {
                menu.add(new JSeparator());
            } else {
                String item = parent + "." + it;
                if (Strings.hasString(item)) {
                    JMenu submenu = new JMenu();
                    processMenu(submenu, item);
                    submenu.setAction(createAction(item, listener));
                    setMnemonic(submenu, item);
                    setAccessibleContext(submenu.getAccessibleContext(), item);
                    menu.add(submenu);
                } else {
                    String a = getAccelerator(item);
                    JMenuItem mi = new JMenuItem();
                    mi.setAction(createAction(item, listener));
                    setMnemonic(mi, item);
                    setAccessibleContext(mi.getAccessibleContext(), item);
                    if (a != null) {
                        parseAccelerator(mi, a);
                    }
                    menu.add(mi);
                }
            }
        }
    }

    private static String getAccelerator(String item) {
        String label = getLabel(item);
        int from = label.indexOf('[');
        int to = label.indexOf(']');
        if (from < 0 || to < 0) { // accelerator is not localized. Use English version:
            label = Strings.getEnglishString(item + ".label");
            from = label.indexOf('[');
            to = label.indexOf(']');
        }
        return 0 <= from && from < to ? label.substring(from + 1, to) : null;
    }

    private static String removeAccelerator(String s) {
        int from = s.indexOf('[');
        int to = s.indexOf(']');
        return 0 <= from && from < to ? s.substring(0, from) : s;
    }

    private static void addSpacer(JToolBar tb, int width, int height) {
        JComponent spacer = new JPanel();
        spacer.setPreferredSize(new Dimension(width, height));
        spacer.setOpaque(false);
        tb.add(spacer);
    }

    private static void addButton(JToolBar tb, String item, final int n) {
        String image = n + "x" + n + "/" + Strings.getString("toolbar." + item + ".image");
        item = "menu." + item;
        String tooltip = getLongDescription(item);
        String accel = getAccelerator(item);
        AbstractAction aa = createAction(item, null);
        JButton btn = new JButton(aa) {
            public Dimension getPreferredSize() {
                return new Dimension(n + 9, n + 9);
            }
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
        };
        btn.setBorderPainted(false);
        btn.setRolloverEnabled(true);
        btn.setVerticalAlignment(SwingConstants.CENTER);
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        setIcons(btn, Icons.getImageIcon(image, n));
        String tt = "<html><body style='padding: 4 4 4 4;'>" +
                "<table width=200><tr><td>" + tooltip +
                (accel != null ? "<br><strong>" + accel + "</strong>" : "") +
                "</td></tr></table></body></html>";
        btn.setToolTipText(tt);
        btn.setAction(aa);
        btn.setFocusable(false);
        btn.setText(null);
        tb.add(btn);
    }

    private static void setIcons(JButton btn, ImageIcon ii) {
        Image i = ii.getImage();
        btn.setIcon(ii);
        BufferedImage bi = Icons.asBufferedImage(i);
        Icons.adjustHSB(bi, 1.2f, 2); // increase brightness by 20%
        btn.setRolloverIcon(new ImageIcon(bi));
        btn.setRolloverSelectedIcon(new ImageIcon(bi));

        bi = Icons.asBufferedImage(i);
        Icons.adjustHSB(bi, 0.2f, 1); // decrease saturation by -80% (almost grey scale)
        btn.setDisabledIcon(new ImageIcon(bi));
        btn.setDisabledSelectedIcon(new ImageIcon(bi));

        bi = Icons.asBufferedImage(i, 1, 1); // shifted left down direction by (+1,+1)
        btn.setPressedIcon(new ImageIcon(bi));
        btn.setSelectedIcon(new ImageIcon(bi));
    }

    private static void parseAccelerator(JMenuItem item, String a) {
        int mask = 0;
        while (a.length() > 0) {
            if (a.startsWith("Meta+")) {
                mask |= InputEvent.META_MASK;
                a = a.substring(5);
            } else if (a.startsWith("Ctrl+")) {
                mask |= InputEvent.CTRL_MASK;
                a = a.substring(5);
            } else if (a.startsWith("Alt+")) {
                mask |= InputEvent.ALT_MASK;
                a = a.substring(4);
            } else if (a.startsWith("Shift+")) {
                mask |= InputEvent.SHIFT_MASK;
                a = a.substring(6);
            } else {
                if ("Enter".equals(a)) {
                    a = "ENTER";
                } else if ("Del".equals(a)) {
                    a = "DELETE";
                }
                if (a.length() > 1) {
                    int kc = KeyStroke.getKeyStroke(a).getKeyCode();
                    item.setAccelerator(KeyStroke.getKeyStroke(kc, mask));
                } else if (a.charAt(0) == '?' && mask != 0) {
                    // VK_QUESTIONMARK does not exist, however KeyEvent.VK_SLASH with
                    // SHIFT_MASK gives desired accelerator (Mac oSX shows it correctly, Win - not)
                    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH,
                            mask|InputEvent.SHIFT_MASK));
                } else {
                    item.setAccelerator(KeyStroke.getKeyStroke(a.charAt(0), mask));
                }
                break;
            }
        }
        // Note: http://developer.apple.com/technotes/tn/tn2042.html
        // possibly better and more portable way:
        // item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
        // Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    }

    private static void updateCommandState() {
        Misc.invokeLater(100, new Runnable() {
            public void run() { updateCommandState(); }
        });
        long time = System.currentTimeMillis();
        HashMap state = new HashMap(commands.size() * 3 / 2 + 1);
        if (enabled) {
            Events.collectCommandState(state);
        }
        boolean changed = false;
        for (Iterator i = commands.keySet().iterator(); i.hasNext();) {
            String command = (String)i.next();
            if (!Boolean.TRUE.equals(state.get(command))) {
                AbstractAction aa = (AbstractAction)commands.get(command);
                if (aa.isEnabled()) {
                    changed = true;
                    aa.setEnabled(false);
                }
            }
        }
        for (Iterator i = state.keySet().iterator(); i.hasNext();) {
            String command = (String)i.next();
            if (Boolean.TRUE.equals(state.get(command))) {
                AbstractAction aa = (AbstractAction)commands.get(command);
                if (aa != null && !aa.isEnabled()) {
                    changed = true;
                    aa.setEnabled(true);
                }
            }
        }
        if (changed) {
            TopFrame.repaintMenuBars();
        }
        time = System.currentTimeMillis() - time;
        if (time > 50) {
            Debug.traceln("updateCommandState: WARNING time = " + time + " milli");
        }
    }

}
