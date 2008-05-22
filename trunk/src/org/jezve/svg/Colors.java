package org.jezve.svg;

import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class Colors {

    private static final Map colorTable;
    private static final char[] c6 = new char[6];

    static {
        HashMap m = new HashMap();
        m.put("aliceblue", new Color(0xf0f8ff));
        m.put("antiquewhite", new Color(0xfaebd7));
        m.put("aqua", new Color(0x00ffff));
        m.put("aquamarine", new Color(0x7fffd4));
        m.put("azure", new Color(0xf0ffff));
        m.put("beige", new Color(0xf5f5dc));
        m.put("bisque", new Color(0xffe4c4));
        m.put("black", new Color(0x000000));
        m.put("blanchedalmond", new Color(0xffebcd));
        m.put("blue", new Color(0x0000ff));
        m.put("blueviolet", new Color(0x8a2be2));
        m.put("brown", new Color(0xa52a2a));
        m.put("burlywood", new Color(0xdeb887));
        m.put("cadetblue", new Color(0x5f9ea0));
        m.put("chartreuse", new Color(0x7fff00));
        m.put("chocolate", new Color(0xd2691e));
        m.put("coral", new Color(0xff7f50));
        m.put("cornflowerblue", new Color(0x6495ed));
        m.put("cornsilk", new Color(0xfff8dc));
        m.put("crimson", new Color(0xdc143c));
        m.put("cyan", new Color(0x00ffff));
        m.put("darkblue", new Color(0x00008b));
        m.put("darkcyan", new Color(0x008b8b));
        m.put("darkgoldenrod", new Color(0xb8860b));
        m.put("darkgray", new Color(0xa9a9a9));
        m.put("darkgrey", new Color(0xa9a9a9));
        m.put("darkgreen", new Color(0x006400));
        m.put("darkkhaki", new Color(0xbdb76b));
        m.put("darkmagenta", new Color(0x8b008b));
        m.put("darkolivegreen", new Color(0x556b2f));
        m.put("darkorange", new Color(0xff8c00));
        m.put("darkorchid", new Color(0x9932cc));
        m.put("darkred", new Color(0x8b0000));
        m.put("darksalmon", new Color(0xe9967a));
        m.put("darkseagreen", new Color(0x8fbc8f));
        m.put("darkslateblue", new Color(0x483d8b));
        m.put("darkslategray", new Color(0x2f4f4f));
        m.put("darkslategrey", new Color(0x2f4f4f));
        m.put("darkturquoise", new Color(0x00ced1));
        m.put("darkviolet", new Color(0x9400d3));
        m.put("deeppink", new Color(0xff1493));
        m.put("deepskyblue", new Color(0x00bfff));
        m.put("dimgray", new Color(0x696969));
        m.put("dimgrey", new Color(0x696969));
        m.put("dodgerblue", new Color(0x1e90ff));
        m.put("feldspar", new Color(0xd19275));
        m.put("firebrick", new Color(0xb22222));
        m.put("floralwhite", new Color(0xfffaf0));
        m.put("forestgreen", new Color(0x228b22));
        m.put("fuchsia", new Color(0xff00ff));
        m.put("gainsboro", new Color(0xdcdcdc));
        m.put("ghostwhite", new Color(0xf8f8ff));
        m.put("gold", new Color(0xffd700));
        m.put("goldenrod", new Color(0xdaa520));
        m.put("gray", new Color(0x808080));
        m.put("grey", new Color(0x808080));
        m.put("green", new Color(0x008000));
        m.put("greenyellow", new Color(0xadff2f));
        m.put("honeydew", new Color(0xf0fff0));
        m.put("hotpink", new Color(0xff69b4));
        m.put("indianred", new Color(0xcd5c5c));
        m.put("indigo", new Color(0x4b0082));
        m.put("ivory", new Color(0xfffff0));
        m.put("khaki", new Color(0xf0e68c));
        m.put("lavender", new Color(0xe6e6fa));
        m.put("lavenderblush", new Color(0xfff0f5));
        m.put("lawngreen", new Color(0x7cfc00));
        m.put("lemonchiffon", new Color(0xfffacd));
        m.put("lightblue", new Color(0xadd8e6));
        m.put("lightcoral", new Color(0xf08080));
        m.put("lightcyan", new Color(0xe0ffff));
        m.put("lightgoldenrodyellow", new Color(0xfafad2));
        m.put("lightgrey", new Color(0xd3d3d3));
        m.put("lightgray", new Color(0xd3d3d3));
        m.put("lightgreen", new Color(0x90ee90));
        m.put("lightpink", new Color(0xffb6c1));
        m.put("lightsalmon", new Color(0xffa07a));
        m.put("lightseagreen", new Color(0x20b2aa));
        m.put("lightskyblue", new Color(0x87cefa));
        m.put("lightslateblue", new Color(0x8470ff));
        m.put("lightslategray", new Color(0x778899));
        m.put("lightslategrey", new Color(0x778899));
        m.put("lightsteelblue", new Color(0xb0c4de));
        m.put("lightyellow", new Color(0xffffe0));
        m.put("lime", new Color(0x00ff00));
        m.put("limegreen", new Color(0x32cd32));
        m.put("linen", new Color(0xfaf0e6));
        m.put("magenta", new Color(0xff00ff));
        m.put("maroon", new Color(0x800000));
        m.put("mediumaquamarine", new Color(0x66cdaa));
        m.put("mediumblue", new Color(0x0000cd));
        m.put("mediumorchid", new Color(0xba55d3));
        m.put("mediumpurple", new Color(0x9370d8));
        m.put("mediumseagreen", new Color(0x3cb371));
        m.put("mediumslateblue", new Color(0x7b68ee));
        m.put("mediumspringgreen", new Color(0x00fa9a));
        m.put("mediumturquoise", new Color(0x48d1cc));
        m.put("mediumvioletred", new Color(0xc71585));
        m.put("midnightblue", new Color(0x191970));
        m.put("mintcream", new Color(0xf5fffa));
        m.put("mistyrose", new Color(0xffe4e1));
        m.put("moccasin", new Color(0xffe4b5));
        m.put("navajowhite", new Color(0xffdead));
        m.put("navy", new Color(0x000080));
        m.put("oldlace", new Color(0xfdf5e6));
        m.put("olive", new Color(0x808000));
        m.put("olivedrab", new Color(0x6b8e23));
        m.put("orange", new Color(0xffa500));
        m.put("orangered", new Color(0xff4500));
        m.put("orchid", new Color(0xda70d6));
        m.put("palegoldenrod", new Color(0xeee8aa));
        m.put("palegreen", new Color(0x98fb98));
        m.put("paleturquoise", new Color(0xafeeee));
        m.put("palevioletred", new Color(0xd87093));
        m.put("papayawhip", new Color(0xffefd5));
        m.put("peachpuff", new Color(0xffdab9));
        m.put("peru", new Color(0xcd853f));
        m.put("pink", new Color(0xffc0cb));
        m.put("plum", new Color(0xdda0dd));
        m.put("powderblue", new Color(0xb0e0e6));
        m.put("purple", new Color(0x800080));
        m.put("red", new Color(0xff0000));
        m.put("rosybrown", new Color(0xbc8f8f));
        m.put("royalblue", new Color(0x4169e1));
        m.put("saddlebrown", new Color(0x8b4513));
        m.put("salmon", new Color(0xfa8072));
        m.put("sandybrown", new Color(0xf4a460));
        m.put("seagreen", new Color(0x2e8b57));
        m.put("seashell", new Color(0xfff5ee));
        m.put("sienna", new Color(0xa0522d));
        m.put("silver", new Color(0xc0c0c0));
        m.put("skyblue", new Color(0x87ceeb));
        m.put("slateblue", new Color(0x6a5acd));
        m.put("slategray", new Color(0x708090));
        m.put("slategrey", new Color(0x708090));
        m.put("snow", new Color(0xfffafa));
        m.put("springgreen", new Color(0x00ff7f));
        m.put("steelblue", new Color(0x4682b4));
        m.put("tan", new Color(0xd2b48c));
        m.put("teal", new Color(0x008080));
        m.put("thistle", new Color(0xd8bfd8));
        m.put("tomato", new Color(0xff6347));
        m.put("turquoise", new Color(0x40e0d0));
        m.put("violet", new Color(0xee82ee));
        m.put("violetred", new Color(0xd02090));
        m.put("wheat", new Color(0xf5deb3));
        m.put("white", new Color(0xffffff));
        m.put("whitesmoke", new Color(0xf5f5f5));
        m.put("yellow", new Color(0xffff00));
        m.put("yellowgreen", new Color(0x9acd32));
        colorTable = Collections.unmodifiableMap(m);
    }

    static Color parseColor(String s) {
        Parser.Integer intParser = new Parser.Integer(",");
        Parser.Double doubleParser = new Parser.Double(",");
        if (s.charAt(0) == '#') {
            String x = s.substring(1);
            if (x.length() == 3) {
                c6[0] = c6[1] = x.charAt(0);
                c6[2] = c6[3] = x.charAt(1);
                c6[4] = c6[5] = x.charAt(2);
                x = new String(c6);
            }
            return new Color(intParser.parseHex(x, 0));
        } else {
            if (s.startsWith("rgb(") && s.endsWith(")")) {
                // guci22/guci22_Mountains.svg
                String c = s.substring(4, s.length() - 1);
                if (c.indexOf('%') >= 0) {
                    float r = (float)doubleParser.parse(c, 0);
                    assert c.charAt(doubleParser.getPosition()) == '%';
                    doubleParser.nextChar(s);
                    float g = (float)doubleParser.parse(c, doubleParser.getPosition());
                    assert c.charAt(doubleParser.getPosition()) == '%';
                    doubleParser.nextChar(s);
                    float b = (float)doubleParser.parse(c, doubleParser.getPosition());
                    assert c.charAt(doubleParser.getPosition()) == '%';
                    doubleParser.nextChar(s);
                    assert 0 <= r && r <= 100 : s;
                    assert 0 <= g && g <= 100 : s;
                    assert 0 <= b && b <= 100 : s;
                    return new Color(Math.round(r * 255 / 100),
                                     Math.round(g * 255 / 100),
                                     Math.round(b * 255 / 100));
                } else {
                    int r = intParser.parse(c, 0);
                    int g = intParser.parse(c, intParser.getPosition());
                    int b = intParser.parse(c, intParser.getPosition());
                    return new Color(r, g, b);
                }
            } else {
                // http://www.w3.org/TR/css3-color/#currentcolor
                assert !"currentColor".equals(s);
                Color c = (Color)colorTable.get(s.toLowerCase());
                assert c != null : "unknonw color: " + s.toLowerCase();
                return c;
            }
        }
    }

}
