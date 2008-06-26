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

import java.text.CharacterIterator;

/**
 * This class is an implementation of MText, a modifyable, styled text storage model.
 *
 * @see MText
 */

public final class StyledText implements MText {

    /* unicode storage */
    private CharBuffer fCharBuffer;
    /* character style storage */
    private StyleBuffer fStyleBuffer;
    /* paragraph style storage */
    private ParagraphBuffer fParagraphBuffer;

    private transient int fTimeStamp = 0;
    private transient int[] fDamagedRange = {Integer.MAX_VALUE, Integer.MIN_VALUE};

    private static class ForceModifier extends StyleModifier {

        private AttributeMap fStyle = AttributeMap.EMPTY_ATTRIBUTE_MAP;

        void setStyle(AttributeMap style) {
            fStyle = style;
        }

        public AttributeMap modifyStyle(AttributeMap style) {
            return fStyle;
        }
    }

    private transient ForceModifier forceModifier = null;

    public StyledText() {
        this(0);
    }

    /**
     * Create an empty text object ready to hold at least capacity chars.
     *
     * @param capacity the minimum capacity of the internal text buffer
     */
    public StyledText(int capacity) {
        fCharBuffer = capacity > 0 ? new CharBuffer(capacity) : new CharBuffer();
        fStyleBuffer = new StyleBuffer(this, AttributeMap.EMPTY_ATTRIBUTE_MAP);
        fParagraphBuffer = new ParagraphBuffer(fCharBuffer);
    }

    /**
     * Create a text object with the characters in the string,
     * in the given style.
     *
     * @param string       the initial contents
     * @param initialStyle the style of the initial text
     */
    public StyledText(String string, AttributeMap initialStyle) {
        fCharBuffer = new CharBuffer(string.length());
        fCharBuffer.replace(0, 0, string, 0, string.length());

        fStyleBuffer = new StyleBuffer(this, initialStyle);
        fParagraphBuffer = new ParagraphBuffer(fCharBuffer);
    }

    /**
     * Create a text object from the given source.
     *
     * @param source the text to copy
     */
    public StyledText(MConstText source) {
        this(source.length());
        append(source);
    }

    /**
     * Create a text object from a subrange of the given source.
     *
     * @param source   the text to copy from
     * @param srcStart the index of the first character to copy
     * @param srcLimit the index after the last character to copy
     */
    public StyledText(MConstText source, int srcStart, int srcLimit) {
        this();
        replace(0, 0, source, srcStart, srcLimit);
    }

    //--------------------------------------------------------
    // character access
    //--------------------------------------------------------

    /**
     * Return the character at offset <code>pos</code>.
     *
     * @param pos a valid offset into the text
     * @return the character at offset <code>pos</code>
     */
    public char at(int pos) {
        return fCharBuffer.at(pos);
    }

    /**
     * Copy the characters in the range [<code>start</code>, <code>limit</code>)
     * into the array <code>dst</code>, beginning at <code>dstStart</code>.
     *
     * @param start offset of first character which will be copied into the array
     * @param limit offset immediately after the last character which will be copied into the array
     * @param dst   array in which to copy characters.  The length of <code>dst</code> must be at least
     *              (<code>dstStart + limit - start</code>).
     */
    public void extractChars(int start, int limit, char[] dst, int dstStart) {
        fCharBuffer.at(start, limit, dst, dstStart);
    }

    //-------------------------------------------------------
    // text model creation
    //-------------------------------------------------------
/**
 * Create an MConstText containing the characters and styles in the range
 * [<code>start</code>, <code>limit</code>).
 *
 * @param start offset of first character in the new text
 * @param limit offset immediately after the last character in the new text
 * @return an MConstText object containing the characters and styles in the given range
 */
    public MConstText extract(int start, int limit) {
        return extractWritable(start, limit);
    }

    /**
     * Create an MText containing the characters and styles in the range
     * [<code>start</code>, <code>limit</code>).
     *
     * @param start offset of first character in the new text
     * @param limit offset immediately after the last character in the new text
     * @return an MConstText object containing the characters and styles in the given range
     */
    public MText extractWritable(int start, int limit) {
        MText text = new StyledText();
        text.replace(0, 0, this, start, limit);
        text.resetDamagedRange();
        return text;
    }

    //--------------------------------------------------------
    // size/capacity
    //--------------------------------------------------------
/**
 * Return the length of the MConstText object.  The length is the number of characters in the text.
 *
 * @return the length of the MConstText object
 */
    public int length() {
        return fCharBuffer.length();
    }

    public CharacterIterator createCharacterIterator() {
        return createCharacterIterator(0, length());
    }

    /**
     * Create a <code>CharacterIterator</code> over the range [<code>start</code>, <code>limit</code>).
     *
     * @param start the beginning of the iterator's range
     * @param limit the limit of the iterator's range
     * @return a valid <code>CharacterIterator</code> over the specified range
     * @see java.text.CharacterIterator
     */
    public CharacterIterator createCharacterIterator(int start, int limit) {
        return fCharBuffer.createCharacterIterator(start, limit);
    }

    //--------------------------------------------------------
    // character styles
    //--------------------------------------------------------

    /**
     * Return the index of the first character in the character style run
     * containing pos.  All characters in a style run have the same character
     * style.
     *
     * @return the style at offset <code>pos</code>
     */
    public int characterStyleStart(int pos) {

        checkPos(pos, LESS_THAN_LENGTH);
        return fStyleBuffer.styleStart(pos);
    }

    /**
     * Return the index after the last character in the character style run
     * containing pos.  All characters in a style run have the same character
     * style.
     *
     * @return the style at offset <code>pos</code>
     */
    public int characterStyleLimit(int pos) {

        checkPos(pos, NOT_GREATER_THAN_LENGTH);
        return fStyleBuffer.styleLimit(pos);
    }

    /**
     * Return the style applied to the character at offset <code>pos</code>.
     *
     * @param pos a valid offset into the text
     * @return the style at offset <code>pos</code>
     */
    public AttributeMap characterStyleAt(int pos) {
        checkPos(pos, NOT_GREATER_THAN_LENGTH);
        return fStyleBuffer.styleAt(pos);
    }

    //--------------------------------------------------------
    // paragraph boundaries and styles
    //--------------------------------------------------------
/**
 * Return the start of the paragraph containing the character at offset <code>pos</code>.
 *
 * @param pos a valid offset into the text
 * @return the start of the paragraph containing the character at offset <code>pos</code>
 */
    public int paragraphStart(int pos) {
        checkPos(pos, NOT_GREATER_THAN_LENGTH);
        return fParagraphBuffer.paragraphStart(pos);
    }

    /**
     * Return the limit of the paragraph containing the character at offset <code>pos</code>.
     *
     * @param pos a valid offset into the text
     * @return the limit of the paragraph containing the character at offset <code>pos</code>
     */
    public int paragraphLimit(int pos) {
        checkPos(pos, NOT_GREATER_THAN_LENGTH);
        return fParagraphBuffer.paragraphLimit(pos);
    }

    /**
     * Return the paragraph style applied to the paragraph containing offset <code>pos</code>.
     *
     * @param pos a valid offset into the text
     * @return the paragraph style in effect at <code>pos</code>
     */
    public AttributeMap paragraphStyleAt(int pos) {
        checkPos(pos, NOT_GREATER_THAN_LENGTH);
        return fParagraphBuffer.paragraphStyleAt(pos);
    }

    /**
     * Return the current time stamp.  The time stamp is
     * incremented whenever the contents of the MConstText changes.
     *
     * @return the current paragraph style time stamp
     */
    public int getTimeStamp() {
        return fTimeStamp;
    }

    //--------------------------------------------------------
    // character modfication functions
    //--------------------------------------------------------

    private void updateDamagedRange(int deleteStart, int deleteLimit, int insertLength) {
        fDamagedRange[0] = Math.min(fDamagedRange[0], deleteStart);
        if (fDamagedRange[1] >= deleteLimit) {
            int lengthChange = insertLength - (deleteLimit - deleteStart);
            fDamagedRange[1] += lengthChange;
        }
        else {
            fDamagedRange[1] = deleteStart + insertLength;
        }
    }

    /**
     * Replace the characters and styles in the range [<code>start</code>, <code>limit</code>) with the characters
     * and styles in <code>srcText</code> in the range [<code>srcStart</code>, <code>srcLimit</code>).  <code>srcText</code> is not
     * modified.
     *
     * @param start    the offset at which the replace operation begins
     * @param limit    the offset at which the replace operation ends.  The character and style at
     *                 <code>limit</code> is not modified.
     * @param text     the source for the new characters and styles
     * @param srcStart the offset into <code>srcText</code> where new characters and styles will be obtained
     * @param srcLimit the offset into <code>srcText</code> where the new characters and styles end
     */
    public void replace(int start, int limit, MConstText text, int srcStart, int srcLimit) {
        if (text == this) {
            text = new StyledText(text);
        }

        if (start == limit && srcStart == srcLimit) {
            return;
        }

        checkStartLimit(start, limit);

        updateDamagedRange(start, limit, srcLimit - srcStart);

        fCharBuffer.replace(start, limit, text, srcStart, srcLimit);
        fStyleBuffer.replace(start, limit, text, srcStart, srcLimit);
        fParagraphBuffer.replace(start, limit, text, srcStart, srcLimit, fDamagedRange);
        fTimeStamp += 1;
    }

    /**
     * Replace the characters and styles in the range [<code>start</code>, <code>limit</code>) with the characters
     * and styles in <code>srcText</code>.  <code>srcText</code> is not
     * modified.
     *
     * @param start the offset at which the replace operation begins
     * @param limit the offset at which the replace operation ends.  The character and style at
     *              <code>limit</code> is not modified.
     * @param text  the source for the new characters and styles
     */
    public void replace(int start, int limit, MConstText text) {
        replace(start, limit, text, 0, text.length());
    }

    /**
     * Replace the characters in the range [<code>start</code>, <code>limit</code>) with the characters
     * in <code>srcChars</code> in the range [<code>srcStart</code>, <code>srcLimit</code>).  New characters take on the style
     * <code>charsStyle</code>.
     * <code>srcChars</code> is not modified.
     *
     * @param start      the offset at which the replace operation begins
     * @param limit      the offset at which the replace operation ends.  The character at
     *                   <code>limit</code> is not modified.
     * @param srcChars   the source for the new characters
     * @param srcStart   the offset into <code>srcChars</code> where new characters will be obtained
     * @param srcLimit   the offset into <code>srcChars</code> where the new characters end
     * @param charsStyle the style of the new characters
     */
    public void replace(int start, int limit, char[] srcChars, int srcStart, int srcLimit,
            AttributeMap charsStyle) {
        checkStartLimit(start, limit);

        if (start == limit && srcStart == srcLimit) {
            return;
        }

        updateDamagedRange(start, limit, srcLimit - srcStart);

        fCharBuffer.replace(start, limit, srcChars, srcStart, srcLimit);

        replaceCharStylesWith(start, limit, start + (srcLimit - srcStart), charsStyle);

        fParagraphBuffer.deleteText(start, limit, fDamagedRange);
        fParagraphBuffer.insertText(start, srcChars, srcStart, srcLimit);

        fTimeStamp += 1;
    }

    private void replaceCharStylesWith(int start, int oldLimit, int newLimit, AttributeMap style) {

        if (start < oldLimit) {
            fStyleBuffer.deleteText(start, oldLimit);
        }
        if (start < newLimit) {
            if (forceModifier == null) {
                forceModifier = new ForceModifier();
            }
            forceModifier.setStyle(style);
            fStyleBuffer.insertText(start, newLimit);
            fStyleBuffer.modifyStyles(start, newLimit, forceModifier, null);
        }
    }

    /**
     * Replace the characters in the range [<code>start</code>, <code>limit</code>) with the character <code>srcChar</code>.
     * The new character takes on the style <code>charStyle</code>
     *
     * @param start     the offset at which the replace operation begins
     * @param limit     the offset at which the replace operation ends.  The character at
     *                  <code>limit</code> is not modified.
     * @param srcChar   the new character
     * @param charStyle the style of the new character
     */
    public void replace(int start, int limit, char srcChar, AttributeMap charStyle) {
        checkStartLimit(start, limit);

        updateDamagedRange(start, limit, 1);

        fCharBuffer.replace(start, limit, srcChar);

        replaceCharStylesWith(start, limit, start + 1, charStyle);

        if (start < limit) {
            fParagraphBuffer.deleteText(start, limit, fDamagedRange);
        }

        fParagraphBuffer.insertText(start, srcChar);

        fTimeStamp += 1;
    }

    /**
     * Replace the entire contents of this MText (both characters and styles) with
     * the contents of <code>srcText</code>.
     *
     * @param srcText the source for the new characters and styles
     */
    public void replaceAll(MConstText srcText) {
        replace(0, length(), srcText, 0, srcText.length());
    }

    /**
     * Insert the contents of <code>srcText</code> (both characters and styles) into this
     * MText at the position specified by <code>pos</code>.
     *
     * @param pos     The character offset where the new text is to be inserted.
     * @param srcText The text to insert.
     */
    public void insert(int pos, MConstText srcText) {
        replace(pos, pos, srcText, 0, srcText.length());
    }

    /**
     * Append the contents of <code>srcText</code> (both characters and styles) to the
     * end of this MText.
     *
     * @param srcText The text to append.
     */
    public void append(MConstText srcText) {
        replace(length(), length(), srcText, 0, srcText.length());
    }

    /**
     * Delete the specified range of characters (and styles).
     *
     * @param start Offset of the first character to delete.
     * @param limit Offset of the first character after the range to delete.
     */
    public void remove(int start, int limit) {
        replace(start, limit, (char[])null, 0, 0, AttributeMap.EMPTY_ATTRIBUTE_MAP);
    }

    /**
     * Delete all characters and styles.  Always increments time stamp.
     */
    public void remove() {
        // rather than going through replace(), just reinitialize the StyledText,
        // letting the old data structures fall on the floor
        fCharBuffer = new CharBuffer();
        fStyleBuffer = new StyleBuffer(this, AttributeMap.EMPTY_ATTRIBUTE_MAP);
        fParagraphBuffer = new ParagraphBuffer(fCharBuffer);
        fTimeStamp += 1;
        fDamagedRange[0] = fDamagedRange[1] = 0;
    }

    // Minimize the amount of memory used by the MText object.
    public void compress() {
        fCharBuffer.compress();
        fStyleBuffer.compress();
        fParagraphBuffer.compress();
    }

    //--------------------------------------------------------
    // style modification
    //--------------------------------------------------------

    /**
     * Set the style of all characters in the MText object to
     * <code>AttributeMap.EMPTY_ATTRIBUTE_MAP</code>.
     */
    public void removeCharacterStyles() {
        fStyleBuffer = new StyleBuffer(this, AttributeMap.EMPTY_ATTRIBUTE_MAP);
        fTimeStamp += 1;
        fDamagedRange[0] = 0;
        fDamagedRange[1] = length();
    }

    /**
     * Invoke the given modifier on all character styles from start to limit.
     *
     * @param modifier the modifier to apply to the range.
     * @param start    the start of the range of text to modify.
     * @param limit    the limit of the range of text to modify.
     */
    public void modifyCharacterStyles(int start, int limit, StyleModifier modifier) {
        checkStartLimit(start, limit);
        if (fStyleBuffer.modifyStyles(start, limit, modifier, fDamagedRange)) {
            fTimeStamp += 1;
        }
    }

    /**
     * Invoke the given modifier on all paragraph styles in paragraphs
     * containing characters in the range [start, limit).
     *
     * @param modifier the modifier to apply to the range.
     * @param start    the start of the range of text to modify.
     * @param limit    the limit of the range of text to modify.
     */
    public void modifyParagraphStyles(int start, int limit, StyleModifier modifier) {
        checkStartLimit(start, limit);
        if (fParagraphBuffer.modifyParagraphStyles(start, limit, modifier, fDamagedRange)) {
            fTimeStamp += 1;
        }
    }

    /**
     * Reset the damaged range to an empty interval, and begin accumulating the damaged
     * range.  The damaged range includes every index where a character, character style,
     * or paragraph style has changed.
     *
     * @see #damagedRangeStart
     * @see #damagedRangeLimit
     */
    public void resetDamagedRange() {
        fDamagedRange[0] = Integer.MAX_VALUE;
        fDamagedRange[1] = Integer.MIN_VALUE;
    }

    /**
     * Return the start of the damaged range.
     * If the start is
     * <code>Integer.MAX_VALUE</code> and the limit is
     * <code>Integer.MIN_VALUE</code>, then the damaged range
     * is empty.
     *
     * @return the start of the damaged range
     * @see #damagedRangeLimit
     * @see #resetDamagedRange
     */
    public int damagedRangeStart() {
        return fDamagedRange[0];
    }

    /**
     * Return the limit of the damaged range.
     * If the start is
     * <code>Integer.MAX_VALUE</code> and the limit is
     * <code>Integer.MIN_VALUE</code>, then the damaged range
     * is empty.
     *
     * @return the limit of the damaged range
     * @see #damagedRangeStart
     * @see #resetDamagedRange
     */
    public int damagedRangeLimit() {
        return fDamagedRange[1];
    }

    public String toString() {
        String result = "";
        for (int i = 0; i < length(); i++) {
            result += at(i);
        }
        return result;
    }

    //======================================================
    // IMPLEMENTATION
    //======================================================

    /* check a range to see if it is well formed and within the bounds of the text */

    private void checkStartLimit(int start, int limit) {
        if (start > limit) {
            throw new IllegalArgumentException(
                    "Start is greater than limit. start:" + start + "; limit:" + limit);
        }
        if (start < 0) {
            throw new IllegalArgumentException("Start is negative. start:" + start);
        }
        if (limit > length()) {
            throw new IllegalArgumentException("Limit is greater than length.  limit:" + limit);
        }
    }

    private static final byte LESS_THAN_LENGTH = 0;
    private static final byte NOT_GREATER_THAN_LENGTH = 1;

    private void checkPos(int pos, byte endAllowed) {
        int lastValidPos = length();
        if (endAllowed == LESS_THAN_LENGTH) {
            --lastValidPos;
        }
        if (pos < 0 || pos > lastValidPos) {
            throw new IllegalArgumentException("Position is out of range.");
        }
    }

    /**
     * Compare this to another Object for equality.  This is
     * equal to rhs if rhs is an MConstText which is equal
     * to this.
     *
     * @param rhs Object to compare to
     * @return true if this equals <code>rhs</code>
     */
    public final boolean equals(Object rhs) {
        MConstText otherText;
        try {
            otherText = (MConstText)rhs;
        }
        catch (ClassCastException e) {
            return false;
        }
        return equals(otherText);
    }

    /**
     * Compare this to another MConstText for equality.  This is
     * equal to rhs if the characters and styles in rhs are the
     * same as this.  Subclasses may override this implementation
     * for efficiency, but they should preserve these semantics.
     * Determining that two MConstText instances are equal may be
     * an expensive operation, since every character and style must
     * be compared.
     *
     * @param rhs Object to compare to
     * @return true if this equals <code>rhs</code>
     */
    public boolean equals(MConstText rhs) {

        if (rhs == null) {
            return false;
        }

        if (rhs == this) {
            return true;
        }

        if (hashCode() != rhs.hashCode()) {
            return false;
        }

        int length = length();
        if (length != rhs.length()) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (i < length && at(i) != rhs.at(i)) {
                return false;
            }
        }

        for (int start = 0; start < length;) {
            if (!characterStyleAt(start).equals(rhs.characterStyleAt(start))) {
                return false;
            }
            int limit = characterStyleLimit(start);
            if (limit != rhs.characterStyleLimit(start)) {
                return false;
            }
            start = limit;
        }

        for (int start = 0; start < length;) {

            if (!paragraphStyleAt(start).equals(rhs.paragraphStyleAt(start))) {
                return false;
            }
            start = paragraphLimit(start);
        }
        return paragraphStyleAt(length).equals(rhs.paragraphStyleAt(length));
    }

    /**
     * Return the hashCode for this MConstText.  An empty MConstText
     * has hashCode 0;  a nonempty MConstText's hashCode is
     * <blockquote><pre>
     *       at(0) +
     *       at(length/2)*31^1 +
     *       at(length-1)*31^2 +
     *       characterStyleAt(0).hashCode()*31^3 +
     *       paragraphStyleAt(length-1).hashCode()*31^4
     * </pre></blockquote>
     * where <code>^</code> is exponentiation (not bitwise XOR).
     */
    public final int hashCode() {
        int hashCode = 0;
        int length = length();
        if (length > 0) {
            hashCode = paragraphStyleAt(length - 1).hashCode();
            hashCode = hashCode * 31 + characterStyleAt(0).hashCode();
            hashCode = hashCode * 31 + at(length - 1);
            hashCode = hashCode * 31 + at(length / 2);
            hashCode = hashCode * 31 + at(0);
        }
        return hashCode;
    }
}
