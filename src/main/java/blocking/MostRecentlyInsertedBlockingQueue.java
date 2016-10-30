package blocking;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MostRecentlyInsertedBlockingQueue<E> extends AbstractQueue<E>
        implements BlockingQueue<E> {

    /**
     * Linked list node class
     */
    private static class Node<E> {
        E item;
        Node<E> next;

        public Node(E item) {
            this.item = item;
        }
    }

    /**
     * Head of linked list.
     * Invariant: head.item == null
     */
    transient Node<E> head;

    /**
     * Tail of linked list.
     * Invariant: last.next == null
     */
    private transient Node<E> tail;

    /**
     * Current number of elements
     */
    private final AtomicInteger amountOfElements = new AtomicInteger();

    /**
     * Capacity bound
     */
    private final int capacity;

    /**
     * Lock held by take, poll, etc
     */
    private final ReentrantLock takeLock = new ReentrantLock();

    /**
     * Wait queue for waiting takes
     */
    private final Condition notEmpty = takeLock.newCondition();

    /**
     * Lock held by put, offer, etc
     */
    private final ReentrantLock putLock = new ReentrantLock();

    /**
     * Wait queue for waiting puts
     */
    private final Condition notFull = putLock.newCondition();

    /**
     * Signals a waiting take. Called only from put/offer (which do not
     * otherwise ordinarily lock takeLock.)
     */
    private void signalNotEmpty() {
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
    }

    /**
     * Signals a waiting put. Called only from take/poll.
     */
    private void signalNotFull() {
        final ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            notFull.signal();
        } finally {
            putLock.unlock();
        }
    }

    /**
     * Links node at end of queue.
     *
     * @param node the node
     */
    private void insertItemToTail(Node<E> node) {
        tail = tail.next = node;
    }

    /**
     * Removes a node from head of queue.
     *
     * @return the node
     */
    private E removeNodeFromHead() {
        Node<E> h = head;
        Node<E> first = h.next;
        h.next = h; // help GC
        head = first;
        E x = first.item;
        first.item = null;
        return x;
    }

    /**
     * Locks to prevent both puts and takes.
     */
    void fullyLock() {
        putLock.lock();
        takeLock.lock();
    }

    /**
     * Unlocks to allow both puts and takes.
     */
    void fullyUnlock() {
        takeLock.unlock();
        putLock.unlock();
    }

    /**
     * Creates a {@code LinkedBlockingQueue} with the given (fixed) capacity.
     *
     * @param capacity the capacity of this queue
     * @throws IllegalArgumentException if {@code capacity} is not greater
     *                                  than zero
     */
    public MostRecentlyInsertedBlockingQueue(int capacity) {
        this.capacity = capacity;
        tail = head = new Node<>(null);
    }

    /**
     * Returns the number of elements in this queue.
     *
     * @return the number of elements in this queue
     */
    public int size() {
        return amountOfElements.get();
    }

    /**
     * Inserts the specified element at the tail of this queue, waiting if
     * necessary for space to become available.
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public void put(E item) throws InterruptedException {
        checkNotNull(item);

        int c = -1;
        Node<E> node = new Node<>(item);
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.amountOfElements;
        putLock.lockInterruptibly();
        try {
            /*
             * Note that count is used in wait guard even though it is
             * not protected by lock. This works because count can
             * only decrease at this point (all other puts are shut
             * out by lock), and we (or some other waiting put) are
             * signalled if it ever changes from capacity. Similarly
             * for all other uses of count in other wait guards.
             */
            while (count.get() == capacity) {
                notFull.await();
            }
            insertItemToTail(node);
            c = count.getAndIncrement();
            if (c + 1 < capacity)
                notFull.signal();
        } finally {
            putLock.unlock();
        }
        if (c == 0)
            signalNotEmpty();

    }

    /**
     * Inserts the specified element at the tail of this queue, waiting if
     * necessary up to the specified wait time for space to become available.
     *
     * @return {@code true} if successful, or {@code false} if
     * the specified waiting time elapses before space is available
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public boolean offer(E item, long timeout, TimeUnit unit) throws InterruptedException {
        checkNotNull(item);

        long nanos = unit.toNanos(timeout);
        putLock.lock();
        Node<E> node = new Node<>(item);
        final ReentrantLock putLock = this.putLock;

        if (amountOfElements.intValue() < capacity) {
            try {
                insertItemToTail(node);
                amountOfElements.getAndIncrement();
            } finally {
                putLock.unlock();
            }
            return true;
        } else {
            try {
                if(nanos <= 0){
                    return false;
                }
                signalNotEmpty();
                poll();
                insertItemToTail(node);
                amountOfElements.incrementAndGet();
                notFull.awaitNanos(nanos);
            } finally {
                putLock.unlock();
            }
            return true;
        }
    }

    /**
     * Inserts the specified element at the tail of this queue
     *
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E item) {
        checkNotNull(item);

        putLock.lock();
        Node<E> node = new Node<>(item);
        final ReentrantLock putLock = this.putLock;

        if (amountOfElements.intValue() < capacity) {
            try {
                insertItemToTail(node);
                amountOfElements.getAndIncrement();
            } finally {
                putLock.unlock();
            }
            return true;
        } else {
            try {
                signalNotEmpty();
                poll();
                insertItemToTail(node);
                amountOfElements.incrementAndGet();
            } finally {
                putLock.unlock();
            }
            return true;
        }
    }


    public E take() throws InterruptedException {
        E x;
        int c = -1;
        final AtomicInteger count = this.amountOfElements;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly();
        try {
            while (count.get() == 0) {
                notEmpty.await();
            }
            x = removeNodeFromHead();
            c = count.getAndDecrement();
            if (c > 1)
                notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
        if (c == capacity)
            signalNotFull();
        return x;
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E x = null;
        int c = -1;
        long nanos = unit.toNanos(timeout);
        final AtomicInteger count = this.amountOfElements;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly();
        try {
            while (count.get() == 0) {
                if (nanos <= 0)
                    return null;
                nanos = notEmpty.awaitNanos(nanos);
            }
            x = removeNodeFromHead();
            c = count.getAndDecrement();
            if (c > 1)
                notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
        if (c == capacity)
            signalNotFull();
        return x;
    }

    /**
     * Retrieves and removes the head of this queue.
     *
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    public E poll() {
        final AtomicInteger count = this.amountOfElements;
        if (count.get() == 0)
            throw new NoSuchElementException("Empty queue!!");
        E x = null;
        int c = -1;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            if (count.get() > 0) {
                x = removeNodeFromHead();
                c = count.getAndDecrement();
                if (c > 1)
                    notEmpty.signal();
            }
        } finally {
            takeLock.unlock();
        }
        if (c == capacity)
            signalNotFull();
        return x;
    }

    /**
     * Retrieves, but does not remove, the head of this queue
     *
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    public E peek() {
        if (amountOfElements.get() == 0) {
            throw new NoSuchElementException("Empty queue!!");
        }

        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            Node<E> first = head.next;
            if (first == null)
                return null;
            else
                return first.item;
        } finally {
            takeLock.unlock();
        }
    }


    /**
     * Unlinks interior Node p with predecessor trail.
     */
    void unlink(Node<E> p, Node<E> trail) {
        // assert isFullyLocked();
        // p.next is not changed, to allow iterators that are
        // traversing p to maintain their weak-consistency guarantee.
        p.item = null;
        trail.next = p.next;
        if (tail == p)
            tail = trail;
        if (amountOfElements.getAndDecrement() == capacity)
            notFull.signal();
    }

    /**
     * Atomically removes all of the elements from this queue.
     * The queue will be empty after this call returns.
     */
    public void clear() {
        fullyLock();
        try {
            for (Node<E> p, h = head; (p = h.next) != null; h = p) {
                h.next = h;
                p.item = null;
            }
            head = tail;
            // assert head.item == null && head.next == null;
            if (amountOfElements.getAndSet(0) == capacity)
                notFull.signal();
        } finally {
            fullyUnlock();
        }
    }

    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        if (maxElements <= 0)
            return 0;
        boolean signalNotFull = false;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            int n = Math.min(maxElements, amountOfElements.get());
            // count.get provides visibility to first n Nodes
            Node<E> h = head;
            int i = 0;
            try {
                while (i < n) {
                    Node<E> p = h.next;
                    c.add(p.item);
                    p.item = null;
                    h.next = h;
                    h = p;
                    ++i;
                }
                return n;
            } finally {
                // Restore invariants even if c.add() threw
                if (i > 0) {
                    // assert h.item == null;
                    head = h;
                    signalNotFull = (amountOfElements.getAndAdd(-i) == capacity);
                }
            }
        } finally {
            takeLock.unlock();
            if (signalNotFull)
                signalNotFull();
        }
    }

    /**
     * Returns the number of additional elements that this queue can ideally
     * (in the absence of memory or resource constraints) accept without
     * blocking. This is always equal to the initial capacity of this queue
     * less the current size of this queue.
     * <p>
     * Note that you cannot always tell if an attempt to insert
     * an element will succeed by inspecting  remainingCapacity
     * because it may be the case that another thread is about to
     * insert or remove an element.
     */
    public int remainingCapacity() {
        return capacity - amountOfElements.get();
    }


    /**
     * Throws NullPointerException if argument is null.
     *
     * @param v the element
     */
    private static void checkNotNull(Object v) {
        if (v == null)
            throw new NullPointerException("You can't put a 'null' element");
    }

    public Iterator<E> iterator() {
        return new Itr();
    }

    private class Itr implements Iterator<E> {
        /*
         * Basic weakly-consistent iterator.  At all times hold the next
         * item to hand out so that if hasNext() reports true, we will
         * still have it to return even if lost race with a take etc.
         */

        private Node<E> current;
        private Node<E> lastRet;
        private E currentElement;

        Itr() {
            fullyLock();
            try {
                current = head.next;
                if (current != null)
                    currentElement = current.item;
            } finally {
                fullyUnlock();
            }
        }

        public boolean hasNext() {
            return current != null;
        }

        /**
         * Returns the next live successor of p, or null if no such.
         * <p>
         * Unlike other traversal methods, iterators need to handle both:
         * - dequeued nodes (p.next == p)
         * - (possibly multiple) interior removed nodes (p.item == null)
         */
        private Node<E> nextNode(Node<E> p) {
            for (; ; ) {
                Node<E> s = p.next;
                if (s == p)
                    return head.next;
                if (s == null || s.item != null)
                    return s;
                p = s;
            }
        }

        public E next() {
            fullyLock();
            try {
                if (current == null)
                    throw new NoSuchElementException();
                E x = currentElement;
                lastRet = current;
                current = nextNode(current);
                currentElement = (current == null) ? null : current.item;
                return x;
            } finally {
                fullyUnlock();
            }
        }

        public void remove() {
            if (lastRet == null)
                throw new IllegalStateException();
            fullyLock();
            try {
                Node<E> node = lastRet;
                lastRet = null;
                for (Node<E> trail = head, p = trail.next;
                     p != null;
                     trail = p, p = p.next) {
                    if (p == node) {
                        unlink(p, trail);
                        break;
                    }
                }
            } finally {
                fullyUnlock();
            }
        }
    }

    public String toString() {
        fullyLock();
        try {
            Node<E> p = head.next;
            if (p == null)
                return "[]";

            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (; ; ) {
                E item = p.item;
                sb.append(item == this ? "(this Collection)" : item);
                p = p.next;
                if (p == null)
                    return sb.append(']').toString();
                sb.append(',').append(' ');
            }
        } finally {
            fullyUnlock();
        }
    }
}
