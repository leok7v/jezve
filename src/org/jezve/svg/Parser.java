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

package org.jezve.svg;

//import java.util.StringTokenizer;

/** There are two reasons for collection of number parsers to be implemented:
 *  1. Double, Float, Long, Integere, Short parseXXX methods all allocate
 *     a new instance of a boxing class which in tight long loops can produce
 *     significant load on garbadge collector.
 *  2. It is desirable to be able to parse numbers and stop at the end of number
 *     syntax reporting back stop position. Otherwise it is necessary to scan a string
 *     before calling e.g. Double.parseDouble basically doing the same thing twice.
 *  3. Skipping whitespaces and certain extra separator characters (e.g. ',' or ';')
 *     is an added convinience to generic parsing.
 */

class Parser {

    /** Parser.isDigit is different from Character.isDigit.
     *  It's true only for Unicode Latin plane numbers.
     *  @param ch character
     *  @return true if character is latin digit
     */
    static boolean isDigit(char ch) {
        return '0' <= ch && ch <= '9';
    }

    /** Parser.isWhitespace is different from Character.isWhitespace.
     *  It's true only for Unicode Latin plane whitespace characters.
     *  @param ch character
     *  @return true if character is whitespace (space, CR, LF, TAB)
     */
    static boolean isWhitespace(char ch) {
        return ' ' == ch || '\n' == ch || '\r' == ch || '\t' == ch;
    }

    static int skipWhitespace(String s, int pos) {
        int len = s.length();
        while (pos < len && isWhitespace(s.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    static int skipWhitespace(String s, int pos, String extra) {
        int len = s.length();
        while (pos < len) {
            char ch = s.charAt(pos);
            if (!isWhitespace(s.charAt(pos)) && (extra == null || extra.indexOf(ch) < 0)) {
                break;
            }
            pos++;
        }
        return pos;
    }

    private final static double pow10[] = {
        1e00, 1e01, 1e02, 1e03, 1e04, 1e05, 1e06, 1e07, 1e08, 1e09,
        1e10, 1e11, 1e12, 1e13, 1e14, 1e15, 1e16, 1e17, 1e18, 1e19,
        1e20, 1e21, 1e22, 1e23, 1e24, 1e25, 1e26, 1e27, 1e28, 1e29,
        1e30, 1e31, 1e32, 1e33, 1e34, 1e35, 1e36, 1e37, 1e38, 1e39,
        1e40, 1e41, 1e42, 1e43, 1e44, 1e45, 1e46, 1e47, 1e48, 1e49,
        1e50, 1e51, 1e52, 1e53, 1e54, 1e55, 1e56, 1e57, 1e58, 1e59,
        1e60, 1e61, 1e62, 1e63, 1e64, 1e65, 1e66, 1e67, 1e68, 1e69,
        1e70, 1e71, 1e72, 1e73, 1e74, 1e75, 1e76, 1e77, 1e78, 1e79,
        1e80, 1e81, 1e82, 1e83, 1e84, 1e85, 1e86, 1e87, 1e88, 1e89,
        1e90, 1e91, 1e92, 1e93, 1e94, 1e95, 1e96, 1e97, 1e98, 1e99
    };

    private static double pow10(int exp) {
        if (exp == 0) {
            return 1.0;
        } else if (0 < exp && exp < pow10.length) {
            return pow10[exp];
        } else if (0 < -exp && -exp < pow10.length) {
            return 1.0 / pow10[-exp];
        } else {
            return Math.pow(10, exp);
        }
    }

    private static class Base {

        protected int position;
        protected String whitespaces;

        String getWhitespaces() {
            return whitespaces;
        }

        void setWhitespaces(String whitespaces) {
            this.whitespaces = whitespaces;
        }

        int getPosition() {
            return position;
        }

        void skipWhitespace(String s) {
            position = Parser.skipWhitespace(s, position, whitespaces);
        }

        char nextChar(String s) {
            return s.charAt(position++);
        }

        void rewind() {
            position = 0;
        }

    }

    static class Double extends Base {

        Double() {
        }

        Double(String extraWhitespaces) {
            whitespaces = extraWhitespaces;
        }

        /** Similar but not entirely the same as java.lang.Double.parseDouble.
         *  The rounding error may accumulate resulting in ~pow(10, -(56/2)) deltas e.g.
         *  java.lang.Double.parseDouble("-8.9383918e-006") == -8.9383918e-006;
         *  Parser.Double.parse("-8.9383918e-006") == -8.9383918e-006 + 1.6940658945086007E-21;
         * because both 1e-6 and 8.9383918 binary double represintations are inexact.
         * Use with caution.
         * @param s string to parse
         * @param start position
         * @return resulting parsed double value
         */
        double parse(String s, int start) {
            int len = s.length();
            int pos = Parser.skipWhitespace(s, start, whitespaces);
            char ch;
            boolean negative = false;
            if (pos < len) {
                ch = s.charAt(pos);
                if (ch == '-') {
                    negative = true;
                    pos++;
                } else if (ch == '+') {
                    pos++;
                }
            }
            if (pos >= len || !isDigit(s.charAt(pos)) && s.charAt(pos) != '.') {
                position = pos;
                throw new NumberFormatException("not a digit");
            }
            double v = 0;
            while (pos < len && isDigit(ch = s.charAt(pos))) {
                int d = ch - '0';
                v = (v * 10) + d;
                pos++;
            }
            if (pos < len && s.charAt(pos) == '.') {
                pos++;
                double ex = 1;
                double m = 0;
                while (pos < len && isDigit(ch = s.charAt(pos))) {
                    int d = ch - '0';
                    ex /= 10;
                    m = m + d * ex;
                    pos++;
                }
                v = v + m;
            }
            if (pos < len && (s.charAt(pos) == 'e' || s.charAt(pos) == 'E')) {
                pos++;
                boolean n = false;
                if (pos < len) {
                    ch = s.charAt(pos);
                    if (ch == '-') {
                        n = true;
                        pos++;
                    } else if (ch == '+') {
                        pos++;
                    }
                }
                int exp = 0;
                while (pos < len && isDigit(ch = s.charAt(pos))) {
                    int d = ch - '0';
                    exp = exp * 10 + d;
                    if (exp > 325) { // see http://en.wikipedia.org/wiki/IEEE_754-1985
                        position = pos;
                        throw new NumberFormatException("exponent overflow");
                    }
                    pos++;
                }
                if (exp != 0) {
                    v = v * pow10(n ? -exp : exp);
                }
            }
            position = Parser.skipWhitespace(s, pos, whitespaces);
            return negative ? -v : v;
        }

        /** Same as parse(s, 0)
         * @param s string to parse
         * @return resulting parsed double value
         */
        double parse(String s) {
            return parse(s, 0);
        }

        double nextDouble(String s) {
            return parse(s, getPosition());
        }

        float nextFloat(String s) {
            return (float)nextDouble(s);
        }

    }

    static class Long extends Base {

        private static final String Long_MIN_VALUE = ("" + java.lang.Long.MIN_VALUE).substring(1);

        Long() {
        }

        Long(String extraWhitespaces) {
            whitespaces = extraWhitespaces;
        }
        /** Parses decimal text representation of long value
         * @param s string to parse
         * @param start position
         * @return resulting parsed long value
         */
        long parse(String s, int start) {
            int len = s.length();
            int pos = Parser.skipWhitespace(s, start, whitespaces);
            char ch;
            boolean negative = false;
            if (pos < len) {
                ch = s.charAt(pos);
                if (ch == '-') {
                    negative = true;
                    pos++;
                } else if (ch == '+') {
                    pos++;
                }
            }
            if (pos >= len || !isDigit(s.charAt(pos))) {
                position = pos;
                throw new NumberFormatException("not a digit");
            }
            while (pos < len && s.charAt(pos) == '0') {
                pos++;
            }
            int pos0 = pos;
            long v = 0;
            while (pos < len && isDigit(ch = s.charAt(pos))) {
                int d = ch - '0';
                if (v > (java.lang.Long.MAX_VALUE - d) / 10) {
                    // special case
                    if (negative && s.substring(pos0, pos + 1).equals(Long_MIN_VALUE)
                            && (pos + 1 == len || !isDigit(s.charAt(pos + 1)))) {
                        position = pos + 1;
                        return java.lang.Long.MIN_VALUE;
                    }
                    position = pos;
                    throw new NumberFormatException("overflow");
                }
                v = (v * 10) + d;
                pos++;
            }
            position = Parser.skipWhitespace(s, pos, whitespaces);
            return negative ? -v : v;
        }

        /** Same as parse(s, 0)
         * @param s string to parse
         * @return resulting parsed long value
         */
        long parse(String s) {
            return parse(s, 0);
        }

        long parseHex(String s, int start) {
            int len = s.length();
            int pos = start = Parser.skipWhitespace(s, start, whitespaces);
            int pos0 = -1;
            long v = 0;
            while (pos < len) {
                char ch = s.charAt(pos);
                int d;
                if (ch >= '0' && ch <= '9') {
                    d = ch - '0';
                } else if (ch >= 'a' && ch <= 'z') {
                    d = ch - 'a' + 10;
                } else if (ch >= 'A' && ch <= 'Z') {
                    d = ch - 'A' + 10;
                } else {
                    break;
                }
                if (pos0 == -1 && d != 0) {
                    pos0 = pos;
                }
                pos++;
                if (pos - pos0 > 16) {
                    throw new NumberFormatException("overflow");
                }
                v  = (v << 4) | d;
            }
            if (pos == start) {
                throw new NumberFormatException("not a digit");
            }
            position = Parser.skipWhitespace(s, pos, whitespaces);
            return v;
        }

        double nextLong(String s) {
            return parse(s, getPosition());
        }
    }

    static class Integer extends Base {

        private static final String Int_MIN_VALUE = ("" + java.lang.Integer.MIN_VALUE).substring(1);

        Integer() {
        }

        Integer(String extraWhitespaces) {
            whitespaces = extraWhitespaces;
        }

        /** Parses decimal text representation of long value
         * @param s string to parse
         * @param start position
         * @return resulting parsed long value
         */
        int parse(String s, int start) {
            int len = s.length();
            int pos = Parser.skipWhitespace(s, start, whitespaces);
            char ch;
            boolean negative = false;
            if (pos < len) {
                ch = s.charAt(pos);
                if (ch == '-') {
                    negative = true;
                    pos++;
                } else if (ch == '+') {
                    pos++;
                }
            }
            if (pos >= len || !isDigit(s.charAt(pos))) {
                position = pos;
                throw new NumberFormatException("not a digit");
            }
            while (pos < len && s.charAt(pos) == '0') {
                pos++;
            }
            int pos0 = pos;
            int v = 0;
            while (pos < len && isDigit(ch = s.charAt(pos))) {
                int d = ch - '0';
                if (v > (java.lang.Integer.MAX_VALUE - d) / 10) {
                    // special case
                    if (negative && s.substring(pos0, pos + 1).equals(Int_MIN_VALUE)
                            && (pos + 1 == len || !isDigit(s.charAt(pos + 1)))) {
                        position = pos + 1;
                        return java.lang.Integer.MIN_VALUE;
                    }
                    position = pos;
                    throw new NumberFormatException("overflow");
                }
                v = (v * 10) + d;
                pos++;
            }
            position = Parser.skipWhitespace(s, pos, whitespaces);
            return negative ? -v : v;
        }

        /** Same as parse(s, 0)
         * @param s string to parse
         * @return resulting parsed long value
         */
        int parse(String s) {
            return parse(s, 0);
        }

        int parseHex(String s, int start) {
            int len = s.length();
            int pos = start = Parser.skipWhitespace(s, start, whitespaces);
            int pos0 = -1;
            int v = 0;
            while (pos < len) {
                char ch = s.charAt(pos);
                int d;
                if (ch >= '0' && ch <= '9') {
                    d = ch - '0';
                } else if (ch >= 'a' && ch <= 'z') {
                    d = ch - 'a' + 10;
                } else if (ch >= 'A' && ch <= 'Z') {
                    d = ch - 'A' + 10;
                } else {
                    break;
                }
                if (pos0 == -1 && d != 0) {
                    pos0 = pos;
                }
                pos++;
                if (pos - pos0 > 8) {
                    throw new NumberFormatException("overflow");
                }
                v  = (v << 4) | d;
            }
            if (pos == start) {
                throw new NumberFormatException("not a digit");
            }
            position = Parser.skipWhitespace(s, pos, whitespaces);
            return v;
        }

        double nextInteger(String s) {
            return parse(s, getPosition());
        }
    }

/*
    static void test() {

        {
            Parser.Double parser = new Parser.Double();
            String s = "M 1 2 3 1.2 1e34 1.234E-35 1.e+13 -2.1579186e-005,-7.0710768 L -7.0710894,-8.9383918e-006 L -2.1579186e-005,7.0710589 L 7.0710462,-8.9383918e-006 L -2.1579186e-005,-7.0710768 0.1234567890123456789012345678901234567890e-290 0.1234567890123456789012345678901234567890e+290 z ";
//          Debug.traceln(s);
            StringTokenizer st = new StringTokenizer(s, ", ");
            int pos = 0;
            for (;;) {
                while (pos < s.length() && Character.isLetter(s.charAt(pos))) {
                    pos++;
                }
                pos = Parser.skipWhitespace(s, pos, ",");
                if (pos >= s.length()) {
                    break;
                }
                double d = parser.parse(s, pos);
                String t = st.nextToken().trim();
                while (!t.startsWith("+") && !t.startsWith("-") && !isDigit(t.charAt(0))) {
                    t = st.nextToken();
                }
                double p = java.lang.Double.parseDouble(t);
                assert Math.abs(d - p) <= 1e-18;
//              Debug.traceln(d + " " + t + " " + p + " " + Math.abs(d - p));
                pos = parser.getPosition();
                pos = Parser.skipWhitespace(s, pos, ",");
            }
//          Debug.traceln("" + parser.parse("0.1234567890123456789012345678901234567890"));

            boolean thrown = false;
            try {
                parser.parse("+123e+340");
            } catch (NumberFormatException x) {
                thrown = true;
            }
            assert thrown;
            thrown = false;
            try {
                parser.parse("+123e-340");
            } catch (NumberFormatException x) {
                thrown = true;
            }
            assert thrown;
            thrown = false;
            try {
                parser.parse(" +*123.456 ");
            } catch (NumberFormatException x) {
                thrown = true;
            }
            assert thrown;
        }

        {
            Parser.Long parser = new Parser.Long();
            long x1 = parser.parseHex("00000001234567890ABCDEF", 0);
            assert x1 == 0x1234567890ABCDEFL;
            long x2 = parser.parseHex("0000000F234567890ABCDEF", 0);
            assert x2 == 0xF234567890ABCDEFL;
            boolean thrown = false;
            try {
                parser.parseHex("  1234567812345678F ", 0);
            } catch (NumberFormatException x) {
                thrown = true;
            }
            assert thrown;
            thrown = false;
            try {
                parser.parseHex(" *1234 ", 0);
            } catch (NumberFormatException x) {
                thrown = true;
            }
            assert thrown;

            long x3 = parser.parse("9223372036854775807");
            assert x3 == java.lang.Long.MAX_VALUE;
            long x4 = parser.parse("-000000000000009223372036854775808");
            assert x4 == java.lang.Long.MIN_VALUE;
            thrown = false;
            try {
                parser.parse("+9223372036854775808");
            } catch (NumberFormatException x) {
                thrown = true;
            }
            assert thrown;
            thrown = false;
            try {
                parser.parse("-9223372036854775809");
            } catch (NumberFormatException x) {
                thrown = true;
            }
            assert thrown;
            thrown = false;
            try {
                parser.parse("+*123");
            } catch (NumberFormatException x) {
                thrown = true;
            }
            assert thrown;
        }

        {
            Parser.Integer parser = new Parser.Integer();
            long x1 = parser.parseHex("000000012345678", 0);
            assert x1 == 0x12345678;
            long x2 = parser.parseHex("0000000F2345678", 0);
            assert x2 == 0xF2345678;
            boolean thrown = false;
            try {
                parser.parseHex("  12345678F ", 0);
            } catch (NumberFormatException x) {
                thrown = true;
            }
            assert thrown;
            thrown = false;
            try {
                parser.parseHex(" *1234 ", 0);
            } catch (NumberFormatException x) {
                thrown = true;
            }
            assert thrown;

            long x3 = parser.parse("2147483647");
            assert x3 == java.lang.Integer.MAX_VALUE;
            long x4 = parser.parse("-000000000000002147483648");
            assert x4 == java.lang.Integer.MIN_VALUE;
            thrown = false;
            try {
                parser.parse("+2147483648");
            } catch (NumberFormatException x) {
                thrown = true;
            }
            assert thrown;
            thrown = false;
            try {
                parser.parse("-2147483649");
            } catch (NumberFormatException x) {
                thrown = true;
            }
            assert thrown;
            thrown = false;
            try {
                parser.parse("+*123");
            } catch (NumberFormatException x) {
                thrown = true;
            }
            assert thrown;
        }

    }

*/

}
