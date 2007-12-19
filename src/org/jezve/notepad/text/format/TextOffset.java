package org.jezve.notepad.text.format;

/**
 * A TextOffset indicates both an integer offset into text and a placement
 * on one of the characters adjacent to the offset.  An offset is a
 * position between two characters;  offset n
 * is between character n-1 and character n.  The placement specifies whether
 * it is associated with the character
 * after the offset
 * (character n) or the character before the offset (character n-1).
 * <p/>
 * Knowing which character the TextOffset is associated with is necessary
 * when displaying carets.  In bidirectional text, a single offset may
 * have two distinct carets.  Also, in multiline text, an offset at a line
 * break has a possible caret on each line.
 * <p/>
 * Most clients will not be interested in the placement, and will just use
 * the offset.
 */
public final class TextOffset {

    /**
     * Indicates that the TextOffset is associated with character
     * <code>offset - 1</code> - ie the character before its offset.
     */
    public final static byte BEFORE = 1;

    /**
     * Indicates that the TextOffset is associated with character
     * <code>offset</code> - ie the character after its offset.
     */
    public final static byte AFTER = 0;

    /**
     * The offset into the text.
     */
    public int offset = 0;

    /**
     * The placement - before or after.
     */
    public byte bias = AFTER;

    /**
     * Constructs a new TextOffset
     *
     * @param offset the offset into the text to represent.  Placement is implicitly AFTER.
     */
    public TextOffset(int offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset is negative in TextOffset constructor.");
        }
        this.offset = offset;
        bias = AFTER;
    }

    /**
     * Constructs a new TextOffset at 0, with placement AFTER.
     */
    public TextOffset() {
        this(0);
    }

    /**
     * Constructs a new TextOffset with the given offset and placement.
     *
     * @param offset    the offset into the text
     * @param placement indicates the position of the caret; one of BEFORE or AFTER
     */
    public TextOffset(int offset, byte placement) {
        if (offset < 0) {
            throw new IllegalArgumentException("TextOffset constructor offset < 0: " + offset);
        }
        this.offset = offset;
        bias = placement;
    }

    /**
     * Constructs a new TextOffset from an existing one.
     *
     * @param rhs the TextOffset to copy
     */
    public TextOffset(TextOffset rhs) {
        this(rhs.offset, rhs.bias);
    }

    /**
     * Set the value of the TextOffset
     *
     * @param offset    the offset into the text
     * @param placement indicates the position of the caret; one of BEFORE or AFTER
     */
    public void setOffset(int offset, byte placement) {
        if (offset < 0) {
            throw new IllegalArgumentException("TextOffset setOffset offset < 0: " + offset);
        }
        this.offset = offset;
        bias = placement;
    }

    /**
     * Compare this to another Object.
     */
    public boolean equals(Object other) {
        try {
            return equals((TextOffset)other);
        }
        catch (ClassCastException e) {
            return false;
        }
    }

    /**
     * Return true if offset and placement are the same.
     *
     * @param other offset to compare against
     * @return true if both offsets are equal
     */
    public boolean equals(TextOffset other) {
        return offset == other.offset && bias == other.bias;
    }

    /**
     * Return the hashCode for this object.
     */
    public int hashCode() {
        return bias == AFTER ? offset : -offset;
    }

    /**
     * Return true if this offset is 'greaterThan' other.  If the offset fields are equal, the
     * placement field is considered, and AFTER is considered 'greaterThan' BEFORE.
     *
     * @param rhs the other offset
     * @return true if this offset appears after other
     */
    public boolean greaterThan(TextOffset rhs) {
        return offset > rhs.offset || (offset == rhs.offset && bias == AFTER && rhs.bias == BEFORE);
    }

    /**
     * Return true if this offset is 'lessThan' other.  If the offset fields are equal, the
     * placement field is considered, and BEFORE is considered 'lessThan' AFTER.
     *
     * @param rhs the other offset
     * @return true if this offset appears before other
     */
    public boolean lessThan(TextOffset rhs) {
        return offset < rhs.offset || (offset == rhs.offset && bias == BEFORE && rhs.bias == AFTER);
    }

    /**
     * Copy the value of another TextOffset into this
     *
     * @param other the TextOffset to copy
     */
    public void assign(TextOffset other) {
        offset = other.offset;
        bias = other.bias;
    }

    /**
     * Return a string representation of this object.
     */
    public String toString() {
        return "[" + (bias == BEFORE ? "before " : "after ") + offset + "]";
    }
}
