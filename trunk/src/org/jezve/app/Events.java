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
import java.util.List;
import java.awt.*;
import java.lang.reflect.*;

public final class Events {

    private static List listeners = new LinkedList();

    public static void addListener(Object listener) {
        assert EventQueue.isDispatchThread();
        assert !listeners.contains(listener) : "can only be added once";
        listeners.add(listener);
    }

    public static void removeListener(Object listener) {
        assert EventQueue.isDispatchThread();
        assert listeners.contains(listener) : "not added or already removed";
        listeners.remove(listener);
    }

    /** Thread safe
     * @param method to invoke later from all listeners on empty stack
     */
    public static void postEvent(final String method) {
        EventQueue.invokeLater(new Runnable(){
            public void run() {
                invokeMethod(method);
            }
        });
    }

    /** Thread safe
     * @param method to invoke later from all listeners on empty stack
     * @param param parameter to pass to the method
     */
    public static void postEvent(final String method, final Object param) {
        EventQueue.invokeLater(new Runnable(){
            public void run() {
                invokeMethod(method, Call.OBJECT, new Object[]{param});
            }
        });
    }

    /** call updateCommandState(Map) from all listeners that implement it.
      * @param state command state (commandFileOpen -> Boolean)
     */
    public static void collectCommandState(Map state) {
        Object[] p = new Object[]{state};
        for (Iterator i = listeners.iterator(); i.hasNext(); ) {
            Object listener = i.next();
            try {
                Class c = listener.getClass();
                Method updateCommandState = c.getMethod("updateCommandState", Call.MAP);
                Call.call(updateCommandState, listener, p);
            } catch (NoSuchMethodException e) {
                /* the updateCommandState is optional */
            }
        }
    }

    private static void invokeMethod(String method) {
        invokeMethod(method, Call.VOID, Call.NONE);
    }

    private static void invokeMethod(String method, Class[] signature, Object[] params) {
        Object[] clone = listeners.toArray();
        for (int i = 0; i < clone.length; i++) {
            Object listener = clone[i];
            try {
                Method m = listener.getClass().getMethod(method, signature);
                Call.call(m, listener, params);
            } catch (NoSuchMethodException e) {
                /* method is optional */
            }
        }
    }

    private Events() { }

}
