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

import org.jezve.notepad.text.document.MConstText;

import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.util.*;

final class LineLayout {

    final static class Segment {
        TextLayout layout;
        Rectangle2D.Float bounds;
        float offset; // was fDistanceFromLeadingMargin
    }

    // offset in text to start of line (was fStart)
    // neg. values indicate distance from end of text
    private int fCharStart;

    // min pixel offset in fill direction
    // negative values indicate distance from bottom of text view
    private float fGraphicStart;

    int fCharLength;      // number of characters on line (was fLength)
    float fAscent;
    float fDescent;
    float fLeading;
    float fVisibleAdvance;  // distance along line direction ie width
    float fTotalAdvance;    // distance along line direction including trailing whitespace

    float fLeadingMargin;   // screen distance from leading margin

    boolean fLeftToRight; // true iff the orientation is left-to-right

    final Vector fSegments = new Vector(); // segments to render, in logical order

    /*
        These methods are for storing Layouts in a gap-storage,
        relative to either the start of end of text.  See Formatter.

        If you just want absolute (that is, start-relative) char and
        graphic starts, don't make them end-relative.
    */

    public final int getCharStart(int lengthBasis) {

        if (fCharStart >= 0) {
            return fCharStart;
        }
        else {
            return lengthBasis + fCharStart;
        }
    }

    public final float getGraphicStart(float graphicBasis) {

        if (fGraphicStart >= 0) {
            return fGraphicStart;
        }
        else {
            return graphicBasis + fGraphicStart;
        }
    }

    public final void setCharStart(int beginningRelativeStart) {

        if (beginningRelativeStart < 0) {
            throw new IllegalArgumentException("charStart must be nonnegavitve");
        }
        fCharStart = beginningRelativeStart;
    }

    public final void setGraphicStart(float beginningRelativeStart) {

        if (beginningRelativeStart < 0) {
            throw new IllegalArgumentException("charStart must be nonnegavitve");
        }
        fGraphicStart = beginningRelativeStart;
    }

    public final void makeRelativeToBeginning(int lengthBasis, float graphicBasis) {
        if (lengthBasis < 0 || graphicBasis < 0) {
            throw new IllegalArgumentException("Bases must be positive.");
        }
        if (fCharStart >= 0 || fGraphicStart >= 0) {
            throw new Error("Already start-relative.");
        }
        fCharStart += lengthBasis;
        fGraphicStart += graphicBasis;
    }

    public final void makeRelativeToEnd(int lengthBasis, float graphicBasis) {
        if (lengthBasis < 0 || graphicBasis < 0) {
            throw new IllegalArgumentException("Bases must be positive.");
        }
        if (fCharStart < 0 || fGraphicStart < 0) {
            throw new Error("Already end-relative.");
        }
        fCharStart -= lengthBasis;
        fGraphicStart -= graphicBasis;
    }

    public int getCharLength() {
        return fCharLength;
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

    public float getVisibleAdvance() {
        return fVisibleAdvance;
    }

    public float getTotalAdvance() {
        return fTotalAdvance;
    }

    public float getLeadingMargin() {
        return fLeadingMargin;
    }

    public boolean isLeftToRight() {
        return fLeftToRight;
    }

    public float getHeight() {
        return fAscent + fDescent + fLeading;
    }

    public String toString() {
        return "LayoutInfo(charStart: " + getCharStart(0) + ", fCharLength: " + fCharLength +
                ", fAscent: " + fAscent + ", fDescent: " + fDescent + ", fVisibleAdvance: " +
                fVisibleAdvance + ", fTotalAdvance: " + fTotalAdvance + ", fLeadingMargin: " +
                fLeadingMargin + ")";
    }

    public void renderWithHighlight(int lengthBasis, Graphics2D g, float lineBound, float x,
            float y, TextOffset selStart, TextOffset selStop, Color highlightColor) {

        int lineCharStart = getCharStart(lengthBasis);

        if (selStart != null && selStop != null && !selStart.equals(selStop) &&
                fCharLength != 0 && selStart.offset < lineCharStart + fCharLength &&
                selStop.offset > lineCharStart) {

            Shape highlight = getHighlightShape(lengthBasis, lineBound, selStart.offset, selStop.offset);
            if (highlight != null) {
                Color c = g.getColor();
                g.setColor(highlightColor);
                AffineTransform at = g.getTransform();
                g.translate(x, y + fAscent);
                g.fill(highlight);
                g.setTransform(at);
                g.setColor(c);
            }
        }
        render(lengthBasis, g, lineBound, x, y);
    }
    
    public void render(int lengthBasis, Graphics2D g, float lineBound, float x, float y) {
        float leadingMargin = (fLeftToRight) ? x + fLeadingMargin : x + lineBound - fLeadingMargin;
        float baseline = y + fAscent;
        int segmentCount = fSegments.size();

        for (int i = 0; i < segmentCount; i++) {
            LineLayout.Segment segment = (Segment)fSegments.elementAt(i);
            float drawX;
            if (fLeftToRight) {
                drawX = leadingMargin + segment.offset;
            }
            else {
                drawX = leadingMargin - segment.offset;
            }
            segment.layout.draw(g, drawX, baseline);
        }
    }

    public void renderCaret(MConstText text, int lengthBasis, Graphics2D g, float lineBound,
            float x, float y, int charOffset, Color strongCaretColor, Color weakCaretColor) {

        final int segmentCount = fSegments.size();
        final int lineStart = getCharStart(lengthBasis);

        int currentStart = lineStart;
        Segment segment = null;
        int segmentIndex;

        for (segmentIndex = 0; segmentIndex < segmentCount; segmentIndex++) {
            segment = (Segment)fSegments.elementAt(segmentIndex);
            int currentEndpoint = currentStart + segment.layout.getCharacterCount();
            if (currentEndpoint > charOffset) {
                break;
            }
            currentStart = currentEndpoint;
        }

        /*
            There are two choices here:
            1. get carets from a TextLayout and render them, or
            2. make up a caret ourselves and render it.
            We want to do 2 when:
                * there is no text on the line, or
                * the line ends with a tab and we are drawing the last caret on the line
            Otherwise, we want 1.
        */

        if (segmentIndex == segmentCount && segmentCount > 0) {
            // If we get here, line length is not 0, and charOffset is at end of line
            if (!ParagraphRenderer.isTab(text.at(charOffset - 1))) {
                segmentIndex = segmentCount - 1;
                segment = (Segment)fSegments.elementAt(segmentIndex);
                currentStart = lineStart + getCharLength() - segment.layout.getCharacterCount();
            }
        }

        if (segmentIndex < segmentCount) {
            TextLayout layout = segment.layout;
            int offsetInLayout = charOffset - currentStart;
            Shape[] carets = layout.getCaretShapes(offsetInLayout, segment.bounds);
            float layoutPos = fLeadingMargin + segment.offset;
            float layoutX = fLeftToRight ? x + layoutPos : x + lineBound - layoutPos;
            float layoutY = y + fAscent;

            AffineTransform at = g.getTransform();
            Shape clip = g.getClip();
            g.translate(layoutX, layoutY);
            g.clip(segment.bounds);

            g.setColor(strongCaretColor);
            g.draw(carets[0]);
            if (carets[1] != null) {
                g.setColor(weakCaretColor);
                g.draw(carets[1]);
            }
            g.setClip(clip);
            g.setTransform(at);
        }
        else {
            float lineEnd = fLeadingMargin + fTotalAdvance;
            float endX = fLeftToRight ? lineEnd : lineBound - lineEnd;
            endX += x;
            g.drawLine((int)endX, (int)y, (int)endX, (int)(y + getHeight() - 1));
        }
    }

    // Return the offset at the point (x, y).  (x, y) is relative to the top-left of the line.
    // The leading edge of a right-aligned line is aligned to lineBound.
    public TextOffset pixelToOffset(int lengthBasis, TextOffset result, float lineBound,
            float x, float y) {

        if (result == null) {
            result = new TextOffset();
        }

        float yInSegment = y - fAscent;
        float leadingMargin = (fLeftToRight) ? fLeadingMargin : lineBound - fLeadingMargin;
        final int lineCharStart = getCharStart(lengthBasis);

        // first see if point is before leading edge of line
        final int segmentCount = fSegments.size();
        {
            float segLeadingMargin = leadingMargin;
            if (segmentCount > 0) {
                Segment firstSeg = (Segment)fSegments.elementAt(0);
                if (fLeftToRight) {
                    segLeadingMargin += firstSeg.offset;
                }
                else {
                    segLeadingMargin -= firstSeg.offset;
                    segLeadingMargin += (float)firstSeg.bounds.getMaxX();
                }
            }
            if (fLeftToRight == (x <= segLeadingMargin)) {
                result.offset = lineCharStart;
                result.bias = TextOffset.AFTER;
                return result;
            }
        }

        int segmentCharStart = lineCharStart;

        for (int i = 0; i < segmentCount; i++) {

            Segment segment = (Segment)fSegments.elementAt(i);
            float segmentOrigin = fLeftToRight ?
                    leadingMargin + segment.offset :
                    leadingMargin - segment.offset;
            float xInSegment = x - segmentOrigin;
            if (fLeftToRight) {
                if (segment.bounds.getMaxX() > xInSegment) {
                    return hitTestSegment(result, segmentCharStart, segment, xInSegment,
                            yInSegment);
                }
            }
            else {
                if (segment.bounds.getX() < xInSegment) {
                    return hitTestSegment(result, segmentCharStart, segment, xInSegment,
                            yInSegment);
                }
            }
            segmentCharStart += segment.layout.getCharacterCount();
        }
        result.offset = lineCharStart + fCharLength;
        result.bias = TextOffset.BEFORE;
        return result;
    }

    public FRectangle caretBounds(MConstText text, int lengthBasis, float lineBound,
            int charOffset, float x, float y) {

        final int segmentCount = fSegments.size();
        final int lineStart = getCharStart(lengthBasis);
        int currentStart = lineStart;
        Segment segment = null;
        int segmentIndex;

        for (segmentIndex = 0; segmentIndex < segmentCount; segmentIndex++) {
            segment = (Segment)fSegments.elementAt(segmentIndex);
            int currentEndpoint = currentStart + segment.layout.getCharacterCount();
            if (currentEndpoint > charOffset) {
                break;
            }
            currentStart = currentEndpoint;
        }

        if (segmentIndex == segmentCount && segmentCount > 0) {
            // If we get here, line length is not 0, and charOffset is at end of line
            if (!ParagraphRenderer.isTab(text.at(charOffset - 1))) {
                segmentIndex = segmentCount - 1;
                segment = (Segment)fSegments.elementAt(segmentIndex);
                currentStart = lineStart + getCharLength() - segment.layout.getCharacterCount();
            }
        }

        FRectangle r;

        if (segmentIndex < segmentCount) {
            TextLayout layout = segment.layout;
            int offsetInLayout = charOffset - currentStart;
            Shape[] carets = layout.getCaretShapes(offsetInLayout, segment.bounds);
            r = new FRectangle(carets[0].getBounds());
            r.add(r.x, 0);
            r.add(r.x, getHeight());
            if (carets[1] != null) {
                r.add(carets[1].getBounds());
            }
            r.width += 1;

            float layoutPos = fLeadingMargin + segment.offset;
            if (fLeftToRight) {
                r.x += layoutPos;
            }
            else {
                r.x += lineBound - layoutPos;
            }
            r.y += fAscent;
        }
        else {
            r = new FRectangle();
            r.height = getHeight();
            r.width = 1;
            float lineEnd = fLeadingMargin + fTotalAdvance;
            if (fLeftToRight) {
                r.x = lineEnd;
            }
            else {
                r.x = lineBound - lineEnd;
            }
        }
        r.translate(x, y);
        return r;
    }

    public float strongCaretBaselinePosition(int lengthBasis, float lineBound, int charOffset) {

        final int segmentCount = fSegments.size();
        int currentStart = getCharStart(lengthBasis);
        Segment segment = null;
        int segmentIndex;

        for (segmentIndex = 0; segmentIndex < segmentCount; segmentIndex++) {
            segment = (Segment)fSegments.elementAt(segmentIndex);
            int currentEndpoint = currentStart + segment.layout.getCharacterCount();
            if (currentEndpoint > charOffset) {
                break;
            }
            currentStart = currentEndpoint;
        }

        if (segmentIndex < segmentCount) {
            TextLayout layout = segment.layout;
            int offsetInLayout = charOffset - currentStart;
            TextHitInfo hit = TextHitInfo.afterOffset(offsetInLayout);
            hit = TextLayout.DEFAULT_CARET_POLICY.getStrongCaret(hit, hit.getOtherHit(), layout);
            float[] info = layout.getCaretInfo(hit);
            float layoutPos = fLeadingMargin + segment.offset;
            if (fLeftToRight) {
                return layoutPos + (int)info[0];
            }
            else {
                return lineBound - layoutPos + (int)info[0];
            }
        }
        else {
            float lineEnd = fLeadingMargin + fTotalAdvance;
            if (fLeftToRight) {
                return lineEnd;
            }
            else {
                return lineBound - lineEnd;
            }
        }
    }

    public int getNextOffset(int lengthBasis, int charOffset, short dir) {
        
        if (dir != IFormatter.eLeft && dir != IFormatter.eRight) {
            throw new IllegalArgumentException("Invalid direction.");
        }

        // find segment containing offset:
        final int segmentCount = fSegments.size();
        final int lineCharStart = getCharStart(lengthBasis);

        int currentStart = lineCharStart;
        Segment segment = null;
        int segmentIndex;

        for (segmentIndex = 0; segmentIndex < segmentCount; segmentIndex++) {
            segment = (Segment)fSegments.elementAt(segmentIndex);
            int currentEndpoint = currentStart + segment.layout.getCharacterCount();
            if (currentEndpoint > charOffset ||
                    (segmentIndex == segmentCount - 1 && currentEndpoint == charOffset)) {
                break;
            }
            currentStart = currentEndpoint;
        }

        final boolean logAdvance = (dir == IFormatter.eRight) == (fLeftToRight);

        int result;

        if (segmentIndex < segmentCount) {
            TextLayout layout = segment.layout;
            int offsetInLayout = charOffset - currentStart;
            TextHitInfo hit = (dir == IFormatter.eLeft) ? layout.getNextLeftHit(offsetInLayout) :
                    layout.getNextRightHit(offsetInLayout);
            if (hit == null) {
                result = logAdvance ? currentStart + layout.getCharacterCount() + 1 :
                        currentStart - 1;
            }
            else {
                result = hit.getInsertionIndex() + currentStart;
            }
        }
        else {
            result = logAdvance ? lineCharStart + fCharLength + 1 : lineCharStart - 1;
        }
        return result;
    }

    private Shape getHighlightShape(int lengthBasis, float lineBound, int hlStart, int hlLimit) {

        if (hlStart >= hlLimit) {
            throw new IllegalArgumentException("Highlight range length is not positive.");
        }

        float leadingMargin = (fLeftToRight) ? fLeadingMargin : lineBound - fLeadingMargin;
        int segmentCount = fSegments.size();

        Shape rval = null;
        GeneralPath highlightPath = null;

        int layoutStart = getCharStart(lengthBasis);

        for (int i = 0; i < segmentCount && layoutStart < hlLimit; i++) {

            Segment segment = (Segment)fSegments.elementAt(i);
            TextLayout layout = segment.layout;
            int charCount = layout.getCharacterCount();
            int layoutLimit = layoutStart + charCount;

            if (hlStart < layoutLimit && layoutStart < hlLimit) {
                Shape currentHl = layout.getLogicalHighlightShape(
                        Math.max(hlStart - layoutStart, 0),
                        Math.min(hlLimit - layoutStart, charCount), segment.bounds);

                float xTranslate;
                if (fLeftToRight) {
                    xTranslate = leadingMargin + segment.offset;
                }
                else {
                    xTranslate = leadingMargin - segment.offset;
                }

                if (xTranslate != 0) {
                    AffineTransform xform = AffineTransform.getTranslateInstance(xTranslate, 0);
                    currentHl = xform.createTransformedShape(currentHl);
                }

                if (rval == null) {
                    rval = currentHl;
                }
                else {
                    if (highlightPath == null) {
                        highlightPath = new GeneralPath();
                        highlightPath.append(rval, false);
                        rval = highlightPath;
                    }
                    highlightPath.append(currentHl, false);
                }
            }
            layoutStart = layoutLimit;
        }
        return rval;
    }

    private TextOffset hitTestSegment(TextOffset result, int segmentCharStart, Segment segment,
            float xInSegment, float yInSegment) {

        final TextLayout layout = segment.layout;
        final int charCount = layout.getCharacterCount();
        final int layoutAdvance = (int)Math.ceil(layout.getAdvance());
        Rectangle2D bounds = segment.bounds;

        final boolean ltr = layout.isLeftToRight();

        if (ltr && (xInSegment >= layoutAdvance) || !ltr && (xInSegment <= 0)) {

            // pretend the extra space at the end of the line is a tab and 'hit-test' it.
            double tabCenter;
            if (ltr) {
                tabCenter = (layoutAdvance + bounds.getMaxX()) / 2;
            }
            else {
                tabCenter = bounds.getX() / 2;
            }

            if ((xInSegment >= tabCenter) == ltr) {
                result.offset = charCount;
                result.bias = TextOffset.BEFORE;
            }
            else {
                result.offset = charCount - 1;
                result.bias = TextOffset.AFTER;
            }
        }
        else {
            TextHitInfo info = layout.hitTestChar(xInSegment, yInSegment, segment.bounds);
            result.offset = info.getInsertionIndex();
            if (result.offset == 0) {
                result.bias = TextOffset.AFTER;
            }
            else if (result.offset == charCount) {
                result.bias = TextOffset.BEFORE;
            }
            else {
                result.bias = info.isLeadingEdge() ? TextOffset.AFTER : TextOffset.BEFORE;
            }
        }
        result.offset += segmentCharStart;
        return result;
    }
}
