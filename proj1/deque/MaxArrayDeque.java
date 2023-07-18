package deque;

import java.util.Comparator;

public class MaxArrayDeque<T> extends ArrayDeque<T> {

    Comparator<T> defaultComparator;
    public MaxArrayDeque(Comparator<T> c) {
        this.defaultComparator = c;
    }

    public T max() {
        T maxItem = get(0);
        for (int i = 1; i < size(); i++) {
            if (defaultComparator.compare(maxItem, get(i)) < 0) {
                maxItem = get(i);
            }
        }
        return maxItem;
    }

    public T max(Comparator<T> c) {
        T maxItem = get(0);
        for (int i = 1; i < size(); i++) {
            if (c.compare(maxItem, get(i)) < 0) {
                maxItem = get(i);
            }
        }
        return maxItem;
    }
}
