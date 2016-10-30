package core;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

public class MostRecentlyInsertedQueue<E> extends AbstractQueue<E> implements
        Queue<E> {

    private Node<E> head;
    private Node<E> tail;
    private int capacity;
    private int amountOfElements;

    //helper class
    private static class Node<E> {
        private E item;
        private Node<E> next;
    }

    public MostRecentlyInsertedQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.capacity = capacity;
        this.amountOfElements = 0;
        this.head = null;
        this.tail = null;
    }

    /**
     * Returns true if this queue is empty.
     *
     * @return true if this queue is empty; false otherwise
     */
    public boolean isEmpty() {
        return head == null;
    }

    /**
     * Returns the number of items in this queue.
     *
     * @return the number of items in this queue
     */
    public int size() {
        return amountOfElements;
    }

    /**
     * Inserts the specified element into this queue if it is possible to do so immediately
     * without violating capacity restrictions.
     *
     * @param item the item to add
     */
    public boolean offer(E item) {

        checkNotNull(item);
        if (amountOfElements < capacity) {
            insertElementToQueue(item);
            amountOfElements++;
            return true;
        } else if (amountOfElements >= capacity) {
            poll();
            insertElementToQueue(item);
            amountOfElements++;
            return true;
        }
        return false;
    }

    /**
     * Adds the item to this queue.
     *
     * @param item the item to add
     */
    private void insertElementToQueue(E item) {
        Node current = tail;
        tail = new Node<>();
        tail.item = item;
        if (isEmpty()) {
            head = tail;
        } else {
            current.next = tail;
        }
    }

    /**
     * Retrieves and removes the head of this queue.
     *
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    public E poll() {

        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        E item = head.item;
        head = head.next;
        amountOfElements--;

        if (isEmpty()) {
            tail = null;
        }

        return item;
    }

    /**
     * Retrieves, but does not remove, the head of this queue
     *
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    public E peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        return head.item;
    }

    /**
     * Clear the queue
     */
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
            throw new NullPointerException("You can't offer a 'null' element");
    }

    /**
     * Returns an iterator that iterates over the items in this queue in FIFO order.
     *
     * @return an iterator that iterates over the items in this queue in FIFO order
     */
    public Iterator<E> iterator() {
        return new MyQueueIterator<>(head);
    }

    private class MyQueueIterator<E> implements Iterator<E> {

        private Node<E> current;

        public MyQueueIterator(Node<E> first) {
            current = first;
        }

        public boolean hasNext() {
            return (current != null);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            E item = current.item;
            current = current.next;

            return item;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Node tmp = head;
        sb.append("[");
        while (tmp != null) {
            if (tmp == tail)
                sb.append(tmp.item).append("");
            else
                sb.append(tmp.item).append(", ");
            tmp = tmp.next;
        }
        sb.append("]");
        return sb.toString();
    }
}
