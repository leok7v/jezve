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

import org.jezve.notepad.text.document.AttributeMap;
import org.jezve.notepad.text.document.MConstText;
import org.jezve.notepad.text.document.MText;
import org.jezve.notepad.text.document.StyleModifier;
import org.jezve.notepad.text.format.FRectangle;
import org.jezve.notepad.text.format.TextOffset;

import java.awt.event.*;

// All changes to the text should happen in this class, or in its TypingInteractor.
class EditBehavior extends Behavior {

    private JEditorComponent fTextComponent;
    private SelectionBehavior fSelection;
    private MText fText;

    private SimpleCommandLog fCommandLog;

    private EventBroadcaster fListener;
    private TypingBehavior fTypingInteractor = null;

    private AttributeMap fSavedTypingStyle = null;
    private int fSavedInsPt = 0;

    public EditBehavior(JEditorComponent textComponent, SelectionBehavior selection,
            EventBroadcaster listener) {

        fTextComponent = textComponent;
        fSelection = selection;
        fText = textComponent.getModifiableText();
        fCommandLog = new SimpleCommandLog(listener);
        fListener = listener;
    }

    public boolean textControlEventOccurred(TextEvent event, Object what) {
        boolean handled = true;

        if (event == TextEvent.CHARACTER_STYLE_MOD || event == TextEvent.PARAGRAPH_STYLE_MOD) {
            doStyleChange(event, what);
        }
        else if (event == TextEvent.CUT) {
            doCut();
        }
        else if (event == TextEvent.PASTE) {
            doPaste();
        }
        else if (event == TextEvent.CLEAR) {
            doClear();
        }
        else if (event == TextEvent.REPLACE) {
            doUndoableReplace((TextReplacement)what);
        }
        else if (event == TextEvent.UNDO) {
            fCommandLog.undo();
        }
        else if (event == TextEvent.REDO) {
            fCommandLog.redo();
        }
        else if (event == TextEvent.SET_MODIFIED) {
            fCommandLog.setModified(what == Boolean.TRUE);
        }
        else if (event == TextEvent.CLEAR_COMMAND_LOG) {
            fCommandLog.clearLog();
        }
        else if (event == TextEvent.SET_COMMAND_LOG_SIZE) {
            fCommandLog.setLogSize(((Integer)what).intValue());
        }
        else {
            handled = super.textControlEventOccurred(event, what);
        }
        checkSavedTypingStyle();
        return handled;
    }

    /*
     * It's unfortunate that the text is modified and reformatted in three different methods.
     * This method is the "common prologue" for all text modifications.
     *
     * This method should be called before modifying and reformatting the text.  It does three
     * things:  stops caret blinking, stops background formatting, and returns the Rectangle
     * containing the current (soon-to-be obsolete) selection.
     */
    private FRectangle prepareForTextEdit() {
        fSelection.stopCaretBlinking();
        return fTextComponent.getBoundingRect(fSelection.getStart(), fSelection.getEnd());
    }

    private void doClear() {
        TextRange selRange = fSelection.getSelectionRange();
        if (selRange.start == selRange.limit) {
            return;
        }
        doUndoableTextChange(selRange.start, selRange.limit, null, new TextOffset(selRange.
                start), new TextOffset(selRange.start));
    }

    private void doCut() {
        TextRange selRange = fSelection.getSelectionRange();

        if (selRange.start == selRange.limit) {
            return;
        }

        fTextComponent.getClipboard().setContents(fText.extract(selRange.start, selRange.limit));
        doUndoableTextChange(selRange.start, selRange.limit, null, new TextOffset(selRange.start),
                new TextOffset(selRange.start));

        fListener.textStateChanged(TextEvent.CLIPBOARD_CHANGED);
    }

    private void doPaste() {
        TextRange selRange = fSelection.getSelectionRange();
        MConstText clipText =
                fTextComponent.getClipboard().getContents(AttributeMap.EMPTY_ATTRIBUTE_MAP);

        if (clipText != null) {
            doUndoableTextChange(selRange.start, selRange.limit, clipText,
                    new TextOffset(selRange.start + clipText.length()),
                    new TextOffset(selRange.start + clipText.length()));
        }
        else {
            fListener.textStateChanged(TextEvent.CLIPBOARD_CHANGED);
        }
    }

    private void doUndoableReplace(TextReplacement replacement) {
        doUndoableTextChange(replacement.getStart(), replacement.getLimit(), replacement.getText(),
                replacement.getSelectionStart(), replacement.getSelectionLimit());
    }

    // Only TypingInteractor and TextCommand should call this!
    void doReplaceText(int start, int limit, MConstText newText, TextOffset newSelStart,
            TextOffset newSelEnd) {

        int textLength;
        fText.resetDamagedRange();
        FRectangle oldSelRect = prepareForTextEdit();

        if (newText == null) {
            textLength = 0;
            fText.remove(start, limit);
        }
        else {
            textLength = newText.length();
            fText.replace(start, limit, newText, 0, textLength);
        }
        fSelection.setSelectionRange(newSelStart, newSelEnd, newSelStart);
        reformatAndDrawText(fSelection.getStart(), fSelection.getEnd(), oldSelRect);
    }

    // Only the typing interactor should call this!
    void doReplaceSelectedText(char ch, AttributeMap charStyle) {
        int start = fSelection.getStart().offset;
        int limit = fSelection.getEnd().offset;
        TextOffset newOffset = new TextOffset(start + 1);
        doReplaceText(start, limit, ch, charStyle, newOffset, newOffset);
    }

    private void doReplaceText(int start, int limit, char ch, AttributeMap charStyle,
            TextOffset newSelStart, TextOffset newSelEnd) {

        fText.resetDamagedRange();
        FRectangle oldSelRect = prepareForTextEdit();
        fText.replace(start, limit, ch, charStyle);
        fSelection.setSelectionRange(newSelStart, newSelEnd, newSelStart);
        reformatAndDrawText(fSelection.getStart(), fSelection.getEnd(), oldSelRect);
    }

    private void doStyleChange(TextEvent event, Object what) {

        TextRange selRange = fSelection.getSelectionRange();
        boolean character = (event == TextEvent.CHARACTER_STYLE_MOD);

        if (selRange.start != selRange.limit || !character) {
            doUndoableStyleChange(what, character);
        }
        else {
            TypingBehavior interactor = new TypingBehavior(fTextComponent, fSelection,
                    fSavedTypingStyle, this, fCommandLog, fListener);

            interactor.addToOwner(fTextComponent);
            interactor.textControlEventOccurred(event, what);
        }
    }

    // Only text commands should call this method!
    void doModifyStyles(int start, int limit, StyleModifier modifier, boolean character,
            TextOffset newSelStart, TextOffset newSelEnd) {

        fText.resetDamagedRange();
        FRectangle oldSelRect = prepareForTextEdit();
        if (character) {
            fText.modifyCharacterStyles(start, limit, modifier);
        }
        else {
            fText.modifyParagraphStyles(start, limit, modifier);
        }
        fSelection.setSelectionRange(newSelStart, newSelEnd, newSelStart);
        reformatAndDrawText(newSelStart, newSelEnd, oldSelRect);
    }

    private void doUndoableStyleChange(Object what, boolean character) {

        TextOffset selStart = fSelection.getStart();
        TextOffset selEnd = fSelection.getEnd();

        MText oldText = fText.extractWritable(selStart.offset, selEnd.offset);
        StyleChangeCommand command = new StyleChangeCommand(this, oldText, selStart, selEnd,
                (StyleModifier)what, character);

        fCommandLog.addAndDo(command);

        fListener.textStateChanged(TextEvent.SELECTION_STYLES_CHANGED);
    }

    private void doUndoableTextChange(int start, int limit, MConstText newText,
            TextOffset newSelStart, TextOffset newSelEnd) {

        TextChangeCommand command = new TextChangeCommand(this, fText.extractWritable(start, limit),
                newText, start, fSelection.getStart(), fSelection.getEnd(), newSelStart, newSelEnd);

        fCommandLog.addAndDo(command);
    }

    public boolean canUndo() {

        boolean canUndo = false;

        if (fTypingInteractor != null) {
            canUndo = fTypingInteractor.hasPendingCommand();
        }

        if (!canUndo) {
            canUndo = fCommandLog.canUndo();
        }

        return canUndo;
    }

    public boolean canRedo() {

        return fCommandLog.canRedo();
    }

    public boolean isModified() {

        if (fTypingInteractor != null) {
            if (fTypingInteractor.hasPendingCommand()) {
                return true;
            }
        }
        return fCommandLog.isModified();
    }

    public int getCommandLogSize() {

        return fCommandLog.getLogSize();
    }

    public AttributeMap getInsertionPointStyle() {

        if (fTypingInteractor != null) {
            return fTypingInteractor.getTypingStyle();
        }

        if (fSavedTypingStyle != null) {
            return fSavedTypingStyle;
        }

        TextRange range = fSelection.getSelectionRange();
        return typingStyleAt(fText, range.start, range.limit);
    }

    public boolean keyPressed(KeyEvent e) {

        boolean handled = true;
        if (TypingBehavior.handledByTypingInteractor(e)) {
            TypingBehavior interactor = new TypingBehavior(fTextComponent, fSelection,
                    fSavedTypingStyle, this, fCommandLog, fListener);

            interactor.addToOwner(fTextComponent);
            interactor.keyPressed(e);
        }
        else {
            handled = super.keyPressed(e);
            checkSavedTypingStyle();
        }

        return handled;
    }

    public boolean keyTyped(KeyEvent e) {

        boolean handled = true;
        if (TypingBehavior.handledByTypingInteractor(e)) {
            TypingBehavior interactor = new TypingBehavior(fTextComponent, fSelection,
                    fSavedTypingStyle, this, fCommandLog, fListener);

            interactor.addToOwner(fTextComponent);
            interactor.keyTyped(e);
        }
        else {
            handled = super.keyTyped(e);
            checkSavedTypingStyle();
        }

        return handled;
    }

    public boolean mouseReleased(FMouseEvent e) {

        boolean result = super.mouseReleased(e);
        checkSavedTypingStyle();
        return result;
    }

    private void reformatAndDrawText(TextOffset selStart, TextOffset selLimit,
            FRectangle oldSelRect) {
        if (!fSelection.enabled()) {
            selStart = selLimit = null;
        }

        int reformatStart = fText.damagedRangeStart();
        int reformatLength = fText.damagedRangeLimit() - reformatStart;

        if (reformatStart != Integer.MAX_VALUE) {
            fTextComponent.reformatAndDrawText(reformatStart, reformatLength, selStart, selLimit,
                    oldSelRect, fSelection.getHighlightColor());
        }

        fSelection.scrollToShowSelection();

        // sometimes this should send SELECTION_STYLES_CHANGED
        fListener.textStateChanged(TextEvent.TEXT_CHANGED);

        fSelection.restartCaretBlinking(true);
    }

    // Only TypingInteractor should call this.
    void setTypingInteractor(TypingBehavior interactor) {
        fTypingInteractor = interactor;
    }

    // Only TypingInteractor should call this.
    void setSavedTypingStyle(AttributeMap style, int insPt) {
        fSavedTypingStyle = style;
        fSavedInsPt = insPt;
    }

    private void checkSavedTypingStyle() {

        if (fSavedTypingStyle != null) {
            int selStart = fSelection.getStart().offset;
            int selLimit = fSelection.getEnd().offset;
            if (selStart != fSavedInsPt || selStart != selLimit) {
                fSavedTypingStyle = null;
            }
        }
    }

    // Return the style appropriate for typing on the given selection range.
    public static AttributeMap typingStyleAt(MConstText text, int start, int limit) {

        if (start < limit) {
            return text.characterStyleAt(start);
        }
        else if (start > 0) {
            return text.characterStyleAt(start - 1);
        }
        else {
            return text.characterStyleAt(0);
        }
    }
}
