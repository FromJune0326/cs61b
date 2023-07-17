package deque;

import java.util.Iterator;

public class LinkedListDeque<T> implements Iterable<T>{
    private class Node {
        public T item;
        public Node next;
        public Node pre;
        public Node(T item, Node pre, Node next) {
            this.item = item;
            this.pre = pre;
            this.next = next;
        }
    }

    private Node sentinel;
    private int size;

    public LinkedListDeque() {
        sentinel = new Node(null, null, null);
        sentinel.pre = sentinel;
        sentinel.next = sentinel;
        size = 0;
    }

    public LinkedListDeque(T item) {
        sentinel = new Node(null, null, null);
        sentinel.next = new Node(item, sentinel, sentinel);
        sentinel.pre = sentinel.next;
        size = 1;
    }

    public void addFirst(T item) {
        Node newNode = new Node(item, sentinel, sentinel.next);
        sentinel.next.pre = newNode;
        sentinel.next = newNode;
        size = size + 1;
    }

    public void addLast(T item) {
        Node newNode = new Node(item, sentinel.pre, sentinel);
        sentinel.pre.next = newNode;
        sentinel.pre = newNode;
        size = size + 1;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public void printDeque() {
        Node p = sentinel;
        StringBuilder returnStr = new StringBuilder();
        while (p.next.next != sentinel) {
            p = p.next;
            returnStr.append(p.item);
            returnStr.append(" ");
        }
        returnStr.append(p.next.item);
        System.out.println(returnStr);
    }

    public T removeFirst() {
        Node first = sentinel.next;
        if (first != sentinel) {
            first.next.pre = sentinel;
            sentinel.next = first.next;
            size -= 1;
            return first.item;
        }
        return null;
    }

    public T removeLast() {
        Node last = sentinel.pre;
        if (last != sentinel) {
            last.pre.next = sentinel;
            sentinel.pre = last.pre;
            size -= 1;
            return last.item;
        }
        return null;
    }

    public T get(int index) {
        if (index >= size) return null;
        Node p = sentinel;
        int i = 0;
        while (p.next != sentinel) {
            p = p.next;
            if (i == index) {
                return p.item;
            }
            i += 1;
        }
        return null;
    }

    public T getRecursive(int index) {
        if (index >= size) return null;
        return getRecursiveStartAtNode(sentinel, index);
    }

    public Iterator<T> iterator() {
        return new LinkedListDequeIterator();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof LinkedListDeque otherList) {
            if (otherList.size != this.size) return false;
            Node p = this.sentinel;
            Node q = otherList.sentinel;
            while (p.next != sentinel) {
                p = p.next;
                q = q.next;
                if (!p.item.equals(q.item)) return false;
            }
        }
        return true;
    }

    private T getRecursiveStartAtNode(Node p, int i) {
        if (i == 0) return p.next.item;
        if (p.next == sentinel && i > 0) return null;
        return getRecursiveStartAtNode(p.next, i-1);
    }

    private class LinkedListDequeIterator implements Iterator<T> {
        private int wisPos;
        private Node wisNode;
        public LinkedListDequeIterator() {
            wisPos = 0;
            wisNode = sentinel;
        }

        @Override
        public boolean hasNext() {
            return wisPos < size;
        }

        @Override
        public T next() {
            wisNode = wisNode.next;
            T returnItem = wisNode.item;
            wisPos += 1;
            return returnItem;
        }
    }

}
