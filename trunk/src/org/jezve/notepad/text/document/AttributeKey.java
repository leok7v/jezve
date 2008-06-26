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
 * This class provides a cannonical mapping between fields in TextAttribute
 * and instances of itself.  It is used by AttributeMap to serialize
 * and deserialize TextAttribute to preserve uniqueness of TextAttribute
 * instances (ie so that TextAttribute instances remain singletons),
 * and to provide compatability between 1.1 and 1.2 versions of
 * TextAttribute.
 * <p/>
 * Example use - instead of doing this:
 * <blockquote><pre>
 *     out.writeObject(anAttribute);
 * </pre></blockquote>
 * do this:
 * <blockquote><pre>
 *     out.writeObject(AttributeKey.mapAttributeToKey(anAttribute));
 * </pre></blockquote>
 * Similarly, instead of this:
 * <blockquote><pre>
 *     anAttribute = in.readObject();
 * </pre></blockquote>
 * do this:
 * <blockquote><pre>
 *     anAttribute = AttributeKey.mapKeyToAttribute(in.readObject());
 * </pre></blockquote>
 * <p/>
 * If anAttribute is not a known TextAttribute, then <code>mapAttributeToKey</code>
 * will just return its argument.  Similarly, <code>mapKeyToAttribute</code> will
 * return its argument if the argument is not a known AttributeKey.
 */

final class AttributeKey {

    /*
        In this implementation, two parallel Vectors are
        maintained.  TextAttribute(i) maps to AttributeKey(i).
        For compatability with existing data, this mapping must
        be maintained in the future!  So, when new attributes
        are added, add them to the end of the list.
    */
    private static Object[] fgTextAttributes;
    private static Object[] fgAttributeKeys;

    static {
        fgTextAttributes = new Object[]{TextAttribute.FONT, TextAttribute.FAMILY,
                TextAttribute.WEIGHT, TextAttribute.POSTURE, TextAttribute.SIZE,
                TextAttribute.SUPERSCRIPT, TextAttribute.FOREGROUND, TextAttribute.BACKGROUND,
                TextAttribute.UNDERLINE, TextAttribute.STRIKETHROUGH,
                TextAttribute.CHAR_REPLACEMENT, TextAttribute.EXTRA_LINE_SPACING,
                TextAttribute.FIRST_LINE_INDENT, TextAttribute.MIN_LINE_SPACING,
                TextAttribute.LINE_FLUSH, TextAttribute.LEADING_MARGIN,
                TextAttribute.TRAILING_MARGIN, TextAttribute.TAB_RULER, TextAttribute.RUN_DIRECTION,
                TextAttribute.BIDI_EMBEDDING, TextAttribute.JUSTIFICATION,};

        final int attrCount = fgTextAttributes.length;
        fgAttributeKeys = new Object[attrCount];

        for (int i = 0; i < attrCount; i += 1) {
            fgAttributeKeys[i] = new AttributeKey(i);
        }
    }

    /**
     * Return the TextAttribute corresponding to the given key.
     * If key is an instance of AttributeKey it will be mapped to
     * a TextAttribute.  Otherwise, the key is returned.
     *
     * @param key the key to map to a TextAttribute field
     * @return the TextAttribute for <code>key</code> if <code>key</code>
     *         is an AttributeKey; otherwise <code>key</code> is returned
     */
    /*public*/
    static Object mapKeyToAttribute(Object key) {
        try {
            AttributeKey aKey = (AttributeKey)key;
            if (aKey.fId < fgTextAttributes.length) {
                return fgTextAttributes[aKey.fId];
            }
            else {
                return key;
            }
        }
        catch (ClassCastException e) {
            return key;
        }
    }

    /**
     * If attribute is a known TextAttribute, return an AttributeKey
     * for it.  Otherwise the object is returned.
     *
     * @param attribute the attribute to map to an AttributeKey
     * @return an AttributeKey for <code>attribute</code>
     *         if <code>attribute</code> is a known attribute; otherwise
     *         <code>attribute</code> is returned
     */
    static Object mapAttributeToKey(Object attribute) {
        final int attrCount = fgTextAttributes.length;
        for (int index = 0; index < attrCount; index += 1) {
            if (fgTextAttributes[index].equals(attribute)) {
                return fgAttributeKeys[index];
            }
        }
        return attribute;
    }

    private int fId;

    private AttributeKey(int id) {
        fId = id;
    }

    public boolean equals(Object rhs) {
        try {
            return ((AttributeKey)rhs).fId == fId;
        }
        catch (ClassCastException e) {
            return false;
        }
    }

    public int hashCode() {
        return fId;
    }
}