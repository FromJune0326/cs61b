package deque;

import org.junit.Test;

import java.util.Comparator;

import static org.junit.Assert.*;

public class MaxArrayDequeTest {

    public static class IntComparator implements Comparator<Integer> {
        public int compare(Integer x, Integer y) {
            return x - y;
        }
    }

    public static class IntStrComparator implements Comparator<Integer> {
        public int compare(Integer x, Integer y) {
            return x.toString().length() - y.toString().length();
        }
    }
    @Test
    public void noItemTest() {

        MaxArrayDeque<Integer> maxArrayDeque1 = new MaxArrayDeque<>(new IntComparator());

        assertEquals("Max of Empty List should be null", null, maxArrayDeque1.max());
    }

    @Test
    public void maxTest() {
        MaxArrayDeque<Integer> maxArrayDeque1 = new MaxArrayDeque<>(new IntComparator());

        maxArrayDeque1.addFirst(-5);
        maxArrayDeque1.addLast(-100);
        maxArrayDeque1.addLast(0);
        maxArrayDeque1.addLast(3);

        assertEquals("Max int in the list should be 3", 3, (int)maxArrayDeque1.max());
        assertEquals("Int with max string length in the list should be -100", -100, (int)maxArrayDeque1.max(new IntStrComparator()));

        maxArrayDeque1.addLast(41234);
        assertEquals("Max int in the list should be 41234", 41234, (int)maxArrayDeque1.max());
        assertEquals("Int with max string length in the list should be 41234", 41234, (int)maxArrayDeque1.max(new IntStrComparator()));
        maxArrayDeque1.removeLast();
        assertEquals("Max int in the list should be 3", 3, (int)maxArrayDeque1.max());
        assertEquals("Int with max string length in the list should be -100", -100, (int)maxArrayDeque1.max(new IntStrComparator()));
    }
}
