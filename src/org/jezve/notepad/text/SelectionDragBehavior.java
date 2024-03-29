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

import org.jezve.notepad.text.format.TextOffset;

import java.awt.event.*;

final class SelectionDragInteractor extends Behavior implements Runnable {

    private JEditorComponent editor;
    private SelectionBehavior fSelection;

    private TextOffset fAnchorStart; // aliases text offsets - client beware
    private TextOffset fAnchorEnd;
    private TextOffset fCurrent;

    private final boolean fWasZeroLength;

    private float fCurrentX;
    private float fCurrentY;
    private boolean fMouseOutside;

    private Thread fAutoscrollThread = null;
    private boolean fThreadRun = true;

    private static final int kScrollSleep = 300;

    public SelectionDragInteractor(SelectionBehavior selection, JEditorComponent textComponent,
            TextOffset anchorStart, TextOffset anchorEnd, TextOffset current, float initialX,
            float initialY, boolean wasZeroLength) {

        editor = textComponent;
        fSelection = selection;
        fAnchorStart = anchorStart;
        fAnchorEnd = anchorEnd;
        fCurrent = current;

        fCurrentX = initialX;
        fCurrentY = initialY;
        fMouseOutside = false;
        fWasZeroLength = wasZeroLength;

        setSelection();
    }

    public boolean textControlEventOccurred(TextEvent event, Object what) {
        return true;
    }

    public boolean focusGained(FocusEvent event) {
        return true;
    }

    public boolean focusLost(FocusEvent event) {
        return true;
    }

    public boolean keyPressed(KeyEvent event) {
        return true;
    }

    public boolean keyTyped(KeyEvent event) {
        return true;
    }

    public boolean keyReleased(KeyEvent event) {
        return true;
    }

    public synchronized boolean mouseDragged(FMouseEvent e) {
        float x = e.getFX(), y = e.getFY();
        if (fCurrentX != x || fCurrentY != y) {
            fCurrentX = x;
            fCurrentY = y;
            processMouseLocation();
        }
        return true;
    }

    public synchronized boolean mouseEnter(FMouseEvent e) {
        fMouseOutside = false;
        return true;
    }

    public synchronized boolean mouseExited(FMouseEvent e) {
        if (fAutoscrollThread == null) {
            fAutoscrollThread = new Thread(this);
            fAutoscrollThread.start();
        }
        fMouseOutside = true;
        notify();
        return true;
    }

    public synchronized boolean mouseReleased(FMouseEvent e) {

        fMouseOutside = false;
        fThreadRun = false;
        if (fAutoscrollThread != null) {
            fAutoscrollThread.interrupt();
        }

        removeFromOwner();
        boolean isZeroLength =
                SelectionBehavior.rangeIsZeroLength(fAnchorStart, fAnchorEnd, fCurrent);
        fSelection.mouseReleased(isZeroLength != fWasZeroLength);
        return true;
    }

    private void processMouseLocation() {
        editor.scrollToShow(fCurrentX, fCurrentY);
        editor.pointToTextOffset(fCurrent, fCurrentX, fCurrentY, null, true);
        setSelection();
    }

    private void setSelection() {
        if (fCurrent.greaterThan(fAnchorEnd)) {
            fSelection.advanceToNextBoundary(fCurrent);
            fSelection.setSelRangeAndDraw(fAnchorStart, fCurrent, fAnchorStart);
        }
        else if (fCurrent.lessThan(fAnchorStart)) {
            fSelection.advanceToPreviousBoundary(fCurrent);
            fSelection.setSelRangeAndDraw(fCurrent, fAnchorEnd, fAnchorStart);
        }
        else {
            fCurrent.assign(fAnchorEnd);
            fSelection.setSelRangeAndDraw(fAnchorStart, fAnchorEnd, fAnchorStart);
        }
    }

    public void run() {
        while (fThreadRun) {
            try {
                Thread.sleep(kScrollSleep);
            }
            catch (InterruptedException e) {
                return; // just quit scrolling
            }
            synchronized (this) {
                while (!fMouseOutside) {
                    try {
                        wait();
                    }
                    catch (InterruptedException e) {
                        return; // just quit scrolling
                    }
                }
                processMouseLocation();
            }
        }
    }
}
