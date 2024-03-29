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

import java.util.*;
import java.io.*;

@SuppressWarnings({"UnnecessaryBoxing"})
public final class UserSettings {

    /* Known issues (also on Mac for non-admin):
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4398496
     * and
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6572807
     * leaves Preferences usage out of question (despite of promissed convinience).
     *
     * UserSettings are used from main thread durring static { } initalization
     * and from EDT later on. To be on a safe side the access is synchronized.
     *
     * Note that Properties are based on HashTable which has synchronized access.
     */

    private static Properties user;
    private static File prefs;
    private static boolean isDirty;

    public static void sync() {
        Misc.invokeLater(2000, new Runnable(){
            public void run() {
                flush(true);
            }
        });
    }

    public static void flush() {
        Misc.invokeLater(2000, new Runnable(){
            public void run() {
                flush(false);
            }
        });
    }

    public static void flushNow() {
        flush(false);
    }

    public static void clear()  {
        synchronized (getUser()) {
            prefs.delete();
            getUser().clear();
        }
    }

    /** Map of all properties as String -> String name value pairs
     * @return unmodifiable view of all user properties.
     */
    public static Map getAll() {
        Properties u = getUser();
        synchronized (u) {
            return Collections.unmodifiableMap(u);
        }
    }

    private static Object putObject(String key, Object value) {
        assert value != null;
        synchronized (getUser()) {
            String v = value.toString();
            Object was = getUser().put(key, v);
            isDirty = isDirty || !Misc.equals(was, value);
            return was;
        }
    }

    public static Object put(String key, String value) {
        return putObject(key, value);
    }

    public static Object putInt(String key, int value) {
        return putObject(key, new Integer(value));
    }

    public static Object putLong(String key, long value) {
        return putObject(key, new Long(value));
    }

    public static Object putBoolean(String key, boolean value) {
        return putObject(key, Boolean.valueOf(value));
    }

    public static String get(String key, String value) {
        String r = (String)getUser().get(key);
        return r == null ? value : r;
    }

    public static int getInt(String key, int value) {
        String r = (String)getUser().get(key);
        return r == null ? value : Integer.parseInt(r, 10);
    }

    public static long getLong(String key, long value) {
        String r = (String)getUser().get(key);
        return r == null ? value : Long.parseLong(r, 10);
    }

    public static boolean getBoolean(String key, boolean value) {
        String r = (String)getUser().get(key);
        return r == null ? value : "true".equals(r);
    }

    private static synchronized Properties getUser() {
        if (prefs == null) {
            String app = Main.APPLICATION.toLowerCase();
            String pkg = Main.class.getPackage().getName();
            File dir = new File(IO.getUserPreferences(), pkg);
            dir.mkdirs();
            prefs = new File(dir, app + ".properties");
        }
        if (user == null) {
            user = new Properties();
            load();
            sync();
        }
        return user;
    }

    private static void flush(boolean sync) {
        int retry = 3;
        while (retry >= 0) {
            OutputStream os = null;
            try {
                if (isDirty) {
                    os = new FileOutputStream(prefs);
                    getUser().store(os, Main.APPLICATION.toLowerCase() + " preferences");
                    os.flush();
                    isDirty = false;
//                  Debug.traceln("UserSettings.flushed");
                }
                if (sync) {
                    load();
                }
                return;
            } catch (IOException e) {
                if (retry == 0) {
                    Debug.printStackTrace(e);
                    throw new Error(e);
                }
            } finally {
                IO.close(os);
            }
            // multiple instances of Application may call sync simulteniously.
            Misc.sleep((int)(100 * Math.random()) + 1);
            retry--;
        }
    }

    private static void load() {
        int retry = 3;
        while (retry >= 0) {
            InputStream is = null;
            try {
                if (prefs.exists()) {
                    is = new FileInputStream(prefs);
                    user.load(is);
//                  Debug.traceln("UserSettings.loaded");
                }
                return;
            } catch (IOException e) {
                if (retry == 0) {
                    Debug.printStackTrace(e);
                    throw new Error(e);
                }
            } finally {
                IO.close(is);
            }
            // multiple instances of Application may call load/flush/sync simulteniously.
            Misc.sleep((int)(100 * Math.random()) + 1);
            retry--;
        }
    }

    private UserSettings() { }

}
