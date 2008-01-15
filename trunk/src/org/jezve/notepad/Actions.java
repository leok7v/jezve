package org.jezve.notepad;

import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import javax.swing.*;
import javax.swing.Timer;

final class Actions extends HashMap {

    private static List listeners = new LinkedList();
    private static boolean enabled = true;
    private static Actions instance = new Actions();

    private Actions() {
        addListener(this);
        invokeLater(100, new Runnable() {
            public void run() { updateCommandState(); }
        });
    }

    public static void addAction(String method, AbstractAction aa) {
        assert EventQueue.isDispatchThread();
        AbstractAction a = getAction(method);
        if (a == null) {
            instance.put(method, aa);
        } else {
            assert a == aa;
        }
    }

    public static AbstractAction getAction(String method) {
        assert EventQueue.isDispatchThread();
        return (AbstractAction)instance.get(method);
    }

    public static void addListener(Object listener) {
        assert EventQueue.isDispatchThread();
        assert !listeners.contains(listener) : "can only be added once";
        listeners.add(listener);
    }

    public static void removeListener(Object listener) {
        assert EventQueue.isDispatchThread();
        assert listeners.contains(listener) : "not added or already removed";
        listeners.remove(listener);
    }

    /** Thread safe
     * @param method to invoke later from all listeners on empty stack
     */
    public static void postEvent(final String method) {
        EventQueue.invokeLater(new Runnable(){
            public void run() {
                invokeMethod(method);
            }
        });
    }

    public static final Class[] VOID = new Class[]{};
    public static final Class[] OBJECT = new Class[]{Object.class};
    public static final Class[] STRING = new Class[]{String.class};
    public static final Class[] MAP = new Class[]{Map.class};
    public static final Object[] NONE = new Object[]{};
    /** Thread safe
     * @param method to invoke later from all listeners on empty stack
     * @param param parameter to pass to the method
     */
    public static void postEvent(final String method, final Object param) {
        EventQueue.invokeLater(new Runnable(){
            public void run() {
                invokeMethod(method, OBJECT, new Object[]{param});
            }
        });
    }

    public interface MenuItemListener {
        void run(String method);
    }

    public static void showContextMenu(Component c, Point pt, String[] spec,
            MenuItemListener listener) {
        JPopupMenu pm = new JPopupMenu();
        for (int i = 0; i < spec.length; i++) {
            if ("---".equals(spec[i])) {
                pm.add(new JSeparator());
            } else {
                pm.add(parseItem(spec[i], null, listener));
            }
        }
        pm.show(c, pt.x, pt.y);
    }

    static JMenuBar createMenuBar() {
        Map menus = new HashMap();
        JMenuBar mb = new JMenuBar();
        // TODO: commands below should be moved to localizable properties bundle
        addMenu(mb, menus, "&File|&New", "commandFileNew");
        addMenu(mb, menus, "&File|&Open Ctrl+O", "commandFileOpen");
        addMenu(mb, menus, "&File|&Close", "commandFileClose");
        addMenu(mb, menus, "&File|---", null);
        addMenu(mb, menus, "&File|E&xit Alt+F4", "commandFileExit");
        addMenu(mb, menus, "&Edit|&Undo Ctrl+Z", "commandEditUndo");
        addMenu(mb, menus, "&Edit|&Redo Ctrl+Y", "commandEditRedo");
        addMenu(mb, menus, "&Edit|---", null);
        addMenu(mb, menus, "&Edit|Cu&t Ctrl+X", "commandEditCut");
        addMenu(mb, menus, "&Edit|&Copy Ctrl+C", "commandEditCopy");
        addMenu(mb, menus, "&Edit|&Paste Ctrl+V", "commandEditPaste");
        addMenu(mb, menus, "&Edit|&Delete DELETE", "commandEditDelete");
        addMenu(mb, menus, "&View|Zoom &In Ctrl+=", "commandViewZoomIn");
        addMenu(mb, menus, "&View|Zoom &Out Ctrl+-", "commandViewZoomOut");
        addMenu(mb, menus, "&Tools|&Options Alt+F7", "commandToolsOptions");
        addMenu(mb, menus, "&Help|&About", "commandHelpAbout");
        addMenu(mb, menus, "&Help|&Index F1", "commandHelpIndex");
        mb.setBorder(null);
        return mb;
    }

    static JToolBar createToolBar() {
        JToolBar tb = new JToolBar();
        tb.setRollover(true);
        tb.setFloatable(false);
        // surround default LnF border with etched frame
        tb.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
                tb.getBorder()));
        tb.setFocusable(true);
        tb.setRequestFocusEnabled(true);
        addButton(tb, "open128x128", "commandFileOpen", "open file");
        tb.addSeparator(new Dimension(2, 1));
        addButton(tb, "pictures128x128", "commandFileGetPictures", "get pictures");
        addButton(tb, "newpage128x128", "commandFileNewPage", "new page");
        addSpacer(tb, 10);
        addButton(tb, "settings128x128", "commandToolsOptions", "Preferences");
        return tb;
    }


    private static void addSpacer(JToolBar tb, int width) {
        JComponent spacer = new JPanel();
        spacer.setPreferredSize(new Dimension(width, 40));
        spacer.setOpaque(false);
        tb.add(spacer);
    }

    private static void addButton(JToolBar tb, String iconname, final String method,
            String tooltip) {
        AbstractAction aa = Actions.getAction(method);
        if (aa == null) {
            aa = new AbstractAction(){
                public void actionPerformed(ActionEvent actionEvent) {
                    invokeMethod(method);
                }
            };
            Actions.addAction(method, aa);
        }
        JButton btn = new JButton(aa);
        btn.setPreferredSize(new Dimension(64 + 4, 64 + 4));
        btn.setBorderPainted(false);
        btn.setRolloverEnabled(true);
        btn.setIcon(Resources.getImageIcon(iconname, 64));
        if (Resources.hasImageIcon(iconname + "h")) {
            btn.setRolloverIcon(Resources.getImageIcon(iconname + "h", 64));
        }
        String tt = "<html><body><h5>&nbsp;" + tooltip + "&nbsp;</h5></body></html>";
        btn.setToolTipText(tt);
        btn.setAction(aa);
        btn.setFocusable(false);
        tb.add(btn);
    }

    private static void addMenu(JMenuBar mb, Map menus, String command,
            String action) {
        StringTokenizer st = new StringTokenizer(command, "|");
        String top = st.nextToken();
        String s = top.replaceAll("&", "");
        JMenu menu = (JMenu)menus.get(s);
        if (menu == null) {
            menu = new JMenu(s);
            menus.put(s, menu);
            int ix = top.indexOf('&');
            if (ix >= 0 && ix < top.length() - 1) {
                menu.setMnemonic(top.charAt(ix + 1));
                menu.setDisplayedMnemonicIndex(ix);
            }
            mb.add(menu);
        }
        String cmd = st.nextToken();
        if ("---".equals(cmd)) {
            menu.add(new JSeparator());
            return;
        }
        JMenuItem item = parseItem(cmd, action, null);
        menu.add(item);
    }

    private static JMenuItem parseItem(String cmd, String action, MenuItemListener listener) {
        assert (listener != null) != (action != null) : "mutually exclusive";
        int ix = cmd.lastIndexOf(' ');
        String a = null;
        if (ix >= 0) {
            a = cmd.substring(ix + 1).trim();
            cmd = cmd.substring(0, ix);
        }
        String s = cmd.replaceAll("&", "");
        final String method = action != null ? action :
                "command" + s.replaceAll(" ", "").replaceAll("_", "");
        AbstractAction aa = getAction(method);
        if (aa == null) {
            final MenuItemListener mil = listener != null ? listener :
                new MenuItemListener() {
                    public void run(String method) {
                        invokeMethod(method);
                    }
                };
            aa = new AbstractAction(){
                public void actionPerformed(ActionEvent actionEvent) {
                    mil.run(method);
                }
            };
        }
        JMenuItem item = new JMenuItem(aa);
        item.setText(s.replaceAll("_", " "));
        ix = cmd.indexOf('&');
        if (ix >= 0 && ix < cmd.length() - 1) {
            item.setMnemonic(cmd.charAt(ix + 1));
            item.setDisplayedMnemonicIndex(ix);
        }
        if (a != null) {
            parseAccelerator(item, a);
        }
        if (action != null) { // global broadcast action
            addAction(method, aa);
        }
        return item;
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
                if (a.length() > 1) {
                    int kc = KeyStroke.getKeyStroke(a).getKeyCode();
                    item.setAccelerator(KeyStroke.getKeyStroke(kc, mask));
                } else if (a.charAt(0) == '?') {
                    // VK_QUESTIONMARK does not exist, however KeyEvent.VK_SLASH with
                    // SHIFT_MASK gives desired accelerator
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

    private static void invokeMethod(String method) {
        invokeMethod(method, VOID, NONE);
    }

    private static void invokeMethod(String method, Class[] signature, Object[] params) {
        Object[] clone = listeners.toArray();
        for (int i = 0; i < clone.length; i++) {
            Object listener = clone[i];
            try {
                Method m = listener.getClass().getMethod(method, signature);
                m.invoke(listener, params);
            } catch (NoSuchMethodException e) {
                /* method is optional */
            } catch (IllegalAccessException e) {
                throw new Error(method + " must be declared public in " + listener.getClass(), e);
            } catch (InvocationTargetException e) {
                throw new Error(e);
            }
        }
    }

    public static void setEnabled(boolean b) {
        enabled = b;
        updateCommandState();
    }

    /** method to enable commands state
     * @param map command ids (like "commandFileOpen" to Boolean.TRUE/FALSE
     * @noinspection UnusedDeclaration
     */
    public static void updateCommandState(Map map) {
    }

    private static void updateCommandState() {
//      System.err.println("updateCommandState");
        invokeLater(100, new Runnable() {
            public void run() { updateCommandState(); }
        });
        long time = System.currentTimeMillis();
        HashMap state = new HashMap(instance.size() * 3 / 2);
        Object[] p = new Object[]{state};
        if (enabled) {
            for (Iterator i = listeners.iterator(); i.hasNext(); ) {
                Object listener = i.next();
                try {
                    Method updateCommandState = listener.getClass().getMethod("updateCommandState",
                            MAP);
                    updateCommandState.invoke(listener, p);
                } catch (NoSuchMethodException e) {
                    /* the updateCommandState is optional */
                  } catch (IllegalAccessException e) {
                    throw new Error(listener.getClass().getName() +
                                    " method updateCommandState(Map) is not public", e);
                } catch (InvocationTargetException e) {
                    throw new Error(e);
                }
            }
        }
        boolean changed = false;
        for (Iterator i = instance.keySet().iterator(); i.hasNext(); ) {
            String method = (String)i.next();
            if (!Boolean.TRUE.equals(state.get(method))) {
                AbstractAction aa = (AbstractAction)instance.get(method);
                if (aa.isEnabled()) {
                    changed = true;
                    aa.setEnabled(false);
                }
            }
        }
        for (Iterator i = state.keySet().iterator(); i.hasNext(); ) {
            String method = (String)i.next();
            if (Boolean.TRUE.equals(state.get(method))) {
                AbstractAction aa = (AbstractAction)instance.get(method);
                if (aa != null && !aa.isEnabled()) {
                    changed = true;
                    aa.setEnabled(true);
                }
            }
        }
        if (changed) {
            Notepad.frame.getJMenuBar().repaint();
        }
        time = System.currentTimeMillis() - time;
        if (time > 1000) {
//            Debug.traceln("updateCommandState: WARNING time = " + time + " milli");
        }
    }

    public static void invokeLater(int milliseconds, final Runnable r) {
        Timer timer = new Timer(milliseconds, new ActionListener(){
            public void actionPerformed(ActionEvent actionEvent) {
                r.run();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

}
