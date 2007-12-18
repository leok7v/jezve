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
import java.lang.reflect.InvocationTargetException;

public final class Time {

    private static Object sun_misc_Perf;
    private static long ticksPerSecond;
    private static Method highResCounter;

    /** @return microsecond count (since some point in time)
     */
    public static long microseconds() {
        if (Platform.getJavaVersion() >= 1.5)
            return ((Long)Call.callStatic("java.lang.System.nanoTime", Call.NONE)).longValue() / 1000;
        else {
            if (ticksPerSecond == 0) {
                loadPerf();
            }
            if (highResCounter != null) {
                long n = getCounter();
                if (ticksPerSecond > 1000000000) {
                    return n * 1000L / (ticksPerSecond / 1000);
                } else {
                    return n * 1000000L / ticksPerSecond;
                }
            }
            return System.currentTimeMillis() * 1000000;
        }
    }

    /**
     * @param microseconds time to format into string
     * @return string formated milliseconds like 1234 microseconds becomes "1.23" milliseconds
     */
    public static String milliseconds(long microseconds) {
        microseconds /= 10;
        int d = (int)(microseconds % 100);
        return microseconds / 100 + (d < 10 ? ".0" + d : "." + d);
    }

    private static void loadPerf() {
        ticksPerSecond = 1000;
        try {
            Class c = Class.forName("sun.misc.Perf");
            Method m = c.getMethod("getPerf", Call.VOID);
            sun_misc_Perf = m.invoke(c, Call.NONE);
            Method highResFrequency = c.getMethod("highResFrequency", Call.VOID);
            highResCounter = c.getMethod("highResCounter", Call.VOID);
            ticksPerSecond = ((Long)highResFrequency.invoke(sun_misc_Perf, Call.NONE)).longValue();
        }
        catch (Throwable ignore) { // ClassNotFoundException, NoSuchMethodException, IllegalAccessException
                                   // InvocationTargetException, NativeMethodNotFound
            ignore.printStackTrace();
            highResCounter = null;
        }
    }

    private static long getCounter() {
        assert highResCounter != null;
        try {
            return ((Long)highResCounter.invoke(sun_misc_Perf, Call.NONE)).longValue();
        } catch (InvocationTargetException e) { /* ignore */
        } catch (IllegalAccessException e) { /* ignore */
        }
        return System.currentTimeMillis();
    }

    private Time() { /* static class */ }
}
