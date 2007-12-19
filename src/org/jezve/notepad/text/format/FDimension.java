package org.jezve.notepad.text.format;

import java.awt.geom.*;
import java.io.Serializable;

public class FDimension extends Dimension2D implements Serializable {

    public float width;
    public float height;

    private static final long serialVersionUID = 4723952579491349524L;

    public FDimension() {
        this(0, 0);
    }

    public FDimension(FDimension d) {
        this(d.width, d.height);
    }

    public FDimension(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public void setSize(double width, double height) {
        this.width = (float)width;
        this.height = (float)height;
    }

    public FDimension getSize() {
        return new FDimension(width, height);
    }

    public void setSize(FDimension d) {
        setSize(d.width, d.height);
    }

    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public boolean equals(Object obj) {
        if (obj instanceof FDimension) {
            FDimension d = (FDimension)obj;
            return (width == d.width) && (height == d.height);
        }
        return false;
    }

    public int hashCode() {
        float sum = width + height;
        return (int)(sum * (sum + 1) / 2 + width);
    }

    public String toString() {
        return getClass().getName() + "[width=" + width + ",height=" + height + "]";
    }
}
