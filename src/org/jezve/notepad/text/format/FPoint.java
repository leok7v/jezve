package org.jezve.notepad.text.format;

import java.awt.geom.*;
import java.io.Serializable;

public class FPoint extends Point2D implements Serializable {

    public float x;
    public float y;

    private static final long serialVersionUID = -5276940640259749857L;

    public FPoint() {
        this(0, 0);
    }

    public FPoint(FPoint p) {
        this(p.x, p.y);
    }

    public FPoint(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public FPoint getLocation() {
        return new FPoint(x, y);
    }

    public void setLocation(FPoint p) {
        setLocation(p.x, p.y);
    }

    public void setLocation(float x, float y) {
        move(x, y);
    }

    public void setLocation(double x, double y) {
        this.x = (float)x;
        this.y = (float)y;
    }

    public void move(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void translate(float dx, float dy) {
        this.x += dx;
        this.y += dy;
    }

    public boolean equals(Object obj) {
        if (obj instanceof FPoint) {
            FPoint pt = (FPoint)obj;
            return (x == pt.x) && (y == pt.y);
        }
        return super.equals(obj);
    }

    public String toString() {
        return getClass().getName() + "[x=" + x + ",y=" + y + "]";
    }
}
