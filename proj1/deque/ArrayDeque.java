package deque;

import java.util.Iterator;

public class ArrayDeque<T> implements Deque<T>, Iterable<T> {
    private T[] items;
    private int size;
    private int nextFirst;
    private int nextLast;

    public ArrayDeque() {
        items = (T[]) new Object[8];
        size = 0;
        nextFirst = 0;
        nextLast = items.length - 1;
    }

    private void resize(int capacity) {
        T[] a = (T[]) new Object[capacity];
        for (int i = 0; i < nextFirst; i++) {
            a[i] = items[i];
        }
        int sizeDiff = capacity - items.length;
        for (int i = nextLast + 1 + sizeDiff; i < capacity; i++) {
            a[i] = items[i - sizeDiff];
        }
        nextLast = nextLast + sizeDiff;
        items = a;
    }

    @Override
    public void addFirst(T item) {
        if (size == items.length) {
            resize(size * 2);
        }
        items[nextFirst] = item;
        nextFirst = (nextFirst + 1) % items.length;
        size += 1;
    }

    @Override
    public void addLast(T item) {
        if (size == items.length) {
            resize(size * 2);
        }
        items[nextLast] = item;
        nextLast = (nextLast - 1 + items.length) % items.length;
        size += 1;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void printDeque() {
        StringBuilder returnStr = new StringBuilder();
        for(T x: this) {
            returnStr.append(x);
            returnStr.append(" ");
        }
        System.out.println(returnStr);
    }

    @Override
    public T removeFirst() {
        if ((size < items.length / 4) && (size > 8)) {
            resize(items.length / 4);
        }
        int newNextFirst = (nextFirst - 1 + items.length) % items.length;
        if (items[newNextFirst] == null) {
            return null;
        }
        T x = items[newNextFirst];
        items[newNextFirst] = null;
        nextFirst = newNextFirst;
        size -= 1;
        return x;
    }

    @Override
    public T removeLast() {
        if ((size < items.length / 4) && (size > 8)) {
            resize(items.length / 4);
        }
        int newNextLast = (nextLast + 1) % items.length;
        if (items[newNextLast] == null) {
            return null;
        }
        T x = items[newNextLast];
        items[newNextLast] = null;
        nextLast = newNextLast;
        size -= 1;
        return x;
    }

    @Override
    public T get(int index) {
        if (index >= size) {
            return null;
        }
        return items[(nextFirst - index - 1 + items.length) % items.length];
    }

    @Override
    public Iterator<T> iterator() {
        return new ArrayDequeIterator();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) { return false; }
        if (this == other) { return true; }
        if (this.getClass() != other.getClass()) { return false; }
        ArrayDeque<T> otherList = (ArrayDeque<T>) other;
        if (otherList.size != this.size) { return false; }
        for (int i = 0; i < size; i++) {
            if (!this.get(i).equals(otherList.get(i))) { return false; }
        }
        return true;
    }

    private class ArrayDequeIterator implements Iterator<T> {
        private int wisPos;
        public ArrayDequeIterator() {
            wisPos = 0;
        }

        @Override
        public boolean hasNext() {
            return wisPos < size;
        }

        @Override
        public T next() {
            T returnItem = get(wisPos);
            wisPos += 1;
            return returnItem;
        }
    }

}
