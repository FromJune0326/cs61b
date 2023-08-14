package bstmap;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class BSTMap<K extends Comparable, V>  implements Map61B<K, V>{

    private Node root;
    int size = 0;

    @Override
    public void clear() {
        root = null;
        size = 0;
    }

    @Override
    public boolean containsKey(K key) {
        if (root == null) {
            return false;
        }
        return root.get(key) != null;
    }

    @Override
    public V get(K key) {
        if (root == null) {
            return null;
        }
        Node node =  root.get(key);
        if (node != null) {
            return root.get(key).value;
        }
        return null;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void put(K key, V value) {
        root = insert(root, key, value);
        size += 1;
    }

    private Node insert(Node node, K k, V v) {
        if (node == null) {
            return new Node(k, v);
        }
        if (k.compareTo(node.key) < 0) {
            node.left = insert(node.left, k, v);
        }
        else if (k.compareTo(node.key) > 0) {
            node.right = insert(node.right, k ,v);
        }
        else if (k.equals(node.key)) {
            node.value = v;
        }
        return node;
    }

    private class Node {
        K key;
        V value;
        Node left = null;
        Node right = null;

        Node(K k, V v) {
            key = k;
            value = v;
        }

        Node get(K k) {
            if (k == null) {
                return null;
            }
            if (k.equals(key)) {
                return this;
            } else if (k.compareTo(key) < 0 && left != null) {
                return left.get(k);
            } else if (k.compareTo(key) > 0 && right != null){
                return right.get(k);
            }
            return null;
        }

    }

    @Override
    public Set<K> keySet() {
        Set<K> keys = new HashSet<>();
        traverseKey(root, keys);
        return keys;
    }

    private void traverseKey(Node n, Set<K> keys) {
        if (n == null) {
            return;
        }
        keys.add(n.key);
        traverseKey(n.left, keys);
        traverseKey(n.right, keys);
    }

    @Override
    public V remove(K key) {
        if (root == null) {
            return null;
        }
        Node node = root.get(key);
        if (node == null) {
            return null;
        } else {
            root = removeHelper(root, key);
            size--;
            return node.value;
        }
    }

    @Override
    public V remove(K key, V value) {
        if (root == null) {
            return null;
        }
        Node node = root.get(key);
        if (node == null || !node.value.equals(value)) {
            return null;
        } else {
            root = removeHelper(root, key);
            size--;
            return node.value;
        }
    }

    private Node removeHelper(Node node, K key) {
        if (key.equals(node.key)) {
            // remove key return value
            if (node.left == null) {
                return node.right;
            } else if (node.right == null ) {
                return node.left;
            }
            Node maxNode = getMaxNode(node.left);
            node.left = removeMaxNode(node.left);
            maxNode.left = node.left;
            maxNode.right = node.right;
            return maxNode;
        } else if (key.compareTo(node.key) < 0) {
            node.left = removeHelper(node.left, key);
        } else {
            node.right = removeHelper(node.right, key);
        }
        return node;
    }

    private Node removeMaxNode(Node node) {
        if (node.right == null) {
            return node.left;
        } else {
            node.right = removeMaxNode(node.right);
        }
        return node;
    }

    private Node getMaxNode(Node node) {
        if (node.right == null) {
            return node;
        } else {
            return getMaxNode(node.right);
        }
    }

    @Override
    public Iterator<K> iterator() {
        return keySet().iterator();
    }

}
