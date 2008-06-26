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

/**
 * This class maintains intervals within a piece of text.  Interval boundaries
 * are stored in the fRunStart array.  Interval boundaries may have a
 * positive or negative representation.  A positive boundary is given as an offset
 * from 0.  A negative boundary is given as a negative offset from the ned of the text.
 * The RunArray stores positive boundaries in the entries [0, fPosEnd], and negative
 * boundaries in the entries [fNegStart, fLength).  New boundaries may be inserted into
 * the undefined middle of the RunArray.  If fPosEnd < 0, there are no positive entries.
 * If fNegStart >= fRunArray.length, there are no negative netries.  It's possible to have
 * a runarray with neither positive or negative entries.
 * <p/>
 * As an example of how the RunArray works, consider a piece of text with 5 intervals,
 * where each interval is 3 characters in length.  The RunArray for this text could
 * look like:
 * fCurTextLength = 15, fPosEnd = 5, fNegStart = 10,
 * fRunStart = { 0, 3, 6, 9, 12, U, U, U, U, U };
 * where U is an undefined array element.
 * <p/>
 * An equivalent representation would be:
 * fCurTextLength = 15, fPosEnd = 3, fNegStart = 8,
 * fRunStart = { 0, 3, 6, U, U, U, U, U, -6, -3 };
 * <p/>
 * The RunArray class is used in the StyleBuffer and the ParagraphBuffer.  In the StyleBuffer,
 * the entries in fRunStart give the offsets where style runs begin.  In the
 * ParagraphBuffer, the fRunStart entries store offsets of paragraph breaks.
 * <p/>
 * This class provides methods for shifting the run table to a particular position, expanding the
 * run table, and returning the index of the run containing a particular offset in the text.  All
 * other functionality is implemented in the RunArray clients.
 * <p/>
 * RunArray uses FastIntBinarySearch for searches.  The searches are constructed on demand in
 * the findRunContaining method.  The searches are invalidated when the run array is shifted;
 * however, the RunArray can be modified by other classes.  Thus, if another class modifies
 * the entries in fRunArray, or modifies fPosEnd or fNegStart, it is responsible for
 * calling runStartsChanged.
 */

final class RunArray {

    int[] fRunStart;
    private int fCurTextLength;
    int fPosEnd, fNegStart;

    transient private FastIntBinarySearch fPosSearch;
    transient private boolean fPosSearchValid;
    transient private FastIntBinarySearch fNegSearch;
    transient private boolean fNegSearchValid;

    RunArray(int initialSize, int curTextLength) {

        fRunStart = new int[initialSize];
        fCurTextLength = curTextLength;
        fPosEnd = -1;
        fNegStart = initialSize;

        fPosSearch = new FastIntBinarySearch(fRunStart, 0, 1);
        fNegSearch = new FastIntBinarySearch(fRunStart, 0, 1);
        fPosSearchValid = fNegSearchValid = false;
    }

    public int getCurTextLength() {
        return fCurTextLength;
    }

    public void setCurTextLength(int curTextLength) {
        fCurTextLength = curTextLength;
    }

    public void addToCurTextLength(int delta) {
        fCurTextLength += delta;
    }

    public void runStartsChanged() {
        fPosSearchValid = fNegSearchValid = false;
    }

    // Returns the index of the last valid run.
    int lastRun() {
        return (fNegStart == fRunStart.length) ? fPosEnd : fRunStart.length - 1;
    }

    // Returns the length of the run array.  Replaces old fLength member.
    int getArrayLength() {
        return fRunStart.length;
    }

    // Shifts style table such that the last positive run starts before pos.
    void shiftTableTo(int pos) {
        int oldPosEnd = fPosEnd;
        while (fPosEnd >= 0 && fRunStart[fPosEnd] >= pos) {
            fNegStart--;
            fRunStart[fNegStart] = fRunStart[fPosEnd] - fCurTextLength;
            fPosEnd--;
        }
        pos -= fCurTextLength;
        while (fNegStart < fRunStart.length && fRunStart[fNegStart] < pos) {
            fPosEnd++;
            fRunStart[fPosEnd] = fRunStart[fNegStart] + fCurTextLength;
            fNegStart++;
        }
        if (oldPosEnd != fPosEnd) {
            fPosSearchValid = fNegSearchValid = false;
        }
    }

    /*
     * Returns index of style run containing pos.  If first style run starts before
     * pos, -1 is returned.  If pos is greater than text length, lastrun is returned.
     */
    int findRunContaining(int pos) {

        FastIntBinarySearch search;
        final int length = fRunStart.length;

        if (fNegStart < length && (pos - fCurTextLength >= fRunStart[fNegStart])) {

            pos -= fCurTextLength;

            if (!fNegSearchValid) {
                fNegSearch.setData(fRunStart, fNegStart, length - fNegStart);
            }
            search = fNegSearch;
        }
        else if (fPosEnd >= 0) {

            if (!fPosSearchValid) {
                fPosSearch.setData(fRunStart, 0, fPosEnd + 1);
            }
            search = fPosSearch;
        }
        else {
            return -1;
        }
        return search.findIndex(pos);
    }

    int getLogicalRunStart(int run) {

        if (run == -1) {
            return 0;
        }
        else if (run == fRunStart.length) {
            return fCurTextLength;
        }
        else {
            if (run <= fPosEnd) {
                return fRunStart[run];
            }
            else if (run >= fNegStart) {
                return fRunStart[run] + fCurTextLength;
            }
            else {
                throw new IllegalArgumentException("Illegal run");
            }
        }
    }

    // Increases size of run table.  Current implementation doubles the run table's size.
    void expandRunTable() {
        resizeRunTable(fRunStart.length * 2);
    }

    // Return the minimum number of elements possible in fRunStart.
    private int getMinSize() {
        return Math.max(fPosEnd + (fRunStart.length - fNegStart) + 1, 1);
    }

    void compress() {
        int minSize = getMinSize();
        if (fRunStart.length > minSize) {
            resizeRunTable(minSize);
        }
    }

    private void resizeRunTable(int newSize) {
        if (newSize < getMinSize()) {
            throw new IllegalArgumentException("Attempt to make RunArray too small.");
        }
        final int oldLength = fRunStart.length;
        int newRunStart[] = new int[newSize];
        System.arraycopy(fRunStart, 0, newRunStart, 0, fPosEnd + 1);
        int newNegStart = newRunStart.length - (oldLength - fNegStart);
        System.arraycopy(fRunStart, fNegStart, newRunStart, newNegStart, (oldLength - fNegStart));
        fNegStart = newNegStart;
        fRunStart = newRunStart;
        fPosSearchValid = fNegSearchValid = false;
    }
}
