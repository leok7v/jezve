package org.jezve.notepad.text.document;

/**
 * TabStop represents a position on a tab ruler.  Each tab stop has a
 * position, giving its location on the ruler, and one of several
 * types.  The type determines how a segment controled by this TabStop
 * is positioned on a line:
 * <ul>
 * <li><code>kLeading</code> - the leading edge of the segment is aligned to
 * the TabStop's position</li>
 * <li><code>kCenter</code> - the segment is centered on this TabStop's
 * position</li>
 * <li><code>kTrailing</code> - the trailing edge of the segment is aligned to
 * the TabStop's position</li>
 * <li><code>kDecimal</code> - the first decimal in the segment is aligned to
 * the TabStop's position</li>
 * <li><code>kAuto</code> - semantically the same as <code>kLeading</code>.
 * Used by tab rulers to indicate that all subsequent tab stops
 * will be at autospaced intervals.</li>
 * </ul>
 *
 * @see TabRuler
 */
public final class TabStop {

    private byte fType;    // left, center, right, decimal
    private float fPosition; // tab stop position from line origin.

    /**
     * A TabStop with this type aligns its segment's leading edge
     * to the TabStop's position.
     */
    public static final byte kLeading = 0;

    /**
     * A TabStop with this type aligns its segment's center
     * to the TabStop's position.
     */
    public static final byte kCenter = 1;

    /**
     * A TabStop with this type aligns its segment's trailing edge
     * to the TabStop's position.
     */
    public static final byte kTrailing = 2;

    /**
     * A TabStop with this type aligns its segment's first decimal
     * to the TabStop's position.
     */
    public static final byte kDecimal = 3;

    /**
     * A TabStop with this type aligns its segment's leading edge
     * to the TabStop's position.  After a TabStop of this type,
     * all tabs are at autospace intervals.  Usually, clients will
     * not construct TabStops with this type.
     */
    public static final byte kAuto = 4;

    /**
     * Create a TabStop with position 0 and type <code>kLeading</code>.
     */
    public TabStop() {

        this(0, kLeading);
    }

    /**
     * Create a TabStop with the given position and type.
     *
     * @param position the TabStop's position
     * @param type     the TabStop's type.  Must be one of constants
     *                 in this class.
     */
    public TabStop(float position, byte type) {

        if (type < kLeading || type > kAuto) {
            throw new IllegalArgumentException("Invalid tab type");
        }

        fPosition = position;
        fType = type;
    }

    /**
     * Compare this to another Object.  TabStops are equal if
     * their position and type are the same.
     */
    public boolean equals(Object rhs) {
        if (rhs == null) {
            return false;
        }
        TabStop rhsTab;
        try {
            rhsTab = (TabStop)rhs;
        }
        catch (ClassCastException e) {
            return false;
        }
        return fType == rhsTab.fType && fPosition == rhsTab.fPosition;
    }

    /**
     * Return the hash code for this TabStop.  The hash code is
     * <code>position << type</code>.
     */
    public int hashCode() {
        return (int)fPosition << fType;
    }

    public String toString() {
        char typeChar;
        switch (fType) {
        case kLeading:
            typeChar = 'L';
            break;
        case kCenter:
            typeChar = 'C';
            break;
        case kTrailing:
            typeChar = 'R';
            break;
        case kDecimal:
            typeChar = 'D';
            break;
        case kAuto:
            typeChar = 'A';
            break;
        default:
            typeChar = '?';
            break;
        }
        return "TabStop[" + Float.toString(fPosition) + typeChar + ']';
    }

    /*
     * Return the type of this TabStop.  Will be one of the constants in this class.
     */
    public byte getType() {
        return fType;
    }

    /*
     * Return the position of this TabStop.
     */
    public float getPosition() {
        return fPosition;
    }
}
