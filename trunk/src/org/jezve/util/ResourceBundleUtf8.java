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

import java.io.UnsupportedEncodingException;
import java.util.*;

// see: http://www.thoughtsabout.net/blog/archives/000044.html

@SuppressWarnings({"unchecked"})
public abstract class ResourceBundleUtf8 {

    public static ResourceBundle getBundle(String baseName) {
        ResourceBundle bundle = ResourceBundle.getBundle(baseName);
        return createUtf8PropertyResourceBundle(bundle);
    }

    public static ResourceBundle getBundle(String baseName, Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale);
        return createUtf8PropertyResourceBundle(bundle);
    }

    public static ResourceBundle getBundle(String baseName, Locale locale, ClassLoader loader) {
        ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale, loader);
        return createUtf8PropertyResourceBundle(bundle);
    }

    private static ResourceBundle createUtf8PropertyResourceBundle(ResourceBundle bundle) {
        if (!(bundle instanceof PropertyResourceBundle)) {
            return bundle;
        }
        return new Utf8PropertyResourceBundle((PropertyResourceBundle)bundle);
    }

    private static class Utf8PropertyResourceBundle extends ResourceBundle {

        PropertyResourceBundle bundle;

        private Utf8PropertyResourceBundle(PropertyResourceBundle bundle) {
            this.bundle = bundle;
        }

        public Enumeration getKeys() {
            return bundle.getKeys();
        }

        protected Object handleGetObject(String key) {
            String value = bundle.getString(key);
            if (value == null) {
                return value;
            }
            try {
                return new String(value.getBytes("ISO-8859-1"), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                assert false : value;
                return null;
            }
        }

    }
}
