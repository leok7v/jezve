package org.jezve.notepad;

import java.awt.*;
import java.util.*;
import java.util.prefs.*;
import javax.swing.*;

class Notepad implements Runnable {

    static boolean initialized = setSystemLookAndFeel();

    static final Preferences pref = Preferences.userNodeForPackage(Notepad.class);
    static final Preferences user = pref.node(System.getProperty("user.name"));

    static String args[];

    static TopFrame frame;

    public void run() {
        Actions.addListener(this);
        frame = new TopFrame("Notepad");
        frame.setVisible(true);
    }

    static public void main(String[] a) {
        args = a;
        EventQueue.invokeLater(new Notepad());
    }

    static boolean setSystemLookAndFeel() {
        try {
            System.setProperty("swing.handleTopLevelPaint", "true");
            System.setProperty("sun.awt.noerasebackground", "true");
            System.setProperty("swing.disableFileChooserSpeedFix", "true");
            // http://developer.apple.com/releasenotes/Java/java141/system_properties/chapter_4_section_3.html
            Toolkit.getDefaultToolkit().setDynamicLayout(true);
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
        } catch (Throwable e) {
            throw new Error(e);
        }
        return true;
    }

    public void updateCommandState(Map m) {
        m.put("commandFileNew", Boolean.TRUE);
        m.put("commandFileOpen", Boolean.TRUE);
        m.put("commandFileClose", Boolean.TRUE);
        m.put("commandFileExit", Boolean.TRUE);
    }

    public void commandFileNew() {
    }

    public void commandFileOpen() {
    }

    public void commandFileClose() {
        commandFileClose(false);
    }

    public void commandFileClose(boolean exiting) {
    }

    public void commandFileExit() {
        commandFileClose(true);
        System.exit(0);
    }
}
