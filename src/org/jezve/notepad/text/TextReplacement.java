package org.jezve.notepad.text;

import org.jezve.notepad.text.document.MConstText;
import org.jezve.notepad.text.format.TextOffset;

/**
 * This class is used to pass a REPLACE command to Behaviors.
 */
public final class TextReplacement {

    private int fStart;
    private int fLimit;
    private MConstText fText;
    private TextOffset fSelStart;
    private TextOffset fSelLimit;

    public TextReplacement(int start, int limit, MConstText text, TextOffset selStart,
            TextOffset selLimit) {

        fStart = start;
        fLimit = limit;
        fText = text;
        fSelStart = selStart;
        fSelLimit = selLimit;
    }

    public int getStart() {
        return fStart;
    }

    public int getLimit() {
        return fLimit;
    }

    public MConstText getText() {
        return fText;
    }

    public TextOffset getSelectionStart() {
        return fSelStart;
    }

    public TextOffset getSelectionLimit() {
        return fSelLimit;
    }
}
