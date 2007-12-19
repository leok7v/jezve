package org.jezve.notepad.text;

/**
 * A <TT>TextRange</TT> represents a range of text bounded by a
 * start (inclusive), and a limit (exclusive).  [start,limit)
 */
public final class TextRange {

    public int start = 0;
    public int limit = 0;

    public TextRange(int start, int limit) {
        this.start = start;
        this.limit = limit;
    }

    public TextRange() {
        this.start = this.limit = 0;
    }
}
