package org.jezve.notepad.text;

import org.jezve.notepad.text.document.MConstText;
import org.jezve.notepad.text.format.FRectangle;
import org.jezve.notepad.text.format.IFormatter;
import org.jezve.notepad.text.format.TextOffset;

import java.awt.*;
import java.awt.event.*;
import java.text.BreakIterator;

public class SelectionBehavior extends Behavior implements Runnable {

    static final Color HIGHLIGHTCOLOR = Color.pink;

    private JEditorComponent host;
    private MConstText fText;
    private TextOffset fStart;
    private TextOffset fLimit;
    private TextOffset fAnchor;
    private TextOffset fUpDownAnchor = null;
    private BreakIterator fBoundaries = null;
    private Color fHighlightColor = HIGHLIGHTCOLOR;

    private EventBroadcaster fListener;

    private boolean fMouseDown = false;
    private boolean fHandlingKeyOrCommand = false;

    private boolean fCaretShouldBlink;
    private boolean fCaretIsVisible;
    private int fCaretCount;

    // formerly in base class
    private boolean fEnabled;

    private FMouseEvent fPendingMouseEvent = null;

    private static final int kCaretInterval = 500;

    public void run() {
        while (true) {
            synchronized (this) {
                while (!fCaretShouldBlink) {
                    try {
                        wait();
                    }
                    catch (InterruptedException e) {
                        System.out.println("Caught InterruptedException in caret thread.");
                    }
                }
                ++fCaretCount;
                if (fCaretCount % 2 == 0) {
                    fCaretIsVisible = !fCaretIsVisible;
                    drawSelection(fCaretIsVisible);
                }
            }
            try {
                Thread.sleep(kCaretInterval);
            }
            catch (InterruptedException e) {
            }
        }
    }


    public SelectionBehavior(JEditorComponent textComponent, EventBroadcaster broadcaster) {

        host = textComponent;
        fText = textComponent.getText();
        fListener = broadcaster;

        fStart = new TextOffset();
        fLimit = new TextOffset();
        fAnchor = new TextOffset();
        fMouseDown = false;

        fCaretCount = 0;
        fCaretIsVisible = true;
        fCaretShouldBlink = false;
        setEnabled(false);

        Thread caretThread = new Thread(this);
        caretThread.setDaemon(true);
        caretThread.start();
    }

    boolean enabled() {
        return fEnabled;
    }

    private void setEnabled(boolean enabled) {
        fEnabled = enabled;
    }

    public boolean textControlEventOccurred(TextEvent event, Object what) {

        boolean result;
        fHandlingKeyOrCommand = true;

        if (event == TextEvent.SELECT) {
            select((TextRange)what);
            result = true;
        }
        else if (event == TextEvent.COPY) {
            host.getClipboard()
                    .setContents(fText.extract(fStart.offset, fLimit.offset));
            fListener.textStateChanged(TextEvent.CLIPBOARD_CHANGED);
            result = true;
        }
        else {
            result = false;
        }
        fHandlingKeyOrCommand = false;
        return result;
    }

    protected void advanceToNextBoundary(TextOffset offset) {
        // If there's no boundaries object, or if position at the end of the
        // document, return the offset unchanged
        if (fBoundaries == null) {
            return;
        }
        int position = offset.offset;
        if (position >= fText.length()) {
            return;
        }
        // If position is at a boundary and offset is before position,
        // leave it unchanged.  Otherwise move to next boundary.
        int nextPos = fBoundaries.following(position);
        if (fBoundaries.previous() == position && offset.bias == TextOffset.BEFORE) {
            return;
        }
        offset.setOffset(nextPos, TextOffset.AFTER);
    }

    protected void advanceToPreviousBoundary(TextOffset offset) {
        advanceToPreviousBoundary(offset, false);
    }

    private void advanceToPreviousBoundary(TextOffset offset, boolean alwaysMove) {
        // if there's no boundaries object, or if we're sitting at the beginning
        // of the document, return the offset unchanged
        if (fBoundaries == null) {
            return;
        }

        if (offset.offset == 0) {
            return;
        }

        int position = offset.offset;
        // If position is at a boundary, leave it unchanged.  Otherwise
        // move to previous boundary.
        if (position == fText.length()) {
            fBoundaries.last();
        }
        else {
            fBoundaries.following(position);
        }

        int prevPos = fBoundaries.previous();
        if (prevPos == position) {
            if (!alwaysMove && offset.bias == TextOffset.AFTER) {
                return;
            }
            prevPos = fBoundaries.previous();
        }
        // and finally update the real offset with this new position we've found
        offset.setOffset(prevPos, TextOffset.AFTER);
    }

    private void doArrowKey(KeyEvent e, int key) {

        // when there's a selection range, the left and up arrow keys place an
        // insertion point at the beginning of the range, and the right and down
        // keys place an insertion point at the end of the range (unless the shift
        // key is down, of course)

        if (!fStart.equals(fLimit) && !e.isShiftDown()) {
            if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_UP) {
                setSelRangeAndDraw(fStart, fStart, fStart);
            }
            else {
                setSelRangeAndDraw(fLimit, fLimit, fLimit);
            }
        }
        else {
            if (!fAnchor.equals(fStart)) fAnchor.assign(fLimit);

            TextOffset liveEnd = (fStart.equals(fAnchor)) ? fLimit : fStart;
            TextOffset newPos = new TextOffset();

            // if the control key is down, the left and right arrow keys move by whole
            // word in the appropriate direction (we use a line break object so that we're
            // not treating spaces and punctuation as words)
            if (e.isControlDown() && (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT)) {
                fUpDownAnchor = null;
                fBoundaries = BreakIterator.getLineInstance();
                fBoundaries.setText(fText.createCharacterIterator());

                newPos.assign(liveEnd);
                if (key == KeyEvent.VK_RIGHT) {
                    advanceToNextBoundary(newPos);
                }
                else {
                    advanceToPreviousBoundary(newPos, true);
                }
            }

            // if we get down to here, this is a plain-vanilla insertion-point move,
            // or the shift key is down and we're extending or shortening the selection
            else {

                // fUpDownAnchor is used to keep track of the horizontal position
                // across a run of up or down arrow keys (this prevents accumulated
                // error from destroying our horizontal position)
                if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
                    fUpDownAnchor = null;
                }
                else {
                    if (fUpDownAnchor == null) {
                        fUpDownAnchor = new TextOffset(liveEnd);
                    }
                }

                short direction = IFormatter.eRight;  // just to have a default...

                switch (key) {
                case KeyEvent.VK_UP:
                    direction = IFormatter.eUp;
                    break;
                case KeyEvent.VK_DOWN:
                    direction = IFormatter.eDown;
                    break;
                case KeyEvent.VK_LEFT:
                    direction = IFormatter.eLeft;
                    break;
                case KeyEvent.VK_RIGHT:
                    direction = IFormatter.eRight;
                    break;
                }
                // use the formatter to determine the actual effect of the arrow key
                host.getCaretOffset(newPos, fUpDownAnchor, liveEnd, direction);
            }

            // if the shift key is down, the selection range is from the anchor point
            // the site of the last insertion point or the beginning point of the last
            // selection drag operation) to the newly-calculated position; if the
            // shift key is down, the newly-calculated position is the insertion point position
            if (!e.isShiftDown()) {
                setSelRangeAndDraw(newPos, newPos, newPos);
            }
            else {
                if (newPos.lessThan(fAnchor)) {
                    setSelRangeAndDraw(newPos, fAnchor, fAnchor);
                }
                else {
                    setSelRangeAndDraw(fAnchor, newPos, fAnchor);
                }
            }
        }

        scrollToShowSelectionEnd();
        fBoundaries = null;
    }

    private void doEndKey(KeyEvent e) {
        // ctrl-end moves the insertsion point to the end of the document,
        // ctrl-shift-end extends the selection so that it ends at the end
        // of the document

        TextOffset activeEnd, anchor;

        if (fAnchor.equals(fStart)) {
            activeEnd = new TextOffset(fStart);
            anchor = new TextOffset(fLimit);
        }
        else {
            activeEnd = new TextOffset(fLimit);
            anchor = new TextOffset(fStart);
        }

        if (e.isControlDown()) {
            TextOffset end = new TextOffset(fText.length(), TextOffset.BEFORE);

            if (e.isShiftDown()) {
                setSelRangeAndDraw(anchor, end, anchor);
            }
            else {
                setSelRangeAndDraw(end, end, end);
            }
        }

        // end moves the insertion point to the end of the line containing
        // the end of the current selection
        // shift-end extends the selection to the end of the line containing
        // the end of the current selection

        else {

            int oldOffset = activeEnd.offset;

            activeEnd.offset = host.lineRangeLimit(host.lineContaining(activeEnd));
            activeEnd.bias = TextOffset.BEFORE;

            if (fText.paragraphLimit(oldOffset) == activeEnd.offset &&
                    activeEnd.offset != fText.length() && activeEnd.offset > oldOffset) {
                activeEnd.offset--;
                activeEnd.bias = TextOffset.AFTER;
            }

            if (!e.isShiftDown()) {
                setSelRangeAndDraw(activeEnd, activeEnd, activeEnd);
            }
            else {
                if (activeEnd.lessThan(anchor)) {
                    setSelRangeAndDraw(activeEnd, anchor, anchor);
                }
                else {
                    setSelRangeAndDraw(anchor, activeEnd, anchor);
                }
            }
        }

        scrollToShowSelectionEnd();
        fBoundaries = null;
        fUpDownAnchor = null;
    }

    private void doHomeKey(KeyEvent e) {
        // ctrl-home moves the insertion point to the beginning of the document,
        // ctrl-shift-home extends the selection so that it begins at the beginning
        // of the document

        TextOffset activeEnd, anchor;

        if (fAnchor.equals(fStart)) {
            activeEnd = new TextOffset(fStart);
            anchor = new TextOffset(fLimit);
        }
        else {
            activeEnd = new TextOffset(fLimit);
            anchor = new TextOffset(fStart);
        }

        if (e.isControlDown()) {

            TextOffset start = new TextOffset(0, TextOffset.AFTER);
            if (e.isShiftDown()) {
                setSelRangeAndDraw(start, anchor, anchor);
            }
            else {
                setSelRangeAndDraw(start, start, start);
            }
        }

        // home moves the insertion point to the beginning of the line containing
        // the beginning of the current selection
        // shift-home extends the selection to the beginning of the line containing
        // the beginning of the current selection

        else {

            activeEnd.offset = host.lineRangeLow(host.lineContaining(activeEnd));
            activeEnd.bias = TextOffset.AFTER;

            if (!e.isShiftDown()) {
                setSelRangeAndDraw(activeEnd, activeEnd, activeEnd);
            }
            else {
                if (activeEnd.lessThan(anchor)) {
                    setSelRangeAndDraw(activeEnd, anchor, anchor);
                }
                else {
                    setSelRangeAndDraw(anchor, activeEnd, anchor);
                }
            }
        }

        scrollToShowSelectionEnd();
        fBoundaries = null;
        fUpDownAnchor = null;
    }

    /**
     * draws or erases the current selection
     * Draws or erases the highlight region or insertion caret for the current selection
     * range.
     *
     * @param visible If true, draw the selection; if false, erase it
     */
    protected void drawSelection(boolean visible) {
        drawSelectionRange(fStart, fLimit, visible);
    }

    /**
     * draws or erases a selection highlight at the specfied positions
     * Draws or erases a selection highlight or insertion caret corresponding to
     * the specified selecion range
     *
     * @param start   The beginning of the range to highlight
     * @param limit   The end of the range to highlight
     * @param visible If true, draw; if false, erase
     */
    protected void drawSelectionRange(TextOffset start, TextOffset limit, boolean visible) {
        Graphics2D g = host.lock();
        if (g == null) {
            return;
        }
        FRectangle sel = host.getBoundingRect(start, limit);
        sel.width = Math.max(1, sel.width);
        sel.height = Math.max(1, sel.height);

        host.drawText(g, sel, visible, start, limit, fHighlightColor);
        host.unlock(g);
    }

    protected TextOffset getAnchor() {
        return fAnchor;
    }

    public TextOffset getEnd() {
        return fLimit;
    }

    public Color getHighlightColor() {
        return fHighlightColor;
    }

    public TextOffset getStart() {
        return fStart;
    }

    public TextRange getSelectionRange() {
        return new TextRange(fStart.offset, fLimit.offset);
    }

    public boolean focusGained(FocusEvent e) {

        setEnabled(true);
        drawSelection(true);
        restartCaretBlinking(true);
        if (fPendingMouseEvent != null) {
            mousePressed(fPendingMouseEvent);
            fPendingMouseEvent = null;
        }
        fListener.textStateChanged(TextEvent.CLIPBOARD_CHANGED);
        return true;
    }

    public boolean focusLost(FocusEvent e) {
        stopCaretBlinking();
        setEnabled(false);
        drawSelection(false);
        return true;
    }

    // Return true if the given key event can affect the selection range.
    public static boolean keyAffectsSelection(KeyEvent e) {

        if (e.getID() != KeyEvent.KEY_PRESSED) {
            return false;
        }

        int key = e.getKeyCode();

        switch (key) {
        case KeyEvent.VK_HOME:
        case KeyEvent.VK_END:
        case KeyEvent.VK_LEFT:
        case KeyEvent.VK_RIGHT:
        case KeyEvent.VK_UP:
        case KeyEvent.VK_DOWN:
            return true;

        default:
            return false;
        }
    }

    public boolean keyPressed(KeyEvent e) {

        fHandlingKeyOrCommand = true;
        int key = e.getKeyCode();
        boolean result = true;

        switch (key) {
        case KeyEvent.VK_HOME:
            doHomeKey(e);
            break;

        case KeyEvent.VK_END:
            doEndKey(e);
            break;

        case KeyEvent.VK_LEFT:
        case KeyEvent.VK_RIGHT:
        case KeyEvent.VK_UP:
        case KeyEvent.VK_DOWN:
            doArrowKey(e, key);
            break;

        default:
            fUpDownAnchor = null;
            result = false;
            break;
        }

        fHandlingKeyOrCommand = false;
        return result;
    }

    public boolean mousePressed(FMouseEvent e) {

        if (!enabled()) {
            fPendingMouseEvent = e;
            host.requestFocus();
            return false;
        }

        if (fMouseDown) throw new Error("fMouseDown is out of sync with mouse in TextSelection.");

        fMouseDown = true;
        stopCaretBlinking();

        float x = e.getFX(), y = e.getFY();
        boolean wasZeroLength = rangeIsZeroLength(fStart, fLimit, fAnchor);

        TextOffset current = host.pointToTextOffset(null, x, y, null, true);
        TextOffset anchorStart = new TextOffset();
        TextOffset anchorEnd = new TextOffset();

        fUpDownAnchor = null;

        // if we're not extending the selection...
        if (!e.isShiftDown()) {

            // if there are multiple clicks, create the appopriate type of BreakIterator
            // object for finding text boundaries (single clicks don't use a BreakIterator
            // object)
            if (e.getClickCount() == 2) {
                fBoundaries = BreakIterator.getWordInstance();
            }
            else if (e.getClickCount() == 3) {
                fBoundaries = BreakIterator.getSentenceInstance();
            }
            else {
                fBoundaries = null;
            }

            // if we're using a BreakIterator object, use it to find the nearest boundaries
            // on either side of the mouse-click position and make them our anchor range
            if (fBoundaries != null) fBoundaries.setText(fText.createCharacterIterator());

            anchorStart.assign(current);
            advanceToPreviousBoundary(anchorStart);
            anchorEnd.assign(current);
            advanceToNextBoundary(anchorEnd);
        }

        // if we _are_ extending the selection, determine our anchor range as follows:
        // fAnchor is the start of the anchor range;
        // the next boundary (after fAnchor) is the limit of the anchor range.

        else {

            if (fBoundaries != null) fBoundaries.setText(fText.createCharacterIterator());

            anchorStart.assign(fAnchor);
            anchorEnd.assign(anchorStart);

            advanceToNextBoundary(anchorEnd);
        }
        SelectionDragInteractor interactor = new SelectionDragInteractor(this, host, anchorStart,
                anchorEnd, current, x, y, wasZeroLength);
        interactor.addToOwner(host);
        return true;
    }

    public boolean mouseReleased(FMouseEvent e) {
        fPendingMouseEvent = null;
        return false;
    }

    // drag interactor calls this
    void mouseReleased(boolean zeroLengthChange) {
        fMouseDown = false;

        if (zeroLengthChange) {
            fListener.textStateChanged(TextEvent.SELECTION_EMPTY_CHANGED);
        }
        fListener.textStateChanged(TextEvent.SELECTION_RANGE_CHANGED);
        fListener.textStateChanged(TextEvent.SELECTION_STYLES_CHANGED);
        // if caret drawing during mouse drags is supressed, draw caret now.
        restartCaretBlinking(true);
    }

    /*
     * draws the selection
     * Provided, of course, that the selection is visible, the adorner is enabled,
     * and we're calling it to adorn the view it actually belongs to
     *
     * @param g The graphics environment to draw into
     * @return true if we actually drew
     */
    public boolean paint(Graphics2D g, Rectangle drawRect) {
        // don't draw anything unless we're enabled and the selection is visible
        if (!enabled()) return false;
        host.drawText(g, new FRectangle(drawRect), true, fStart, fLimit, fHighlightColor);
        return true;
    }

    /**
     * scrolls the view to reveal the live end of the selection
     * (i.e., the end that moves if you use the arrow keys with the shift key down)
     */
    public void scrollToShowSelection() {
        FRectangle selRect = host.getBoundingRect(fStart, fLimit);
        host.scrollToShow(selRect);
    }

    /**
     * scrolls the view to reveal the live end of the selection
     * (i.e., the end that moves if you use the arrow keys with the shift key down)
     */
    public void scrollToShowSelectionEnd() {
        TextOffset liveEnd;

        if (fAnchor.equals(fStart)) {
            liveEnd = fLimit;
        }
        else {
            liveEnd = fStart;
        }
        host.scrollToShow(host.getCaretRect(liveEnd));
    }

    private void select(TextRange range) {
        // variable not used int textLength = host.getText().length();

        TextOffset start = new TextOffset(range.start);

        stopCaretBlinking();
        setSelRangeAndDraw(start, new TextOffset(range.limit), start);
        restartCaretBlinking(true);
    }

    public void setHighlightColor(Color newColor) {
        fHighlightColor = newColor;
        if (enabled()) {
            drawSelection(true);
        }
    }

    static boolean rangeIsZeroLength(TextOffset start, TextOffset limit, TextOffset anchor) {
        return start.offset == limit.offset && anchor.offset == limit.offset;
    }

    // sigh... look out for aliasing
    public void setSelectionRange(TextOffset newStart, TextOffset newLimit, TextOffset newAnchor) {

        boolean zeroLengthChange = rangeIsZeroLength(newStart, newLimit, newAnchor) !=
                rangeIsZeroLength(fStart, fLimit, fAnchor);
        TextOffset tempNewAnchor;
        if (newAnchor == fStart || newAnchor == fLimit) {
            tempNewAnchor = new TextOffset(newAnchor); // clone in case of aliasing
        }
        else {
            tempNewAnchor = newAnchor;
        }
        // DEBUG {jbr}
        if (newStart.greaterThan(newLimit)) {
            throw new IllegalArgumentException("Selection limit is before selection start.");
        }
        if (newLimit != fStart) {
            fStart.assign(newStart);
            fLimit.assign(newLimit);
        }
        else {
            fLimit.assign(newLimit);
            fStart.assign(newStart);
        }
        fAnchor.assign(tempNewAnchor);
        if (fStart.offset == fLimit.offset) {
            fStart.bias = fAnchor.bias;
            fLimit.bias = fAnchor.bias;
        }
        if (!fMouseDown) {
            if (zeroLengthChange) {
                fListener.textStateChanged(TextEvent.SELECTION_EMPTY_CHANGED);
            }
            fListener.textStateChanged(TextEvent.SELECTION_RANGE_CHANGED);
            if (fHandlingKeyOrCommand) {
                fListener.textStateChanged(TextEvent.SELECTION_STYLES_CHANGED);
            }
        }
    }

    private void sortOffsets(TextOffset offsets[]) {
        int i, j;
        for (i = 0; i < offsets.length - 1; i++) {
            for (j = i + 1; j < offsets.length; j++) {
                if (offsets[j].lessThan(offsets[i])) {
                    TextOffset temp = offsets[j];
                    offsets[j] = offsets[i];
                    offsets[i] = temp;
                }
            }
        }
        // DEBUG {jbr}
        for (i = 0; i < offsets.length - 1; i++) {
            if (offsets[i].greaterThan(offsets[i + 1])) throw new Error("sortOffsets failed!");
        }
    }

    FRectangle getSelectionChangeRect(TextOffset rangeStart, TextOffset rangeLimit,
            TextOffset oldStart, TextOffset oldLimit, TextOffset newStart, TextOffset newLimit,
            boolean drawIfInsPoint) {

        if (!rangeStart.equals(rangeLimit)) {
            return new FRectangle(host.getBoundingRect(rangeStart, rangeLimit));
        }

        // here rangeStart and rangeLimit are equal
        if (rangeStart.equals(oldLimit)) {
            // range start is OLD insertion point.  Redraw if caret is currently visible.
            if (fCaretIsVisible) {
                return new FRectangle(host.getBoundingRect(rangeStart, rangeStart));
            }
        }
        else if (rangeStart.equals(newLimit)) {
            // range start is NEW insertion point.
            if (drawIfInsPoint) {
                return new FRectangle(host.getBoundingRect(rangeStart, rangeStart));
            }
        }
        return null;
    }

    private static boolean rectanglesOverlapVertically(FRectangle r1, FRectangle r2) {
        return r1 != null && r2 != null && r1.y <= r2.y + r2.height && r2.y <= r1.y + r1.height;
    }

    // Update to show new selection, redrawing as little as possible
    private void updateSelectionDisplay(TextOffset oldStart, TextOffset oldLimit,
            TextOffset newStart, TextOffset newLimit, boolean drawIfInsPoint) {

        TextOffset off[] = new TextOffset[4];

        off[0] = oldStart;
        off[1] = oldLimit;
        off[2] = newStart;
        off[3] = newLimit;

        sortOffsets(off);

        FRectangle r1 = getSelectionChangeRect(off[0], off[1], oldStart, oldLimit, newStart,
                newLimit, drawIfInsPoint);
        FRectangle r2 = getSelectionChangeRect(off[2], off[3], oldStart, oldLimit, newStart,
                newLimit, drawIfInsPoint);

        boolean drawSelection = drawIfInsPoint || !newStart.equals(newLimit);

        if (rectanglesOverlapVertically(r1, r2)) {
            Graphics2D g = host.lock();
            host.drawText(g, r1.union(r2), drawSelection, newStart, newLimit, fHighlightColor);
            host.unlock(g);
        }
        else {
            if (r1 != null) {
                Graphics2D g = host.lock();
                host.drawText(g, r1, drawSelection, newStart, newLimit, fHighlightColor);
                host.unlock(g);
            }
            if (r2 != null) {
                Graphics2D g = host.lock();
                host.drawText(g, r2, drawSelection, newStart, newLimit, fHighlightColor);
                host.unlock(g);
            }
        }
    }

    public void setSelRangeAndDraw(TextOffset newStart, TextOffset newLimit, TextOffset newAnchor) {

        // if the old and new selection ranges are the same, don't do anything
        if (fStart.equals(newStart) && fLimit.equals(newLimit) && fAnchor.equals(newAnchor)) return;

        if (enabled()) stopCaretBlinking();

        // update the selection on screen if we're enabled and visible

        TextOffset oldStart = new TextOffset(fStart), oldLimit = new TextOffset(fLimit);
        setSelectionRange(newStart, newLimit, newAnchor);

        if (enabled()) {
            // To supress drawing a caret during a mouse drag, pass !fMouseDown instead of true:
            updateSelectionDisplay(oldStart, oldLimit, fStart, fLimit, true);
        }
        if (!fMouseDown && enabled()) restartCaretBlinking(true);
    }

    public void stopCaretBlinking() {
        synchronized (this) {
            fCaretShouldBlink = false;
        }
    }

    /**
     * Resume blinking the caret, if the selection is an insertion point.
     *
     * @param caretIsVisible true if the caret is displayed when this is called.
     *                       This method relies on the client to display (or not display) the caret.
     */
    public void restartCaretBlinking(boolean caretIsVisible) {

        synchronized (this) {
            fCaretShouldBlink = fStart.equals(fLimit);
            fCaretCount = 0;
            fCaretIsVisible = caretIsVisible;

            if (fCaretShouldBlink) {
                try {
                    notify();
                }
                catch (IllegalMonitorStateException e) {
                    System.out.println("Caught IllegalMonitorStateException: " + e);
                }
            }
        }
    }

    public void removeFromOwner() {
        stopCaretBlinking();
        super.removeFromOwner();
    }
}
