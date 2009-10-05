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

import org.jezve.app.Main;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.io.*;

@SuppressWarnings({"unchecked","ForLoopReplaceableByForEach"})
public final class Debug {

    private static boolean debug = true; // to debug static initialization
    private static ExceptionHandler crashHandler;
    private static byte[] reserve = new byte[1024*1024]; // 1MB
    private static final Set exclude = new HashSet() {{
        add("java.runtime.name");
        add("swing.handleTopLevelPaint");
        add("sun.awt.exception.handler");
        add("sun.awt.noerasebackground");
        add("java.vendor.url.bug");
        add("file.separator");
        add("swing.aatext");
        add("java.vendor");
        add("sun.awt.erasebackgroundonresize");
        add("java.specification.vendor");
        add("java.vm.specification.version");
        add("java.awt.printerjob");
        add("java.class.version");
        add("sun.management.compiler");
        add("java.specification.name");
        add("user.variant");
        add("java.vm.specification.vendor");
        add("line.separator");
        add("java.endorsed.dirs");
        add("java.awt.graphicsenv");
        add("java.vm.specification.name");
        add("sun.java.launcher");
        add("path.separator");
        add("java.vendor.url");
        add("java.vm.name");
        add("java.vm.vendor");
    }};

    public static void init(boolean b) {
        debug = b;
        System.setProperty("sun.awt.exception.handler", ExceptionHandler.class.getName());
        crashHandler = new ExceptionHandler();
        for (int i = 0; i < reserve.length; i++) {
            reserve[i] = (byte)(i & 0xFF); // make memory commited
        }
    }

    public static void traceln(String s) {
        if (isDebug()) {
            System.err.println(s);
        }
    }

    public static void trace(String s) {
        if (isDebug()) {
            System.err.print(s);
        }
    }

    public static void traceln() {
        if (isDebug()) {
            System.err.println();
        }
    }

    public static boolean isDebug() {
        return debug;
    }

    public static void printStackTrace(Throwable t) {
        printStackTrace(null, t);
    }

    public static void printStackTrace(String msg, Throwable t) {
        if (isDebug()) {
            if (msg != null) {
                Debug.traceln(msg);
            }
            t.printStackTrace();
        }
    }

    public static class ExceptionHandler {

        private static boolean reported;

        public void handle(Throwable x) {
            reserve = null;
            System.gc();
            Misc.sleep(1000);
            if (this != crashHandler) {
                crashHandler.handle(x);
            } else if (!reported) {
                reported = true;
                report(x);
            }
        }

        public static void report(Throwable x) {
            Throwable cause = x;
            for (; ;) {
                if (cause instanceof InvocationTargetException &&
                    ((InvocationTargetException)cause).getTargetException() != null) {
                    cause = ((InvocationTargetException)cause).getTargetException();
                } else if (cause.getCause() != null) {
                    cause = cause.getCause();
                } else {
                    break;
                }
            }
            // noinspection CallToPrintStackTrace
            cause.printStackTrace();
            StringWriter sw = new StringWriter();
            PrintWriter pw = null;
            String method;
            try {
                pw = new PrintWriter(sw);
                pw.println("This email might help to fix an issue. " +
                           "Include any additional information right here:\n" +
                           " . . .");
                method = printStackTrace(cause, pw);
            } finally {
                if (pw != null) {
                    pw.close();
                }
            }
            StringBuffer sb = sw.getBuffer();
            Map p = System.getProperties();
            for (Iterator j = p.keySet().iterator(); j.hasNext();) {
                Object key = j.next();
                if (!exclude.contains(key)) {
                    sb.append(key).append("=").append(p.get(key)).append("\n");
                }
            }
            String subject = "[" + Main.APPLICATION + " crash] " + " " + shorten(cause.toString()) +
                             (method != null ? " at " + shorten(method) : "");
            try {
                Web.sendMail("crash@" + Main.APPLICATION + ".com", subject, sb.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.exit(1);
        }

        private static String printStackTrace(Throwable t, PrintWriter s) {
            String method = null;
            String first = null;
            synchronized (t) {
                s.println(t);
                StackTraceElement[] trace = t.getStackTrace();
                for (int i = 0; i < trace.length; i++) {
                    String f = "" + trace[i];
                    if (first == null) {
                        first = trace[i].getClassName() + "." + trace[i].getMethodName();
                    }
                    String pkg = Main.class.getPackage() == null ? "" : Main.class.getPackage().getName() + ".";
                    if (pkg.length() > 0 && f.startsWith(pkg)) {
                        f = f.substring(pkg.length());
                        if (method == null) {
                            method = trace[i].getClassName() + "." + trace[i].getMethodName();
                        }
                    }
                    else if (f.startsWith("java.util.")) {
                        f = f.substring("java.util.".length());
                    }
                    else if (f.startsWith("java.io.")) {
                        f = f.substring("java.io.".length());
                    }
                    else if (f.startsWith("java.awt.")) {
                        f = f.substring("java.awt.".length());
                    }
                    else if (f.startsWith("javax.swing.")) {
                        f = f.substring("javax.swing.".length());
                    }
                    f = f.replaceAll(".java:", ":");
                    if (f.indexOf("EventDispatchThread.pumpOneEventForHierarchy") >= 0) {
                        break; // cut bottom of the stack
                    }
                    s.println(f);
                }
                Throwable ourCause = t.getCause();
                if (ourCause != null) {
                    s.println("caused by: ");
                    printStackTrace(t, s);
                }
            }
            return method == null ? first : method;
        }

        private static String shorten(String message) {
            if (message.startsWith("java.lang.")) {
                return message.substring("java.lang.".length());
            } else if (message.startsWith("java.util.")) {
                return message.substring("java.util.".length());
            } else if (message.startsWith("java.io.")) {
                return message.substring("java.io.".length());
            } else if (message.startsWith("javax.swing.")) {
                return message.substring("javax.swing.".length());
            } else {
                return message;
            }
        }

    }

    private Debug() { /* no instantiation */ }

}
