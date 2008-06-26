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

import java.util.*;

/**
 * This class is a standard implementation of MTabRuler.
 * It can have a finite number of client-specified TabStops.  After
 * the client-specified TabStops, all TabStops have type
 * <code>TabStop.kAuto</code> and are at the autospace intervals.
 *
 * @see TabStop
 */
public final class TabRuler {

    private static final TabStop AUTO_ZERO = new TabStop(0, TabStop.kAuto);

    private TabStop[] fTabs = null;
    private float fAutoSpacing = 36; // every 1/2 inch.

    /**
     * Create a TabRulerUI with only auto tabs, with spacing of 36.
     */
    public TabRuler() {
    }

    /**
     * Create a TabRulerUI with only auto tabs, with the
     * given autoSpacing.
     *
     * @param autoSpacing the autoSpacing for this tab ruler
     */
    public TabRuler(float autoSpacing) {
        fAutoSpacing = autoSpacing;
    }

    /**
     * Create a TabRulerUI.  The first TabStops on the ruler will be
     * the TabStops in the <code>tabs</code> array.  After these tabs all
     * tabs are auto tabs.
     *
     * @param tabs        an array of TabStops.  The TabStops in the array must
     *                    be in strictly increasing order (of positions), and cannot have
     *                    type <code>TabStop.kAuto</code>.
     * @param autoSpacing the autoSpacing interval to use after the last
     *                    client-specified tab.
     */
    public TabRuler(TabStop[] tabs, float autoSpacing) {
        if (tabs.length > 0) {
            validateTabArray(tabs);
            fTabs = (TabStop[])tabs.clone();
        }
        else {
            fTabs = null;
        }
        fAutoSpacing = autoSpacing;
    }

    /* Tabs as provided, then autoSpacing after the last tab to eternity.  Use this constructor when
       munging a ruler, it does no validation on the tabs in the vector. Vector may not be null. */
    TabRuler(Vector v, float autoSpacing) {
        fTabs = tabArrayFromVector(v);
        fAutoSpacing = autoSpacing;
    }

    /* Construct from another ruler. No validation. Ruler may not be null. */
    TabRuler(TabRuler ruler) {
        if (ruler == null) {
            throw new IllegalArgumentException("ruler may not be null");
        }
        fTabs = tabArrayFromVector(vectorFromTabRuler(ruler));
        fAutoSpacing = ruler.autoSpacing();
    }

    /*
     * Return first tab in the ruler.  If an autoTab, it is at position zero, and
     * all subsequent tabs will be autotabs at autoSpacing intervals.
     */
    public TabStop firstTab() {
        if (fTabs != null && fTabs.length > 0) {
            return fTabs[0];
        }
        return AUTO_ZERO;
    }

    /*
     * Return the first tab in the ruler with fPosition > position.  If it is an
     * autotab, it is at an increment of autoSpacing, and all subsequent tabs will be
     * autotabs at autoSpacing intervals.
     *
     * @param position the position of the TabStop returned will be greater than this parameter
     */
    public TabStop nextTab(float position) {
        if (fTabs != null) {
            for (int i = 0; i < fTabs.length; ++i) {
                if (position < fTabs[i].getPosition()) {
                    return fTabs[i];
                }
            }
        }
        if (position >= 4000) { // debug: sanity check
            System.out.println("auto tab past 4000");
        }
        return new TabStop(((position / fAutoSpacing) + 1) * fAutoSpacing, TabStop.kAuto);
    }

    /*
     * Return the interval for autotabs.
     */
    public float autoSpacing() {
        return fAutoSpacing;
    }

    /**
     * Compare this to another Object. Returns true if the object
     * is an MTabRuler with the same autoSpacing and tabs.
     */
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof TabRuler)) return false;

        TabRuler rhs = (TabRuler)o;

        if (fAutoSpacing != rhs.autoSpacing()) {
            return false;
        }

        TabStop rhsTab = rhs.firstTab();
        if (fTabs != null) {
            for (int i = 0; i < fTabs.length; ++i) {
                if (!fTabs[i].equals(rhsTab)) {
                    return false;
                }
                rhsTab = rhs.nextTab(rhsTab.getPosition());
            }
        }
        return rhsTab.getType() == TabStop.kAuto;
    }

    /**
     * Return debug information about this tab ruler.
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer(super.toString());
        buffer.append(" auto: ");
        buffer.append(Float.toString(fAutoSpacing));

        if (fTabs != null) {
            for (int i = 0; i < fTabs.length; ++i) {
                buffer.append(fTabs[i].toString());
            }
        }

        return buffer.toString();
    }

    /* Utility to convert a vector of tabs to an array. */
    private static TabStop[] tabArrayFromVector(Vector v) {
        int count = v.size();
        TabStop[] tabs = new TabStop[count];
        for (int i = 0; i < count; ++i) {
            tabs[i] = (TabStop)v.elementAt(i);
        }
        return tabs;
    }

    /* Utility to convert a ruler to a vector of tabs, for munging. */
    private static Vector vectorFromTabRuler(TabRuler ruler) {
        Vector v = new Vector();
        for (TabStop tab = ruler.firstTab(); tab != null && tab.getType() != TabStop.kAuto;
                tab = ruler.nextTab(tab.getPosition())) {
            v.addElement(tab);
        }
        return v;
    }

    /* Utility to validate an array of tabs.  The array must not be null, must not contain null
       entries, must not be kAuto, and positions must in increasing order. */
    private static void validateTabArray(TabStop[] tabs) {
        float pos = Float.MIN_VALUE;
        for (int i = 0; i < tabs.length; ++i) {
            if (tabs[i].getType() == TabStop.kAuto) {
                throw new IllegalArgumentException("can't explicitly specify an auto tab.");
            }
            float nextpos = tabs[i].getPosition();
            if (nextpos <= pos) {
                throw new IllegalArgumentException("tab positions must be in increasing order.");
            }
            pos = nextpos;
        }
    }

    /**
     * Return a tab ruler identical to the given ruler, except with the
     * given tab added.
     *
     * @param ruler    the original ruler.  The MTabRuler will be the same as
     *                 this except for the additional tab.  <code>ruler</code> is not modified.
     * @param tabToAdd the tab to add to the new tab ruler
     * @return an MTabRuler resulting from this operation
     */
    static TabRuler addTabToRuler(TabRuler ruler, TabStop tabToAdd) {
        if (ruler == null || tabToAdd == null) {
            throw new IllegalArgumentException("ruler and tabToAdd may not be null");
        }

        Vector vector = new Vector();

        float pos = 0;
        boolean added = false;
        for (TabStop tab = ruler.firstTab(); tab.getType() != TabStop.kAuto;
                tab = ruler.nextTab(pos)) {
            pos = tab.getPosition();

            if (!added && pos >= tabToAdd.getPosition()) {
                if (pos == tabToAdd.getPosition()) {
                    tab = null;
                }
                vector.addElement(tabToAdd);
                added = true;
            }

            if (tab != null) {
                vector.addElement(tab);
            }
        }
        if (!added) {
            vector.addElement(tabToAdd);
        }
        return new TabRuler(vector, ruler.autoSpacing());
    }

    /**
     * Return a tab ruler identical to the given ruler, except with the
     * given tab removed.
     *
     * @param ruler    the original ruler.  The MTabRuler will be the same as
     *                 this except for the removed tab.  <code>ruler</code> is not modified.
     * @param position the position of the tab to remove from the new tab ruler
     * @return an MTabRuler resulting from this operation
     */
    static TabRuler removeTabFromRuler(TabRuler ruler, float position) {
        if (ruler == null) {
            throw new IllegalArgumentException("ruler may not be null");
        }

        Vector vector = new Vector();

        float pos = 0;
        boolean removed = false;
        for (TabStop tab = ruler.firstTab(); tab.getType() != TabStop.kAuto;
                tab = ruler.nextTab(pos)) {
            pos = tab.getPosition();

            if (!removed && pos >= position) {
                if (pos == position) {
                    removed = true;
                    continue; // skip this tab and continue with the remainder
                }
                break; // we didn't remove a tab, but skipped position, so don't bother with the rest
            }
            vector.addElement(tab);
        }
        if (!removed) {
            // no change
            return ruler;
        }
        if (vector.size() == 0) {
            return new TabRuler(ruler.autoSpacing());
        }
        return new TabRuler(vector, ruler.autoSpacing());
    }

    /**
     * Return a tab ruler identical to the given ruler, except with the
     * tab at position <code>fromPosition</code> moved to position
     * <code>toPosition</code>.
     *
     * @param ruler        the original ruler.  The MTabRuler will be the same as
     *                     this except for the moved tab.  <code>ruler</code> is not modified.
     * @param fromPosition the position of the tab to move
     * @param toPosition   the new position of the tab
     * @return an MTabRuler resulting from this operation
     */
    static TabRuler moveTabOnRuler(TabRuler ruler, float fromPosition, float toPosition) {
        if (ruler == null) {
            throw new IllegalArgumentException("ruler may not be null");
        }

        Vector vector = new Vector();

        float pos = 0;
        boolean moved = false;
        for (TabStop tab = ruler.firstTab(); tab.getType() != TabStop.kAuto;
                tab = ruler.nextTab(pos)) {
            pos = tab.getPosition();

            if (!moved && pos == fromPosition) {
                moved = true;
                tab = new TabStop(toPosition, tab.getType()); // copy it
            }
            vector.addElement(tab);
        }
        if (!moved) {
            // no change
            return ruler;
        }
        return new TabRuler(vector, ruler.autoSpacing());
    }

    /**
     * Compute the hashCode for this ruler.  The hashCode is the
     * hashCode of the first tab multiplied by the autoSpacing
     * interval.
     */
    public final int hashCode() {
        return firstTab().hashCode() * (int)autoSpacing();
    }

    /**
     * Return true if this tab ruler contains the given tab.
     *
     * @param tabToTest the tab to search for
     * @return true if this tab ruler contains <code>tabToTest</code>
     */
    public boolean containsTab(TabStop tabToTest) {
        for (TabStop tab = firstTab(); tab.getType() != TabStop.kAuto;
                tab = nextTab(tab.getPosition())) {
            if (tab.getPosition() >= tabToTest.getPosition()) {
                return tabToTest.equals(tab);
            }
        }
        return false;
    }

    /**
     * Return a tab ruler identical to this ruler, except with the
     * given tab added.  This ruler is not modified.
     *
     * @param tabToAdd the tab to add to the new tab ruler
     * @return an MTabRuler resulting from this operation
     */
    public TabRuler addTab(TabStop tabToAdd) {
        return addTabToRuler(this, tabToAdd);
    }

    /**
     * Return a tab ruler identical to the given ruler, except with the
     * tab at the given position removed.  This ruler is not modified.
     *
     * @param position the position of the tab to remove from the new tab ruler
     * @return an MTabRuler resulting from this operation
     */
    public TabRuler removeTab(float position) {
        return removeTabFromRuler(this, position);
    }

    /**
     * Return a tab ruler identical to this ruler, except with the
     * tab at position <code>fromPosition</code> moved to position
     * <code>toPosition</code>.  This ruler is not modified.
     *
     * @param fromPosition the position of the tab to move
     * @param toPosition   the new position of the tab
     * @return an MTabRuler resulting from this operation
     */
    public TabRuler moveTab(int fromPosition, int toPosition) {
        return moveTabOnRuler(this, fromPosition, toPosition);
    }
}
