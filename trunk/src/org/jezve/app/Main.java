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
import java.util.*;
import java.io.File;
import java.awt.*;

public final class Main implements Runnable {

    public static final String APPLICATION = Strings.getApplicationString("application.label");
    private static boolean initialized = setSystemLookAndFeel(APPLICATION, true);
    private static ArrayList args = null;
    private static Set options = new HashSet();

    public static void main(String[] a) {
        assert initialized; // make sure the Plaf is initialized
        // order of initialization is important.
        args = new ArrayList(Arrays.asList(a));
        // add start directory to java.library.path
        String pwd = System.getProperty("user.dir");
        String lp = System.getProperty("java.library.path");
        lp = lp == null || lp.length() == 0 ? pwd : lp + File.pathSeparator + pwd;
        System.setProperty("java.library.path", lp);
        Debug.init(args.contains("-debug") || args.contains("--debug") || args.contains("-g"));
        EventQueue.invokeLater(new Main());
    }

    public void run() {
        parseOptions();
        Events.addListener(new Controller());
        final TopFrame f = new TopFrame();
        assert !f.isVisible();
        if (Platform.isMac()) {
            // MacOSX.init must be called after frame is created
            MacOSX.init(new MacOSX.Events(){
                public void postEvent(String event) { Events.postEvent(event); }
                public void postEvent(String event, Object p) { Events.postEvent(event, p); }
            });
        }
        f.setVisible(true);
        f.toFront();
    }

    private void parseOptions() {
        for (Iterator i = args.iterator(); i.hasNext();) {
            String opt = (String)i.next();
            if (opt.startsWith("--")) {
                options.add(opt.substring(2));
                i.remove();
            } else if (opt.startsWith("-")) {
                options.add(opt.substring(1));
                i.remove();
            }
        }
    }

    public boolean hasOption(String option) {
        return options.contains(option);
    }

    public int getArgumentCount() {
        return args.size();
    }

    public String getArgument(int i) {
        return ((String)args.get(i)).trim();
    }

    private static boolean setSystemLookAndFeel(String name, boolean metal) {
        /* it is important that some system properties e.g. apple.awt.brushMetalLook
           are set before any code from ATW is executed. E.g. having static Dimension field
           that is initialized before setSystemLookAndFeel will make metal to disappear
           on the Macintosh. For this purpose setSystemLookAndFeel is actually called
           from static field initialization which will still not guarantee that it is
           executed before AWT initialized. If you experience lose of Brushed Metal L&F
           hunt via versions and see when AWT initialization kicked in the static section.
           Any call to UIManager also initializes AWT on Mac OS X.
          */
        try {
            if (Platform.isMac()) {
                MacOSX.setSystemLookAndFeel(name, metal);
            }
            System.setProperty("swing.handleTopLevelPaint", "true");
            System.setProperty("sun.awt.noerasebackground", "true");
            System.setProperty("swing.disableFileChooserSpeedFix", "true");
            Toolkit.getDefaultToolkit().setDynamicLayout(true);
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // see: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4525475
            UIManager.put("FileChooser.readOnly", Boolean.TRUE);
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
        } catch (Throwable e) {
            throw new Error(e);
        }
        return true;
    }

    private Main() { /* static class */ }

}
