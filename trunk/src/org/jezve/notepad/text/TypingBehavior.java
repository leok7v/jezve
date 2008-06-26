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

package org.jezve.notepad.text;

import org.jezve.notepad.text.document.*;
import org.jezve.notepad.text.format.TextOffset;

import java.awt.event.*;
import java.text.BreakIterator;

final class TypingBehavior extends Behavior {

    private static final char BACKSPACE = 8;
    private static final char TAB = '\t';
    private static final char RETURN = '\r';
    private static final char LINE_FEED = '\n';
//    private static final char PARAGRAPH_SEP = '\u2029';

    private SelectionBehavior fSelection;
    private AttributeMap fTypingStyle;
    private MConstText fText;
    private EditBehavior fParent;

    private TextChangeCommand fCommand = null;
    private SimpleCommandLog fCommandLog;
    private EventBroadcaster fListener;

    private BreakIterator fCharBreak = null;

    /*
     * Not all characters that come from the keyboard are handled
     * as input.  For example, ctrl-c is not a typable character.
     * This method determines whether a particular character from
     * the keyboard will affect the text.
     */
    private static boolean isTypingInteractorChar(char ch) {
        return ch >= ' ' || ch == LINE_FEED || ch == RETURN || ch == TAB || ch == BACKSPACE;
    }

    /*
     * This method determines whether a TypingInteractor should
     * handle the given KeyEvent.
     */
    static boolean handledByTypingInteractor(KeyEvent event) {
        final int id = event.getID();
        if (id == KeyEvent.KEY_TYPED) {
            return isTypingInteractorChar(event.getKeyChar());
        }
        else {
            return (id == KeyEvent.KEY_PRESSED && event.getKeyCode() == KeyEvent.VK_DELETE);
        }
    }

    public TypingBehavior(JEditorComponent textComponent, SelectionBehavior selection,
            AttributeMap typingStyle, EditBehavior parent, SimpleCommandLog commandLog,
            EventBroadcaster listener) {

        fText = textComponent.getText();
        fSelection = selection;
        fTypingStyle = typingStyle;
        fParent = parent;
        fCommandLog = commandLog;
        fListener = listener;

        fParent.setTypingInteractor(this);
    }

    private void endInteraction() {
        removeFromOwner();
        postTextChangeCommand();
        int selStart = fSelection.getStart().offset;
        int selLimit = fSelection.getEnd().offset;
        fParent.setSavedTypingStyle(selStart == selLimit ? fTypingStyle : null, selStart);
        fParent.setTypingInteractor(null);
    }

    public boolean textControlEventOccurred(TextEvent event, Object what) {
        if (fCommand == null && event == TextEvent.CHARACTER_STYLE_MOD) {
            pickUpTypingStyle();
            fTypingStyle = ((StyleModifier)what).modifyStyle(fTypingStyle);
            fListener.textStateChanged(TextEvent.SELECTION_STYLES_CHANGED);
            return true;
        }
        else {
            Behavior next = nextBehavior(); // save because removeFromOwner() will trash this
            endInteraction();
            return next != null && next.textControlEventOccurred(event, what);
        }
    }

    private void doBackspace() {

        int selStart = fSelection.getStart().offset;
        int selLimit = fSelection.getEnd().offset;

        if (selStart == selLimit) {
            if (selStart != 0) {
                fTypingStyle = null;
                pickUpTypingStyle();
                makeTextChangeCommand();
                if (selStart <= fCommand.affectedRangeStart()) {
                    fCommand.prependToOldText(fText.extract(selStart - 1, selStart));
                }
                TextOffset insPt = new TextOffset(selStart - 1);
                fParent.doReplaceText(selStart - 1, selStart, null, insPt, insPt);
            }
        }
        else {
            fTypingStyle = null;
            makeTextChangeCommand();
            TextOffset insPt = new TextOffset(selStart);
            fParent.doReplaceText(selStart, selLimit, null, insPt, insPt);
        }
    }

    private void doFwdDelete(boolean ignoreCharBreak) {
        int selStart = fSelection.getStart().offset;
        int selLimit = fSelection.getEnd().offset;

        TextOffset insPt = new TextOffset(selStart);

        if (selStart == selLimit) {
            if (selStart != fText.length()) {
                fTypingStyle = null;
                makeTextChangeCommand();
                int numChars;
                if (ignoreCharBreak) {
                    numChars = 1;
                }
                else {
                    if (fCharBreak == null) {
                        fCharBreak = BreakIterator.getCharacterInstance();
                    }
                    fCharBreak.setText(fText.createCharacterIterator());
                    numChars = fCharBreak.following(selStart) - selStart;
                }
                fCommand.appendToOldText(fText.extract(selStart, selStart + numChars));
                fParent.doReplaceText(selStart, selStart + numChars, null, insPt, insPt);
            }
        }
        else {
            fTypingStyle = null;
            makeTextChangeCommand();
            fParent.doReplaceText(selStart, selLimit, null, insPt, insPt);
        }
    }

    private void doNormalKey(char ch) {

        // Sigh - 1.1 reports enter key events as return chars, but
        // 1.2 reports them as linefeeds.
        if (ch == RETURN) {
            ch = LINE_FEED;
        }
        pickUpTypingStyle();
        makeTextChangeCommand();
        fParent.doReplaceSelectedText(ch, fTypingStyle);
    }

    public boolean focusGained(FocusEvent e) {
        // pass through, but stick around...
        return super.focusGained(e);
    }

    public boolean focusLost(FocusEvent e) {
        // pass through, but stick around...
        return super.focusLost(e);
    }

    public boolean keyTyped(KeyEvent e) {
        if (e.getKeyChar() == BACKSPACE) {
            doBackspace();
        }
        else {
            if (isTypingInteractorChar(e.getKeyChar())) {
                doNormalKey(e.getKeyChar());
            }
        }
        return true;
    }

    public boolean keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_DELETE) {
            doFwdDelete(e.isShiftDown());
            return true;
        }
        Behavior next = nextBehavior();
        if (SelectionBehavior.keyAffectsSelection(e)) {
            endInteraction();
        }
        return next.keyPressed(e);
    }

    public boolean keyReleased(KeyEvent e) {
        return true;
    }

    private void makeTextChangeCommand() {
        if (fCommand == null) {
            TextOffset selStart = fSelection.getStart();
            TextOffset selEnd = fSelection.getEnd();

            MText writableText = new StyledText();
            writableText.replace(0, 0, fText, selStart.offset, selEnd.offset);
            fCommand = new TextChangeCommand(fParent, writableText, null, selStart.offset,
                    selStart, selEnd, new TextOffset(), new TextOffset());

            fListener.textStateChanged(TextEvent.UNDO_STATE_CHANGED);
        }
    }

    public boolean mouseDragged(FMouseEvent e) {
        return true;
    }

    public boolean mouseEntered(FMouseEvent e) {
        return true;
    }

    public boolean mouseExited(FMouseEvent e) {
        return true;
    }

    public boolean mouseMoved(FMouseEvent e) {
        return true;
    }

    public boolean mousePressed(FMouseEvent e) {
        Behavior next = nextBehavior(); // save because removeFromOwner() will trash this
        endInteraction();
        return next != null && next.mousePressed(e);
    }

    public boolean mouseReleased(FMouseEvent e) {
        Behavior next = nextBehavior(); // save because removeFromOwner() will trash this
        endInteraction();
        return next != null && next.mouseReleased(e);
    }

    private void pickUpTypingStyle() {
        if (fTypingStyle == null) {
            int selStart = fSelection.getStart().offset;
            int selLimit = fSelection.getEnd().offset;
            fTypingStyle = EditBehavior.typingStyleAt(fText, selStart, selLimit);
        }
    }

    private void postTextChangeCommand() {
        if (fCommand != null) {
            TextOffset selStart = fSelection.getStart();
            TextOffset selEnd = fSelection.getEnd();

            fCommand.setNewText(fText.extract(fCommand.affectedRangeStart(), selStart.offset));
            fCommand.setSelRangeAfter(selStart, selEnd);
            fCommandLog.add(fCommand);
        }
    }

    boolean hasPendingCommand() {
        return fCommand != null;
    }

    AttributeMap getTypingStyle() {
        pickUpTypingStyle();
        return fTypingStyle;
    }
}
