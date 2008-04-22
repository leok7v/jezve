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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.io.*;

public final class MacOSX {

    private static Method setEnabledPreferencesMenu;
    private static Method setEnabledAboutMenu;
    private static Method setHandled;
    private static Method getFilename;
    private static Method findFolder;
    private static Object application;
    private static boolean isPlafInitialized; // is plaf inititailized yet?
    private static boolean isMetal;

    public static final int
    /*
        /System/Library/Frameworks/CoreServices.framework/Versions/A/Frameworks/CarbonCore.framework/Versions/A/Headers/Folders.h
    */
    // folderDomain:
    kOnSystemDisk                 = -32768, // previously was 0x8000 but that is an unsigned value whereas vRefNum is signed
    kOnAppropriateDisk            = -32767, // Generally, the same as kOnSystemDisk, but it's clearer that this isn't always the 'boot' disk.
                                            // Folder Domains - Carbon only.  The constants above can continue to be used, but the folder/volume returned will
                                            // be from one of the domains below.
    kSystemDomain                 = -32766, // Read-only system hierarchy.
    kLocalDomain                  = -32765, // All users of a single machine have access to these resources.
    kNetworkDomain                = -32764, // All users configured to use a common network server has access to these resources.
    kUserDomain                   = -32763, // Read/write. Resources that are private to the user.
    kClassicDomain                = -32762, // Domain referring to the currently configured Classic System Folder

    // folderType:

    kSystemFolderType             = fourCC("macs"), // the system folder
    kDesktopFolderType            = fourCC("desk"), // the desktop folder; objects in this folder show on the desk top.
    kSystemDesktopFolderType      = fourCC("sdsk"), // the desktop folder at the root of the hard drive), never the redirected user desktop folder
    kTrashFolderType              = fourCC("trsh"), // the trash folder; objects in this folder show up in the trash
    kSystemTrashFolderType        = fourCC("strs"), // the trash folder at the root of the drive), never the redirected user trash folder
    kWhereToEmptyTrashFolderType  = fourCC("empt"), // the "empty trash" folder; Finder starts empty from here down
    kPrintMonitorDocsFolderType   = fourCC("prnt"), // Print Monitor documents
    kStartupFolderType            = fourCC("strt"), // Finder objects (applications), documents), DAs), aliases), to...) to open at startup go here
    kShutdownFolderType           = fourCC("shdf"), // Finder objects (applications), documents), DAs), aliases), to...) to open at shutdown go here
    kAppleMenuFolderType          = fourCC("amnu"), // Finder objects to put into the Apple menu go here
    kControlPanelFolderType       = fourCC("ctrl"), // Control Panels go here (may contain INITs)
    kSystemControlPanelFolderType = fourCC("sctl"), // System control panels folder - never the redirected one), always "Control Panels" inside the System Folder
    kExtensionFolderType          = fourCC("extn"), // System extensions go here
    kFontsFolderType              = fourCC("font"), // Fonts go here
    kPreferencesFolderType        = fourCC("pref"), // preferences for applications go here
    kSystemPreferencesFolderType  = fourCC("sprf"), // System-type Preferences go here - this is always the system's preferences folder), never a logged in user's
                                                    //   On Mac OS X), items in the temporary items folder on the boot volume will be deleted a certain amount of time after their
                                                    //    last access.  On non-boot volumes), items in the temporary items folder may never get deleted.  Thus), the use of the
                                                    //    temporary items folder on Mac OS X is discouraged), especially for long lived data.  Using this folder temporarily ( like
                                                    //    to write a temporary copy of a document to during a save), after which you FSpExchangeFiles() to swap the new contents with
                                                    //    the old version ) is certainly ok), but using the temporary items folder to cache data is not a good idea.  Instead), look
                                                    //    at tmpfile() and its cousins for a better way to do this kind of thing.  On Mac OS X 10.4 and later), this folder is inside a
                                                    //    folder named ".TemporaryItems" and in earlier versions of Mac OS X this folder is inside a folder named "Temporary Items".
                                                    //    On Mac OS 9.x), items in the the Temporary Items folder are never automatically deleted.  Instead), when a 9.x machine boots
                                                    //    up the temporary items folder on a volume ( if one still exists), and is not empty ) is moved into the trash folder on the
                                                    //    same volume and renamed "Rescued Items from <diskname>".
    kTemporaryFolderType          = fourCC("temp")  // temporary files go here (deleted periodically), but don't rely on it.)
    ;


    public interface Events {
        void postEvent(String event);
        void postEvent(String event, Object p);
    }

    public static boolean isMetal() {
        return isMetal;
    }

    public static void setSystemProperties(String name, boolean metal) {
        // http://developer.apple.com/releasenotes/Java/java141/system_properties/chapter_4_section_3.html
        assert Platform.isMac();
        assert !isPlafInitialized;
        isMetal = metal;
        if (metal) {
            // GrowBox painting is broken in Java prio to 1.6
            // StatusBar will paint it by hand.
/*
            System.setProperty("apple.awt.brushMetalLook", "true");
            System.setProperty("apple.awt.brushMetalRounded", "true");
*/
            System.setProperty("apple.awt.showGrowBox", "false");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "true");
        }
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.graphics.UseQuartz", "true"); // DDX?
        System.setProperty("apple.awt.textantialiasing","true");
        System.setProperty("com.apple.mrj.application.live-resize", "true");
        System.setProperty("com.apple.macos.smallTabs","false");
        // this must be set before first call to Mac OS X, otherwise it won't work:
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", name);
        // also see: http://developer.apple.com/technotes/tn2007/tn2196.html
        isPlafInitialized = true;
    }

    public static void init(Events dispatch) {
        assert Platform.isMac();
        assert isPlafInitialized : "setSystemLookAndFeel must be already called";
        try {
            Class classApplication = Class.forName("com.apple.eawt.Application");
            Class classListenner = Class.forName("com.apple.eawt.ApplicationListener");
            Class classApplicationEvent = Class.forName("com.apple.eawt.ApplicationEvent");
            application = Call.callStatic("com.apple.eawt.Application.getApplication", Call.NONE);
            setEnabledPreferencesMenu = Call.getMethod(classApplication, "setEnabledPreferencesMenu", Call.BOOLEAN);
            setEnabledAboutMenu = Call.getMethod(classApplication, "setEnabledAboutMenu", Call.BOOLEAN);
            setHandled = Call.getMethod(classApplicationEvent, "setHandled", Call.BOOLEAN);
            getFilename = Call.getMethod(classApplicationEvent, "getFilename", Call.VOID);

            ApplicationListenerInvocationHandler handler = new ApplicationListenerInvocationHandler(dispatch);
            Object listener = Proxy.newProxyInstance(classListenner.getClassLoader(),
                    new Class[] { classListenner }, handler);
            Object[] params = new Object[]{listener};
            Class[] sig = new Class[]{classListenner};
            Method addApplicationListener = Call.getMethod(classApplication, "addApplicationListener", sig);
            Call.call(addApplicationListener, application, params);

            setEnabledAboutMenu(true);
            setEnabledPreferencesMenu(true);
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }
    }


    public static void setEnabledAboutMenu(boolean e) {
        assert Platform.isMac();
        Object[] p = new Object[]{e ? Boolean.TRUE : Boolean.FALSE };
        Call.call(setEnabledAboutMenu, application, p);
    }

    public static String findFolder(int folderDomain, int folderType, boolean create) throws FileNotFoundException {
        assert isPlafInitialized : "setSystemLookAndFeel must be already called";
        if (findFolder == null) {
            Class[] sig = new Class[]{short.class, int.class, boolean.class};
            findFolder = Call.getDeclaredMethod("com.apple.eio.FileManager.findFolder", sig);
        }
        Object[] p = new Object[]{
            new Short((short)folderDomain), new Integer(folderType), Boolean.valueOf(create)
        };
        return (String)Call.call(findFolder, null, p);
    }

    public static void setEnabledPreferencesMenu(boolean e) {
        assert Platform.isMac();
        Object[] p = new Object[]{e ? Boolean.TRUE : Boolean.FALSE };
        Call.call(setEnabledPreferencesMenu, application, p);
    }

    private static class ApplicationListenerInvocationHandler
            implements java.lang.reflect.InvocationHandler {

        private Events dispatch;

        private ApplicationListenerInvocationHandler(Events events) {
            dispatch = events;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String m = method.getName();
            Object e = args[0];
            setHandled(e, true);
            if ("handleAbout".equals(m)) {
                dispatch.postEvent("commandHelpAbout");
            } else if ("handleQuit".equals(m)) {
                dispatch.postEvent("commandFileExit");
            } else if ("handlePreferences".equals(m)) {
                dispatch.postEvent("commandPreferences");
            } else if ("handleOpenApplication".equals(m)) {
                dispatch.postEvent("commandOpenAppication");
            } else if ("handleOpenFile".equals(m)) {
                Object[] p = new Object[]{getFilename(e)};
                dispatch.postEvent("commandOpenAppication", p);
            } else if ("handlePrintFile".equals(m)) {
                Object[] p = new Object[]{getFilename(e)};
                dispatch.postEvent("commandFilePrint", p);
            } else if ("handleReOpenApplication".equals(m)) {
                dispatch.postEvent("commandReOpenApplication");
            } else {
                throw new Error("unhandled method: " + m);
            }
            return null;
        }

        private String getFilename(Object e) {
            return (String)Call.call(getFilename, e, Call.NONE);
        }

        private void setHandled(Object e, boolean b) {
            Object[] p = new Object[]{b ? Boolean.TRUE : Boolean.FALSE};
            Call.call(setHandled, e, p);
        }

    }

    public static int fourCC(String s) {
        // TODO: test on PPC
        // should fourCC work in reverse on PPC? Hell if I know!
        assert s.length() == 4;
        long cc4 = 0;
        for (int i = 0; i < s.length(); i++) {
            cc4 = (cc4 << 8) | (s.charAt(i) & 0xFF);
        }
        return (int)cc4;
    }

}
