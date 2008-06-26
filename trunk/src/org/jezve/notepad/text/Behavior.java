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

import org.jezve.notepad.text.format.FRectangle;

import java.awt.*;
import java.awt.event.*;

abstract class Behavior {

    private Behavior next = null;
    private JEditorComponent host = null;

    public Behavior() {
    }

    public void addToOwner(JEditorComponent owner) {
        removeFromOwner();
        host = owner;
        setNextBehavior(owner.getBehavior());
        owner.setBehavior(this);
    }

    public boolean focusGained(FocusEvent e) {
        return next != null && next.focusGained(e);
    }

    public boolean focusLost(FocusEvent e) {
        return next != null && next.focusLost(e);
    }

    public boolean keyPressed(KeyEvent e) {
        return next != null && next.keyPressed(e);
    }

    public boolean keyTyped(KeyEvent e) {
        return next != null && next.keyTyped(e);
    }

    public boolean keyReleased(KeyEvent e) {
        return next != null && next.keyReleased(e);
    }

    public boolean mouseDragged(FMouseEvent e) {
        return next != null && next.mouseDragged(e);
    }

    public boolean mouseEntered(FMouseEvent e) {
        return next != null && next.mouseEntered(e);
    }

    public boolean mouseExited(FMouseEvent e) {
        return next != null && next.mouseExited(e);
    }

    public boolean mouseMoved(FMouseEvent e) {
        return next != null && next.mouseMoved(e);
    }

    public boolean mousePressed(FMouseEvent e) {
        return next != null && next.mousePressed(e);
    }

    public boolean mouseReleased(FMouseEvent e) {
        return next != null && next.mouseReleased(e);
    }

    public final Behavior nextBehavior() {
        return next;
    }

    public boolean paint(Graphics2D g, FRectangle drawRect) {
        return next != null && next.paint(g, drawRect);
    }

    public void removeFromOwner() {
        if (host != null) {
            if (host.getBehavior() == this) {
                host.setBehavior(nextBehavior());
            }
            else {
                Behavior current = host.getBehavior();

                while (current != null && current.nextBehavior() != this) {
                    current = current.nextBehavior();
                }
                if (current != null) current.setNextBehavior(nextBehavior());
            }
            setNextBehavior(null);
            host = null;
        }
    }

    public final void setNextBehavior(Behavior next) {
        this.next = next;
    }

    public boolean textControlEventOccurred(TextEvent event, Object what) {
        return next != null && next.textControlEventOccurred(event, what);
    }
}
