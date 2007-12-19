package org.jezve.notepad.text.document;

import java.util.*;


/**
 * AttributeMap is an immutable Map.  Additionally, there are
 * several methods for common operations (union,
 * remove, intersect);  these methods return new AttributeMap
 * instances.
 * <p/>
 * Although any non-null Object can be a key or value in an
 * AttributeMap, typically the keys are fields of TextAttribute.
 *
 * @see TextAttribute
 */
public final class AttributeMap implements java.util.Map {

    private static final String errString = "AttributeMaps are immutable.";

    // This is passed to the HashMap constructor as the load factor argument.  It is chosen to
    // avoid resizing the Hashtable whenever possible. I think that 1 does this.
    private static final int LOAD_FACTOR = 1;

    private HashMap styles;
    private AttributeSet cachedKeySet = null;
    private Collection cachedValueCollection = null;
    private Set cachedEntrySet = null;

    public static final AttributeMap EMPTY_ATTRIBUTE_MAP = new AttributeMap();

    /**
     * Create a new, empty AttributeMap. EMPTY_STYLE_SET can be used in place of an AttributeMap
     * produced by this constructor.
     */
    public AttributeMap() {
        styles = new HashMap(1, LOAD_FACTOR);
    }

    /**
     * Create an AttributeMap with the same key-value entries as the given Map.
     *
     * @param map a Map whose key-value entries will become the entries for this AttributeMap.
     *              <code>map</code> is not modified, and must not contain null keys or values.
     */
    public AttributeMap(java.util.Map map) {
        styles = new HashMap(map.size(), LOAD_FACTOR);
        styles.putAll(map);
    }

    /**
     * Create an AttributeMap with the same key-value
     * entries as the given Hashtable.
     *
     * @param hashtable a Hashtable whose key-value entries will
     *                  become the entries for this AttributeMap. <code>table</code>
     *                  is not modified.
     */
    public AttributeMap(Hashtable hashtable) {
        this((java.util.Map)hashtable);
    }

    /**
     * Create an AttributeMap with a single entry of
     * <code>{attribute, value}</code>.
     *
     * @param key   the key in this AttributeMap's single entry
     * @param value the value in this AttributeMap's single entry
     */
    public AttributeMap(Object key, Object value) {
        styles = new HashMap(1, LOAD_FACTOR);
        // hashtable checks value for null
        styles.put(key, value);
    }

    // For internal use only.
    private AttributeMap(HashMap table, boolean clone) {
        if (clone) {
            styles = (HashMap)table.clone();
        }
        else {
            styles = table;
        }
    }

// ==============
// Map interface
// ==============

    // queries

    /**
     * Return the number of entries in the AttributeMap.
     *
     * @return the number of entries in the AttributeMap
     */
    public int size() {
        return styles.size();
    }

    /**
     * Return true if the number of entries in the AttributeMap
     * is 0.
     *
     * @return true if the number of entries in the AttributeMap
     *         is 0
     */
    public boolean isEmpty() {
        return styles.isEmpty();
    }

    /**
     * Return true if the given key is in this AttributeMap.
     *
     * @param key the key to test
     * @return true if <code>key</code> is in this AttributeMap
     */
    public boolean containsKey(Object key) {
        return styles.containsKey(key);
    }

    /**
     * Return true if the given value is in this AttributeMap.
     *
     * @param value the value to test
     * @return true if <code>value</code> is in this AttributeMap
     */
    public boolean containsValue(Object value) {
        return styles.containsValue(value);
    }

    /**
     * Return the value associated with the given key.  If the
     * key is not in this AttributeMap null is returned.
     *
     * @param key the key to look up
     * @return the value associated with <code>key</code>, or
     *         null if <code>key</code> is not in this AttributeMap
     */
    public Object get(Object key) {
        return styles.get(key);
    }

// modifiers - all throw exceptions

    /**
     * Throws UnsupportedOperationException.
     *
     * @throws UnsupportedOperationException
     * @see #addAttribute
     */
    public Object put(Object key, Object value) {
        throw new UnsupportedOperationException(errString);
    }

    /**
     * Throws UnsupportedOperationException.
     *
     * @throws UnsupportedOperationException
     * @see #removeAttributes
     */
    public Object remove(Object key) {
        throw new UnsupportedOperationException(errString);
    }

    /**
     * Throws UnsupportedOperationException.
     *
     * @throws UnsupportedOperationException
     * @see #addAttributes
     */
    public void putAll(java.util.Map t) {
        throw new UnsupportedOperationException(errString);
    }

    /**
     * Throws UnsupportedOperationException.
     *
     * @throws UnsupportedOperationException
     * @see #EMPTY_ATTRIBUTE_MAP
     */
    public void clear() {
        throw new UnsupportedOperationException(errString);
    }

// views

    /**
     * Return an AttributeSet containing every key in this AttributeMap.
     *
     * @return an AttributeSet containing every key in this AttributeMap
     */
    public Set keySet() {
        return styles.keySet();
    }

    /**
     * Return an AttributeSet containing every key in this AttributeMap.
     *
     * @return an AttributeSet containing every key in this AttributeMap
     */
    public AttributeSet getKeySet() {
        if (cachedKeySet == null) {
            cachedKeySet = AttributeSet.createKeySet(styles.keySet());
        }
        return cachedKeySet;
    }

    /**
     * Return a Collection containing every value in this AttributeMap.
     *
     * @return a Collection containing every value in this AttributeMap
     */
    public Collection values() {
        if (cachedValueCollection == null) {
            cachedValueCollection = Collections.unmodifiableCollection(styles.values());
        }
        return cachedValueCollection;
    }

    /**
     * Return a Set containing all entries in this AttributeMap.
     */
    public Set entrySet() {
        if (cachedEntrySet == null) {
            cachedEntrySet = Collections.unmodifiableSet(styles.entrySet());
        }
        return cachedEntrySet;
    }

    public boolean equals(Object rhs) {
        return rhs == this || rhs instanceof AttributeMap && styles.equals(((AttributeMap)rhs).styles); 
    }

    public int hashCode() {
        return styles.hashCode();
    }

    public String toString() {
        return styles.toString();
    }

// ==============
// Operations
// ==============

    /**
     * Return a AttributeMap which contains entries in this AttributeMap,
     * along with an entry for <attribute, value>.  If attribute
     * is already present in this AttributeMap its value becomes value.
     */
    public AttributeMap addAttribute(Object key, Object value) {
        HashMap map = new HashMap(styles.size() + 1, LOAD_FACTOR);
        map.putAll(styles);
        map.put(key, value);
        return new AttributeMap(map, false);
    }

    /**
     * Return a AttributeMap which contains entries in this AttributeMap
     * and in rhs.  If an attribute appears in both StyleSets the
     * value from rhs is used.
     */
    public AttributeMap addAttributes(AttributeMap rhs) {
        if (size() == 0)  return rhs;
        if (rhs.size() == 0) return this;

        HashMap map = new HashMap(size() + rhs.size(), LOAD_FACTOR);
        map.putAll(styles);
        map.putAll(rhs);
        return new AttributeMap(map, false);

    }

    /**
     * Return a AttributeMap which contains entries in this AttributeMap
     * and in rhs.  If an attribute appears in both StyleSets the
     * value from rhs is used.
     * The Map's keys and values must be non-null.
     */
    public AttributeMap addAttributes(java.util.Map rhs) {
        if (rhs instanceof AttributeMap) {
            return addAttributes((AttributeMap)rhs);
        }
        if (rhs.size() == 0) return this;

        HashMap map = new HashMap(size() + rhs.size(), LOAD_FACTOR);
        map.putAll(styles);
        map.putAll(rhs);
        return new AttributeMap(map, false);
    }

    /**
     * Return a AttributeMap with the entries in this AttributeMap, but
     * without attribute as a key.
     */
    public AttributeMap removeAttribute(Object attribute) {
        if (!containsKey(attribute)) {
            return this;
        }
        HashMap map = new HashMap(styles.size(), LOAD_FACTOR);
        map.putAll(styles);
        map.remove(attribute);
        return new AttributeMap(map, false);
    }

    /**
     * Return a AttributeMap with the entries of this AttributeMap whose
     * attributes are <b>not</b> in the Set.
     */
    public AttributeMap removeAttributes(AttributeSet attributes) {
        return removeAttributes((Set)attributes);
    }

    /**
     * Return a AttributeMap with the entries of this AttributeMap whose
     * attributes are <b>not</b> in the Set.
     */
    public AttributeMap removeAttributes(Set attributes) {

        // Create newTable on demand;  if null at
        // end of iteration then return this set.
        // Should we intersect styles.keySet with
        // attributes instead?

        HashMap map = null;
        for (Object current : attributes) {
            if (current != null && styles.containsKey(current)) {
                if (map == null) {
                    map = new HashMap(styles.size(), LOAD_FACTOR);
                    map.putAll(styles);
                }
                map.remove(current);
            }
        }
        if (map != null) {
            return new AttributeMap(map, false);
        }
        else {
            return this;
        }
    }

    /**
     * Return a AttributeMap with the keys of this AttributeMap which
     * are also in the Set.  The set must not contain null.
     */
    public AttributeMap intersectWith(AttributeSet attributes) {
        return intersectWith((Set)attributes);
    }

    /**
     * Return a AttributeMap with the keys of this AttributeMap which
     * are also in the Set.  The set must not contain null.
     */
    public AttributeMap intersectWith(Set attributes) {
        
        // For now, forget about optimizing for the case when
        // the return value is equivalent to this set.

        HashMap map = new HashMap(Math.min(attributes.size(), styles.size()), LOAD_FACTOR);

        if (attributes.size() < styles.size()) {
            for (Object current : attributes) {
                if (current != null) {
                    Object value = styles.get(current);
                    if (value != null) {
                        map.put(current, value);
                    }
                }
            }
        }
        else {
            for (Object current : keySet()) {
                if (attributes.contains(current)) {
                    map.put(current, styles.get(current));
                }
            }
        }
        return new AttributeMap(map, false);
    }

    /**
     * Put all entries in this AttributeMap into the given Map.
     *
     * @param rhs the Map into which entries are placed
     */
    public void putAllInto(java.util.Map rhs) {
        rhs.putAll(this);
    }
}
