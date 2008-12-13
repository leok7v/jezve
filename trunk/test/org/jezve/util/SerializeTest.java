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
package org.jezve.util;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import java.beans.*;
import java.awt.*;
import java.security.*;

public class SerializeTest {

    // don't forget: -ea -Xmx128m

    public static class Point3 extends Point {

        private double z;

        public Point3() {
        }

        public boolean equals(Object o) {
            if (o == null || !(o instanceof Point3)) {
                return false;
            }
            Point3 p3 = (Point3)o;
            return super.equals(p3) && p3.z == z;
        }

        public int hashCode() {
            return super.hashCode() * java.lang.Float.floatToIntBits((float)z);
        }
    }

    private static boolean deepEquals(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1 == null) {
            return o2 == null;
        }
        if (o2 == null) {
            return o1 == null;
        }
        if (o1.equals(o2)) {
            return true;
        }
        if (o1.getClass().isArray() && o2.getClass().isArray()) {
            int n = Array.getLength(o1);
            if (n != Array.getLength(o2)) {
                return false;
            }
            for (int i = 0; i < n; i++) {
                if (!deepEquals(Array.get(o1, i), Array.get(o2, i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static void test(Object o) throws IOException {
        test(o, true, false);
    }

    private static void test(Object o, boolean trace, boolean cycle) throws IOException {
        long time0 = System.nanoTime();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Serialize.Encoder encoder = new Serialize.Encoder(os);
        encoder.encode(o);
        encoder.close();
        os.close();
        InputStream in = new ByteArrayInputStream(os.toByteArray());
        Serialize.Decoder decoder = new Serialize.Decoder(in);
        Object i = decoder.decode();
        decoder.close();
        in.close();
        time0 = (System.nanoTime() - time0) / 1000;
        assert cycle || deepEquals(o, i); // do not call deepEquals for known cycle

        long time1 = System.nanoTime();
        ByteArrayOutputStream xml = new ByteArrayOutputStream();
        XMLEncoder xmlencoder = new XMLEncoder(xml);
        xmlencoder.writeObject(o);
        xmlencoder.close();
        xml.close();
        time1 = (System.nanoTime() - time1) / 1000;
        if (trace) {
            System.err.println("binary=" + os.toByteArray().length + " bytes  " + time0 + " usec" +
                    " xml=" + xml.toByteArray().length +
                    " bytes  " + time1 + " usec");
        }
    }

    public static void main(String[] args) throws IOException {
        
        for (int i = 2; i < 0x20000; i++) {
            test(new Integer(i), false, false);
        }
        test(null);
        test(new Integer(0));
        test(new Integer(1));
        test(Boolean.TRUE);
        test(Boolean.FALSE);
        test(new Integer(Integer.MIN_VALUE));
        test(new Integer(Integer.MAX_VALUE));
        test(new Long(Long.MIN_VALUE));
        test(new Long(Long.MAX_VALUE));
        test(new Integer(153));
        test(new Long(0xBADBEEF00BADF00DL));
        test("Hello");
        test(new Long(153));
        test(new Integer(153));
        test(new Short((short)153));
        test(new Byte((byte)153));
        test(new Character('X'));
        test(new Double(Math.PI));
        test(new Float((float)Math.E));
        test(new Date(System.currentTimeMillis()));
        test(new long[]{1,2,3,4,5});
        test(new int[]{1,2,3,4,5});
        test(new short[]{1,2,3,4,5});
        test(new byte[]{1,2,3,4,5});
        test(new char[]{1,2,3,4,5});
        test(new boolean[]{true, false});
        test(new double[]{Math.PI, Math.E});
        test(new float[]{(float)Math.PI, (float)Math.E});
        test(new String[]{"Hello", "World"});
        test(new Point[][]{new Point[]{new Point(1,2), new Point(3,4)}, new Point[]{new Point(5,6)}});
        test(new boolean[][]{new boolean[]{true, false}, new boolean[]{true, false}});

        test(new String[][]{new String[]{"Hello", "World"}, new String[]{"Fare", "well"}});

        Point3 p3 = new Point3();
        p3.z = 123.456;
        test(p3);

        Object[] cycle = new Object[]{"Hello", null, "World"};
        cycle[1] = cycle;
        test(cycle, true, true);

        ArrayList array = new ArrayList();
        Map map = new HashMap();
        Set set = new HashSet();
        set.add(array);
        map.put("Hello", "World");
        map.put("Now", new Date(System.currentTimeMillis()));
        map.put("xyz", array);
        array.add(set);
        array.add(new Point(1,3));
        array.add(new Rectangle(1,3,4,5));
        array.add(new StringBuffer("abc"));
        array.add(p3);
//      XMLEncoder/Decoder do not support BigInteger
//      array.add(new BigInteger(+1, new byte[]{(byte)0xBA, (byte)0xDB, (byte)0xEE, (byte)0xF0}));
        test(map, true, true);
        test(set, true, true);
        test(array, true, true);
        array.clear();
        long v = 13;
        for (int i = 0; i < 64; i++) {
            array.add(new Long(v));
            array.add(new Long(-v));
            v = v * 2 + 1;
        }
        SecureRandom r = new SecureRandom();
        for (int i = 0; i < 10000; i++) {
            array.add(new Long(r.nextLong()));
        }
        for (int i = 2; i < 0x20000; i++) {
            array.add(new Long(i));
        }
        test(array);
        test(array.toArray());
        long[] a = new long[array.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = ((Long)array.get(i)).longValue();
        }
        test(a);
    }

}
