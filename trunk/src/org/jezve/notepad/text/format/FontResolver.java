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

package org.jezve.notepad.text.format;

import org.jezve.notepad.text.document.AttributeMap;
import org.jezve.notepad.text.document.TextAttribute;

import java.awt.*;
import java.util.*;

final class FontResolver {

    static {
        // Even though it violates the Prime Directive I'll conditionalize this anyway,
        // since it is just a 1.2 workaround which I greatly resent.
        java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
    }

    private Hashtable styleMap;
    private final AttributeMap fDefaultFontMap;

    public FontResolver(AttributeMap defaults) {
        styleMap = new Hashtable();
        Hashtable tempMap = new Hashtable();
        tempMap.put(TextAttribute.FAMILY, defaults.get(TextAttribute.FAMILY));
        tempMap.put(TextAttribute.WEIGHT, defaults.get(TextAttribute.WEIGHT));
        tempMap.put(TextAttribute.POSTURE, defaults.get(TextAttribute.POSTURE));
        tempMap.put(TextAttribute.SIZE, defaults.get(TextAttribute.SIZE));
        fDefaultFontMap = new AttributeMap(tempMap);
    }

    // Fetch result of resolve(style) from cache, if present.
    public AttributeMap applyFont(AttributeMap style) {
        Object cachedMap = styleMap.get(style);
        if (cachedMap == null) {
            AttributeMap resolvedMap = resolve(style);
            styleMap.put(style, resolvedMap);
            return resolvedMap;
        }
        else {
            return (AttributeMap)cachedMap;
        }
    }

    // Return an AttributeMap containing a Font computed from the attributes in <tt>style</tt>.
    public AttributeMap resolve(AttributeMap style) {
        if (style.get(TextAttribute.FONT) != null) {
            return style;
        }
        Font font = Font.getFont(fDefaultFontMap.addAttributes(style));
        return style.addAttribute(TextAttribute.FONT, font);
    }
}
