package org.jezve.notepad.text;

import org.jezve.notepad.text.document.MConstText;
import org.jezve.notepad.text.document.MText;
import org.jezve.notepad.text.format.TextOffset;

class TextChangeCommand extends TextCommand {

    private MConstText fNewText;
    private TextOffset fSelStartAfter;
    private TextOffset fSelEndAfter;

    public TextChangeCommand(EditBehavior behavior, MText originalText, MConstText newText,
            int affectedRangeStart, TextOffset selStartBefore, TextOffset selEndBefore,
            TextOffset selStartAfter, TextOffset selEndAfter) {
        super(behavior, originalText, affectedRangeStart, selStartBefore, selEndBefore);
        fNewText = newText;
        fSelStartAfter = new TextOffset();
        fSelStartAfter.assign(selStartAfter);
        fSelEndAfter = new TextOffset();
        fSelEndAfter.assign(selEndAfter);
    }

    public int affectedRangeEnd() {
        if (fNewText == null) {
            return fAffectedRangeStart;
        }
        else {
            return fAffectedRangeStart + fNewText.length();
        }
    }

    public void execute() {
        fBehavior.doReplaceText(fAffectedRangeStart, fAffectedRangeStart + text.length(), fNewText,
                fSelStartAfter, fSelEndAfter);
    }

    public int affectedRangeStart() {
        return fAffectedRangeStart;
    }

    public void setNewText(MConstText newText) {
        fNewText = newText;
    }

    public void setSelRangeAfter(TextOffset start, TextOffset end) {
        if (fSelStartAfter == null) fSelStartAfter = new TextOffset();
        if (fSelEndAfter == null) fSelEndAfter = new TextOffset();
        fSelStartAfter.assign(start);
        fSelEndAfter.assign(end);
    }

    public void prependToOldText(MConstText newText) {
        text.insert(0, newText);
        fAffectedRangeStart -= newText.length();
    }

    public void appendToOldText(MConstText newText) {
        text.append(newText);
    }
}
