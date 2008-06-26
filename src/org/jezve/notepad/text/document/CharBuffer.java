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

package org.jezve.notepad.text.document;

import java.text.CharacterIterator;

final class CharBuffer {

    private static final int kGrowSize = 0x80; // small size for testing

    transient Validation fValidation = null;
    private char[] fArray;
    transient private int fArraySize;
    transient private int fGap;

    public CharBuffer() {
    }

    public CharBuffer(int capacity) {
        fArray = allocate(capacity);
    }

    private void invalidate() {
        if (fValidation != null) {
            fValidation.invalidate();
            fValidation = null;
        }
    }

    // not ThreadSafe - could end up with two Validations being generated
    private Validation getValidation() {
        if (fValidation == null) {
            fValidation = new Validation();
        }
        return fValidation;
    }

    /*
     * Replace the chars from start to limit with the chars from srcStart to srcLimit in srcChars.
     * This is the core routine for manipulating the buffer.
     */
    public void replace(int start, int limit, char[] srcChars, int srcStart, int srcLimit) {
        invalidate();
        int dstLength = limit - start;
        int srcLength = srcLimit - srcStart;

        if (dstLength < 0 || srcLength < 0) {
            throw new IllegalArgumentException(
                    "replace(int start, int limit, char[] srcChars, int srcStart, int srcLimit)");
        }

        int gapAlloc = 0;
        if (srcChars == null) {
            gapAlloc = srcLength;
            srcLength = 0;
        }

        int newSize = fArraySize - dstLength + srcLength;

        if (fArray == null) {
            if (start != 0 || limit != 0) {
                throw new IllegalArgumentException(
                        "replace(int start, int limit, char[] srcChars, int srcStart, int srcLimit)");
            }
            if (newSize + gapAlloc > 0) {
                fArray = allocate(newSize + gapAlloc);
                if (srcLength > 0) {
                    System.arraycopy(srcChars, srcStart, fArray, 0, srcLength);
                    fArraySize = srcLength;
                    fGap = srcLength;
                }
            }
        }
        else {
            int newGap = start + srcLength;
            int gapLimit = fArray.length - fArraySize + fGap;

            if (newSize + gapAlloc > fArray.length) {
                char[] temp = allocate(newSize + gapAlloc);

                //move stuff at beginning that we aren't writing over
                if (start > 0) {
                    at(0, start, temp, 0);
                }
                //move stuff from src array that we are copying
                if (srcLength > 0) {
                    System.arraycopy(srcChars, srcStart, temp, start, srcLength);
                }
                //move stuff at end that we aren't copying over
                if (limit < fArraySize) {
                    at(limit, fArraySize, temp, temp.length - newSize + newGap);
                    //change 7-23-96
                    //    at(limit, fArraySize - limit, temp, temp.length - newSize + newGap);
                }

                fArray = temp;
            }
            else {
                if (start > fGap) {
                    System.arraycopy(fArray, gapLimit, fArray, fGap, start - fGap);
                }
                if (limit < fGap) {
                    System.arraycopy(fArray, limit, fArray, fArray.length - newSize + newGap,
                            fGap - limit);
                }
                if (srcLength > 0) {
                    System.arraycopy(srcChars, srcStart, fArray, start, srcLength);
                }
            }

            fArraySize = newSize;
            fGap = newGap;
        }
    }

    /* Replace the chars from start to limit with the chars from srcStart to srcLimit in srcString.
       This implements optimizations for null text or inserting text that fits at the gap,
       and defaults to call the core replace routine if these optimizations fail.
     */
    public void replace(int start, int limit, String srcString, int srcStart, int srcLimit) {
        invalidate();
        int length = limit - start;
        int srcLength = srcLimit - srcStart;

        if (fArray == null) {
            if (start != 0 || limit != 0) {
                throw new IllegalArgumentException(
                        "replace(int start, int limit, String srcString, int srcStart, int srcLimit)");
            }
            if (srcLength > 0) {
                fArray = allocate(srcLength);
                srcString.getChars(srcStart, srcLimit, fArray, 0);
                fArraySize = srcLength;
                fGap = srcLength;
            }
        }
        else {
            if (start == fGap && fArray.length >= fArraySize - length + srcLength) {
                if (srcLimit > 0) {
                    srcString.getChars(srcStart, srcLimit, fArray, fGap);
                    fGap += srcLength;
                }
                fArraySize += srcLength - length;
            }
            else {
                replace(start, limit, srcString != null ? srcString.toCharArray() : null, srcStart,
                        srcLimit);
            }
        }
    }

    public void replace(int start, int limit, MConstText srcText, int srcStart, int srcLimit) {
        invalidate();
        int length = limit - start;
        int srcLength = srcLimit - srcStart;

        if (fArray == null) {
            if (start != 0 || limit != 0) {
                throw new IllegalArgumentException(
                        "replace(int start, int limit, String srcString, int srcStart, int srcLimit)");
            }
            if (srcLength > 0) {
                fArray = allocate(srcLength);
                srcText.extractChars(srcStart, srcLimit, fArray, 0);
                fArraySize = srcLength;
                fGap = srcLength;
            }
        }
        else {
            if (start == fGap && fArray.length >= fArraySize - length + srcLength) {
                if (srcLimit > 0) {
                    srcText.extractChars(srcStart, srcLimit, fArray, fGap);
                    fGap += srcLength;
                }
                fArraySize += srcLength - length;
            }
            else {
                char[] temp = srcLength == 0 ? null : new char[srcLength];
                if (temp != null) {
                    srcText.extractChars(srcStart, srcLimit, temp, 0);
                }
                replace(start, limit, temp, 0, srcLimit - srcStart);
            }
        }
    }

    /*
     * Replace the chars from start to limit with srcChar.
     * 
     * This implements optimizations for null text or replacing a character that fits into the gap,
     * and defaults to call the core replace routine if these optimizations fail.
     */

    public void replace(int start, int limit, char srcChar) {
        invalidate();
        if (fArray == null) {
            if (start != 0 || limit != 0) {
                throw new IllegalArgumentException("replace(int start, int limit, char srcChar)");
            }
            fArray = allocate(1);
            fArray[0] = srcChar;
            fArraySize = 1;
            fGap = 1;
        }
        else {
            int length = limit - start;
            if (start == fGap && fArray.length > fArraySize - length) {
                fArray[fGap] = srcChar;
                fGap += 1;
                fArraySize += 1 - length;
            }
            else {
                replace(start, limit, new char[]{srcChar}, 0, 1);
            }
        }
    }

    public char at(int pos) {
        if (pos < 0 || pos >= fArraySize) {
            throw new IllegalArgumentException();
        }
        return pos < fGap ? fArray[pos] : fArray[fArray.length - fArraySize + pos];
    }

    /*
     * Copy the chars from start to limit to dst starting at dstStart.
     */
    public void at(int start, int limit, char[] dst, int dstStart) {
        int length = limit - start;

        if (start < 0 || limit < start || limit > fArraySize) {
            throw new IllegalArgumentException();
        }

        if (limit <= fGap) {
            System.arraycopy(fArray, start, dst, dstStart, length);
        }
        else if (start >= fGap) {
            System.arraycopy(fArray, fArray.length - fArraySize + start, dst, dstStart, length);
        }
        else {
            System.arraycopy(fArray, start, dst, dstStart, fGap - start);
            System.arraycopy(fArray, fArray.length - fArraySize + fGap, dst,
                    dstStart + fGap - start, limit - fGap);
        }
    }

    public final int length() {
        return fArraySize;
    }

    public final int capacity() {
        return fArray != null ? fArray.length : 0;
    }

    /*
     * Reserve capacity chars at start. Utility to optimize a sequence of operations at start.
     */
    public void reserveCapacity(int start, int capacity) {
        replace(start, start, (char[])null, 0, capacity);
    }

    /**
     * Minimize the storage used by the buffer.
     */
    public void compress() {
        invalidate();
        if (fArraySize == 0) {
            fArray = null;
            fGap = 0;
        }
        else if (fArraySize != fArray.length) {
            char[] temp = new char[fArraySize];
            at(0, fArraySize, temp, 0);
            fArray = temp;
            fGap = fArraySize;
        }
    }

    /**
     * Display the buffer.
     */
    public String toString() {
        if (fArray != null) {
            return new StringBuffer()
                    .append("limit: ").append(fArray.length)
                    .append(", size: ").append(fArraySize)
                    .append(", gap: ").append(fGap)
                    .append(", ").append(fArray, 0, fGap)
                    .append(fArray, fArray.length - fArraySize + fGap, fArraySize - fGap)
                    .toString();
        }
        else {
            return new String("The buffer is empty.");
        }
    }

    public CharacterIterator createCharacterIterator(int start, int limit) {
        Validation val = getValidation();
        return new Iterator(start, limit, fArray, fArraySize, fGap, val);
    }

    /*
     * The resizing algorithm. Return a value >= minSize.
     */
    protected int allocation(int minSize) {
        //    return (minSize + kGrowSize) & ~(kGrowSize - 1);
        return minSize < kGrowSize ? kGrowSize : (minSize * 2 + kGrowSize) & ~(kGrowSize - 1);
    }

    /*
     * Allocate a new character array of limit >= minSize.
     */
    protected char[] allocate(int minSize) {
        return new char[allocation(minSize)];
    }

    static final class Iterator implements CharacterIterator, Cloneable {
        private int start;
        private int limit;
        private int current;
        private char data[];
        private int gap;
        private int gapLength;
        private Validation valid;

        Iterator(int start, int limit, char[] storage, int length, int gap, Validation v) {
            if (start > limit) {
                throw new IllegalArgumentException("start > limit");
            }
            this.start = start;
            this.limit = limit;
            current = this.start;
            data = storage;
            this.gap = gap;
            gapLength = (storage == null ? 0 : storage.length) - length;
            this.valid = v;
        }

        private void checkValidation() {
            if (!valid.isValid()) {
                throw new Error("Iterator is no longer valid");
            }
        }

        public char first() {
            return setIndex(start);
        }

        public char last() {
            return setIndex(limit - 1);
        }

        public char current() {
            checkValidation();
            if (current < start || current >= limit) {
                return DONE;
            }
            int i = (current < gap) ? current : (current + gapLength);
            return data[i];
        }

        public char next() {
            checkValidation();
            current++;
            if (current >= limit) {
                current = limit;
                return DONE;
            }
            int i = (current < gap) ? current : (current + gapLength);
            return data[i];
        }

        public char previous() {
            current--;
            if (current >= start) {
                return current();
            }
            current = start;
            return DONE;
        }

        public char setIndex(int i) {
            if (i < start || i > limit) {
                throw new IllegalArgumentException("Invalid position");
            }
            current = i;
            return current();
        }

        public int getBeginIndex() {
            return start;
        }

        public int getEndIndex() {
            return limit;
        }

        public int getIndex() {
            return current;
        }

        public Object clone() {
            try {
                return super.clone();
            }
            catch (CloneNotSupportedException e) {
                return null;
            }
        }
    }

    static final class Validation {
        private boolean fIsValid = true;

        boolean isValid() {
            return fIsValid;
        }

        void invalidate() {
            fIsValid = false;
        }
    }
}
