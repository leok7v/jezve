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

/** Serialize.Encoder/Serialize.Decoder are not universal. They are simple analog of
 *  java.beans.XMLEncoder/XMLDecoder. It is just about 10 times faster and produces
 *  (on average) 10 times smaller binary output. It is not as sophisticated as
 *  http://hessian.caucho.com/
 *  WARNING: Serializer treats all Collections and Maps in a generic way and does NOT
 *  serialize the fields of class that extend HashMap, HashSet, ArrayList and alike.
 *  <code>
 *  class MyMap extends HashMap {
 *      private Foo bar; // will NOT be serialized
 *  }
 *  </code>
 *  (if you are curious the reason for it is that I cannot or do not want to
 *  distinguish between HashMap fields and your fields, not to mention that
 *  extending HashMap is not such a swell idea - unless you know what you are doing
 *  and support equality axioms by implementation your own equals and hashCode).
 *
 *  Simple data structures like Point and Rectangle are serialized even is accessors
 *  are not implemented for private fields.
 *  The transient fields and fields that are not accessible for any reason are ignored.
 */

import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import java.math.*;

public class Serialize {

    private static final int
            CLASS_DEFINITION = 0xF0, 
            ARRAY = 0xF1,
            COLLECTION = 0xF2,
            MAP = 0xF3,
            REFERENCE = 0xF4,
            // special but very frequent cases:
            TRUE = 0xF5,
            FALSE = 0xF6,
            ZERO = 0xF7,
            ONE = 0xF8,
            NULL = 0xF9;
    private static final Integer INTEGER_ZERO = new Integer(0);
    private static final Integer INTEGER_ONE = new Integer(1);
    private static final Map c2f = new HashMap(); // Class to Map<String, Field>

    public static class SerializationError extends java.lang.Error {

        public SerializationError(Throwable cause) {
            super(cause);
        }

    }

    public static final class Encoder {

        private Object2Int o2i = new Object2Int(); // object reference to object number
        private Object2Int c2i = new Object2Int() {{ // class name (String) to class number
            put(String.class, 1);
            put(long.class, 2);
            put(int.class, 3);
            put(short.class, 4);
            put(byte.class, 5);
            put(char.class, 6);
            put(boolean.class, 7);
            put(double.class, 8);
            put(float.class, 9);
            put(Long.class, 10);
            put(Integer.class, 11);
            put(Short.class, 12);
            put(Byte.class, 13);
            put(Character.class, 14);
            put(Boolean.class, 15);
            put(Double.class, 16);
            put(Float.class, 17);
        }};
        private OutputStream out;

        public Encoder(OutputStream os) {
            assert os != null;
            out = os;
        }

        public void encode(Object o) throws IOException, SerializationError {
            assert o2i != null : "Encoder class is not reusable";
            encode(o, true);
        }

        public void close() throws IOException {
            o2i = null;
            c2i = null;
            if (out != null) {
                out.flush();
            }
            out = null;
        }

        public void encode(Object o, boolean encodeType) throws IOException, SerializationError {
            if (o == null) {
                out.write(NULL);
                return;
            } else if (o.equals(Boolean.TRUE)) {
                out.write(TRUE);
                return;
            } else if (o.equals(Boolean.FALSE)) {
                out.write(FALSE);
                return;
            } else if (o.equals(INTEGER_ZERO)) {
                out.write(ZERO);
                return;
            } else if (o.equals(INTEGER_ONE)) {
                out.write(ONE);
                return;
            }
            int ref = o2i.get(o);
            if (ref != 0) {
                out.write(REFERENCE);
                writeLong(ref);
                return;
            } else {
                o2i.put(o, o2i.size() + 1);
            }
            if (o instanceof Collection) {
                int cr = getClassReference(o.getClass());
                out.write(COLLECTION);
                writeClassRef(cr);
                Collection c = (Collection)o;
                writeLong(c.size());
                for (Iterator i = c.iterator(); i.hasNext();) {
                    Object e = i.next();
                    encode(e);
                }
            } else if (o instanceof Map) {
                int cr = getClassReference(o.getClass());
                out.write(MAP);
                writeClassRef(cr);
                Map m = (Map)o;
                writeLong(m.size());
                for (Iterator i = m.entrySet().iterator(); i.hasNext();) {
                    Map.Entry e = (Map.Entry)i.next();
                    encode(e.getKey());
                    encode(e.getValue());
                }
            } else if (o.getClass().isArray()) {
                Class type = o.getClass().getComponentType();
                int cr = getClassReference(type);
                out.write(ARRAY);
                writeClassRef(cr);
                int n = Array.getLength(o);
                writeLong(n);
                boolean primitive = type.isPrimitive() || type == String.class;
                for (int i = 0; i < n; i++) {
                    encode(Array.get(o, i), !primitive);
                }
            } else {
                writeAtomic(o, encodeType);
            }
        }

        private int getClassReference(Class c) throws IOException, SerializationError {
            String className = c.getName();
            int cn = c2i.get(c);
            if (cn == 0) {
                cn = c2i.size() + 1;
                writeClassDef(className);
                c2i.put(c, cn);
            }
            return cn;
        }

        private void writeAtomic(Object o, boolean encodeType) throws IOException,
                SerializationError {
            if (encodeType) {
                writeClassRef(getClassReference(o.getClass()));
            }
            if (o instanceof String) {
                writeString(((String)o));
            } else if (o instanceof StringBuffer) {
                writeString(o.toString());
            } else if (o instanceof BigInteger) {
                writeBytes(((BigInteger)o).toByteArray());
            } else if (o instanceof Long) {
                writeLong(((Long)o).longValue());
            } else if (o instanceof Integer) {
                writeLong(((Integer)o).intValue());
            } else if (o instanceof Short) {
                writeLong(((Short)o).shortValue());
            } else if (o instanceof Byte) {
                writeLong(((Byte)o).byteValue());
            } else if (o instanceof Character) {
                writeLong((int)((Character)o).charValue());
            } else if (o instanceof Boolean) {
                writeLong(((Boolean)o).booleanValue() ? 1 : 0);
            } else if (o instanceof Double) {
                writeLong(Double.doubleToLongBits(((Double)o).doubleValue()));
            } else if (o instanceof Float) {
                writeLong(Float.floatToIntBits(((Float)o).floatValue()));
            } else if (o instanceof Date) {
                writeLong(((Date)o).getTime());
            } else {
                writeFields(o);
            }
        }

        private void writeFields(Object o) throws IOException, SerializationError {
            Map m = getFields(o);
            int n = m.size();
            writeLong(n);
            for (Iterator i = m.values().iterator(); i.hasNext(); ) {
                Field f = (Field)i.next();
                writeString(f.getName());
                encode(getField(f, o));
            }
        }

        private void writeClassDef(String className) throws IOException, SerializationError {
            out.write(CLASS_DEFINITION);
            writeString(className);
        }

        private void writeLong(long v) throws IOException {
            int tag;
            long data;
            int bytes = 0;
            if (v == Long.MIN_VALUE) {
                data = 0;
                tag = 0xFF; // special case Long.MIN_VALUE
            } else {
                if (v < 0) {
                    data = -v;
                    tag = 0x80;
                } else {
                    tag = 0;
                    data = v;
                }
                assert data >= 0;
                if (data < 0x40) {
                    tag = tag | (int)data; // 0..0x3F represent themselves
                } else {
                    long b = data;
                    while (b != 0) {
                        b = b >>> 8;
                        bytes++;
                    }
                    assert bytes <= 8;
                    tag = tag | 0x40 | bytes;
                }
            }
            out.write(tag);
            for (int i = 0; i < bytes; i++) {
                int b = ((int)data) & 0xFF;
                assert b >= 0 && b <= 0xFF;
                out.write(b);
                data >>>= 8;
            }
        }

        private void writeClassRef(int cn) throws IOException {
            writeLong(cn);
        }

        private void writeString(String s) throws IOException, SerializationError {
            try {
                writeBytes(s.getBytes("UTF8"));
            } catch (UnsupportedEncodingException e) {
                throw new SerializationError(e); // this should not ever happen
            }
        }

        private void writeBytes(byte[] bytes) throws IOException {
            writeLong(bytes.length);
            out.write(bytes);
        }
    }

    public static final class Decoder {

        private InputStream in;

        private ArrayList r2o = new ArrayList() {{
            add(new Object());
        }};
        private ArrayList r2c = new ArrayList() {{
            add(new Object());
            add(String.class);
            add(long.class);
            add(int.class);
            add(short.class);
            add(byte.class);
            add(char.class);
            add(boolean.class);
            add(double.class);
            add(float.class);
            add(Long.class);
            add(Integer.class);
            add(Short.class);
            add(Byte.class);
            add(Character.class);
            add(Boolean.class);
            add(Double.class);
            add(Float.class);
        }};

        public Decoder(InputStream is) {
            assert is != null;
            in = is;
        }

        public Object decode() throws IOException, SerializationError {
            assert r2o != null : "Decoder class is not reusable";
            return decode(null);
        }

        public void close() {
            r2o = null;
            r2c = null;
            in = null;
        }

        private static Class classForName(String name) throws SerializationError {
            if ("long".equals(name)) {
                return long.class;
            } else if ("int".equals(name)) {
                return int.class;
            } else if ("short".equals(name)) {
                return short.class;
            } else if ("byte".equals(name)) {
                return byte.class;
            } else if ("char".equals(name)) {
                return char.class;
            } else if ("boolean".equals(name)) {
                return boolean.class;
            } else if ("double".equals(name)) {
                return double.class;
            } else if ("float".equals(name)) {
                return float.class;
            } else {
                try {
                    return Class.forName(name);
                } catch (ClassNotFoundException e) {
                    throw new SerializationError(e);
                }
            }
        }

        private Object decode(Class type) throws IOException, SerializationError {
            int tag = in.read();
            while (tag == CLASS_DEFINITION) {
                String s = readString();
                r2c.add(classForName(s));
                tag = in.read();
            }
            Object o;
            if (tag == NULL) {
                return null;
            } else if (tag == TRUE) {
                return Boolean.TRUE;
            } else if (tag == FALSE) {
                return Boolean.FALSE;
            } else if (tag == ZERO) {
                return INTEGER_ZERO;
            } else if (tag == ONE) {
                return INTEGER_ONE;
            } else if (tag == REFERENCE) {
                int ref = readInt();
                assert ref != 0;
                o = r2o.get(ref);
            } else if (tag == COLLECTION) {
                Class c = readClassRef();
                int n = readInt();
                o = newInstance(c);
                r2o.add(o);
                Collection a = (Collection)o;
                for (int i = 0; i < n; i++) {
                    a.add(decode());
                }
            } else if (tag == MAP) {
                Class c = readClassRef();
                int n = readInt();
                o = newInstance(c);
                r2o.add(o);
                Map m = (Map)o;
                for (int i = 0; i < n; i++) {
                    Object k = decode();
                    m.put(k, decode());
                }
            } else if (tag == ARRAY) {
                Class c = readClassRef();
                int n = readInt();
                o = Array.newInstance(c, n);
                r2o.add(o);
                Class etype = c.isPrimitive() || String.class.equals(c) ? c : null;
                for (int i = 0; i < n; i++) {
                    Array.set(o, i, decode(etype));
                }
            } else {
                o = readAtomic(tag, type);
                r2o.add(o);
            }
            assert o != null;
            return o;
        }

        private Class readClassRef() throws IOException {
            int cr = readInt();
            return (Class)r2c.get(cr);
        }

        private Class readClassRef(int tag) throws IOException {
            int cr = (int)readLong(tag);
            return (Class)r2c.get(cr);
        }

        private Object readAtomic(int tag, Class type) throws IOException, SerializationError {
            boolean usetag = type != null;
            if (type == null) {
                type = readClassRef(tag);
            }
            if (type == String.class) {
                return readString(tag, usetag);
            } else if (type == StringBuffer.class) {
                return new StringBuffer(readString(tag, usetag));
            } else if (type == BigInteger.class) {
                return new BigInteger(readBytes(tag, usetag));
            }
            long data = usetag ? readLong(tag) : readLong();
            if (type == Long.class || type == long.class) {
                return new Long(data);
            } else if (type == Integer.class || type == int.class) {
                return new Integer((int)data);
            } else if (type == Short.class || type == short.class) {
                return new Short((short)data);
            } else if (type == Byte.class || type == byte.class) {
                return new Byte((byte)data);
            } else if (type == Character.class || type == char.class) {
                return new Character((char)data);
            } else if (type == Boolean.class || type == boolean.class) {
                return data == 0 ? Boolean.FALSE : Boolean.TRUE;
            } else if (type == Double.class || type == double.class) {
                return new Double(Double.longBitsToDouble(data));
            } else if (type == Float.class || type == float.class) {
                return new Float(Float.intBitsToFloat((int)data));
            } else if (type == Date.class) {
                return new Date(data);
            } else {
                return readFields((int)data, type);
            }
        }

        private Object readFields(int n, Class type) throws IOException, SerializationError {
            Object o = newInstance(type);
            Map m = getFields(o);
            assert n == m.size() : type.getName() +  " fields " + m.size() + " expected " + n;
            for (int i = 0; i < n; i++) {
                String name = readString();
                Object value = decode();
                Field f = (Field)m.get(name);
                setField(f, o, value);
            }
            return o;
        }

        private long readLong() throws IOException {
            return readLong(in.read() & 0xFF);
        }

        private long readLong(int tag) throws IOException {
            if (tag == 0xFF) {
                return Long.MIN_VALUE;
            }
            boolean neg = (tag & 0x80) != 0;
            tag = tag & ~0x80;
            if (tag < 0x40) {
                return neg ? -tag : tag;
            }
            assert (tag & 0x40) == 0x40;
            int bytes = tag & ~0x40;
            long data = readLongData(bytes);
            return neg ? -data : data;
        }

        private int readInt() throws IOException {
            int tag = in.read();
            assert tag != 0xFF;
            boolean neg = (tag & 0x80) != 0;
            assert !neg : "readInt is optimization of readLong only used on positive numbers";
            tag = tag & ~0x80;
            if (tag < 0x40) {
                return neg ? -tag : tag;
            }
            assert (tag & 0x40) == 0x40;
            int bytes = tag & ~0x40;
            int data = readIntData(bytes);
            return neg ? -data : data;
        }

        private int readIntData(int bytes) throws IOException {
            assert 0 < bytes && bytes <= 4 : bytes;
            int data = 0;
            int k = 0;
            for (int i = 0; i < bytes; i++) {
                int b = in.read() & 0xFF;
                data = data | (b << k);
                k+= 8;
            }
            return data;
        }

        private long readLongData(long bytes) throws IOException {
            assert 0 < bytes && bytes <= 8 : bytes;
            long data = 0;
            int k = 0;
            for (int i = 0; i < bytes; i++) {
                long b = in.read() & 0xFF;
                data = data | (b << k);
                k+= 8;
            }
            return data;
        }

        private String readString() throws IOException, SerializationError {
            return readString(0, false);
        }

        private String readString(int tag, boolean usetag) throws IOException, SerializationError {
            try {
                return new String(readBytes(tag, usetag), "UTF8");
            } catch (UnsupportedEncodingException e) {
                assert false : "unsupported UTF8?!";
                throw new SerializationError(e);
            }
        }

        private byte[] readBytes(int tag, boolean usetag) throws IOException {
            int n = (int)(usetag ? readLong(tag) : readLong());
            byte[] bytes = new byte[n];
            readFully(bytes);
            return bytes;
        }

        private void readFully(final byte[] buf) throws IOException {
            final int l = buf.length;
            int n = 0;
            do {
                final int r = in.read(buf, n, l - n);
                if (r == -1) {
                    throw new EOFException();
                }
                n += r;
            } while (n < l);
        }

    }

    private static class Object2Int {

        private int mask = 0x3FF;
        private Object[] keys = new Object[mask + 1];
        private int[] values = new int[mask + 1];
        private int size;

        public int size() {
            return size;
        }

        public void put(Object k, int v) {
            assert k != null;
            assert v != 0;
            if (size >= keys.length * 3 / 4) {
                resize((mask + 1) * 2);
            }
            int h = System.identityHashCode(k) & mask;
            for (; ;) {
                Object key = keys[h];
                if (key == k) {
                    break;
                } else if (key == null) {
                    keys[h] = k;
                    size++;
                    break;
                }
                h = (h + 1) % mask;
            }
            values[h] = v;
        }

        public int get(Object k) {
            assert k != null;
            int h = System.identityHashCode(k) & mask;
            for (; ;) {
                Object key = keys[h];
                if (key == k) {
                    return values[h];
                } else if (key == null) {
                    return 0;
                }
                h = (h + 1) % mask;
            }
        }

        private void resize(int n) {
            Object[] k = keys; // save old values
            int[] v = values;
            int s = size;
            {   // make sure we do not leave map in half state OutOfMemoryError:
                Object[] k1 = new Object[n];
                int[] v1 = new int[n];
                keys = k1;
                values = v1;
                mask = n - 1;
                assert (n & mask) == 0 && ((n | mask) + 1) == n * 2;
                size = 0;
            }
            for (int i = 0; i < k.length && size < s; i++) {
                if (k[i] != null) {
                    put(k[i], v[i]);
                }
            }
        }

    }

    private static Object newInstance(Class c) throws SerializationError {
        try {
            return c.newInstance();
        } catch (Throwable e) {
            throw new SerializationError(e);
        }
    }

    private static Object getField(Field f, Object o) throws SerializationError {
        try {
            return f.get(o);
        } catch (Throwable e) {
            throw new SerializationError(e);
        }
    }

    private static void setField(Field f, Object o, Object v) throws SerializationError {
        try {
            f.set(o, v);
        } catch (Throwable e) {
            throw new SerializationError(e);
        }
    }

    private static Map getFields(Object o) {
        synchronized (c2f) {
            Class c = o.getClass();
            Map m = (Map)c2f.get(c);
            if (m == null) {
                m = new HashMap();
                getFields(c, o, m);
                c2f.put(c, m);
            }
            return m;
        }
    }

    private static void getFields(Class c, Object o, Map map) {
        if (c != Object.class && !c.isInterface()) {
            Field[] fields = c.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field f = fields[i];
                try {
                    f.setAccessible(true);
                    getField(f, o);
                } catch (Throwable x) {
                    continue; // field is not accessible: ignore it
                }
                int m = f.getModifiers();
                boolean aes = f.isAccessible() && !f.isEnumConstant() && !f.isSynthetic();
                boolean fst = Modifier.isFinal(m) || Modifier.isStatic(m) || Modifier.isTransient(m);
                if (aes && !fst) {
                    assert !map.containsKey(f.getName()) : "duplicate field: " + f.getName();
                    map.put(f.getName(), f);
                }
            }
            getFields(c.getSuperclass(), o, map);
        }
    }

}
