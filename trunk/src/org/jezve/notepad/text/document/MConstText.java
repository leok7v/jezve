package org.jezve.notepad.text.document;

import java.awt.datatransfer.*;
import java.text.CharacterIterator;

/**
 * MConstText is a base class for text with multiple character and
 * paragraph styles.  The text is a sequence of Unicode characters,
 * represented by <code>char</code>.  Character and paragraph
 * styles are represented by the <code>AttributeMap</code> class.
 * <p/>
 * Characters in the text are accessed with an integer index using the
 * <code>at</code> method.
 * Valid indices are between 0 and (length-1), where length is the number
 * of characters in the text.  Additionally, the
 * characters in the text may be accessed through a
 * <code>java.text.CharacterIterator</code>.
 * <p/>
 * Every character in the text has a character style associated with it,
 * represented by the <code>AttributeMap</code> class.  The character
 * style for a particular character can be obtained using the
 * <code>characterStyleAt</code> method.
 * <p/>
 * Each character in the text is contained in a paragraph.  A paragraph
 * is a range of text including and terminated by a
 * paragraph separator (either <code>\n</code> or <code>U+2029</code>).
 * Every
 * paragraph has a paragraph style associated with it, represented
 * by the <code>AttributeMap</code> class.  Paragraph boundaries and
 * styles can be obtained from the MConstText.
 * <p/>
 * This class does not have methods for modifying the text or styles.
 * However, subclasses may add this capability, so it is not safe to
 * assume that an MConstText instance is immutable.  In particular,
 * the MText class adds modification protocol to this class.  Clients
 * can detect whether an MConstText has changed by keeping track of its
 * timestamp.
 * <p/>
 * A DataFlavor for clipboard content is defined in this class.  Using
 * this DataFlavor insures that all clients will recognize MConstText
 * content on the clipboard.
 *
 * @see MText
 * @see AttributeMap
 * @see java.text.CharacterIterator
 * @see java.awt.datatransfer.DataFlavor
 */
public interface MConstText {

    /**
     * The DataFlavor for MConstText clipboard content.  Used to
     * indicate that clipboard data has an MConstText representation.
     */
    public static final DataFlavor styledTextFlavor = new DataFlavor(MConstText.class, "Styled Text");

    //========================================================
    // CHARACTER ACCESS
    //========================================================

    /**
     * Return the character at offset <code>pos</code>.
     *
     * @param pos a valid offset into the text
     * @return the character at offset <code>pos</code>
     */
    char at(int pos);

    /**
     * Copy the characters in the range [<code>start</code>, <code>limit</code>)
     * into the array <code>dst</code>, beginning at <code>dstStart</code>.
     *
     * @param start    offset of first character which will be copied into the array
     * @param limit    offset immediately after the last character which will be copied into the array
     * @param dst      array in which to copy characters.  The length of <code>dst</code> must be at least
     *                 (<code>dstStart + limit - start</code>).
     * @param dstStart - offset to start the copy to
     */
    void extractChars(int start, int limit, char[] dst, int dstStart);

    /**
     * Create an MConstText containing the characters and styles in the range
     * [<code>start</code>, <code>limit</code>).
     *
     * @param start offset of first character in the new text
     * @param limit offset immediately after the last character in the new text
     * @return an MConstText object containing the characters and styles in the given range
     */
    MConstText extract(int start, int limit);

    /**
     * Create a <code>java.text.CharacterIterator</code> over all
     * of the characters in the text.  Default implementation calls
     * <code>createCharacterIterator(0, length())</code>
     *
     * @return a <code>java.text.CharacterIterator</code> over all
     *         of the characters in the text
     */
    CharacterIterator createCharacterIterator();

    /**
     * Create a <code>java.text.CharacterIterator</code> over the
     * given range of characters in the text.
     *
     * @param start the first index in the iteration range
     * @param limit the index after the last character in the iteration range
     * @return a <code>java.text.CharacterIterator</code> over the
     *         given range
     */
    CharacterIterator createCharacterIterator(int start, int limit);

    //========================================================
    // SIZE/CAPACITY
    //========================================================

    /**
     * Return the length of the MConstText object.  The length is the number of characters in the text.
     *
     * @return the length of the MConstText object
     */
    int length();

    //========================================================
    // Character styles
    //========================================================

    /**
     * Return the index of the first character in the character style run
     * containing pos.  All characters in a style run have the same character
     * style.
     *
     * @param pos - the offset
     * @return the style at offset <code>pos</code>
     */
    int characterStyleStart(int pos);

    /**
     * Return the index after the last character in the character style run
     * containing pos.  All characters in a style run have the same character
     * style.
     *
     * @param pos - the offset
     * @return the style at offset <code>pos</code>
     */
    int characterStyleLimit(int pos);

    /**
     * Return the style applied to the character at offset <code>pos</code>.
     *
     * @param pos a valid offset into the text
     * @return the style at offset <code>pos</code>
     */
    AttributeMap characterStyleAt(int pos);

    //========================================================
    // PARAGRAPH BOUNDARIES
    //========================================================

    /**
     * Return the start of the paragraph containing the character at offset <code>pos</code>.
     *
     * @param pos a valid offset into the text
     * @return the start of the paragraph containing the character at offset <code>pos</code>
     */
    int paragraphStart(int pos);

    /**
     * Return the limit of the paragraph containing the character at offset <code>pos</code>.
     *
     * @param pos a valid offset into the text
     * @return the limit of the paragraph containing the character at offset <code>pos</code>
     */
    int paragraphLimit(int pos);

    /**
     * Return the paragraph style applied to the paragraph containing offset <code>pos</code>.
     *
     * @param pos a valid offset into the text
     * @return the paragraph style in effect at <code>pos</code>
     */
    AttributeMap paragraphStyleAt(int pos);

    /**
     * Return the current time stamp.  The time stamp is
     * incremented whenever the contents of the MConstText changes.
     *
     * @return the current paragraph style time stamp
     */
    int getTimeStamp();

    /**
     * Return the start of the damaged range.  If the start is not less
     * than the the limit of the damaged range, then the damaged range
     * is empty.
     *
     * @return the start of the damaged range
     * @see #damagedRangeLimit
     * @see MText#resetDamagedRange
     */
    int damagedRangeStart();

    /**
     * Return the limit of the damaged range.  If the start is not less
     * than the the limit of the damaged range, then the damaged range
     * is empty.
     *
     * @return the start of the damaged range
     * @see #damagedRangeStart
     * @see MText#resetDamagedRange
     */
    int damagedRangeLimit();
}
