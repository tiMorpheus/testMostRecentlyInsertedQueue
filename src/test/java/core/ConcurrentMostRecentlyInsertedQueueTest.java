package core;


import concurrent.ConcurrentMostRecentlyInsertedQueue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.testng.annotations.BeforeClass;

import java.util.NoSuchElementException;
import java.util.Queue;

public class ConcurrentMostRecentlyInsertedQueueTest {
    static ConcurrentMostRecentlyInsertedQueue<Integer> testQueue = new ConcurrentMostRecentlyInsertedQueue<>(3) ;
    static ConcurrentMostRecentlyInsertedQueue<Integer> testConcurrency;
    static Queue<String> testStringQueue = new ConcurrentMostRecentlyInsertedQueue<>(5);

    @Before
    public void init() throws Exception {
        testQueue.offer(0);
        testQueue.offer(1);
        testQueue.offer(2);
        testQueue.offer(3);
    }
    @BeforeClass
    public void setUp(){
        testConcurrency = new ConcurrentMostRecentlyInsertedQueue<>(5);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void isEmptyTest() {
        Assert.assertEquals(false, testQueue.isEmpty());
    }

    @Test
    public void toStringTest() {
        Assert.assertEquals("[1, 2, 3]", testQueue.toString());
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

        Assert.assertEquals("[One, Two, Three, Four]", testStringQueue.toString());
    }

    @Test
    public void pollTest() {
        Assert.assertEquals((Integer) 1, testQueue.poll());
        Assert.assertEquals((Integer) 2, testQueue.poll());
        Assert.assertEquals((Integer) 3, testQueue.poll());
    }

    @Test
    public void pollEmptyQueueTest() {
        thrown.expect(NoSuchElementException.class);

        testQueue.clear();
        testQueue.poll();
    }

    @Test
    public void peekTest() {

        Assert.assertEquals((Integer) 1, testQueue.peek());
        Assert.assertEquals(3, testQueue.size());
    }

    @Test
    public void peekEmptyQueue() {

        testStringQueue.clear();
        Assert.assertEquals(null, testStringQueue.peek());
    }

    @org.testng.annotations.Test(threadPoolSize = 30, invocationCount = 100, timeOut = 10000)
    public void concurrencyTest() {
        testConcurrency.offer(1);
        testConcurrency.offer(2);
        testConcurrency.offer(3);
        testConcurrency.offer(4);
        testConcurrency.offer(5);
        testConcurrency.offer(6);
        System.out.println(testConcurrency.toString());
    }
}
