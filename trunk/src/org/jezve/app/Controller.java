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

import org.jezve.util.Debug;
import org.jezve.util.Web;

import javax.swing.*;
import java.util.*;
import java.io.IOException;
import java.io.File;
import java.awt.event.WindowEvent;

@SuppressWarnings({"unchecked"})
public final class Controller {

    /** method to enable commands state
     * @param m map command ids:Boolean
     */
    public void updateCommandState(Map m) {
        m.put("commandFileOpen", Boolean.TRUE);
        m.put("commandFileNew", Boolean.TRUE);
        m.put("commandFileExit", Boolean.TRUE);
        m.put("commandPreferences", Boolean.TRUE);
        m.put("commandHelpIndex", Boolean.TRUE);
        m.put("commandHelpAbout", Boolean.TRUE);
        m.put("commandWindowNew", Boolean.TRUE);
        m.put("commandWindowClose", TopFrame.getActiveFrame() != null ? Boolean.TRUE : Boolean.FALSE);
    }

    public static void commandFileExit() {
        commandFileClose(true);
        Debug.traceln("commandFileExit");
        UserSettings.flushNow();
        System.exit(0);
    }

    public static void commandPreferences() {
        Debug.traceln("commandPreferences");
        Preferences.showPreferences();
    }

    public static void commandHelpAbout() {
        Debug.traceln("commandHepAbout");
        About.showMessage();
    }

    public static void commandHelpIndex() {
        Debug.traceln("commandHepIndex");
        try {
            Web.openUrl("http://code.google.com/p/jezve");
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public static void commandFileNew() {
        Debug.traceln("commandFileNew - not implemented");
    }

    public static void commandWindowNew() {
        Debug.traceln("commandWindowNew");
        TopFrame f = new TopFrame();
        f.setVisible(true);
    }

    public static void commandWindowClose() {
        TopFrame active = TopFrame.getActiveFrame();
        if (active != null) {
            active.dispatchEvent(new WindowEvent(active, WindowEvent.WINDOW_CLOSING));
        }
    }

    public static void commandFileOpen() {
        Debug.traceln("commandFileOpen - not implemented");
        TopFrame top = TopFrame.getActiveFrame();
        if (top != null) {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setMultiSelectionEnabled(false);
            fc.setLocale(Locale.getDefault());
            int r = fc.showOpenDialog(TopFrame.getActiveFrame());
            if (r == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                top.setModified(true);
                top.setDocument(file);
                top.setTitle(file.getName());
            }
        }
    }

    public static void commandFilePrint() {
        Debug.traceln("commandFilePrint");
    }

    public static void commandFilePrint(String filename) {
        Debug.traceln("commandFilePrint(" + filename + ") - not implemented");
    }

    public static void commandFileOpen(String filename) {
        Debug.traceln("commandFileOpen(" + filename + ") - not implemented");
    }

    private static void commandFileClose(boolean exiting) {
        Debug.traceln("commandFileClose(" + exiting + ")");
    }

    public static void commandFileClose() {
        commandFileClose(false);
    }

    // Mac specific:

    public static void commandReOpenApplication() {
        Debug.traceln("commandReOpenApplication");
    }

    public static void commandOpenAppication() {
        Debug.traceln("commandOpenApplication");
    }

}
