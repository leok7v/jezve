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

import org.jezve.notepad.text.document.*;
import org.jezve.notepad.text.document.TextAttribute;

import java.awt.font.*;
import java.awt.geom.*;

public final class ParagraphRenderer {

    //private static final int FLUSH_LEADING = TextAttribute.FLUSH_LEADING.intValue();
    private static final int FLUSH_CENTER = TextAttribute.FLUSH_CENTER.intValue();
    private static final int FLUSH_TRAILING = TextAttribute.FLUSH_TRAILING.intValue();
    private static final int FULLY_JUSTIFIED = TextAttribute.FULLY_JUSTIFIED.intValue();

    private AttributeMap cacheStyle = null;

    private float fLeadingMargin;
    private float fTrailingMargin;
    private float fFirstLineIndent;
    private float fMinLineSpacing;
    private float fExtraLineSpacing;

    private int fFlush = -1;
    private TabRuler fTabRuler;

    private boolean fLtrDefault;
    private DefaultCharacterMetric fDefaultCharMetric;

    ParagraphRenderer(AttributeMap pStyle, DefaultCharacterMetric defaultCharMetric) {
        fDefaultCharMetric = defaultCharMetric;
        initRenderer(pStyle);
    }

    private float getFloatValue(Object key, AttributeMap style) {
        return ((Float)style.get(key)).floatValue();
    }

    private int getIntValue(Object key, AttributeMap style) {
        return ((Integer)style.get(key)).intValue();
    }

    /*
     * NOTE:  it is illegal to initialize a StandardParagraphRenderer for any style
     * other than the one it was created with.
     */
    public void initRenderer(AttributeMap pStyle) {

        if (cacheStyle == null) {

            fLeadingMargin = getFloatValue(TextAttribute.LEADING_MARGIN, pStyle);
            fTrailingMargin = getFloatValue(TextAttribute.TRAILING_MARGIN, pStyle);
            fFirstLineIndent = getFloatValue(TextAttribute.FIRST_LINE_INDENT, pStyle);
            fMinLineSpacing = getFloatValue(TextAttribute.MIN_LINE_SPACING, pStyle);
            fExtraLineSpacing = getFloatValue(TextAttribute.EXTRA_LINE_SPACING, pStyle);

            fFlush = getIntValue(TextAttribute.LINE_FLUSH, pStyle);

            fTabRuler = (TabRuler)pStyle.get(TextAttribute.TAB_RULER);

            Object runDir = pStyle.get(TextAttribute.RUN_DIRECTION);
            fLtrDefault = !TextAttribute.RUN_DIRECTION_RTL.equals(runDir);

            cacheStyle = pStyle;
        }
        else if (pStyle != cacheStyle) {
            if (!pStyle.equals(cacheStyle)) {
                throw new Error("Attempt to share ParagraphRenderer between styles!");
            }
            else {
                cacheStyle = pStyle;
            }
        }
    }

    static boolean isTab(char ch) {
        return ch == '\t';
    }

    /*
     * Fill in info with the next line.
     *
     * @param measurer the LineBreakMeasurer for this paragraph.
     *                 Current position should be the first character on the line.
     *                 If null, a 0-length line is generated.  If measurer is null
     *                 then paragraphStart and paragraphLimit should be equal.
     */
    // Usually totalFormatWidth and lineBound will be the same.
    // totalFormatWidth is used for wrapping, but lineBound is
    // for flushing.  These may be different for unwrapped text,
    // for example.
    public LineLayout layout(MConstText text, LineLayout line, LineBreakMeasurer measurer,
            FontRenderContext frc, int paragraphStart, int paragraphLimit, float totalFormatWidth,
            float lineBound) {

        if ((measurer == null) != (paragraphStart == paragraphLimit)) {
            throw new IllegalArgumentException(
                    "measurer, paragraphStart, paragraphLimit are wrong.");
        }
        if (line == null) {
            line = new LineLayout();
        }

        final int lineCharStart = measurer == null ? paragraphStart : measurer.getPosition();
        line.setCharStart(lineCharStart);
        final float lineIndent = (lineCharStart == paragraphStart) ? fFirstLineIndent : 0;
        float formatWidth = totalFormatWidth - (fLeadingMargin + fTrailingMargin);
        computeLineMetrics(text, line, measurer, paragraphStart, paragraphLimit, formatWidth,
                lineIndent);

        // position the line according to the line flush
        if (fFlush == FLUSH_TRAILING || fFlush == FLUSH_CENTER) {
            float lineArea = lineBound - (fLeadingMargin + fTrailingMargin);
            float advanceDifference = lineArea - line.fVisibleAdvance;

            if (fFlush == FLUSH_TRAILING) {
                line.fLeadingMargin = fLeadingMargin + advanceDifference;
            }
            else if (fFlush == FLUSH_CENTER) {
                line.fLeadingMargin = fLeadingMargin + advanceDifference / 2;
            }
        }
        else {
            line.fLeadingMargin = fLeadingMargin;
        }
        return line;
    }

    /*
     * Fill in the following fields in line:
     * fCharLength, fAscent, fDescent, fLeading, fVisibleAdvance,
     * fTotalAdvance.
     * Uses: line.fLeadingMargin
     *
     * @param formatWidth the width to fit the line into.
     */
    private void computeLineMetrics(MConstText text, LineLayout line, LineBreakMeasurer measurer,
            final int paragraphStart, final int paragraphLimit, float formatWidth,
            float lineIndent) {

        int segmentCount = 0;
        /* variable not used boolean firstLine = measurer==null ||
                            measurer.getPosition() == paragraphStart; */

        if (measurer != null) {
            computeSegments(text, line, measurer, paragraphLimit, formatWidth, lineIndent);

            // iterate through segments and accumulate ascent, descent,
            // leading, char length
            float ascent = 0;
            float descent = 0;
            float descentPlusLeading = 0;

            segmentCount = line.fSegments.size();
            for (int i = 0; i < segmentCount; i++) {
                TextLayout layout = ((LineLayout.Segment)line.fSegments.elementAt(i)).layout;
                ascent = Math.max(ascent, layout.getAscent());
                float segDescent = layout.getDescent();
                descent = Math.max(descent, segDescent);
                descentPlusLeading = Math.max(descentPlusLeading, segDescent + layout.getLeading());
                line.fCharLength += layout.getCharacterCount();
            }
            line.fAscent = (float)Math.ceil(ascent);
            line.fDescent = (float)Math.ceil(descent);
            line.fLeading = (float)Math.ceil(descentPlusLeading) - line.fDescent;
        }
        else {
            line.fLeftToRight = fLtrDefault;
            line.fSegments.removeAllElements();

            line.fCharLength = 0;

            AttributeMap style = text.characterStyleAt(paragraphStart);
            DefaultCharacterMetric.Metric cm = fDefaultCharMetric.getMetricForStyle(style);
            line.fAscent = cm.getAscent();
            line.fDescent = cm.getDescent();
            line.fLeading = cm.getLeading();
            line.fVisibleAdvance = line.fTotalAdvance = 0;
        }

        if (fExtraLineSpacing != 0) {
            line.fAscent += (int)Math.ceil(fExtraLineSpacing);
        }

        if (fMinLineSpacing != 0) {
            float height = line.getHeight();
            if (height < fMinLineSpacing) {
                line.fAscent += Math.ceil(fMinLineSpacing - height);
            }
        }

        float lineNaturalAdvance = line.fTotalAdvance;

        line.fTotalAdvance += lineIndent;
        line.fVisibleAdvance += lineIndent;

        if (measurer != null) {
            // Now fill in bounds field of BidiSegments.  bounds should tile
            // the line.
            final float lineHeight = line.getHeight();

            for (int i = 1; i < segmentCount; i++) {

                LineLayout.Segment currentSegment = (LineLayout.Segment)line.fSegments.elementAt(i - 1);
                LineLayout.Segment nextSegment = (LineLayout.Segment)line.fSegments.elementAt(i);

                float origin;
                float width;

                if (line.fLeftToRight) {
                    origin = 0;
                    width = nextSegment.offset - currentSegment
                            .offset;
                }
                else {
                    origin = currentSegment.offset;
                    origin -= nextSegment.offset;
                    origin += nextSegment.layout.getAdvance();
                    width = currentSegment.layout.getAdvance() - origin;
                }
                currentSegment.bounds = new Rectangle2D.Float(origin, -line.fAscent, width, lineHeight);
            }

            // set last segment's bounds
            {
                LineLayout.Segment currentSegment = (LineLayout.Segment)line.fSegments.elementAt(segmentCount - 1);
                float origin;
                float width;

                if (line.fLeftToRight) {
                    origin = 0;
                    width = lineNaturalAdvance - currentSegment.offset;
                }
                else {
                    origin = currentSegment.offset - lineNaturalAdvance;
                    width = currentSegment.layout.getAdvance() - origin;
                }
                currentSegment.bounds = new Rectangle2D.Float(origin, -line.fAscent, width, lineHeight);
            }
        }
    }

    // Fill in fSegments, fLeftToRight.  measurer must not be null
    private void computeSegments(MConstText text, LineLayout line, LineBreakMeasurer measurer,
            final int paragraphLimit, float formatWidth, float lineIndent) {

        // Note on justification:  only the last segment of a line is
        // justified.  
        // Also, if a line ends in a tab it will not be justified.
        // This behavior is consistent with other word processors
        // I tried (MS Word and Lotus Word Pro).

        line.fSegments.removeAllElements();
        line.fCharLength = 0;

        TabStop currentTabStop = new TabStop(fLeadingMargin + lineIndent, TabStop.kLeading);

        int segmentLimit = measurer.getPosition();
        boolean firstSegment = true;

        float advanceFromLeadingMargin = lineIndent;

        boolean computeSegs = true;

        do {
            // compute sementLimit:
            if (segmentLimit <= measurer.getPosition()) {
                while (segmentLimit < paragraphLimit) {
                    if (isTab(text.at(segmentLimit++))) {
                        break;
                    }
                }
            }

            // NOTE:  adjust available width for center tab!!!
            //System.out.println("Format width: " + (formatWidth-advanceFromLeadingMargin) +
            //                   ";  segmentLimit: " + segmentLimit);

            float wrappingWidth = Math.max(formatWidth - advanceFromLeadingMargin, 0);
            TextLayout layout = null;
            if (firstSegment || wrappingWidth > 0 || segmentLimit > measurer.getPosition() + 1) {
                layout = measurer.nextLayout(wrappingWidth, segmentLimit, !firstSegment);
            }

            if (layout == null) {
                if (firstSegment) {
                    // I doubt this would happen, but check anyway
                    throw new Error("First layout is null!");
                }
                break;
            }

            final int measurerPos = measurer.getPosition();
            if (measurerPos < segmentLimit) {
                computeSegs = false;
                if (fFlush == FULLY_JUSTIFIED) {
                    layout = layout.getJustifiedLayout(wrappingWidth);
                }
            }
            else {
                computeSegs = !(measurerPos == paragraphLimit);
            }

            if (firstSegment) {
                firstSegment = false;
                // Have to get ltr off of layout.  Not available from measurer,
                // unfortunately.
                line.fLeftToRight = layout.isLeftToRight();
            }

            LineLayout.Segment segment = new LineLayout.Segment();
            segment.layout = layout;
            int layoutAdvance = (int)Math.ceil(layout.getAdvance());

            // position layout relative to leading margin, update logicalPositionOnLine

            float relativeTabPosition = currentTabStop.getPosition() - (int)fLeadingMargin;
            float logicalPositionOfLayout;
            switch (currentTabStop.getType()) {
            case TabStop.kTrailing:
                logicalPositionOfLayout =
                        Math.max(relativeTabPosition - layoutAdvance, advanceFromLeadingMargin);
                break;
            case TabStop.kCenter:
                logicalPositionOfLayout = Math.max(relativeTabPosition - (layoutAdvance / 2),
                        advanceFromLeadingMargin);
                break;
            default:  // includes decimal tab right now
                logicalPositionOfLayout = relativeTabPosition;
                break;
            }

            // position layout in segment
            if (line.fLeftToRight) {
                segment.offset = logicalPositionOfLayout;
            }
            else {
                segment.offset = logicalPositionOfLayout + layoutAdvance;
            }

            // update advanceFromLeadingMargin
            advanceFromLeadingMargin = logicalPositionOfLayout + layoutAdvance;

            // add segment to segment Vector
            line.fSegments.addElement(segment);

            // get next tab
            currentTabStop = fTabRuler.nextTab(fLeadingMargin + advanceFromLeadingMargin);
            if (currentTabStop.getType() == TabStop.kLeading ||
                    currentTabStop.getType() == TabStop.kAuto) {
                advanceFromLeadingMargin = currentTabStop.getPosition();
            }
            else {
            }

        }
        while (computeSegs);

        // Now compute fTotalAdvance, fVisibleAdvance.  These metrics may be affected by a trailing tab.
        {
            LineLayout.Segment lastSegment = (LineLayout.Segment)line.fSegments.lastElement();
            TextLayout lastLayout = lastSegment.layout;

            if (line.fLeftToRight) {
                line.fTotalAdvance = (int)Math.ceil(lastLayout.getAdvance()) + lastSegment
                        .offset;
                line.fVisibleAdvance = (int)Math.ceil(lastLayout.getVisibleAdvance()) + lastSegment
                        .offset;
            }
            else {
                line.fTotalAdvance = lastSegment.offset;
                line.fVisibleAdvance = lastSegment.offset -
                        (int)Math.ceil(lastLayout.getAdvance() - lastLayout.getVisibleAdvance());
            }

            if (isTab(text.at(measurer.getPosition() - 1))) {
                line.fTotalAdvance = Math.max(line.fTotalAdvance, currentTabStop.getPosition());
            }
        }
    }
}