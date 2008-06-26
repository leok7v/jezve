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