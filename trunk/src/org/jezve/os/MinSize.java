package org.jezve.os;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MinSize extends JFrame {

    static final Dimension SMALLEST = new Dimension(320, 240);  
    static MinSize the;

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                the = new MinSize();
                the.setVisible(true);
            }
        });
    }

    MinSize() {
        super("Multi-platform min size constraint test");
        setBounds(50, 50, SMALLEST.width, SMALLEST.height);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                setContentMinSize(SMALLEST.width, SMALLEST.height);
            }
        });
    }

    // See ~/native/org_jezve_os_MinSize.[chm] for further details
    native void setContentMinSize(int width, int height);

    static {
        try {
            System.loadLibrary("minsize");
        }
        catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
            throw new ExceptionInInitializerError(e.getMessage());
        }
    }
}
