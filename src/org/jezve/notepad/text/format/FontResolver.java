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
