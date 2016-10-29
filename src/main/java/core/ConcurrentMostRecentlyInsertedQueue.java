package core;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;


public class ConcurrentMostRecentlyInsertedQueue<Item> extends AbstractQueue<Item>
        implements Queue<Item> {

    private transient volatile Node<Item> head;
    private transient volatile Node<Item> tail;
    private AtomicInteger capacity;
    private AtomicInteger amountOfElements;

    public ConcurrentMostRecentlyInsertedQueue(int capacity) {
        this.capacity = new AtomicInteger(capacity);
        this.amountOfElements = new AtomicInteger(0);
        head = tail = new Node<>(null);
    }

    private static class Node<Item> {
        private volatile Item item;
        private volatile Node<Item> next;

        Node(Item item) {
            this.item = item;
        }

        private final AtomicReferenceFieldUpdater<Node, Object> elementUpdater =
                AtomicReferenceFieldUpdater.newUpdater(Node.class, Object.class, "item");

        private final AtomicReferenceFieldUpdater<Node, Node> nextUpdater =
                AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "next");

        public Item getElement() {
            return item;
        }

        public boolean casItem(Item expect, Item update) {
            return elementUpdater.compareAndSet(this, expect, update);
        }

        public void setItem(Item item) {
            elementUpdater.set(this, item);
        }

        public Node getNext() {
            return next;
        }

        public boolean casNext(Node<Item> expect, Node<Item> update) {
            return nextUpdater.compareAndSet(this, expect, update);
        }

        public void setNext(Node next) {
            nextUpdater.set(this, next);
        }
    }

    private final AtomicReferenceFieldUpdater<ConcurrentMostRecentlyInsertedQueue, Node> tailUpdater =
            AtomicReferenceFieldUpdater.newUpdater(ConcurrentMostRecentlyInsertedQueue.class, Node.class, "tail");
    private final AtomicReferenceFieldUpdater<ConcurrentMostRecentlyInsertedQueue, Node> headUpdater =
            AtomicReferenceFieldUpdater.newUpdater(ConcurrentMostRecentlyInsertedQueue.class, Node.class, "head");

    public boolean casHead(Node<Item> expect, Node<Item> update) {
        return headUpdater.compareAndSet(this, expect, update);
    }

    public boolean casTail(Node<Item> expect, Node<Item> update) {
        return tailUpdater.compareAndSet(this, expect, update);
    }


    public boolean offer(Item item) {
        checkNotNull(item);

        final Node<Item> newNode = new Node<>(item);

        for (; ; ) {
            Node<Item> expected = tail;
            Node<Item> updated = expected.getNext();
            if (expected == tail && amountOfElements.intValue() < capacity.intValue()) {
                if (updated == null) {
                    if (expected.casNext(updated, newNode)) {
                        casTail(expected, newNode);
                        amountOfElements.incrementAndGet();
                        return true;
                    }
                } else {
                    casTail(expected, updated);
                }
            } else if (expected == tail && amountOfElements.intValue() >= capacity.intValue()) {
                if (updated == null) {
                    if (expected.casNext(updated, newNode)) {
                        poll();
                        casTail(expected, newNode);
                        amountOfElements.incrementAndGet();
                        return true;
                    }
                } else {
                    casTail(expected, updated);
                }
            }
        }
    }

    public Item poll() {
/*        for(;;) {
            Node<Item> beforeFirstNode = head;
            Node<Item> tailOnPolling = tail;
            Node<Item> firstNode = beforeFirstNode.getNext();
            if (beforeFirstNode == head) {
                if (beforeFirstNode == tailOnPolling) {
                    if (firstNode == null) {
                        return null;
                    } else {
                        casTail(tailOnPolling, firstNode);
                    }
                } else if (casHead(beforeFirstNode, firstNode)) {
                    Item item = firstNode.getElement();
                    if (item != null) {
                        firstNode.setElement(null);
                        amountOfElements.decrementAndGet();
                        return item;
                    }
                }
            }
        }*/

        restartFromHead:
        for (; ; ) {
            for (Node<Item> h = head, p = h, q; ; ) {
                Item item = p.getElement();

                if (item != null && p.casItem(item, null)) {
                    // Successful CAS is the linearization point
                    // for item to be removed from this queue.
                    if (p != h) // hop two nodes at a time
                        updateHead(h, ((q = p.getNext()) != null) ? q : p);
                    return item;
                } else if ((q = p.next) == null) {
                    updateHead(h, p);
                    return null;
                } else if (p == q)
                    continue restartFromHead;
                else
                    p = q;
            }
        }
    }

    public Item peek() {
        restartFromHead:
        for (; ; ) {
            for (Node<Item> h = head, p = h, q; ; ) {
                Item item = p.getElement();
                if (item != null || (q = p.getNext()) == null) {
                    updateHead(h, p);
                    return item;
                } else if (p == q)
                    continue restartFromHead;
                else
                    p = q;
            }
        }
    }

    final void updateHead(Node<Item> h, Node<Item> p) {
        if (h != p && casHead(h, p))
            h.setNext(h);
    }

    public int size() {
        return amountOfElements.intValue();
    }

    public void clear() {
        while (!isEmpty()) {
            poll();
        }
    }


    /**
     * Throws NullPointerException if argument is null.
     *
     * @param v the element
     */
    private static void checkNotNull(Object v) {
        if (v == null)
            throw new NullPointerException("Item can't be null");
    }


    public Iterator<Item> iterator() {
        return new ConcurrentMostRecentlyInsertedIterator();
    }

    private class ConcurrentMostRecentlyInsertedIterator implements Iterator<Item> {

        private Node<Item> currentNode;
        private Node<Item> lastNode;
        private Item nextItem;

        ConcurrentMostRecentlyInsertedIterator() {
            advance();
        }

        private Item advance() {
            lastNode = currentNode;
            Item x = nextItem;

            Node<Item> newNode = (currentNode == null) ? firstNode() : currentNode.getNext();
            for (; ; ) {
                if (newNode == null) {
                    currentNode = null;
                    nextItem = null;
                    return x;
                }
                Item item = newNode.getElement();
                if (item != null) {
                    currentNode = newNode;
                    nextItem = item;
                    return x;
                } else
                    newNode = newNode.getNext();
            }
        }

        public boolean hasNext() {
            return currentNode != null;
        }

        public Item next() {
            if (currentNode == null) throw new NoSuchElementException();
            return advance();
        }

        public void remove() {
            Node<Item> l = lastNode;
            if (l == null) throw new IllegalStateException();
            l = null;
            lastNode = null;
        }
    }

    Node<Item> firstNode() {
        for (; ; ) {
            Node<Item> h = head;
            Node<Item> t = tail;
            Node<Item> first = h.getNext();
            if (h == head) {
                if (h == t) {
                    if (first == null)
                        return null;
                    else
                        casTail(t, first);
                } else {
                    if (first.getElement() != null)
                        return first;
                    else casHead(h, first);
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Node tmp = head.getNext();
        sb.append("[");
        while (tmp != null) {
            if (tmp == tail)
                sb.append(tmp.getElement()).append("");
            else
                sb.append(tmp.getElement()).append(", ");
            tmp = tmp.getNext();
        }
        sb.append("]");
        return sb.toString();
    }
}
