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

/**
 * This class exists to work around a clipping bug in JDK 1.2.
 */
final class ClipWorkaround {

    private static final String excuse = "Sorry, this method is a very limited workaround for a JDK 1.2 bug.";

    // Draw the given Shape into the Graphics, translated by (dx, dy) and clipped to clipRect.
    static void translateAndDrawShapeWithClip(Graphics2D g, float dx, float dy,
            Rectangle2D clipRect, Shape shape) {
        // really bogus implementation right now:  basically only
        // draws carets from a TextLayout.
        // Oh yeah, it doesn't really clip correctly either...

        PathIterator pathIter = shape.getPathIterator(null);
        float[] points = new float[6];

        int type = pathIter.currentSegment(points);
        if (type != PathIterator.SEG_MOVETO) {
            throw new Error(excuse);
        }
        float x1 = points[0] + dx;
        float y1 = points[1] + dy;

        if (pathIter.isDone()) {
            throw new Error(excuse);
        }

        pathIter.next();
        type = pathIter.currentSegment(points);
        if (type != PathIterator.SEG_LINETO) {
            throw new Error(excuse);
        }
        float x2 = points[0] + dx;
        float y2 = points[1] + dy;

        float minY = (float)clipRect.getY();
        float maxY = (float)clipRect.getMaxY();

        // Now clip within vertical limits in clipRect
        if (y1 == y2) {
            if (y1 < minY || y1 >= maxY) {
                return;
            }
        }
        else {
            if (y1 > y2) {
                float t = x1;
                x1 = x2;
                x2 = t;
                t = y1;
                y1 = y2;
                y2 = t;
            }

            float invSlope = (x2 - x1) / (y2 - y1);
            if (y1 < minY) {
                x1 -= (minY - y1) * invSlope;
                y1 = minY;
            }
            if (y2 >= maxY) {
                x1 += (y2 - maxY) * invSlope;
                y2 = maxY;
            }
        }
        g.draw(new Line2D.Float(x1, y1, x2, y2));
    }
}
