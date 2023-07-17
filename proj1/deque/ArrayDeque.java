package deque;

import java.util.Iterator;

public class ArrayDeque<T> implements Iterable<T>{
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

    public void addFirst(T item) {
        if (size == items.length) resize(size * 2);
        items[nextFirst] = item;
        nextFirst += 1;
        size += 1;
    }

    public void addLast(T item) {
        if (size == items.length) resize(size * 2);
        items[nextLast] = item;
        nextLast -= 1;
        size += 1;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public void printDeque() {
        StringBuilder returnStr = new StringBuilder();
        for(T x: this) {
            returnStr.append(x);
            returnStr.append(" ");
        }
        System.out.println(returnStr);
    }

    public T removeFirst() {
        if ((size < items.length / 4) && (size > 8)) {
            resize(items.length / 4);
        }
        if (nextFirst == 0) return null;
        T x = items[nextFirst - 1];
        items[nextFirst - 1] = null;
        nextFirst -= 1;
        size -= 1;
        return x;
    }

    public T removeLast() {
        if ((size < items.length / 4) && (size > 8)) {
            resize(items.length / 4);
        }
        if (nextLast == items.length - 1) return null;
        T x = items[nextLast + 1];
        items[nextLast + 1] = null;
        nextLast += 1;
        size -= 1;
        return x;
    }

    public T get(int index) {
        if (index >= size) return null;
        if (index < nextFirst) return items[nextFirst - index - 1];
        return items[nextLast - (index - size)];
    }

    public Iterator<T> iterator() {
        return new ArrayDequeIterator();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        if (this == other) return true;
        if (this.getClass() != other.getClass()) return false;
        ArrayDeque<T> otherList = (ArrayDeque<T>) other;
        if (otherList.size != this.size) return false;
        for (int i = 0; i < size; i++) {
            if (!this.get(i).equals(otherList.get(i))) return false;
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
