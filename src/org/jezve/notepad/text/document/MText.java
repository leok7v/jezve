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
 * This class is a mutable extension of MConstText.  It has methods for
 * inserting, appending, replacing, and removing styled text.  Additionally,
 * it has methods for modifying paragraph and character styles.
 * <p/>
 * Styled characters (from another <code>MConstText</code> instance) added
 * to the text retain their original character styles.  The style of plain characters
 * (specified as a <code>char</code> or <code>char[]</code>) is always
 * specified explicitly when they are added to the text.  MText does not do
 * character style "propagation", where unstyled characters take on the
 * style of previous characters.  Clients can implement this behavior by
 * specifying the styles to propagate.
 * <p/>
 * When unstyled characters are added to the text, their paragraph style
 * is the paragraph style in effect immediately after the last new character.
 * If the characters contain paragraph separators, then every new paragraph
 * will have the same paragraph style.  When styled characters are added
 * to the text, their resulting paragraph style is determined by the
 * following rule:
 * <blockquote>
 * The paragraph styles in the new text
 * become the paragraph styles in the target text, with the exception of the
 * last paragraph in the new text, which takes on the paragraph style in
 * effect immediately after the inserted text.
 * If the new text is added at the end of the target text, the new text's
 * paragraph styles take effect in any paragraph affected by the addition.
 * </blockquote>
 * For example, suppose there is a single paragraph of text with style 'A',
 * delimited with a paragraph separator 'P':
 * <blockquote>
 * AAAAAAP
 * </blockquote>
 * Suppose the following styled paragraphs are inserted into the above text
 * after the fourth character:
 * <blockquote>
 * BBBBPCCCPDDD
 * </blockquote>
 * Then the original paragraph style of each character is:
 * <blockquote>
 * AAAABBBBPCCCPDDDAAP
 * </blockquote>
 * The resulting paragraph styles are:
 * <blockquote>
 * BBBBBBBBPCCCPAAAAAP
 * </blockquote>
 * Similarly, if characters are deleted, the paragraph style immediately
 * after the deletion takes effect on the paragraph containing the deletion.
 * So, if characters 4-16 were deleted in the example above, the paragraph
 * styles would be:
 * <blockquote>
 * AAAAAAP
 * </blockquote>
 * This paragraph-style propagation policy is sometimes referred to as <strong>
 * following styles win</strong>, since styles at the end of the paragraph
 * become the style for the entire paragraph.
 * <p/>
 * This class can accumulate a <strong>damaged range</strong> - an interval in
 * which characters, character styles, or paragraph styles have changed.  This is
 * useful for clients such as text editors which reformat and draw text after
 * changes.  Usually the damaged range is exactly the range of characters
 * operated upon;  however, larger ranges may be damaged if paragraph styles
 * change.
 *
 * @see StyleModifier
 */

public interface MText extends MConstText {

    /**
     * Replace the characters and styles in the range [<code>start</code>, <code>limit</code>) with the characters
     * and styles in <code>srcText</code> in the range [<code>srcStart</code>, <code>srcLimit</code>).  <code>srcText</code> is not
     * modified.
     *
     * @param start    the offset at which the replace operation begins
     * @param limit    the offset at which the replace operation ends.  The character and style at
     *                 <code>limit</code> is not modified.
     * @param srcText  the source for the new characters and styles
     * @param srcStart the offset into <code>srcText</code> where new characters and styles will be obtained
     * @param srcLimit the offset into <code>srcText</code> where the new characters and styles end
     */
    void replace(int start, int limit, MConstText srcText, int srcStart, int srcLimit);

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
    void replace(int start, int limit, MConstText text);

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
    void replace(int start, int limit, char[] srcChars, int srcStart, int srcLimit, AttributeMap charsStyle);

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
    void replace(int start, int limit, char srcChar, AttributeMap charStyle);

    /**
     * Replace the entire contents of this MText (both characters and styles) with
     * the contents of <code>srcText</code>.
     *
     * @param srcText the source for the new characters and styles
     */
    void replaceAll(MConstText srcText);

    /**
     * Insert the contents of <code>srcText</code> (both characters and styles) into this
     * MText at the position specified by <code>pos</code>.
     *
     * @param pos     The character offset where the new text is to be inserted.
     * @param srcText The text to insert.
     */
    void insert(int pos, MConstText srcText);

    /**
     * Append the contents of <code>srcText</code> (both characters and styles) to the
     * end of this MText.
     *
     * @param srcText The text to append.
     */
    void append(MConstText srcText);

    /**
     * Delete the specified range of characters (and styles).
     *
     * @param start Offset of the first character to delete.
     * @param limit Offset of the first character after the range to delete.
     */
    void remove(int start, int limit);

    /**
     * Delete all characters and styles.
     */
    void remove();

    /**
     * Create an MText containing the characters and styles in the range
     * [<code>start</code>, <code>limit</code>).
     *
     * @param start offset of first character in the new text
     * @param limit offset immediately after the last character in the new text
     * @return an MConstText object containing the characters and styles in the given range
     */
    MText extractWritable(int start, int limit);

    //==================================================
    // STORAGE MANAGEMENT
    //==================================================

    /**
     * Minimize the amount of memory used by the MText object.
     */
    void compress();

    //==================================================
    // STYLE MODIFICATION
    //==================================================

    /**
     * Set the character style of all characters in the MText object to
     * <code>AttributeMap.EMPTY_ATTRIBUTE_MAP</code>.
     */
    void removeCharacterStyles();

    /**
     * Invoke the given modifier on all character styles from start to limit.
     *
     * @param modifier the modifier to apply to the range.
     * @param start    the start of the range of text to modify.
     * @param limit    the limit of the range of text to modify.
     */
    void modifyCharacterStyles(int start, int limit, StyleModifier modifier);

    /**
     * Invoke the given modifier on all paragraph styles in paragraphs
     * containing characters in the range [start, limit).
     *
     * @param modifier the modifier to apply to the range.
     * @param start    the start of the range of text to modify.
     * @param limit    the limit of the range of text to modify.
     */
    void modifyParagraphStyles(int start, int limit, StyleModifier modifier);

    //==================================================
    // DAMAGED RANGE
    //==================================================

    /**
     * Reset the damaged range to an empty interval, and begin accumulating the damaged
     * range.  The damaged range includes every index where a character, character style,
     * or paragraph style has changed.
     *
     * @see #damagedRangeStart
     * @see #damagedRangeLimit
     */
    void resetDamagedRange();
}
