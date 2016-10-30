package concurrent;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class ConcurrentMostRecentlyInsertedQueue<Item> extends AbstractQueue<Item>
        implements Queue<Item> {

    private AtomicInteger capacity;
    private AtomicInteger countOfNodes = new AtomicInteger(0);
    private volatile Node<Item> head = new Node<>(null, null);
    private volatile Node<Item> tail = head;

    private static class Node<Item> {
        private volatile Item element;
        private volatile Node next;

        private final AtomicReferenceFieldUpdater<Node, Object> elementUpdater =
                AtomicReferenceFieldUpdater.newUpdater(Node.class, Object.class, "element");

        private final AtomicReferenceFieldUpdater<Node, Node> nextUpdater =
                AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "next");


        public Node(Item element, Node next) {
            this.element = element;
            this.next = next;
        }

        public Item getElement() {
            return element;
        }

        public boolean casElement(Item expect, Item update) {
            return elementUpdater.compareAndSet(this, expect, update);
        }

        public void setElement(Item element) {
            elementUpdater.set(this, element);
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

    public ConcurrentMostRecentlyInsertedQueue(int capacity) {
        this.capacity = new AtomicInteger(capacity);
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

    public int size() {
        return countOfNodes.intValue();
    }

    public boolean offer(Item element) {
        if (element == null)
            throw new NullPointerException("Element can not be null!");
        Node<Item> elementForAdding = new Node<Item>(element, null);
        for (; ; ) {
            Node<Item> expected = tail;
            Node<Item> updated = expected.getNext();
            if (expected == tail && countOfNodes.intValue() < capacity.intValue()) {
                if (updated == null) {
                    if (expected.casNext(updated, elementForAdding)) {
                        casTail(expected, elementForAdding);
                        countOfNodes.incrementAndGet();
                        return true;
                    }
                } else {
                    casTail(expected, updated);
                }
            } else if (expected == tail && countOfNodes.intValue() >= capacity.intValue()) {
                if (updated == null) {
                    if (expected.casNext(updated, elementForAdding)) {
                        poll();
                        casTail(expected, elementForAdding);
                        countOfNodes.incrementAndGet();
                        return true;
                    }
                } else {
                    casTail(expected, updated);
                }
            }
        }
    }

    public Item poll() {
        for (; ; ) {
            Node<Item> beforeFirstNode = head;
            Node<Item> tailOnPolling = tail;
            Node<Item> firstNode = beforeFirstNode.getNext();
            if (beforeFirstNode == head) {
                if (beforeFirstNode == tailOnPolling) {
                    if (firstNode == null)
                        throw new NoSuchElementException("Empty queue");
                    else
                        casTail(tailOnPolling, firstNode);
                } else if (casHead(beforeFirstNode, firstNode)) {
                    Item element = firstNode.getElement();
                    if (element != null) {
                        firstNode.setElement(null);
                        countOfNodes.decrementAndGet();
                        return element;
                    }
                }
            }
        }
    }

    public Item peek() {
        restartFromHead:
        for (;;) {
            for (Node<Item> h = head, p = h, q;;) {
                Item item = p.element;
                if (item != null || (q = p.next) == null) {
                    return item;
                }
                else if (p == q)
                    continue restartFromHead;
                else
                    p = q;
            }
        }
    }

    @Override
    public void clear() {
        while (!isEmpty()) {
            poll();
        }
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
            l.setElement(null);
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