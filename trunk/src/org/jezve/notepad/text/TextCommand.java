package org.jezve.notepad.text;

import org.jezve.notepad.text.document.MText;
import org.jezve.notepad.text.format.TextOffset;

abstract class TextCommand extends Command {

    protected EditBehavior fBehavior;
    protected MText text;
    protected int fAffectedRangeStart;
    protected TextOffset fSelStartBefore;
    protected TextOffset fSelEndBefore;

    public TextCommand(EditBehavior behavior, MText originalText, int affectedRangeStart,
            TextOffset selStartBefore, TextOffset selEndBefore) {

        fBehavior = behavior;
        text = originalText;
        fAffectedRangeStart = affectedRangeStart;
        fSelStartBefore = new TextOffset();
        fSelStartBefore.assign(selStartBefore);
        fSelEndBefore = new TextOffset();
        fSelEndBefore.assign(selEndBefore);
    }

    public abstract int affectedRangeEnd();

    public void undo() {
        fBehavior.doReplaceText(fAffectedRangeStart, affectedRangeEnd(), text, fSelStartBefore,
                fSelEndBefore);
    }
}
