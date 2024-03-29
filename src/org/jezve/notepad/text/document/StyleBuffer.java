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
 * StyleBuffer implements <tt>MStyleBuffer</tt>.  It maintains <tt>AttributeMap</tt> objects to
 * apply to the text in an <tt>MText</tt> object, and the intervals on which those styles apply.
 * <p/>
 * StyleBuffer stores the intervals on which styles apply in a <tt>RunArray</tt> object (see
 * <tt>RunArray</tt> for more information).  The styles are stored in an array of
 * <tt>AttributeMap</tt> objects.
 * <p/>
 * <tt>RunArray</tt> maintains an array of integers which represent offsets into text. The array
 * has a "positive" region in which offsets are given as positive distances from the start of the
 * text, and a "negative" region in which offsets are given as negative distances from the end of
 * the text. Between the positive and negative regions is a gap, into which new offsets may be
 * inserted. This storage scheme allows for efficient response to a series of editing operations
 * which occur in the same area of the text.
 * <p/>
 * StyleBuffer uses the offsets in <tt>RunArray</tt> as the boundaries of style runs. A style run
 * begins at each offset in <tt>RunArray</tt>, and each style run continues to the next offset.
 * The style array is kept in sync with the array of offsets in <tt>RunArray</tt>; that is, the
 * style which begins at RunArray[i] is stored in StyleArray[i].
 * <p/>
 * The first entry in the <tt>RunArray</tt> is always 0.
 *
 * @author John Raley
 * @see AttributeMap
 * @see MText
 * @see RunArray
 */

final class StyleBuffer {

    /**
     * Creates a new style buffer with length equal to the length of <tt>text</tt>,
     * and with a single run of <tt>defaultStyle</tt>.
     */

    private static final int kInitialSize = 10;
    private RunArray fRunArray;

    private AttributeMap fStyleTable[];

    StyleBuffer(MConstText text, AttributeMap initialStyle) {
        this(text.length(), initialStyle);
    }

    /**
     * Creates a new style buffer with length <tt>initialLength</tt> and with a
     * single run of <tt>defaultStyle</tt>.
     *
     * @param initialLength -
     * @param initialStyle  -
     */
    StyleBuffer(int initialLength, AttributeMap initialStyle) {
        fRunArray = new RunArray(kInitialSize, initialLength);
        fRunArray.fPosEnd = 0;
        fRunArray.fRunStart[0] = 0;

        fStyleTable = new AttributeMap[kInitialSize]; // do I really want to do this???

        fStyleTable[0] = initialStyle;
    }

    /**
     * Shift style and run tables such that the last positive run begins before the given position.
     * Since there is always a run start at 0, this method ensures that the first run will not be shifted.
     * This is called by: <tt>insertText</tt> and <tt>deleteText</tt>.
     *
     * @param pos a position in the text.
     */

    private void shiftTableTo(int pos) {

        if (pos == 0) {
            pos = 1;
        }

        int oldNegStart = fRunArray.fNegStart;
        int oldPosEnd = fRunArray.fPosEnd;

        fRunArray.shiftTableTo(pos);

        if (oldPosEnd > fRunArray.fPosEnd) {
            System.arraycopy(fStyleTable, fRunArray.fPosEnd + 1, fStyleTable, fRunArray.fNegStart,
                    oldPosEnd - fRunArray.fPosEnd);
        }
        else if (oldNegStart < fRunArray.fNegStart) {
            System.arraycopy(fStyleTable, oldNegStart, fStyleTable, oldPosEnd + 1,
                    fRunArray.fNegStart - oldNegStart);
        }
    }

    /**
     * Update the style table to reflect a change in the RunArray's size.
     *
     * @param oldNegStart -
     */
    private void handleArrayResize(int oldNegStart) {
        AttributeMap newStyleTable[] = new AttributeMap[fRunArray.getArrayLength()];
        System.arraycopy(fStyleTable, 0, newStyleTable, 0, fRunArray.fPosEnd + 1);
        System.arraycopy(fStyleTable, oldNegStart, newStyleTable, fRunArray.fNegStart,
                (fRunArray.getArrayLength() - fRunArray.fNegStart));
        fStyleTable = newStyleTable;
    }

    /**
     * Minimize the amount of storage used by this object.
     */
    void compress() {
        int oldNegStart = fRunArray.fNegStart;
        fRunArray.compress();
        if (fRunArray.fNegStart != oldNegStart) {
            handleArrayResize(oldNegStart);
        }
    }

    /**
     * Increase the storage capacity of the style and run tables if no room remains.
     */
    private void expandStyleTableIfFull() {

        if (fRunArray.fPosEnd + 1 == fRunArray.fNegStart) {

            int oldNegStart = fRunArray.fNegStart;
            fRunArray.expandRunTable();
            handleArrayResize(oldNegStart);
        }
    }

    /**
     * Respond to an insertion in the text.  The length of the last style run which
     * begins before <tt>start</tt> is increased by <tt>length</tt>.  The run table
     * is shifted such that the run into which text was inserted is the last positive run.
     * This implementation assumes that all styles propogate.
     *
     * @param start the offset where the insertion began
     * @param limit the number of characters inserted
     */
    public void insertText(int start, int limit) {
        shiftTableTo(start);
        fRunArray.addToCurTextLength(limit - start);
    }

    /**
     * Respond to a deletion in the text.  The last style run before
     * <tt>start</tt> is truncated to end at <tt>start</tt>.  The
     * style run containing (<tt>start</tt>+<tt>length</tt>) is set to begin
     * at (<tt>start</tt>+<tt>length</tt>).  Runs in between are deleted.
     * If the deletion occurs entirely within one style run, the length of the style
     * run is reduced by <tt>length</tt>.
     * This implementation assumes that all styles propogate.
     * This method shifts the run table such that the run in which the delete began
     * is the last positive run.  Other methods depend on this "side effect".
     *
     * @param start the offset where the deletion began
     * @param limit the offset where the deletion stopped
     */
    public void deleteText(int start, int limit) {

        int length = limit - start;

        // An optimization - if a whole run wasn't deleted we don't
        // need to check for run merging, which could be expensive.
        boolean wholeRunDeleted = false;

        shiftTableTo(start);

        int firstRunLimit = fRunArray.getCurTextLength();
        if (fRunArray.fNegStart < fRunArray.getArrayLength()) {
            firstRunLimit += fRunArray.fRunStart[fRunArray.fNegStart];
        }

        if (limit == fRunArray.getCurTextLength()) {
            fRunArray.fNegStart = fRunArray.getArrayLength();
        }
        else if (limit >= firstRunLimit) {

            int end = fRunArray.findRunContaining(limit);
            if (end != fRunArray.fPosEnd) {
                fRunArray.fRunStart[end] = limit - fRunArray.getCurTextLength();
                fRunArray.fNegStart = end;
                wholeRunDeleted = true;
            }
        }

        if (fRunArray.fNegStart != fRunArray.getArrayLength()) {
            if (start == 0 && limit >= firstRunLimit) {
                // the first style run was deleted;  move first "negative" run into
                // first position
                fStyleTable[0] = fStyleTable[fRunArray.fNegStart++];
            }
            else if (wholeRunDeleted) {
                if (fStyleTable[fRunArray.fNegStart].equals(fStyleTable[fRunArray.fPosEnd])) {
                    // merge style runs
                    fRunArray.fNegStart++;
                }
            }
        }

        fRunArray.addToCurTextLength(-length);

        fRunArray.runStartsChanged();
        //System.out.println("In deleteText:  number of style runs = " + numRuns(this));
    }

    /**
     * Arrange style table so that old styles in the provided range are removed, and
     * new styles can be inserted into the insertion gap.
     * After calling this method, new style starts and styles may be placed
     * in the insertion gaps of fRunArray.fStyleStart and fStyleTable.
     *
     * @param start offset in the text where insertion operation begins
     */
    private void prepareStyleInsert(int start) {
        if (start == 0) {
            // fRunArray.fPosEnd should be 0 if we're in this branch.
            if (fRunArray.getCurTextLength() > 0) {
                /* Move first existing style run to negative end of buffer.
                   Don't do this if length==0;  that is, if there is no real
                   style run at 0.
                 */
                fRunArray.fNegStart--;
                fStyleTable[fRunArray.fNegStart] = fStyleTable[0];
                fRunArray.fRunStart[fRunArray.fNegStart] = -fRunArray.getCurTextLength();
            }

            fRunArray.fPosEnd = -1;
        }
        else {

            // consistency check: start should be in current gap
            if (fRunArray.fRunStart[fRunArray.fPosEnd] >= start) {
                throw new Error("Inconsistent state!  Start should be within insertion gap.");
            }

            int endOfInsertionGap = fRunArray.getCurTextLength();
            if (fRunArray.fNegStart < fRunArray.getArrayLength()) {
                endOfInsertionGap += fRunArray.fRunStart[fRunArray.fNegStart];
            }

            if (endOfInsertionGap < start) {
                throw new Error("Inconsistent state!  Start should be within insertion gap.");
            }

            // if no break at start (on negative end of buffer) make one

            if (endOfInsertionGap != start) {

                // split style run in insertion gap

                expandStyleTableIfFull();

                fRunArray.fNegStart--;
                fStyleTable[fRunArray.fNegStart] = fStyleTable[fRunArray.fPosEnd];
                fRunArray.fRunStart[fRunArray.fNegStart] = start - fRunArray.getCurTextLength();

                //System.out.println("splitting run.");
            }
        }
    }

    public boolean modifyStyles(int start, int limit, StyleModifier modifier, int[] damagedRange) {

        if (limit == start) {
            return false;
        }

        shiftTableTo(start);

        int currentRunStart = start;
        AttributeMap oldStyle;
        AttributeMap mergeStyle = fStyleTable[fRunArray.fPosEnd];

        if (fRunArray.fNegStart < fRunArray.getArrayLength() &&
                fRunArray.fRunStart[fRunArray.fNegStart] + fRunArray.getCurTextLength() == start) {

            oldStyle = fStyleTable[fRunArray.fNegStart];
            ++fRunArray.fNegStart;
        }
        else {
            oldStyle = mergeStyle;
        }

        boolean modifiedAnywhere = false;
        for (; ;) {

            boolean modified = false;

            // push new style into gap on positive side
            AttributeMap newStyle = modifier.modifyStyle(oldStyle);
            if (damagedRange != null && !newStyle.equals(oldStyle)) {
                modified = modifiedAnywhere = true;
                damagedRange[0] = Math.min(currentRunStart, damagedRange[0]);
            }

            if (!newStyle.equals(mergeStyle)) {

                if (currentRunStart != 0) {
                    expandStyleTableIfFull();
                    ++fRunArray.fPosEnd;
                }

                fStyleTable[fRunArray.fPosEnd] = newStyle;
                fRunArray.fRunStart[fRunArray.fPosEnd] = currentRunStart;
            }

            mergeStyle = newStyle;

            int nextRunStart = fRunArray.getLogicalRunStart(fRunArray.fNegStart);

            if (limit > nextRunStart) {
                oldStyle = fStyleTable[fRunArray.fNegStart];
                currentRunStart = nextRunStart;
                if (modified) {
                    damagedRange[1] = Math.max(currentRunStart, damagedRange[1]);
                }
                ++fRunArray.fNegStart;
            }
            else {
                if (limit < nextRunStart && !oldStyle.equals(mergeStyle)) {
                    expandStyleTableIfFull();
                    ++fRunArray.fPosEnd;
                    fStyleTable[fRunArray.fPosEnd] = oldStyle;
                    fRunArray.fRunStart[fRunArray.fPosEnd] = limit;
                }
                if (modified) {
                    damagedRange[1] = Math.max(limit, damagedRange[1]);
                }
                break;
            }
        }

        // merge last run if needed
        if ((fRunArray.fNegStart < fRunArray.getArrayLength()) &&
                (fStyleTable[fRunArray.fNegStart].equals(fStyleTable[fRunArray.fPosEnd]))) {
            fRunArray.fNegStart++;
        }

        fRunArray.runStartsChanged();

        return modifiedAnywhere;
    }

    public int styleStart(int pos) {

        if (pos == fRunArray.getCurTextLength()) {
            return pos;
        }

        return fRunArray.getLogicalRunStart(fRunArray.findRunContaining(pos));
    }

    public int styleLimit(int pos) {

        if (pos == fRunArray.getCurTextLength()) {
            return pos;
        }

        int run = fRunArray.findRunContaining(pos);

        if (run == fRunArray.fPosEnd) {
            run = fRunArray.fNegStart;
        }
        else {
            ++run;
        }

        return fRunArray.getLogicalRunStart(run);
    }

    /**
     * Return style at location <tt>pos</tt>.
     *
     * @param pos an offset into the text
     * @return the style of the character at <tt>offset</tt>
     */
    public AttributeMap styleAt(int pos) {

        return fStyleTable[fRunArray.findRunContaining(pos)];
    }

    /*
    * Set run start, run length, and run value in an iterator.  This method is
    * only called by a <tt>StyleRunIterator</tt>.
    * @param pos an offset into the text.  The iterator's run start and run limit are
    * set to the run containing <tt>pos</tt>.
    * @param iter the iterator to set
    */
    void setIterator(int pos, StyleRunIterator iter) {

        if ((pos < 0) || (pos > fRunArray.getCurTextLength())) {

            iter.set(null, 0, 0, kNoRun);
            return;
        }

        int run = fRunArray.findRunContaining(pos);

        setIteratorUsingRun(run, iter);
    }

    /**
     * Set run start, run length, and run value in an iterator.  This method is
     * only called by a <tt>StyleRunIterator</tt>.
     *
     * @param run  the index of the run to which the iterator should be set
     * @param iter the iterator to set
     */
    private void setIteratorUsingRun(int run, StyleRunIterator iter) {

        int lastValidRun = fRunArray.lastRun();

        if (run < 0 || run > lastValidRun) {

            iter.set(null, 0, 0, kNoRun);
            return;
        }

        if (run == fRunArray.fPosEnd + 1) {
            run = fRunArray.fNegStart;
        }
        else if (run == fRunArray.fNegStart - 1) {
            run = fRunArray.fPosEnd;
        }

        int runStart = fRunArray.fRunStart[run];
        if (runStart < 0) {
            runStart += fRunArray.getCurTextLength();
        }

        AttributeMap style = fStyleTable[run];

        int nextRun;

        if (run == fRunArray.fPosEnd) {
            nextRun = fRunArray.fNegStart;
        }
        else {
            nextRun = run + 1;
        }

        int runLimit;

        if (nextRun >= fRunArray.getArrayLength()) {
            runLimit = fRunArray.getCurTextLength();
        }
        else {
            runLimit = fRunArray.fRunStart[nextRun];
            if (runLimit < 0) {
                runLimit += fRunArray.getCurTextLength();
            }
        }
        iter.set(style, runStart, runLimit, run);
    }

    /**
     * Replace style runs between offsets <tt>start</tt> and <tt>limit</tt> with styles in
     * <tt>iter</tt>.  This method can be used to perform a "paste" operation.
     *
     * @param start the offset where the replacement begins
     * @param limit the offset where the replacement ends
     */
    public void replace(int start, int limit, MConstText srcText, int srcStart, int srcLimit) {
        deleteText(start, limit);
        if (srcStart == srcLimit) {
            return;
        }
        prepareStyleInsert(start);
        for (int j2 = srcStart; j2 < srcLimit; j2 = srcText.characterStyleLimit(j2)) {
            AttributeMap attributeMap = srcText.characterStyleAt(j2);
            if (fRunArray.fPosEnd < 0 || !fStyleTable[fRunArray.fPosEnd].equals(attributeMap)) {
                expandStyleTableIfFull();
                fRunArray.fPosEnd++;
                fRunArray.fRunStart[fRunArray.fPosEnd] = j2 - srcStart + start;
                fStyleTable[fRunArray.fPosEnd] = attributeMap;
            }
        }
        fRunArray.addToCurTextLength(srcLimit - srcStart);
        if (fRunArray.fNegStart < fRunArray.getArrayLength() &&
                fStyleTable[fRunArray.fNegStart].equals(fStyleTable[fRunArray.fPosEnd])) {
            fRunArray.fNegStart++;
        }
    }

    private static final int kNoRun = -42; // iterator use

    private final class StyleRunIterator /*implements MStyleRunIterator*/ {

        StyleRunIterator(int start, int limit) {
            reset(start, limit, start);
        }

        public void reset(int start, int limit, int pos) {
            fStart = start;
            fLimit = limit;
            setIterator(fStart, this);
        }

        public boolean isValid() {
            return fStyle != null;
        }

        public void next() {
            if (fRunLimit < fLimit) {
                fCurrentRun++;
                setIteratorUsingRun(fCurrentRun, this);
            }
            else {
                set(null, 0, 0, kNoRun);
            }
        }

        public void prev() {
            if (fRunStart > fStart) {
                fCurrentRun--;
                setIteratorUsingRun(fCurrentRun, this);
            }
            else {
                set(null, 0, 0, kNoRun);
            }
        }

        public void set(int pos) {
            if (pos >= fStart && pos < fLimit) {
                setIterator(pos, this);
            }
            else {
                set(null, 0, 0, kNoRun);
            }
        }

        void set(AttributeMap style, int start, int limit, int currentRun) {
            fStyle = style;
            fCurrentRun = currentRun;
            fRunStart = start < fStart ? fStart : start;
            fRunLimit = limit > fLimit ? fLimit : limit;
        }

        public void reset(int start, int limit) {
            reset(start, limit, start);
        }

        public void first() {
            set(fStart);
        }

        public void last() {
            set(fLimit - 1);
        }

        public int rangeStart() {
            return fStart;
        }

        public int rangeLimit() {
            return fLimit;
        }

        public int rangeLength() {
            return fLimit - fStart;
        }

        public AttributeMap style() {
            return fStyle;
        }

        public int runStart() {
            return fRunStart;
        }

        public int runLimit() {
            return fRunLimit;
        }

        public int runLength() {
            return fRunLimit - fRunStart;
        }

        private int fStart;
        private int fLimit;
        private AttributeMap fStyle;
        private int fRunStart;
        private int fRunLimit;
        private int fCurrentRun;
    }
}