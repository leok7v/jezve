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

import java.util.Map;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public final class Call {

    /* frequently used parameters */
    public static final Class[] VOID = new Class[]{};
    public static final Class[] OBJECT = new Class[]{Object.class};
    public static final Class[] STRING = new Class[]{String.class};
    public static final Class[] BOOLEAN = new Class[]{boolean.class};
    public static final Class[] MAP = new Class[]{Map.class};
    public static final Object[] NONE = new Object[]{};

    /** calls static function via fully qualified name.
     * @param method fully qualified method name to call
     * @param params parameters for the method.
     * @return result
     */
    public static Object callStatic(String method, Object[] params) {
        try {
            int ix = method.lastIndexOf('.');
            String cls = method.substring(0, ix);
            String mtd = method.substring(ix + 1);
            Class[] signature;
            if (params.length == 0) {
                signature = VOID;
            } else {
                signature = new Class[params.length];
                for (int i = 0; i < params.length; i++) {
                    signature[i] = params[i].getClass();
                }
            }
            return Class.forName(cls).getMethod(mtd, signature).invoke(null, params);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }

    /* absense of class is treated as fatal error */

    public static Class getClass(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }

    /* absense of method is treated as fatal error */

    public static Method getMethod(Class c, String method, Class[] signature) {
        try {
            return c.getMethod(method, signature);
        } catch (NoSuchMethodException e) {
            throw new Error(e);
        }
    }

    /* illegal access or invocation exception treated as fatal error */

    public static Object call(Method m, Object instance, Object[] p) {
        try {
            return m.invoke(instance, p);
        } catch (IllegalAccessException e) {
            throw new Error(e);
        } catch (InvocationTargetException e) {
            throw new Error(e);
        }
    }

    public static Method getDeclaredMethod(String method, Class[] s) {
        try {
            int ix = method.lastIndexOf('.');
            String cls = method.substring(0, ix);
            String mtd = method.substring(ix + 1);
            Method m = Class.forName(cls).getDeclaredMethod(mtd, s);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            Debug.printStackTrace("no such method " + method, e);
            return null;
        } catch (ClassNotFoundException e) {
            Debug.printStackTrace("class not found " + method, e);
            return null;
        }
    }


    private Call() { /* no instantiation */ }
}
