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

import org.jezve.util.Misc;
import org.jezve.util.Platform;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public final class About {

    public static void showMessage() {
        final JFrame parent = TopFrame.getActiveFrame();
        final JDialog dlg = Misc.createDocumentModalDialog(parent);
        JPanel panel = new JPanel(new BorderLayout()) {
            public Insets getInsets() {
                return new Insets(8, 8, 8, 8);
            }
        };
        dlg.setTitle("About " + Main.APPLICATION);
        String platform = Platform.isMac() ? "Mac OS X" : Platform.isWindows() ? "Win" : "*nix";
        JEditorPane label =
                Misc.createReadOnlyLabel("<html><body style='padding: 30 30 30 30;'>" +
                "<b>" + Main.APPLICATION + "</b>: 0.0.0.0<br>" +
                platform + ": " + Platform.getOsVersion() + "<br>" +
                "java: " + " " + Platform.getJavaVersion() + "<br>" +
                "cpu: " + Platform.getProcessor() + "<br>" +
                "<hr>" +
                "This is placeholder for real About dialog.");
        panel.add(label, BorderLayout.CENTER);

        JButton ok = new JButton("OK");
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dlg.dispose();
            }
        });
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(ok, BorderLayout.EAST);
        panel.add(bottom, BorderLayout.SOUTH);
        dlg.getContentPane().add(panel);
        dlg.pack();
        dlg.setVisible(true);
    }

    private About() { }

}
