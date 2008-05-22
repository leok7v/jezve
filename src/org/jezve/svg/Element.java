package org.jezve.svg;


import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.awt.geom.AffineTransform;
import java.awt.*;
import java.util.*;
import java.util.List;


abstract class Element {

    private static float[] buf = new float[512];
    private final static ArrayList EMPTY = new ArrayList();
    private Element parent;
    private ArrayList children;
    private String id;
    private HashMap styles;
    private HashMap attrs;
    private SVG svg;
    private LinkedList contexts;

    Element getParent() {
        return parent;
    }

    SVG getRoot() {
        return svg;
    }

    List getChildren() {
        return Collections.unmodifiableList(children == null ? EMPTY : children);
    }

    protected void loaderStartElement(SVG s, Attributes a, Element p) throws SAXException {
        parent = p;
        svg = s;
        id = a.getValue("id");
        if (id != null) {
            assert id.equals(id.trim()) && id.length() > 0 : id;
            svg.put(id, this);
        }
        String style = a.getValue("style");
        if (style != null) {
            styles = new HashMap();
            parseStyle(style, styles);
        }
        int n = a.getLength();
        if (n > 0) {
            attrs = new HashMap();
            for (int i = 0; i < n; i++) {
                String name = a.getQName(i);
                String value = a.getValue(i);
                attrs.put(name, value);
            }
        }
    }

    protected void loaderAddChild(Element child) {
        if (children == null) {
            children = new ArrayList();
        }
        children.add(child);
        child.parent = this;
    }

    protected void loaderAddText(String text) {
    }

    protected void loaderEndElement() {
        id = getString("id");
        if (id != null) {
            svg.put(id, this);
        }
    }

    protected void build() {
        String classnames = getString("class");
        if (classnames != null) {
            for (StringTokenizer st = new StringTokenizer(classnames); st.hasMoreTokens(); ) {
                String t = st.nextToken().trim();
                Map m = getRoot().getStyle(t);
                if (m != null) {
                    if (styles == null) {
                        styles = new HashMap();
                    }
                    styles.putAll(m);
                }
            }
        }
    }

    protected void resolve() {
    }

    protected void clear() {
        if (attrs != null) {
            attrs.clear();
            attrs = null;
        }
        if (styles != null) {
            styles.clear();
            styles = null;
        }
    }

    /**
     * Allow nodes to temporarily change their parents. The Use tag
     * can alter the attributes context that a particular node uses.
     */
    protected void pushParentContext(Element context) {
        if (contexts == null) {
            contexts = new LinkedList();
        }
        contexts.addLast(context);
    }

    protected Element popParentContext() {
        return (Element)contexts.removeLast();
    }

    private Element getParentContext() {
        return contexts == null || contexts.isEmpty() ? null : (Element)contexts.getLast();
    }

    String getId() {
        return id;
    }

    private String getStyle(String name) {
        String a = styles == null ? null : (String)styles.get(name);
        if (a != null) {
            return a;
        }
        a = get(name);
        if (a != null) {
            return a;
        }
        Element parentContext = getParentContext();
        if (parentContext != null) {
            return parentContext.getStyle(name);
        }
        return parent != null ? parent.getStyle(name) : null;
    }

    private String get(String name) {
        return attrs == null ? null : (String)attrs.get(name);
    }

    String getString(String name, String def) {
        String s = get(name);
        return s == null ? def : s;
    }

    String getString(String name) {
        return getString(name, null);
    }

    float getFloatUnits(String name, float def) {
        String s = get(name);
        return s == null ? def : parseFloatValueUnits(s);
    }

    float getFloatUnits(String name) {
        String s = get(name);
        assert s != null;
        return parseFloatValueUnits(s);
    }

    protected static String parseHref(String r) {
        if (r == null) {
            return r;
        }
        if (r.startsWith("url(") && r.endsWith(")")) {
            r = r.substring(4, r.length() - 1);
        }
        if (r.startsWith("#")) {
            r = r.substring(1);
        }
        return r;
    }

    int getInt(String name, int def) {
        String s = get(name);
        // ryanlerch/ryanlerch_clubhouse.svg
        // <missing-glyph id="missing-glyph693 horiz-adv-x="1.1346e-306" />
        return s == null ? def : (int)Math.round(new Parser.Double().parse(s));
    }

    float[] getFloats(String name) {
        return parseFloats(getString(name));
    }

    static float[] parseFloats(String s) {
        if (s == null) {
            return null;
        }
        s = s.trim();
        if (s.length() == 0) {
            return null;
        }
        int ix = 0;
        Parser.Double parser = new Parser.Double(",");
        while (parser.getPosition() < s.length()) {
            if (ix >= buf.length) {
                float[] b2 = new float[buf.length * 2];
                System.arraycopy(buf, 0, b2, 0, ix);
                buf = b2;
            }
            buf[ix++] = parser.nextFloat(s);
        }
        float[] r1 = new float[ix];
        System.arraycopy(buf, 0, r1, 0, r1.length);
        return r1;
    }

    private static Color parseColorValue(String s) {
        return Colors.parseColor(s);
    }

    String getStyleString(String name) {
        return getStyleString(name, null);
    }

    String getStyleHref() {
        String href = getStyleString("xlink:href");
        if (href == null) {
            // compuserver_msn_Ford_Focus.svg
            href = getStyleString("href");
        }
        if (href == null) {
            // eady_Three-Cornered-Hat.svg
            href = getStyleString("ns:href");
        }
        return parseHref(href);
    }

    String getStyleString(String name, String def) {
        String s = getStyle(name);
        return s != null ? s : def;
    }

    Color getStyleColor(String name) {
        return getStyleColor(name, null);
    }

    Color getStyleColor(String name, Color def) {
        String s = getStyleString(name);
        if ("none".equals(s)) {
            // http://download.openclipart.org/downloads/ daily_SVG_snapshot.tar.bz2 19-May-2008
            // daily_SVG_snapshot/Anonymous/Anonymous_juice_glass.svg" <stop id="stop8175" offset="0" style="stop-color:none" />
            return null;
        } else if ("currentColor".equalsIgnoreCase(s)) {
            // http://www.w3.org/TR/css3-color/#currentcolor
            // http://www.w3.org/TR/SVG/color.html#ColorProperty
            String q = getParent().getStyleString(name);
            s = q == null ? "black" : q;
        } else if ("inherit".equalsIgnoreCase(s)) {
            // openclipart-0.18-svgonly/clipart/special/patterns/pattern-curves-anglo-roman-2.svg
            String q = getParent().getStyleString(name);
            s = q == null ? "black" : q;
        }
        return s != null ? parseColorValue(s) : def;
    }

    float getStyleRatioValue(String name, float def) {
        String s = getStyleString(name);
        return s != null ? parseRatio(s) : def;
    }

    private static float parseRatio(String val) {
        Parser.Double parser = new Parser.Double();
        return val.charAt(val.length() - 1) == '%' ?
            (float)parser.parse(val.substring(0, val.length() - 1)) :
            (float)parser.parse(val);
    }

    float[] getStyleFloats(String name) {
        return parseFloats(getStyleString(name));
    }

    float getStyleFloatUnits(String name, float def) {
        String s = getStyleString(name);
        return s != null ? parseFloatValueUnits(s) : def;
    }

    Units getUnits(String name) {
        String s = get(name);
        return s != null ? parseUnits(s) : null;
    }

    private float parseFloatValueUnits(String s) {
        Units number = parseUnits(s);
        return Units.convertUnitsToPixels(number.getKind(), number.getValue());
    }

    private static Units parseUnits(String val) {
        if (val == null) {
            return null;
        }
        return new Units(val);
    }

    static AffineTransform parseTransform(String s) {
        AffineTransform at = new AffineTransform();
        while (s.length() > 0) {
            // inkscape:version="0.38.1"
            // openclipart-0.18-svgonly/clipart/animals/birds/baby_tux_01.svg
            // transform="matrix(1.000000,0.000000,9.000000e-2,1.000000,0.000000,0.000000"/>
            String t;
            int ix = s.indexOf(')');
            if (ix > 0) {
                t = s.substring(0, ix + 1);
                parseSingleTransform(t.trim(), at);
                s = ix < s.length() - 1 ? s.substring(ix + 2).trim() : "";
            } else {
                t = s.trim() + ")";
                parseSingleTransform(t, at);
                s = "";
            }
        }
        return at;
    }

    private static AffineTransform parseSingleTransform(String s, AffineTransform at) {
        assert s.endsWith(")");
        int ix = s.indexOf('(');
        assert ix > 0;
        String f = s.substring(0, ix).trim().toLowerCase();
        Parser.Double parser = new Parser.Double(",");
        String vals = s.substring(ix + 1, s.length() - 1);
        if (f.equals("matrix")) {
            at.concatenate(new AffineTransform(parser.nextDouble(vals), parser.nextDouble(vals),
                    parser.nextDouble(vals), parser.nextDouble(vals),
                    parser.nextDouble(vals), parser.nextDouble(vals)));
        } else if (f.equals("translate")) {
            at.translate(parser.nextDouble(vals), parser.nextDouble(vals));
        } else if (f.equals("scale")) {
            double scale = parser.nextDouble(vals);
            if (parser.getPosition() < vals.length()) {
                at.scale(scale, parser.nextDouble(vals));
            } else {
                at.scale(scale, scale);
            }
        } else if (f.equals("rotate")) {
            double alpha = parser.nextDouble(vals);
            if (parser.getPosition() < vals.length()) {
                at.rotate(Math.toRadians(alpha), parser.nextDouble(vals), parser.nextDouble(vals));
            } else {
                at.rotate(Math.toRadians(alpha));
            }
        } else if (f.equals("skewx")) {
            at.shear(Math.toRadians(parser.nextDouble(vals)), 0.0);
        } else if (f.equals("skewy")) {
            at.shear(0.0, Math.toRadians(parser.nextDouble(vals)));
        } else {
            throw new Error("unknown transformation: " + f);
        }
        return at;
    }

    /** parses CSS styles into map.
     * @param s - CSS formatted string of styles, e.g.:
     *            "font-size:12;fill:#d32c27;fill-rule:evenodd;stroke-width:1pt;"
     * @param m   map to add name value pairs to.
     */
    protected static void parseStyle(String s, Map m) {
        for (QuotedStringTokenizer st = new QuotedStringTokenizer(s.trim(), ";"); st.hasMoreTokens(); ) {
            String t = st.nextToken().trim();
            int ix = t.indexOf(':');
            assert ix > 0;
            String k = t.substring(0, ix).trim();
            String v = t.substring(ix + 1).trim();
            m.put(k, v);
        }
    }

    /**
     * StringTokenizer with quoting support.
     * This class is a copy of the java.util.StringTokenizer API and
     * the behaviour is the same, except that single and doulbe quoted
     * string values are recognized.
     * Delimiters within quotes are not considered delimiters.
     * Quotes can be escaped with '\'.
     *
     * @author Greg Wilkins (gregw)
     * @see java.util.StringTokenizer
     */
    private static class QuotedStringTokenizer extends StringTokenizer {

        private String string;
        private String delim = "\t\n\r";
        private boolean returnQuotes = false;
        private boolean returnTokens = false;
        private StringBuffer token;
        private boolean hasToken = false;
        private int position = 0;
        private int lastStart = 0;

        private QuotedStringTokenizer(String s, String d, boolean returnTokens, boolean returnQuotes) {
            super("");
            this.string = s;
            if (d != null) {
                delim = d;
            }
            this.returnTokens = returnTokens;
            this.returnQuotes = returnQuotes;
            if (delim.indexOf('\'') >= 0 || delim.indexOf('"') >= 0) {
                throw new Error("Can't use quotes as delimiters: " + delim);
            }
            this.token = new StringBuffer(string.length() > 1024 ? 512 : string.length() / 2);
        }

        private QuotedStringTokenizer(String s, String d) {
            this(s, d, false, false);
        }

        public boolean hasMoreTokens() {
            if (hasToken) {
                return true;
            }
            lastStart = position;
            int state = 0;
            boolean escape = false;
            while (position < string.length()) {
                char c = string.charAt(position++);
                switch (state) {
                    case 0: // start
                        if (delim.indexOf(c) >= 0) {
                            if (returnTokens) {
                                token.append(c);
                                return hasToken = true;
                            }
                        } else if (c == '\'') {
                            if (returnQuotes) {
                                token.append(c);
                            }
                            state = 2;
                        } else if (c == '\"') {
                            if (returnQuotes) {
                                token.append(c);
                            }
                            state = 3;
                        } else {
                            token.append(c);
                            hasToken = true;
                            state = 1;
                        }
                        break;
                    case 1: // token
                        hasToken = true;
                        if (delim.indexOf(c) >= 0) {
                            if (returnTokens) {
                                position--;
                            }
                            return hasToken;
                        } else if (c == '\'') {
                            if (returnQuotes) {
                                token.append(c);
                            }
                            state = 2;
                        } else if (c == '\"') {
                            if (returnQuotes) {
                                token.append(c);
                            }
                            state = 3;
                        } else {
                            token.append(c);
                        }
                        break;
                    case 2: // single quote
                        hasToken = true;
                        if (escape) {
                            escape = false;
                            token.append(c);
                        } else if (c == '\'') {
                            if (returnQuotes) {
                                token.append(c);
                            }
                            state = 1;
                        } else if (c == '\\') {
                            if (returnQuotes) {
                                token.append(c);
                            }
                            escape = true;
                        } else {
                            token.append(c);
                        }
                        break;
                    case 3: // double quote
                        hasToken = true;
                        if (escape) {
                            escape = false;
                            token.append(c);
                        } else if (c == '\"') {
                            if (returnQuotes) {
                                token.append(c);
                            }
                            state = 1;
                        } else if (c == '\\') {
                            if (returnQuotes) {
                                token.append(c);
                            }
                            escape = true;
                        } else {
                            token.append(c);
                        }
                }
            }
            return hasToken;
        }

        public String nextToken() throws NoSuchElementException {
            if (!hasMoreTokens() || token == null) {
                throw new NoSuchElementException();
            }
            String t = token.toString();
            token.setLength(0);
            hasToken = false;
            return t;
        }

        public String nextToken(String delim) throws NoSuchElementException {
            this.delim = delim;
            position = lastStart;
            token.setLength(0);
            hasToken = false;
            return nextToken();
        }

        public boolean hasMoreElements() {
            return hasMoreTokens();
        }

        public Object nextElement() throws NoSuchElementException {
            return nextToken();
        }

        public int countTokens() {
            return -1;
        }

    }
}
