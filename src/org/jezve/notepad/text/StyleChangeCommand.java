package org.jezve.notepad.text;

import org.jezve.notepad.text.document.MText;
import org.jezve.notepad.text.document.StyleModifier;
import org.jezve.notepad.text.format.TextOffset;

class StyleChangeCommand extends TextCommand {

    private boolean fCharacter;
    private StyleModifier fModifier;

    public StyleChangeCommand(EditBehavior behavior, MText originalText, TextOffset selStartBefore,
            TextOffset selEndBefore, StyleModifier modifier, boolean character) {

        super(behavior, originalText, selStartBefore.offset, selStartBefore, selEndBefore);
        fModifier = modifier;
        fCharacter = character;
    }

    public int affectedRangeEnd() {
        return fSelEndBefore.offset;
    }

    public void execute() {
        fBehavior.doModifyStyles(fAffectedRangeStart, fSelEndBefore.offset, fModifier, fCharacter,
                fSelStartBefore, fSelEndBefore);
    }
}
