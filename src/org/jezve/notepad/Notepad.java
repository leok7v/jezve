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
