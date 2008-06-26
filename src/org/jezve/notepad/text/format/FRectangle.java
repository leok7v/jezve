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

package org.jezve.notepad.text.format;

import java.awt.*;
import java.awt.geom.*;
import java.io.Serializable;

public class FRectangle extends Rectangle2D implements Shape, Serializable {

    public float x;
    public float y;
    public float width;
    public float height;

    private static final long serialVersionUID = -4345857070255674766L;

    public FRectangle() {
        this(0, 0, 0, 0);
    }

    public FRectangle(FRectangle r) {
        this(r.x, r.y, r.width, r.height);
    }

    public FRectangle(Rectangle2D r) {
        this((float)r.getX(), (float)r.getY(), (float)r.getWidth(), (float)r.getHeight());
    }

    public FRectangle(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public FRectangle(float width, float height) {
        this(0, 0, width, height);
    }

    public FRectangle(FPoint p, FDimension d) {
        this(p.x, p.y, d.width, d.height);
    }

    public FRectangle(FPoint p) {
        this(p.x, p.y, 0, 0);
    }

    public FRectangle(FDimension d) {
        this(0, 0, d.width, d.height);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, (int)width, (int)height);
    }

    public Rectangle2D getBounds2D() {
        return new Rectangle2D.Float(x, y, width, height);
    }

    public void setBounds(FRectangle r) {
        setBounds(r.x, r.y, r.width, r.height);
    }

    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setRect(double x, double y, double width, double height) {
        float x0 = (float)x;
        float y0 = (float)y;
        float x1 = (float)(x + width);
        float y1 = (float)(y + height);
        setBounds(x0, y0, x1 - x0, y1 - y0);
    }

    public FPoint getLocation() {
        return new FPoint(x, y);
    }

    public void setLocation(FPoint p) {
        setLocation(p.x, p.y);
    }

    public void setLocation(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void translate(float x, float y) {
        this.x += x;
        this.y += y;
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

    public boolean contains(FPoint p) {
        return contains(p.x, p.y);
    }

    public boolean contains(float X, float Y) {
        float w = this.width;
        float h = this.height;
        if (w < 0 || h < 0) {
            // At least one of the dimensions is negative...
            return false;
        }
        // Note: if either dimension is zero, tests below must return false...
        float x = this.x;
        float y = this.y;
        if (X < x || Y < y) {
            return false;
        }
        w += x;
        h += y;
        //    overflow || intersect
        return ((w < x || w > X) && (h < y || h > Y));
    }

    public boolean contains(FRectangle r) {
        return contains(r.x, r.y, r.width, r.height);
    }

    public boolean contains(float X, float Y, float W, float H) {
        float w = this.width;
        float h = this.height;
        if (w < 0 || h < 0 || W < 0 || H < 0) {
            // At least one of the dimensions is negative...
            return false;
        }
        // Note: if any dimension is zero, tests below must return false...
        float x = this.x;
        float y = this.y;
        if (X < x || Y < y) {
            return false;
        }
        w += x;
        W += X;
        if (W <= X) {
            // X+W overflowed or W was zero, return false if...
            // either original w or W was zero or
            // x+w did not overflow or
            // the overflowed x+w is smaller than the overflowed X+W
            if (w >= x || W > w) return false;
        }
        else {
            // X+W did not overflow and W was not zero, return false if...
            // original w was zero or
            // x+w did not overflow and x+w is smaller than X+W
            if (w >= x && W > w) return false;
        }
        h += y;
        H += Y;
        if (H <= Y) {
            if (h >= y || H > h) return false;
        }
        else {
            if (h >= y && H > h) return false;
        }
        return true;
    }

    public boolean intersects(FRectangle r) {
        float tw = this.width;
        float th = this.height;
        float rw = r.width;
        float rh = r.height;
        if (rw <= 0 || rh <= 0 || tw <= 0 || th <= 0) {
            return false;
        }
        float tx = this.x;
        float ty = this.y;
        float rx = r.x;
        float ry = r.y;
        rw += rx;
        rh += ry;
        tw += tx;
        th += ty;
        //      overflow || intersect
        return ((rw < rx || rw > tx) && (rh < ry || rh > ty) && (tw < tx || tw > rx) &&
                (th < ty || th > ry));
    }

    public FRectangle intersection(FRectangle r) {
        float tx1 = this.x;
        float ty1 = this.y;
        float rx1 = r.x;
        float ry1 = r.y;
        float tx2 = tx1;
        tx2 += this.width;
        float ty2 = ty1;
        ty2 += this.height;
        float rx2 = rx1;
        rx2 += r.width;
        float ry2 = ry1;
        ry2 += r.height;
        if (tx1 < rx1) tx1 = rx1;
        if (ty1 < ry1) ty1 = ry1;
        if (tx2 > rx2) tx2 = rx2;
        if (ty2 > ry2) ty2 = ry2;
        tx2 -= tx1;
        ty2 -= ty1;
        return new FRectangle(tx1, ty1, tx2, ty2);
    }

    public FRectangle union(FRectangle r) {
        float x1 = Math.min(x, r.x);
        float x2 = Math.max(x + width, r.x + r.width);
        float y1 = Math.min(y, r.y);
        float y2 = Math.max(y + height, r.y + r.height);
        return new FRectangle(x1, y1, x2 - x1, y2 - y1);
    }

    public void add(float newx, float newy) {
        float x1 = Math.min(x, newx);
        float x2 = Math.max(x + width, newx);
        float y1 = Math.min(y, newy);
        float y2 = Math.max(y + height, newy);
        x = x1;
        y = y1;
        width = x2 - x1;
        height = y2 - y1;
    }

    public void add(FPoint pt) {
        add(pt.x, pt.y);
    }

    public void add(FRectangle r) {
        float x1 = Math.min(x, r.x);
        float x2 = Math.max(x + width, r.x + r.width);
        float y1 = Math.min(y, r.y);
        float y2 = Math.max(y + height, r.y + r.height);
        x = x1;
        y = y1;
        width = x2 - x1;
        height = y2 - y1;
    }

    public void grow(float h, float v) {
        x -= h;
        y -= v;
        width += h * 2;
        height += v * 2;
    }

    public boolean isEmpty() {
        return (width <= 0) || (height <= 0);
    }

    public int outcode(double x, double y) {
        /*
         * Note on casts to double below.  If the arithmetic of
         * x+w or y+h is done in int, then we may get integer
         * overflow. By converting to double before the addition
         * we force the addition to be carried out in double to
         * avoid overflow in the comparison.
         *
         * See bug 4320890 for problems that this can cause.
         */
        int out = 0;
        if (this.width <= 0) {
            out |= OUT_LEFT | OUT_RIGHT;
        }
        else if (x < this.x) {
            out |= OUT_LEFT;
        }
        else if (x > this.x + (double)this.width) {
            out |= OUT_RIGHT;
        }
        if (this.height <= 0) {
            out |= OUT_TOP | OUT_BOTTOM;
        }
        else if (y < this.y) {
            out |= OUT_TOP;
        }
        else if (y > this.y + (double)this.height) {
            out |= OUT_BOTTOM;
        }
        return out;
    }

    public Rectangle2D createIntersection(Rectangle2D r) {
        if (r instanceof FRectangle) {
            return intersection((FRectangle)r);
        }
        Rectangle2D dest = new Rectangle2D.Double();
        Rectangle2D.intersect(this, r, dest);
        return dest;
    }

    public Rectangle2D createUnion(Rectangle2D r) {
        if (r instanceof FRectangle) {
            return union((FRectangle)r);
        }
        Rectangle2D dest = new Rectangle2D.Double();
        Rectangle2D.union(this, r, dest);
        return dest;
    }

    public boolean equals(Object obj) {
        if (obj instanceof FRectangle) {
            FRectangle r = (FRectangle)obj;
            return ((x == r.x) && (y == r.y) && (width == r.width) && (height == r.height));
        }
        return super.equals(obj);
    }

    public String toString() {
        return getClass().getName() + "[x=" + x + ",y=" + y + ",width=" + width + ",height=" +
                height + "]";
    }
}
