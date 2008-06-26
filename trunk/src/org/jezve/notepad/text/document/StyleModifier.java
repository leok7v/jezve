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

package org.jezve.notepad.text.document;

/**
 * StyleModifier is the base class for operations on AttributeMap.  To implement
 * an operation on AttributeMap, subclass StyleModifier and override
 * <code>modifyStyle</code>.  StyleModifiers are used by MText.
 * <p/>
 * For convenience, this class contains factory methods which will create a
 * StyleModifier for
 * certain common operations: attribute union, attribute removal, and AttributeMap
 * replacement.
 *
 * @see AttributeMap
 * @see AttributeSet
 * @see MText
 */
/*
 * {jbr} StyleModifier is not the best name for this class - styles are immutable and never
 * really modified.
 */
public class StyleModifier {
    
    protected StyleModifier() {
    }

    /**
     * @param style the AttributeMap to perform the operation on
     * @return the result of this StyleModifier's operation on the given style.
     *         Default implementation just returns the given style.
     */
    public AttributeMap modifyStyle(AttributeMap style) {
        return style;
    }

    /**
     * A StyleModifier which simply returns the given style.
     */
    public static final StyleModifier IDENTITY = new StyleModifier();

    /**
     * Create a StyleModifier whose operation is
     * <code>style.addAttributes(s)</code>,
     * where <code>style</code> is the AttributeMap passed to
     * <code>modifyStyle</code>.
     *
     * @param s the AttributeMap to union with
     * @return a StyleModifier for this operation
     */
    public static StyleModifier createAddModifier(AttributeMap s) {
        return new StyleAddModifier(s);
    }

    /**
     * Create a StyleModifier whose operation is
     * <code>style.addAttribute(key, value)</code>,
     * where <code>style</code> is the AttributeMap passed to
     * <code>modifyStyle</code>.
     *
     * @param key   the key to add
     * @param value the value to add
     * @return a StyleModifier for this operation
     */
    public static StyleModifier createAddModifier(Object key, Object value) {
        return new AttributeAddModifier(key, value);
    }

    /**
     * Create a StyleModifier whose operation returns <code>s</code>,
     * ignoring the parameter to <code>modifyStyle</code>.
     *
     * @param s the AttributeMap which will replace any other AttributeMap
     * @return a StyleModifier for this operation
     */
    public static StyleModifier createReplaceModifier(AttributeMap s) {
        return new StyleReplaceModifier(s);
    }

    /**
     * Create a StyleModifier whose operation is
     * <code>style.removeAttributes(s)</code>,
     * where <code>style</code> is the AttributeMap passed to
     * <code>modifyStyle</code>.
     *
     * @param s the AttributeSet of attributes to remove
     * @return a StyleModifier for this operation
     */
    public static StyleModifier createRemoveModifier(AttributeSet s) {
        return new StyleRemoveModifier(s);
    }

    static final class AttributeAddModifier extends StyleModifier {

        private Object fKey;
        private Object fValue;

        public AttributeAddModifier(Object key, Object value) {
            fKey = key;
            fValue = value;
        }

        public AttributeMap modifyStyle(AttributeMap style) {
            return style.addAttribute(fKey, fValue);
        }
    }

    /**
     * Create this with the styles to add.  These styles will add to and override any already
     * present in the style passed to modifyStyle.
     */
    static final class StyleAddModifier extends StyleModifier {
        private AttributeMap fStyle;

        public StyleAddModifier(AttributeMap style) {
            if (style == null) {
                throw new IllegalArgumentException("style is null");
            }
            fStyle = style;
        }

        public AttributeMap modifyStyle(AttributeMap style) {
            return style.addAttributes(fStyle);
        }
    }

    /**
     * Create this with the styles to replace.  All style runs will have only these styles.
     */
    static final class StyleReplaceModifier extends StyleModifier {
        private AttributeMap fStyle;

        public StyleReplaceModifier(AttributeMap style) {
            if (style == null) {
                throw new IllegalArgumentException("style is null");
            }
            fStyle = style;
        }

        public AttributeMap modifyStyle(AttributeMap style) {
            return fStyle;
        }
    }

    static final class StyleRemoveModifier extends StyleModifier {

        private AttributeSet fRemoveSet;

        public StyleRemoveModifier(AttributeSet removeSet) {

            if (removeSet == null) {
                throw new IllegalArgumentException("set is null");
            }
            fRemoveSet = removeSet;
        }

        public AttributeMap modifyStyle(AttributeMap style) {
            return style.removeAttributes(fRemoveSet);
        }
    }
}
