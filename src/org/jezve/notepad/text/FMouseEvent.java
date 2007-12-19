package org.jezve.notepad.text;

import org.jezve.notepad.text.format.FPoint;

import java.awt.*;
import java.awt.event.*;

public class FMouseEvent extends MouseEvent {

    FPoint point;

    FMouseEvent(MouseEvent o, FPoint pt) {
        super(o.getComponent(), o.getID(), o.getWhen(), o.getModifiers(), o.getX(), o.getY(),
                o.getClickCount(), o.isPopupTrigger(), o.getButton());
        point = pt;
    }

    public float getFX() {
        return point.x;
    }

    public float getFY() {
        return point.y;
    }

    public FPoint getFpoint() {
        return new FPoint(point);
    }

    public int getX() { throw new Error("prohibited"); }

    public int getY() { throw new Error("prohibited"); }

    public Point getPoint() { throw new Error("prohibited"); }

    public void translatePoint(int x, int y) { throw new Error("prohibited"); }

}
