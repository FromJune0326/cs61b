package hashmap;

import java.util.*;

/**
 *  A hash table-backed Map implementation. Provides amortized constant time
 *  access to elements via get(), remove(), and put() in the best case.
 *
 *  Assumes null keys will never be inserted, and does not resize down upon remove().
 *  @author YOUR NAME HERE
 */
public class MyHashMap<K, V> implements Map61B<K, V> {

    /**
     * Protected helper class to store key/value pairs
     * The protected qualifier allows subclass access
     */
    protected class Node {
        K key;
        V value;

        Node(K k, V v) {
            key = k;
            value = v;
        }
    }

    /* Instance Variables */
    private Collection<Node>[] buckets;
    // You should probably define some more!
    private int size = 0;
    private double loadFactor = 0.75;

    private int bucketSize = 12;

    /** Constructors */
    public MyHashMap() {
        buckets = createTable(bucketSize);
    }

    public MyHashMap(int initialSize) {
        bucketSize = (int) (initialSize * loadFactor);
        buckets = createTable(bucketSize);
    }

    /**
     * MyHashMap constructor that creates a backing array of initialSize.
     * The load factor (# items / # buckets) should always be <= loadFactor
     *
     * @param initialSize initial size of backing array
     * @param maxLoad maximum load factor
     */
    public MyHashMap(int initialSize, double maxLoad) {
        loadFactor = maxLoad;
        bucketSize = (int) (initialSize * loadFactor);
        buckets = createTable(bucketSize);
    }

    /**
     * Returns a new node to be placed in a hash table bucket
     */
    private Node createNode(K key, V value) {
        return new Node(key, value);
    }

    /**
     * Returns a data structure to be a hash table bucket
     *
     * The only requirements of a hash table bucket are that we can:
     *  1. Insert items (`add` method)
     *  2. Remove items (`remove` method)
     *  3. Iterate through items (`iterator` method)
     *
     * Each of these methods is supported by java.util.Collection,
     * Most data structures in Java inherit from Collection, so we
     * can use almost any data structure as our buckets.
     *
     * Override this method to use different data structures as
     * the underlying bucket type
     *
     * BE SURE TO CALL THIS FACTORY METHOD INSTEAD OF CREATING YOUR
     * OWN BUCKET DATA STRUCTURES WITH THE NEW OPERATOR!
     */
    protected Collection<Node> createBucket() {
        return new LinkedList<>();
    }

    /**
     * Returns a table to back our hash table. As per the comment
     * above, this table can be an array of Collection objects
     *
     * BE SURE TO CALL THIS FACTORY METHOD WHEN CREATING A TABLE SO
     * THAT ALL BUCKET TYPES ARE OF JAVA.UTIL.COLLECTION
     *
     * @param tableSize the size of the table to create
     */
    private Collection<Node>[] createTable(int tableSize) {
        Collection<Node>[] table = new Collection[tableSize];
        for (int i = 0; i < tableSize; i++) {
            table[i] = createBucket();
        }

        return table;
    }

    // TODO: Implement the methods of the Map61B Interface below
    // Your code won't compile until you do so!
    @Override
    public void clear() {
        buckets = createTable(bucketSize);
        size = 0;
    }

    @Override
    public boolean containsKey(K key) {
        if (get(key) != null) {
            return true;
        }
        return false;
    }

    @Override
    public V get(K key) {
        Node node = getNode(key);
        if (node != null) {
            return node.value;
        }
        return null;
    }

    private Node getNode(K key) {
        int index = getIndex(key);
        for (Node node: buckets[index]) {
            if (node.key.equals(key)) {
                return node;
            }
        }
        return null;
    }

    @Override
    public int size() {
        return size;
    }

    private void resize() {
        if ((float)size / bucketSize > loadFactor) {
            bucketSize = bucketSize * 2;
            Collection<Node>[] newBuckets = createTable(bucketSize);
            for (Collection<Node> bucket: buckets) {
                for (Node node: bucket) {
                    int index = getIndex(node.key);
                    newBuckets[index].add(node);
                }
            }
            buckets = newBuckets;
        }
    }

    @Override
    public void put(K key, V value) {
        Node node = getNode(key);
        if (node != null) {
            node.value = value;
            return;
        }

        size += 1;
        resize();
        node = createNode(key, value);
        int index = getIndex(key);
        buckets[index].add(node);
    }

    @Override
    public Set<K> keySet() {
        Set<K> keys = new HashSet<>();
        for (var bucket: buckets) {
            for (Node node: bucket) {
                keys.add(node.key);
            }
        }
        return keys;
    }

    @Override
    public V remove(K key) {
        if (!containsKey(key)) {
            return null;
        }
        int index = getIndex(key);
        for (Node node: buckets[index]) {
            if (node.key.equals(key)) {
                buckets[index].remove(node);
                return node.value;
            }
        }
        return null;
    }

    private int getIndex(K key) {
        return Math.floorMod(key.hashCode(), bucketSize);
    }

    @Override
    public V remove(K key, V value) {
        if (!containsKey(key)) {
            return null;
        }
        int index = getIndex(key);
        for (Node node: buckets[index]) {
            if (node.key.equals(key) && node.value.equals(value)) {
                buckets[index].remove(node);
                return node.value;
            }
        }
        return null;
    }

    @Override
    public Iterator<K> iterator() {
        return keySet().iterator();
    }

}
