package org.jezve.notepad.text.format;

import org.jezve.notepad.text.document.AttributeMap;

import java.awt.font.*;
import java.util.*;

// This class is used by the Formatter to estimate the height of characters in a particular style.
final class DefaultCharacterMetric {

    final class Metric {
        private float fAscent;
        private float fDescent;
        private float fLeading;

        private Metric(float ascent, float descent, float leading) {

            fAscent = ascent;
            fDescent = descent;
            fLeading = leading;
        }

        public float getAscent() {
            return fAscent;
        }

        public float getDescent() {
            return fDescent;
        }

        public float getLeading() {
            return fLeading;
        }
    }

    private final Hashtable fCache = new Hashtable();
    private FontResolver fResolver;
    private FontRenderContext fFrc;

    public DefaultCharacterMetric(FontResolver resolver, FontRenderContext frc) {
        fResolver = resolver;
        fFrc = frc;
    }

    // Get a DefaultCharacterMetric instance for the given style. 
    // The style is first resolved with FontResolver.
    public Metric getMetricForStyle(AttributeMap style) {
        style = fResolver.applyFont(style);
        Metric metric = (Metric)fCache.get(style);
        if (metric == null) {
            TextLayout layout = new TextLayout(" ", style, fFrc);
            metric = new Metric(layout.getAscent(), layout.getDescent(), layout.getLeading());
            fCache.put(style, metric);
        }
        return metric;
    }
}