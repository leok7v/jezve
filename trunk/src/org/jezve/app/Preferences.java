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
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public final class Preferences extends JTabbedPane {

    private boolean canceled;
    private static final String TAB = "Preferences.tab";

    private Preferences() {
        BasePanel general = new General();
        addTab(" General ", null, general, "General Application Settings");
        setMnemonicAt(0, 'G');
        BasePanel advanced = new Advanced();
        addTab(" Advanced ", null, advanced, "Options For Advanced Users");
        setMnemonicAt(1, 'A');
        int i = UserSettings.getInt(TAB, 0);
        if (0 <= i && i <= 2) {
            setSelectedIndex(i);
        }
        addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                int i = getSelectedIndex();
                if (0 <= i && i <= 2) {
                    UserSettings.putInt(TAB, i);
                    UserSettings.sync();
                }
            }
        });
    }

    public static void showPreferences() {
        Map old = new HashMap(UserSettings.getAll());
        old.remove(TAB);
        JDialog dlg = Misc.createDocumentModalDialog(TopFrame.getActiveFrame());
        Preferences s = new Preferences();
        String suffix = Platform.isMac() ? " Preferences" : " Options";
        dlg.setTitle(Main.APPLICATION + suffix);
        dlg.getContentPane().setLayout(new BorderLayout());
        dlg.getContentPane().add(s, BorderLayout.CENTER);
        dlg.getContentPane().add(s.createButtons(dlg), BorderLayout.SOUTH);
        dlg.pack();
        dlg.setResizable(false);
        s.canceled = true;
        dlg.setVisible(true); // nested dispatch loop
        if (!s.canceled) {
            for (int i = 0; i < s.getTabCount(); i++) {
                BasePanel panel = (BasePanel)s.getComponentAt(i);
                panel.saveSettings();
            }
            Map now = new HashMap(UserSettings.getAll());
            now.remove(TAB);
            if (!old.equals(now)) {
                Events.postEvent("preferencesChanged", new Object[]{old, now});
            }
            UserSettings.sync();
        }
        dlg.dispose();
    }

    private JPanel createButtons(final JDialog dlg) {
        JPanel buttons = new JPanel();
        JButton ok = new JButton("Apply");
        ok.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                if (checkSettings()) {
                    canceled = false;
                    dlg.setVisible(false);
                }
            }
        });
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) { dlg.setVisible(false); }
        });
        ok.setDefaultCapable(true);
        dlg.getRootPane().setDefaultButton(ok);
        buttons.add(ok);
        buttons.add(cancel);
        return buttons;
    }

    private boolean checkSettings() {
        boolean check = true;
        for (int i = 0; i < getTabCount(); i++) {
            BasePanel panel = (BasePanel)getComponentAt(i);
            check = panel.checkSettings() && check;
        }
        return check;
    }

    private abstract class BasePanel extends JPanel {

        public Insets getInsets() {
            return new Insets(8, 8, 8, 8);
        }

        public Dimension getPreferredSize() {
            return new Dimension(500, 320);
        }

        protected abstract boolean checkSettings();
        protected abstract void saveSettings();
    }

    private final class General extends BasePanel {

        private JCheckBox promptAll;

        General() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            promptAll = new JCheckBox("Show All Prompts") {
                {
                    addActionListener(new ActionListener(){
                        public void actionPerformed(ActionEvent e) {
                            if (isSelected()) {
                                setEnabled(false);
                            }
                        }
                    });
                }
            };
            promptAll.setSelected(false);
            add(promptAll);
        }

        protected boolean checkSettings() {
            return true;
        }

        protected final void saveSettings() {
            if (promptAll != null && promptAll.isSelected()) {
                promptAll = null;
                UserSettings.putBoolean("show.all.prompts", true);
            }
        }
    }

    private final class Advanced extends BasePanel {

        private JComboBox languages;
        private HashMap locales = new HashMap(); // locale label -> locale id

        Advanced() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            add(new JLabel("Language:"));
            String locale = UserSettings.get("locale", getDefaultLocale().getLanguage());
            String current = "";
            Set keys = Strings.getAllicationKeys();
            ArrayList names = new ArrayList();
            for (Iterator i = keys.iterator(); i.hasNext(); ) {
                String key = (String)i.next();
                if (key.startsWith("locale.")) {
                    String s = key.substring(7);
                    String name = Strings.getString(key);
                    names.add(name);
                    locales.put(name, s);
                    if (locale.equals(s)) {
                        current = name;
                    }
                }
            }
            Collections.sort(names, new Comparator(){
                public int compare(Object o1, Object o2) {
                    String s1 = (String)o1;
                    String s2 = (String)o2;
                    return s1.compareToIgnoreCase(s2);
                }
            });
            languages = new JComboBox(names.toArray()) {
                public Dimension getMaximumSize() {
                    return getPreferredSize();
                }
            };

Font f = new JLabel().getFont();
System.err.println(f);
System.err.println(f.getFamily());
System.err.println(f.getName());
f = new JComboBox().getFont();
System.err.println(f);
System.err.println(f.getFamily());
System.err.println(f.getName());

            languages.setSelectedItem(current);
            languages.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(languages);
        }

        protected boolean checkSettings() {
            return true;
        }

        protected final void saveSettings() {
            String label = (String)languages.getSelectedItem();
            String loc = (String)locales.get(label);
            assert loc != null : label;
            UserSettings.put("locale", loc);
            String language = loc.substring(0, 2);
            String country = loc.length() <= 3 ? "" : loc.substring(3);
            Locale lc = new Locale(language,  country);
            TopFrame.setNewLocale(lc);
        }

    }

}
