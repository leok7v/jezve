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

/*
    All version methods getSomethingVersion() the numbers can be not exact.
    1.6.0_03 may and will be returned as something like 1.600299997...
    because some of decimal numbers do not have exact binary double representation.
    Safe way to check that getJavaVersion() >= 1.6.0.3 is to check instead:
    if (getJavaVersion() > 1.6002) {
         ...
    }
*/

public final class Platform {

    private static boolean isMac = System.getProperty("os.name").toLowerCase().indexOf("mac os x") >= 0;
    private static boolean isLinux = System.getProperty("os.name").toLowerCase().indexOf("linux") >= 0;
    private static boolean isWindows = System.getProperty("os.name").toLowerCase().indexOf("windows") >= 0;
    private static String processor = System.getProperty("os.arch");
    private static float osVersion = parseOsVersion();
    private static final float javaVersion = parseJavaVersion();

    private Platform() { /* no instantiation */ }

    public static boolean isMac() {
        return isMac;
    }

    public static boolean isLinux() {
        return isLinux;
    }

    public static boolean isWindows() {
        return isWindows;
    }

    /**
     * @return known processor types: "i386", "ppc"
     */
    public static String getProcessor() {
        return processor;
    }

    /**
     * @return ~1.5006 for 1.5.0_6.
     */
    public static float getJavaVersion() {
        return javaVersion;
    }

    /**
     * @return 10.48 for 10.4.8 or 10.39 for 10.3.9
     */
    public static float getOsVersion() {
        return osVersion;
    }

    public static float parseJavaVersion() {
        String v = System.getProperty("java.version");
        int ix = v.indexOf('.');
        if (ix > 0) {
            String s = v.substring(ix + 1);
            v = v.substring(0, ix + 1) + s.replaceAll("\\.", "").replaceAll("_", "");
        }
        ix = 0;
        while (ix < v.length() && (Character.isDigit(v.charAt(ix)) ||
                                   v.charAt(ix) == '.')) {
            ix++;
        }
        v = v.substring(0, ix);
        return Float.parseFloat(v);
    }

    public static float parseOsVersion() {
        String v = System.getProperty("os.version");
        // Mac: 10.4.8
        // Windows: 5.1
        if (!isMac && !isWindows) {
            assert false : v + " debug me on Windows";
        }
        int ix = v.indexOf('.');
        if (ix > 0) {
            String s = v.substring(ix + 1);
            v = v.substring(0, ix + 1) + s.replaceAll("\\.", "").replaceAll("_", "");
        }
        ix = 0;
        while (ix < v.length() && (Character.isDigit(v.charAt(ix)) ||
                                   v.charAt(ix) == '.')) {
            ix++;
        }
        v = v.substring(0, ix);
        return Float.parseFloat(v);
    }

}
