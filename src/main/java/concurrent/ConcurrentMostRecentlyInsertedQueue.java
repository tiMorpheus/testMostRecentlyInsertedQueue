package concurrent;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class ConcurrentMostRecentlyInsertedQueue<E> extends AbstractQueue<E>
        implements Queue<E> {

    private AtomicInteger capacity;
    private AtomicInteger countOfNodes = new AtomicInteger(0);
    private volatile Node<E> head = new Node<>(null, null);
    private volatile Node<E> tail = head;

    private static class Node<E> {
        private volatile E element;
        private volatile Node next;

        private final AtomicReferenceFieldUpdater<Node, Object> elementUpdater =
                AtomicReferenceFieldUpdater.newUpdater(Node.class, Object.class, "element");

        private final AtomicReferenceFieldUpdater<Node, Node> nextUpdater =
                AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "next");


        public Node(E element, Node next) {
            this.element = element;
            this.next = next;
        }

        public E getElement() {
            return element;
        }

        public boolean casElement(E expect, E update) {
            return elementUpdater.compareAndSet(this, expect, update);
        }

        public void setElement(E element) {
            elementUpdater.set(this, element);
        }

        public Node getNext() {
            return next;
        }

        public boolean casNext(Node<E> expect, Node<E> update) {
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

    public boolean casHead(Node<E> expect, Node<E> update) {
        return headUpdater.compareAndSet(this, expect, update);
    }

    public boolean casTail(Node<E> expect, Node<E> update) {
        return tailUpdater.compareAndSet(this, expect, update);
    }

    public boolean offer(E element) {
        if (element == null)
            throw new NullPointerException("Element can not be null!");
        Node<E> elementForAdding = new Node<E>(element, null);
        for (; ; ) {
            Node<E> expected = tail;
            Node<E> updated = expected.getNext();
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

    /**
     * Retrieves and removes the head of this queue.
     *
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    public E poll() {
        for (; ; ) {
            Node<E> beforeFirstNode = head;
            Node<E> tailOnPolling = tail;
            Node<E> firstNode = beforeFirstNode.getNext();
            if (beforeFirstNode == head) {
                if (beforeFirstNode == tailOnPolling) {
                    if (firstNode == null)
                        throw new NoSuchElementException("Empty queue");
                    else
                        casTail(tailOnPolling, firstNode);
                } else if (casHead(beforeFirstNode, firstNode)) {
                    E element = firstNode.getElement();
                    if (element != null) {
                        firstNode.setElement(null);
                        countOfNodes.decrementAndGet();
                        return element;
                    }
                }
            }
        }
    }

    /**
     * Retrieves, but does not remove, the head of this queue
     *
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    public E peek() {
        restartFromHead:
        for (; ; ) {
            for (Node<E> h = head, p = h, q; ; ) {
                E item = p.element;
                if (item != null || (q = p.next) == null) {
                    return item;
                } else if (p == q)
                    continue restartFromHead;
                else
                    p = q;
            }
        }
    }

    /**
     * Returns the number of items in this queue.
     *
     * @return the number of items in this queue
     */
    public int size() {
        return countOfNodes.intValue();
    }


    public void clear() {
        while (!isEmpty()) {
            poll();
        }
    }

    public Iterator<E> iterator() {
        return new ConcurrentMostRecentlyInsertedIterator();
    }

    private class ConcurrentMostRecentlyInsertedIterator implements Iterator<E> {

        private Node<E> currentNode;
        private Node<E> lastNode;
        private E nextItem;

        ConcurrentMostRecentlyInsertedIterator() {
            advance();
        }

        private E advance() {
            lastNode = currentNode;
            E x = nextItem;

            Node<E> newNode = (currentNode == null) ? firstNode() : currentNode.getNext();
            for (; ; ) {
                if (newNode == null) {
                    currentNode = null;
                    nextItem = null;
                    return x;
                }
                E item = newNode.getElement();
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

        public E next() {
            if (currentNode == null) throw new NoSuchElementException();
            return advance();
        }

        public void remove() {
            Node<E> l = lastNode;
            if (l == null) throw new IllegalStateException();
            l.setElement(null);
            lastNode = null;
        }
    }

    Node<E> firstNode() {
        for (; ; ) {
            Node<E> h = head;
            Node<E> t = tail;
            Node<E> first = h.getNext();
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