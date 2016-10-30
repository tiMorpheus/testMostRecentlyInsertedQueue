package core;

import blocking.MostRecentlyInsertedBlockingQueue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.NoSuchElementException;

public class MostRecentlyInsertedBlockingQueueTest {

    static MostRecentlyInsertedBlockingQueue<Integer> testQueue = new MostRecentlyInsertedBlockingQueue<>(3);
    static MostRecentlyInsertedBlockingQueue<String> testStringQueue = new MostRecentlyInsertedBlockingQueue<>(5);


    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void init() throws Exception {
        testQueue.offer(0);
        testQueue.offer(1);
        testQueue.offer(2);
        testQueue.offer(3);
        testQueue.offer(4);
        testQueue.offer(5);
    }

    @Test
    public void ititTest() {
        Assert.assertEquals("[3, 4, 5]", testQueue.toString());
    }

    @Test
    public void isEmptyTest() {
        Assert.assertEquals(false, testQueue.isEmpty());
    }

    @Test
    public void clearQueueTest() {
        testQueue.clear();
        Assert.assertEquals(0, testQueue.size());
    }


    @Test
    public void insertNullValueIntoClearQueueTest() {
        thrown.expect(NullPointerException.class);

        testQueue.clear();
        testQueue.offer(null);
    }

    @Test
    public void insertNullValueIntoOverflowQueueTest() {
        thrown.expect(NullPointerException.class);

        testQueue.offer(null);
    }

    @Test
    public void insertIntoStringQueueValuesTest() {

        testStringQueue.offer("One");
        testStringQueue.offer("Two");
        testStringQueue.offer("Three");
        testStringQueue.offer("Four");
        testStringQueue.offer("Five");
        System.out.println(testStringQueue.size());
        Assert.assertEquals("[One, Two, Three, Four, Five]", testStringQueue.toString());
    }

    @Test
    public void pollTest() {
        Assert.assertEquals((Integer) 3, testQueue.poll());
        Assert.assertEquals((Integer) 4, testQueue.poll());
        Assert.assertEquals((Integer) 5, testQueue.poll());
    }

    @Test
    public void pollEmptyQueueTest() {
        thrown.expect(NoSuchElementException.class);

        testQueue.clear();
        testQueue.poll();
    }

    @Test
    public void peekTest() {

        Assert.assertEquals((Integer) 3, testQueue.peek());
        Assert.assertEquals(3, testQueue.size());
    }

    @Test
    public void peekEmptyQueue() {

        thrown.expect(NoSuchElementException.class);

        testStringQueue.clear();
        testStringQueue.peek();
    }
}
