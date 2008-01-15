package org.jezve.notepad.text.document;

import java.util.*;

/**
 * An AttributeSet is an immutable collection of unique Objects.
 * It has several operations which return new AttributeSet instances.
 */
public final class AttributeSet implements Set {

    private static final String errString = "AttributeSet is immutable.";

    private Set elements;

    public static final AttributeSet EMPTY_SET = new AttributeSet();
    public static final Set EMPTY_HASHSET = new HashSet();

    private AttributeSet(Set c) {
        elements = Collections.unmodifiableSet(c);
    }

    public AttributeSet() {
        this(EMPTY_HASHSET);
    }

    public AttributeSet(final Object o) {
        this(new HashSet(1, 1){{ add(o); }});
    }

    /**
     * Return true if the number of elements in this set is 0.
     *
     * @return true if the number of elements in this set is 0
     */
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    /**
     * Return the number of elements in this set.
     *
     * @return the number of elements in this set
     */
    public int size() {
        return elements.size();
    }

    public boolean equals(Object rhs) {
        return rhs instanceof AttributeSet && equals((AttributeSet)rhs);
    }

    public boolean equals(AttributeSet rhs) {
        return rhs != null && (this == rhs || elements.equals(rhs.elements));
    }

    /**
     * Return true if this set contains the given Object
     *
     * @return true if this set contains <code>o</code>
     */
    public boolean contains(Object o) {
        return elements.contains(o);
    }

    /**
     * Return true if this set contains all elements in the given
     * Collection
     *
     * @param coll the collection to compare with
     * @return true if this set contains all elements in the given
     *         Collection
     */
    public boolean containsAll(Collection coll) {
        return elements.containsAll(coll);
    }

    /**
     * Return an Iterator with the elements in this set.
     *
     * @return an Iterator with the elements in this set.
     *         The Iterator cannot be used to modify this AttributeSet.
     */
    public Iterator iterator() {
        return elements.iterator();
    }

    /**
     * Fill in the given array with the elements in this set.
     *
     * @param storage an array to fill with this set's elements. The array cannot be null.
     * @return the <tt>storage</tt> array.
     */
    public Object[] toArray(Object[] storage) {
        return elements.toArray(storage);
    }

    /**
     * Return an array with the elements in this set.
     *
     * @return an array with the elements in this set
     */
    public Object[] toArray() {
        return elements.toArray();
    }

    /**
     * Throws UnsupportedOperationException.
     *
     * @throws UnsupportedOperationException
     * @see #addElement
     */
    public boolean add(Object o) {
        throw new UnsupportedOperationException(errString);
    }

    /**
     * Throws UnsupportedOperationException.
     *
     * @throws UnsupportedOperationException
     */
    public boolean remove(Object o) {
        throw new UnsupportedOperationException(errString);
    }

    /**
     * Throws UnsupportedOperationException.
     *
     * @throws UnsupportedOperationException
     * @see #unionWith
     */
    public boolean addAll(Collection coll) {
        throw new UnsupportedOperationException(errString);
    }

    /**
     * Throws UnsupportedOperationException.
     *
     * @throws UnsupportedOperationException
     * @see #subtract
     */
    public boolean removeAll(Collection coll) {
        throw new UnsupportedOperationException(errString);
    }

    /**
     * Throws UnsupportedOperationException.
     *
     * @throws UnsupportedOperationException
     * @see #intersectWith
     */
    public boolean retainAll(Collection coll) {
        throw new UnsupportedOperationException(errString);
    }

    /**
     * Throws UnsupportedOperationException.
     *
     * @throws UnsupportedOperationException
     * @see #EMPTY_SET
     */
    public void clear() {
        throw new UnsupportedOperationException(errString);
    }

    /**
     * Return an AttributeSet containing the elements of this set and the given element
     *
     * @param o the element to add
     * @return an AttributeSet like this one, with <code>element</code> added
     */
    public AttributeSet addElement(final Object o) {
        return new AttributeSet(new HashSet(elements) {{ add(o); }});
    }

    /**
     * Return an AttributeSet which is the union of this set with the given set.
     *
     * @param s the set to union with
     * @return an AttributeSet of the elements in this set or in <code>s</code>
     */
    public AttributeSet unionWith(final AttributeSet s) {
        return new AttributeSet(new HashSet(elements) {{ addAll(s.elements); }});
    }

    /**
     * Return an AttributeSet which is the intersection of this set with the given set.
     *
     * @param s the set to intersect with
     * @return an AttributeSet of the elements in this set which are in <code>s</code>
     */
    public AttributeSet intersectWith(AttributeSet s) {
        HashSet set = new HashSet();
        for (Iterator i = s.iterator(); i.hasNext(); ) {
            Object next = i.next();
            if (elements.contains(next)) {
                set.add(next);
            }
        }
        return new AttributeSet(set);
    }

    /**
     * Return an AttributeSet with the elements in this set which are not in the given set.
     *
     * @param s the set of elements to exclude
     * @return an AttributeSet of the elements in this set which are not in <code>s</code>
     */
    public AttributeSet subtract(final AttributeSet s) {
        return new AttributeSet(new HashSet(elements) {{ removeAll(s.elements); }});
    }
}