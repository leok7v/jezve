package org.jezve.svg;

import java.awt.*;
import java.awt.geom.*;
import java.util.LinkedList;

public class Path extends SVG.ShapeElement {

    private GeneralPath path;

    protected void build() {
        super.build();
        String fr = getStyleString("fill-rule", "nonzero");
        int fillRule = "evenodd".equalsIgnoreCase(fr) ?
                GeneralPath.WIND_EVEN_ODD : GeneralPath.WIND_NON_ZERO;
        String d = getString("d", "");
        path = PathCommand.buildPath(d, fillRule);
    }

    void render(Graphics2D g) {
        beginLayer(g);
        renderShape(g, path);
        finishLayer(g);
    }

    Shape getShape() {
        return shapeToParent(path);
    }

    Rectangle2D getBoundingBox() {
        return boundsToParent(includeStrokeInBounds(path.getBounds2D()));
    }

    static abstract class PathCommand {

        private boolean isRelative = false;

        PathCommand() {
        }

        PathCommand(boolean isRelative) {
            this.isRelative = isRelative;
        }

        abstract void appendPath(GeneralPath path, BuildHistory hist);

        abstract int getNumKnotsAdded();

        boolean isRelative() {
            return isRelative;
        }

        private static class WorkaroundParser extends Parser.Double {

            WorkaroundParser(String extraWhitespace) {
                super(extraWhitespace);
            }

            float nextFloat(String s) {
                skipWhitespace(s);
                if (position < s.length() - 2 &&
                        s.charAt(position) == 'n' &&
                        s.charAt(position + 1) == 'a' &&
                        s.charAt(position + 2) == 'n') {
                    // special case "nan"
                    // inkscape:version="0.45+devel"
                    // nicubunu/nicubunu_RPG_map_symbols_Mine_2.svg
                    // M 74.422989,83.452273 C nan,nan nan,nan 79.019183,85.573593
                    // underflow?
                    nextChar(s);
                    nextChar(s);
                    nextChar(s);
                    skipWhitespace(s);
                    return 0;
                } else if (position < s.length() - 1 && s.charAt(position) == '-' &&
                        !Parser.isDigit(s.charAt(position + 1))) {
                    // special case "-" as "-0"
                    // inkscape: 0.45.1+0.46pre1+devel
                    // mystica/mystica_15_hearts.svg
                    // 547.328,-61.733 537.031,- C 526.038,-95.605
                    nextChar(s);
                    skipWhitespace(s);
                    return 0;
                }
                return super.nextFloat(s);
            }

        }

        static PathCommand[] parsePathList(String s) {
            LinkedList list = new LinkedList();
            char ch = 0;
            WorkaroundParser parser = new WorkaroundParser(",");
            while (parser.getPosition() < s.length()) {
                parser.skipWhitespace(s);
                if (parser.getPosition() >= s.length()) {
                    break;
                }
                char c = s.charAt(parser.getPosition());
                if (Character.isLetter(c)) {
                    ch = parser.nextChar(s);
                    assert ch == c;
                }
                PathCommand cmd;
                switch (ch) {
                    case'M':
                        cmd = new MoveTo(false, parser.nextFloat(s), parser.nextFloat(s));
                        break;
                    case'm':
                        cmd = new MoveTo(true, parser.nextFloat(s), parser.nextFloat(s));
                        break;
                    case'L':
                        cmd = new LineTo(false, parser.nextFloat(s), parser.nextFloat(s));
                        break;
                    case'l':
                        cmd = new LineTo(true, parser.nextFloat(s), parser.nextFloat(s));
                        break;
                    case'H':
                        cmd = new Horizontal(false, parser.nextFloat(s));
                        break;
                    case'h':
                        cmd = new Horizontal(true, parser.nextFloat(s));
                        break;
                    case'V':
                        cmd = new Vertical(false, parser.nextFloat(s));
                        break;
                    case'v':
                        cmd = new Vertical(true, parser.nextFloat(s));
                        break;
                    case'A':
                    case'a': {
                        // legacy commands: "A 5.868683 5.868652 0 1"
                        parser.skipWhitespace(s);
                        int ix = 0;
                        float[] a = new float[7];
                        while (parser.getPosition() < s.length()) {
                            char nx = s.charAt(parser.getPosition());
                            if (nx == '-' || nx == '+' || Parser.isDigit(nx)) {
                                a[ix++] = parser.nextFloat(s);
                            } else {
                                break;
                            }
                        }
                        if (ix == 7) {
                            cmd = new Arc(ch == 'a', a[0], a[1], a[2], a[3] == 1f, a[4] == 1f, a[5], a[6]);
                        } else {
                            assert ix == 4 : "ix=" + ix;
                            cmd = new Arc(ch == 'a', a[2], a[3], 0, false, false, a[0], a[1]);
                        }
                        break;
                    }
                    case'Q':
                        cmd = new Quadratic(false, parser.nextFloat(s), parser.nextFloat(s), parser.nextFloat(s),
                                parser.nextFloat(s));
                        break;
                    case'q':
                        cmd = new Quadratic(true, parser.nextFloat(s), parser.nextFloat(s), parser.nextFloat(s),
                                parser.nextFloat(s));
                        break;
                    case'T':
                        cmd = new QuadraticSmooth(false, parser.nextFloat(s), parser.nextFloat(s));
                        break;
                    case't':
                        cmd = new QuadraticSmooth(true, parser.nextFloat(s), parser.nextFloat(s));
                        break;
                    case'C':
                        cmd = new Cubic(false, parser.nextFloat(s), parser.nextFloat(s), parser.nextFloat(s), parser.nextFloat(s),
                                parser.nextFloat(s), parser.nextFloat(s));
                        break;
                    case'c':
                        cmd = new Cubic(true, parser.nextFloat(s), parser.nextFloat(s), parser.nextFloat(s), parser.nextFloat(s),
                                parser.nextFloat(s), parser.nextFloat(s));
                        break;
                    case'S':
                        cmd = new CubicSmooth(false, parser.nextFloat(s), parser.nextFloat(s), parser.nextFloat(s),
                                parser.nextFloat(s));
                        break;
                    case's':
                        cmd = new CubicSmooth(true, parser.nextFloat(s), parser.nextFloat(s), parser.nextFloat(s),
                                parser.nextFloat(s));
                        break;
                    case'Z':
                    case'z':
                        cmd = new ClosePath();
                        break;
                    default:
                        throw new Error("Invalid path element");
                }
                list.add(cmd);
            }
            return (PathCommand[])list.toArray(new PathCommand[list.size()]);
        }

        static GeneralPath buildPath(String text, int windingRule) {
            PathCommand[] commands = parsePathList(text);
            int numKnots = 2;
            for (int i = 0; i < commands.length; i++) {
                numKnots += commands[i].getNumKnotsAdded();
            }
            GeneralPath path = new GeneralPath(windingRule, numKnots);
            BuildHistory hist = new BuildHistory();
            for (int i = 0; i < commands.length; i++) {
                PathCommand cmd = commands[i];
                cmd.appendPath(path, hist);
            }
            return path;
        }

    }

    static class BuildHistory {

        private Point2D.Float[] history = {new Point2D.Float(), new Point2D.Float()};

        void setPoint(float x, float y) {
            history[0].setLocation(x, y);
        }

        void setPointAndKnot(float x1, float y1, float kx, float ky) {
            history[0].setLocation(x1, y1);
            history[1].setLocation(kx, ky);
        }

        Point2D.Float[] getHistory() {
            return history;
        }
    }

    private static class Arc extends PathCommand {

        private float rx;
        private float ry;
        private float xAxisRot;
        private boolean largeArc;
        private boolean sweep;
        private float x;
        private float y;

        Arc(boolean isRelative, float rx, float ry, float xAxisRot, boolean largeArc, boolean sweep, float x, float y) {
            super(isRelative);
            this.rx = rx;
            this.ry = ry;
            this.xAxisRot = xAxisRot;
            this.largeArc = largeArc;
            this.sweep = sweep;
            this.x = x;
            this.y = y;
        }

        void appendPath(GeneralPath path, BuildHistory hist) {
            float offx = isRelative() ? hist.getHistory()[0].x : 0f;
            float offy = isRelative() ? hist.getHistory()[0].y : 0f;
            arcTo(path, rx, ry, xAxisRot, largeArc, sweep, x + offx, y + offy, hist.getHistory()[0].x, hist.getHistory()[0].y);
            hist.setPoint(x + offx, y + offy);
        }

        int getNumKnotsAdded() {
            return 6;
        }

        void arcTo(GeneralPath path, float rx, float ry, float angle, boolean largeArcFlag, boolean sweepFlag,
                float x, float y, float x0, float y0) {

            // Ensure radii are valid
            if (rx == 0 || ry == 0) {
                path.lineTo(x, y);
                return;
            }
            if (x0 == x && y0 == y) {
                // If the endpoints (x, y) and (x0, y0) are identical, then this
                // is equivalent to omitting the elliptical arc segment entirely.
                return;
            }
            Arc2D arc = computeArc(x0, y0, rx, ry, angle, largeArcFlag, sweepFlag, x, y);
            if (arc == null) {
                return;
            }
            AffineTransform t =
                    AffineTransform.getRotateInstance(Math.toRadians(angle), arc.getCenterX(), arc.getCenterY());
            Shape s = t.createTransformedShape(arc);
            path.append(s, true);
        }

        static Arc2D computeArc(double x0, double y0, double rx, double ry, double angle, boolean largeArcFlag,
                boolean sweepFlag, double x, double y) {
            //  Elliptical arc implementation based on the SVG specification notes
            //  Compute the half distance between the current and the final point
            double dx2 = (x0 - x) / 2.0;
            double dy2 = (y0 - y) / 2.0;
            //  Convert angle from degrees to radians
            angle = Math.toRadians(angle % 360.0);
            double cosAngle = Math.cos(angle);
            double sinAngle = Math.sin(angle);

            //
            //  Step 1 : Compute (x1, y1)
            //
            double x1 = (cosAngle * dx2 + sinAngle * dy2);
            double y1 = (-sinAngle * dx2 + cosAngle * dy2);
            //  Ensure radii are large enough
            rx = Math.abs(rx);
            ry = Math.abs(ry);
            double Prx = rx * rx;
            double Pry = ry * ry;
            double Px1 = x1 * x1;
            double Py1 = y1 * y1;
            //  check that radii are large enough
            double radiiCheck = Px1 / Prx + Py1 / Pry;
            if (radiiCheck > 1) {
                rx = Math.sqrt(radiiCheck) * rx;
                ry = Math.sqrt(radiiCheck) * ry;
                Prx = rx * rx;
                Pry = ry * ry;
            }

            //  Step 2 : Compute (cx1, cy1)
            double sign = (largeArcFlag == sweepFlag) ? -1 : 1;
            double sq = ((Prx * Pry) - (Prx * Py1) - (Pry * Px1)) / ((Prx * Py1) + (Pry * Px1));
            sq = (sq < 0) ? 0 : sq;
            double coef = (sign * Math.sqrt(sq));
            double cx1 = coef * ((rx * y1) / ry);
            double cy1 = coef * -((ry * x1) / rx);

            //  Step 3 : Compute (cx, cy) from (cx1, cy1)
            double sx2 = (x0 + x) / 2.0;
            double sy2 = (y0 + y) / 2.0;
            double cx = sx2 + (cosAngle * cx1 - sinAngle * cy1);
            double cy = sy2 + (sinAngle * cx1 + cosAngle * cy1);

            //  Step 4 : Compute the angleStart (angle1) and the angleExtent (dangle)
            double ux = (x1 - cx1) / rx;
            double uy = (y1 - cy1) / ry;
            double vx = (-x1 - cx1) / rx;
            double vy = (-y1 - cy1) / ry;
            double p, n;
            //  Compute the angle start
            n = Math.sqrt((ux * ux) + (uy * uy));
            p = ux; //  (1 * ux) + (0 * uy)
            sign = (uy < 0) ? -1d : 1d;
            double angleStart = Math.toDegrees(sign * Math.acos(p / n));

            //  Compute the angle extent
            n = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
            p = ux * vx + uy * vy;
            sign = (ux * vy - uy * vx < 0) ? -1d : 1d;
            double angleExtent = Math.toDegrees(sign * Math.acos(p / n));
            if (!sweepFlag && angleExtent > 0) {
                angleExtent -= 360f;
            } else if (sweepFlag && angleExtent < 0) {
                angleExtent += 360f;
            }
            angleExtent %= 360f;
            angleStart %= 360f;

            //  We can now build the resulting Arc2D in double precision
            Arc2D.Double arc = new Arc2D.Double();
            arc.x = cx - rx;
            arc.y = cy - ry;
            arc.width = rx * 2.0;
            arc.height = ry * 2.0;
            arc.start = -angleStart;
            arc.extent = -angleExtent;
            return arc;
        }

    }

    private static class Cubic extends PathCommand {

        private float k1x;
        private float k1y;
        private float k2x;
        private float k2y;
        private float x;
        private float y;

        Cubic(boolean isRelative, float k1x, float k1y, float k2x, float k2y, float x, float y) {
            super(isRelative);
            this.k1x = k1x;
            this.k1y = k1y;
            this.k2x = k2x;
            this.k2y = k2y;
            this.x = x;
            this.y = y;
        }

        void appendPath(GeneralPath path, BuildHistory hist) {
            float offx = isRelative() ? hist.getHistory()[0].x : 0f;
            float offy = isRelative() ? hist.getHistory()[0].y : 0f;
            path.curveTo(k1x + offx, k1y + offy, k2x + offx, k2y + offy, x + offx, y + offy);
            hist.setPointAndKnot(x + offx, y + offy, k2x + offx, k2y + offy);
        }

        int getNumKnotsAdded() {
            return 6;
        }
    }


    private static class CubicSmooth extends PathCommand {

        private float x;
        private float y;
        private float k2x;
        private float k2y;

        CubicSmooth(boolean isRelative, float k2x, float k2y, float x, float y) {
            super(isRelative);
            this.k2x = k2x;
            this.k2y = k2y;
            this.x = x;
            this.y = y;
        }

        void appendPath(GeneralPath path, BuildHistory hist) {
            float offx = isRelative() ? hist.getHistory()[0].x : 0f;
            float offy = isRelative() ? hist.getHistory()[0].y : 0f;
            float oldKx = hist.getHistory().length >= 2 ? hist.getHistory()[1].x : hist.getHistory()[0].x;
            float oldKy = hist.getHistory().length >= 2 ? hist.getHistory()[1].y : hist.getHistory()[0].y;
            float oldX = hist.getHistory()[0].x;
            float oldY = hist.getHistory()[0].y;
            // Calc knot as reflection of old knot
            float k1x = oldX * 2f - oldKx;
            float k1y = oldY * 2f - oldKy;
            path.curveTo(k1x, k1y, k2x + offx, k2y + offy, x + offx, y + offy);
            hist.setPointAndKnot(x + offx, y + offy, k2x + offx, k2y + offy);
        }

        int getNumKnotsAdded() {
            return 6;
        }
    }

    private static class Horizontal extends PathCommand {

        private float x;

        Horizontal(boolean isRelative, float x) {
            super(isRelative);
            this.x = x;
        }

        void appendPath(GeneralPath path, BuildHistory hist) {
            float offx = isRelative() ? hist.getHistory()[0].x : 0f;
            float offy = hist.getHistory()[0].y;
            path.lineTo(x + offx, offy);
            hist.setPoint(x + offx, offy);
        }

        int getNumKnotsAdded() {
            return 2;
        }
    }

    private static class LineTo extends PathCommand {

        private float x;
        private float y;

        LineTo(boolean isRelative, float x, float y) {
            super(isRelative);
            this.x = x;
            this.y = y;
        }

        void appendPath(GeneralPath path, BuildHistory hist) {
            float offx = isRelative() ? hist.getHistory()[0].x : 0f;
            float offy = isRelative() ? hist.getHistory()[0].y : 0f;
            path.lineTo(x + offx, y + offy);
            hist.setPoint(x + offx, y + offy);
        }

        int getNumKnotsAdded() {
            return 2;
        }
    }

    private static class MoveTo extends PathCommand {

        private float x;
        private float y;

        MoveTo(boolean isRelative, float x, float y) {
            super(isRelative);
            this.x = x;
            this.y = y;
        }

        void appendPath(GeneralPath path, BuildHistory hist) {
            float offx = isRelative() ? hist.getHistory()[0].x : 0f;
            float offy = isRelative() ? hist.getHistory()[0].y : 0f;
            path.moveTo(x + offx, y + offy);
            hist.setPoint(x + offx, y + offy);
        }

        int getNumKnotsAdded() {
            return 2;
        }
    }

    private static class Quadratic extends PathCommand {

        private float kx;
        private float ky;
        private float x;
        private float y;

        Quadratic(boolean isRelative, float kx, float ky, float x, float y) {
            super(isRelative);
            this.kx = kx;
            this.ky = ky;
            this.x = x;
            this.y = y;
        }

        void appendPath(GeneralPath path, BuildHistory hist) {
            float offx = isRelative() ? hist.getHistory()[0].x : 0f;
            float offy = isRelative() ? hist.getHistory()[0].y : 0f;
            path.quadTo(kx + offx, ky + offy, x + offx, y + offy);
            hist.setPointAndKnot(x + offx, y + offy, kx + offx, ky + offy);
        }

        int getNumKnotsAdded() {
            return 4;
        }
    }

    private static class QuadraticSmooth extends PathCommand {

        private float x;
        private float y;

        QuadraticSmooth(boolean isRelative, float x, float y) {
            super(isRelative);
            this.x = x;
            this.y = y;
        }

        void appendPath(GeneralPath path, BuildHistory hist) {
            float offx = isRelative() ? hist.getHistory()[0].x : 0f;
            float offy = isRelative() ? hist.getHistory()[0].y : 0f;
            float oldKx = hist.getHistory().length >= 2 ? hist.getHistory()[1].x : hist.getHistory()[0].x;
            float oldKy = hist.getHistory().length >= 2 ? hist.getHistory()[1].y : hist.getHistory()[0].y;
            float oldX = hist.getHistory()[0].x;
            float oldY = hist.getHistory()[0].y;
            // Calc knot as reflection of old knot
            float kx = oldX * 2f - oldKx;
            float ky = oldY * 2f - oldKy;
            path.quadTo(kx, ky, x + offx, y + offy);
            hist.setPointAndKnot(x + offx, y + offy, kx, ky);
        }

        int getNumKnotsAdded() {
            return 4;
        }
    }

    private static class ClosePath extends PathCommand {

        void appendPath(GeneralPath path, BuildHistory hist) {
            path.closePath();
        }

        int getNumKnotsAdded() {
            return 0;
        }
    }

    private static class Vertical extends PathCommand {

        private float y;

        Vertical(boolean isRelative, float y) {
            super(isRelative);
            this.y = y;
        }

        void appendPath(GeneralPath path, BuildHistory hist) {
            float offx = hist.getHistory()[0].x;
            float offy = isRelative() ? hist.getHistory()[0].y : 0f;
            path.lineTo(offx, y + offy);
            hist.setPoint(offx, y + offy);
        }

        int getNumKnotsAdded() {
            return 2;
        }
    }

}