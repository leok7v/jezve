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

import java.io.*;
import java.util.Set;
import java.util.Iterator;
import java.util.Collection;
import java.lang.reflect.*;

/** util.IO class makes writing code for some IO operations a little bit
 * more readable. It is mostly intended to be used in unsophisticated
 * application logic when the app is dealing with the IO objects that
 * are guarantied to be present (e.g. are resource files of the application itself).
 * Absence of such files or other objects is fatal and is treated as
 * application error and execution cannot continue.
 *
 * close() methods save a lot of clamsiness in try { } finally { } clauses.
 * not that in case close itself throws IOException (e.g. disk is full) it
 * usually happens inside try { } catch (IOException ) { } finally { } clause
 * and thus cannot be gracefully handled. If you need to make sure all the data
 * is commited before close use .flush() and handle IOException accordingly
 * to your application logic before calling close.
 */

public final class IO {

    private static File desktop;

    /** close the stream rethrowing IOException as Error.
     * @param s stream to close; can be null
     */
    public static void close(OutputStream s) {
        try {
            if (s != null) {
                s.close();
            }
        } catch (IOException e) {
            new Error(e);
        }
    }

    /** close the stream rethrowing IOException as Error.
     * @param s stream to close; can be null
     */
    public static void close(InputStream s) {
        try {
            if (s != null) {
                s.close();
            }
        } catch (IOException e) {
            new Error(e);
        }
    }

    /** close reader rethrowing IOException as Error.
     * @param r reader to close; can be null
     */
    public static void close(Reader r) {
        try {
            if (r != null) {
                r.close();
            }
        } catch (IOException e) {
            new Error(e);
        }
    }

    /** close writer rethrowing IOException as Error.
     * @param w writer to close; can be null
     */
    public static void close(Writer w) {
        try {
            if (w != null) {
                w.close();
            }
        } catch (IOException e) {
            new Error(e);
        }
    }

    /** read whole content of the stream into byte buffer
     * @param s stream to read
     * @return bytes
     * @throws IOException may be thrown
     */
    public static byte[] readFully(InputStream s) throws IOException {
        final int N = 4 *1024;
        int a = s.available();
        ByteArrayOutputStream ba = new ByteArrayOutputStream(a == 0 ? N : a);
        byte[] b = new byte[N];
        for (;;) {
            int k = s.read(b);
            if (k < 0) {
                break;
            }
            ba.write(b, 0, k);
        }
        return ba.toByteArray();
    }

    /** read whole content of the file into byte buffer
     * @param f file to read
     * @return bytes
     * @throws Error may be thrown
     */
    public static byte[] readFile(File f) {
        InputStream s = null;
        try {
            s = new FileInputStream(f);
            return readFully(s);
        } catch (IOException e) {
            throw new Error(e);
        } finally {
            close(s);
        }
    }

    /** write whole content of the buffer into the file
     * @param f file to write
     * @param bytes to write into the file
     * @throws Error may be thrown
     */
    public static void writeFile(File f, byte[] bytes) {
        OutputStream s = null;
        try {
            s = new FileOutputStream(f);
            s.write(bytes);
        } catch (IOException e) {
            throw new Error(e);
        } finally {
            close(s);
        }
    }

    /** read all lines from the file into collection of lines
     * @param file to read
     * @param lines collection to add lines to
     * @throws IOException may be thrown
     */
    public static void readLines(File file, Collection lines) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            for (;;) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                lines.add(line);
            }
        } finally {
            close(reader);
        }
    }

    /** write all lines from the collection into the file. Note it creates
     *  temporary file first and moves it upon successful completion of the operation.
     * @param file to write to
     * @param lines collection to add lines to
     * @throws IOException may be thrown
     */
    public static void writeLines(File file, Set lines) throws IOException {
        File tmp = new File(file.getParent(), "." + file.getName() + ".~.tmp");
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(tmp)));
            for (Iterator i = lines.iterator(); i.hasNext();) {
                writer.println(i.next());
            }
            writer.flush();
            writer.close();
            writer = null;
            move(tmp, file);
        } finally {
            close(writer);
        }
    }

    /** similar to File.renameTo() but if the "to" file exists deletes it before renaming.
     * it makes best possible attempt not to lose the "to" file if move is unsuccesfull.
     * @param from file to move
     * @param to file to move to
     * @throws IOException may be thrown
     */
    public static void move(File from, File to) throws IOException {
        boolean b = false;
        if (to.exists()) {
            File tmp = new File(to.getParent(), "." + to.getName() + ".~delete~.tmp");
            if (to.renameTo(tmp)) {
                b = from.renameTo(to);
                if (b) {
                    b = tmp.delete();
                } else {
                    tmp.renameTo(from);
                }
            }
        } else {
            b = from.renameTo(to);
        }
        if (!b) {
            throw new IOException("failed to move file " + from + " file " + to);
        }
    }

    public static String getHome() {
        return System.getProperty("user.home");
    }

    public static File getDocuments() {
        return new File(getHome(), Platform.isMac() ? "Documents" : "My Documents");
    }

    public static File getUserPreferences() {
        File p;
        if (Platform.isMac()) {
            try {
                String up = MacOSX.findFolder(MacOSX.kUserDomain, MacOSX.kPreferencesFolderType, true);
                p = new File(up);
            } catch (IOException e) {
                Debug.printStackTrace(e);
                p = new File(getHome(), "Library/Preferences");
            }
        } else if (Platform.isWindows()) {
            // TODO: ShGetSpecialFolder is a better way:
            p = new File(getHome(), "Local Settings\\Application Data");
        } else {
            // TODO: for Un*x there should be some kind of standard:
            p = new File(getHome(), ".java-apps-user-prefs");
        }
        try {
            p = p.getCanonicalFile();
            p.mkdirs();
            return p;
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public static File getDesktop() {
        if (desktop == null) {
            if (Platform.isMac()) {
                try {
                    String desk = MacOSX.findFolder(MacOSX.kUserDomain, MacOSX.kDesktopFolderType, true);
                    desktop = new File(desk);
                } catch (IOException e) {
                    Debug.printStackTrace(e);
                    desktop = new File(getHome(), "Desktop");
                }
            } else {
                assert Platform.isWindows();
                Method getDesktop = Call.getDeclaredMethod("sun.awt.shell.Win32ShellFolderManager2.getDesktop", Call.VOID);
                desktop = (File)Call.call(getDesktop, null, Call.NONE);
                if (desktop == null) {
                    desktop = new File(getHome(), "Desktop");
                }
            }
        }
        assert desktop.isDirectory() : desktop;
        return desktop;
    }

    private IO() { /* no instantiation */ }

}
