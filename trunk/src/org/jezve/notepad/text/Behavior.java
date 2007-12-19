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
