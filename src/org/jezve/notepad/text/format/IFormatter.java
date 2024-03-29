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

import java.awt.*;

/**
 * This class formats lines of text to a given length.
 * It provides services needed for static text display,
 * and also editable text, including:  displaying text,
 * reformatting text after an edit, converting between
 * screen locations and offsets into the text, calculating
 * areas of the screen for "highlighting,"  and computing
 * offsets into the text resulting from arrow keys.
 * <p/>
 * Text clients instantiate this class with an
 * <tt>MConstText</tt> object and a format width.  Text
 * can be formatted such that all lines fit within the
 * format length.  Alternatively, text can be formatted
 * such that lines end only at the end of paragraphs.
 * <p/>
 * The format length is specified with the <tt>setLineBound()</tt>
 * method.
 * <p/>
 * Methods in the formatter which interact with the graphics
 * system generally take as a paramter a <tt>FPoint</tt> object
 * which represents the "origin" of the text display.  The
 * origin represents the location, in the graphics system used to display the text, of
 * the top-left corner of the text.
 * <p/>
 * To display the text, call <tt>draw()</tt>, passing the
 * a rectangle in which to draw as a parameter.  Only lines
 * of text in the draw rectangle will be drawn.
 * <p/>
 * When the formatter's text changes, it is important to first call
 * <tt>stopBackgroundFormatting()</tt> to prevent the Formatter from
 * accessing the text from a background thread.  After modifications are
 * complete,
 * call the <tt>updateFormat()</tt> method before invoking any other
 * methods of the formatter.  <tt>updateFormat()</tt> reformats the
 * new text, formatting no more text than is necessary.
 * <p/>
 * The formatter provides services for responding to user input from the
 * mouse and keyboard.  The method <tt>pointToTextOffset()</tt> converts
 * a screen location to an offset in the text.  The method <tt>textOffsetToPoint</tt>
 * converts an offset in the text to an array of two <tt>FPoint</tt> objects, which can be
 * used to draw a verticle caret, denoting an insertion point.  <tt>highlightArea</tt>
 * accepts two offsets into the text as paramters, and returns an array of <tt>Polygon</tt>
 * objects representing areas where visual highlighting should be applied.
 * <p/>
 * Finally, for
 * keyboard handling, the <tt>findNewInsertionOffset()</tt> method accepts an "initial"
 * offset, a "previous" offset, as well as a direction, and returns a new offset.  The direction
 * can be up, down, left, or right.  The previous offset is the insertion point location, before
 * the arrow key is processed.  The initial offset is the offset where an up or down arrow
 * key sequence began.  Using the initial offset allows for "intelligent" handling of up and down
 * arrow keys.
 * <p/>
 * Examples of using the IFormatter class
 * are given in the <tt>Formatter</tt> class
 * documentation.
 * <p/>
 *
 * @author John Raley
 * @see org.jezve.notepad.text.document.MText
 */

public interface IFormatter {

    AttributeMap getDefaultValues();

    /**
     * Display text in drawArea, with highlighting.
     * Does not reformat text
     *
     * @param g         the Graphics object in which to draw
     * @param drawArea  the rectangle, in g's coordinate system, in which to draw
     * @param origin    the top-left corner of the text, in g's coordinate system
     * @param selStart  the offset where the current selection begins;  pass <tt>null</tt> if no selection
     * @param selStop   the offset where the current selection ends
     * @param highlight the color of the highlighting
     */
    void draw(Graphics2D g, FRectangle drawArea, FPoint origin, TextOffset selStart,
            TextOffset selStop, Color highlight);

    void draw(Graphics2D g, FRectangle drawArea, FPoint origin);

    /**
     * Specify whether to wrap line at the edge of the destination area.
     * <tt>true</tt> means wrap lines;  <tt>false</tt> means to break lines
     * only when an end-of-line character is reached.
     *
     * @param wrap <tt>true</tt> to break lines at the edge of the destination
     *             area;  <tt>false</tt> otherwise.
     */

    void setWrap(boolean wrap);

    /**
     * @return whether text is wrapped at the edge of the destination area.
     * @see #setWrap
     */
    boolean wrap();

    /**
     * Specify the number of pixels along the "line dimension".
     * Lines are formatted to fit within the line dimension.  The
     * line dimension in Roman script is horizontal.
     *
     * @param lineBound the length, in pixels, to which lines will be formatted
     */
    void setLineBound(float lineBound);

    /**
     * Return the number of pixels along the line dimension.
     *
     * @return the number of pixels along the line dimension.
     */
    float lineBound();

    /**
     * Format text down to given height.
     *
     * @param height the height to which text will be formatted
     */
    void formatToHeight(float height);

    /**
     * Reformat text after a change.
     * After the formatter's text changes, call this method to reformat.  Does
     * not redraw.
     *
     * @param afStart  the offset into the text where modification began;  ie, the
     *                 first character in the text which is "different" in some way.  Does not
     *                 have to be nonnegative.
     * @param afLength the number of new or changed characters in the text.  Should never
     *                 be less than 0.
     * @param viewRect the Rectangle in which the text will be displayed.  This is needed for
     *                 returning the "damaged" area - the area of the screen in which the text must be redrawn.
     * @param origin   the top-left corner of the text, in the display's coordinate system
     * @return a <tt>Rectangle</tt> which specifies the area in which text must be
     *         redrawn to reflect the change to the text.
     */
    FRectangle updateFormat(int afStart, int afLength, FRectangle viewRect, FPoint origin);

    float minY();

    float maxY();

    float minX();

    float maxX();

    float formattedHeight();

    static final short eUp = -10, eDown = 10, eLeft = -1, eRight = 1;

    /*
     * Given a screen location p, return the offset of the character in the text nearest to p.
     * <p/>
     * The offset may or may not include a newline at the end of a line, determined by anchor and infiniteMode.
     * The newline is not included if infiniteMode is true and the anchor is the position before the newline.
     *
     * @param result       TextOffset to modify and return.  If null, one will be allocated, modified, and returned.
     * @param px           the x component of the point.
     * @param py           the y component of the point.
     * @param origin       the top-left corner of the text, in the display's coordinate system
     * @param anchor       the previous offset.  May be null.  Used to determine whether newlines are included.
     * @param infiniteMode if true, treat newlines at end of line as having infinite width.
     */
    TextOffset pointToTextOffset(TextOffset result, float px, float py, FPoint origin,
            TextOffset anchor, boolean infiniteMode);

    /**
     * Given an offset, return the Rectangle bounding the caret at the offset.
     *
     * @param offset an offset into the text
     * @param origin the top-left corner of the text, in the display's coordinate system
     * @return a Rectangle bounding the caret.
     */
    FRectangle getCaretRect(TextOffset offset, FPoint origin);

    /**
     * Draw the caret(s) associated with the given offset into the given Graphics.
     *
     * @param g           the Graphics to draw into
     * @param offset      the offset in the text for which the caret is drawn
     * @param origin      the top-left corner of the text, in the display's coordinate system
     * @param strongColor the color of the strong caret
     * @param weakColor   the color of the weak caret (if any)
     */
    void drawCaret(Graphics2D g, TextOffset offset, FPoint origin, Color strongColor,
            Color weakColor);

    /**
     * @see #getBoundingRect
     */
    static final boolean LOOSE = false;
    /**
     * @see #getBoundingRect
     */
    static final boolean TIGHT = true;

    /**
     * Given two offsets in the text, return a rectangle which encloses the lines containing the offsets.
     * Offsets do not need to be ordered or nonnegative.
     *
     * @param offset1 an offset into the text
     * @param offset2 the other offset into the text
     * @param origin  the top-left corner of the text, in the display's coordinate system
     * @param tight   if equal to TIGHT, the bounds is as small as possible.  If LOOSE, the width
     *                of the bounds is allowed to be wider than necesary.  Loose bounds are easier to compute.
     * @return a <tt>Rectangle</tt>, relative to <tt>origin</tt>, which encloses the lines containing the offsets
     */
    FRectangle getBoundingRect(TextOffset offset1, TextOffset offset2, FPoint origin,
            boolean tight);

    void getBoundingRect(FRectangle boundingRect, TextOffset offset1, TextOffset offset2,
            FPoint origin, boolean tight);

    /**
     * Compute the offset resulting from moving from a previous offset in direction dir.
     * For arrow keys.
     *
     * @param result         - resuse if not null
     * @param previousOffset the insertion offset prior to the arrow key press
     * @param direction      the direction of the arrow key (eUp, eDown, eLeft, or eRight)
     * @return new offset based on direction and previous offset.
     */
    TextOffset findInsertionOffset(TextOffset result, TextOffset previousOffset, short direction);

    /**
     * Compute the offset resulting from moving from a previous offset, starting at an original offset, in direction dir.
     * For arrow keys.  Use this for "smart" up/down keys.
     *
     * @param result         TextOffset to modify and return.  If null, a new TextOffset is created, modified, and returned.
     * @param initialOffset  The offset at which an up-down arrow key sequence began.
     * @param previousOffset The insertion offset prior to the arrow key press.
     * @param direction      The direction of the arrow key (eUp, eDown, eLeft, or eRight)
     * @return new offset based on direction and previous offset(s).
     */
    TextOffset findNewInsertionOffset(TextOffset result, TextOffset initialOffset,
            TextOffset previousOffset, short direction);

    /*
     * Return the index of the line containing the given character index.
     * This method has complicated semantics, arising from not knowing
     * which side of the index to check.  The index will be given an
     * implicit AFTER bias, unless the index is the last index in the text,
     * the text length is non-zero, and there is not a paragraph separator
     * at the end of the text.
     */
    int lineContaining(int index);

    /*
     * Return the index of the line containing the given offset.
     */
    int lineContaining(TextOffset offset);

    /*
     * Return the number of lines.
     */
    int getLineCount();

    /*
     * Return the index of the first character on the given line.
     */
    int lineRangeLow(int lineNumber);

    /*
     * Return the index of the first character following the given line.
     */
    int lineRangeLimit(int lineNumber);

    /*
     * Return the line number at the given graphic height.  If height is greater than
     * the text height, maxLineNumber + 1 is returned.
     */
    int lineAtHeight(float height);

    /*
     * Return the graphic height where the given line begins.  If the lineNumber is
     * maxLineNumber the entire text height is returned.
     */
    float lineGraphicStart(int lineNumber);

    /**
     * Return true if the given line is left-to-right.
     *
     * @param lineNumber a valid line
     * @return true if lineNumber is left-to-right
     */
    boolean lineIsLeftToRight(int lineNumber);
}
