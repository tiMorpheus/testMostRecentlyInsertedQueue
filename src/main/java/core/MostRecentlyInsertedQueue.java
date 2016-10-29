package core;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

public class MostRecentlyInsertedQueue<Item> extends AbstractQueue<Item> implements Queue<Item>,Iterable<Item>{

    private Node<Item> head;
    private Node<Item> tail;
    private int capacity;
    private int amountOfElements;

    //helper class
    private static class Node<Item>{
        private Item item;
        private Node<Item> next;
    }

    public MostRecentlyInsertedQueue(int capacity) {
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
     *  without violating capacity restrictions.
     *
     * @param  item the item to add
     */
    public boolean offer(Item item) {

        checkNotNull(item);
        if (amountOfElements < capacity){
            addItem(item);
            amountOfElements++;
            return true;
        } else if (amountOfElements >= capacity){
            poll();
            addItem(item);
            amountOfElements++;
            return true;
        }
        return false;
    }

    /**
     * Adds the item to this queue.
     *
     * @param  item the item to add
     */
    private void addItem(Item item) {
        Node current = tail;
        tail = new Node<Item>();
        tail.item = item;
        if (isEmpty()){
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
    public Item poll() {

        if (isEmpty()){
            throw new NoSuchElementException("Queue is empty");
        }
        Item item = head.item;
        head = head.next;
        amountOfElements--;

        if (isEmpty()){
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
    public Item peek() {
        if (isEmpty()){
            throw new NoSuchElementException("Queue is empty");
        }
        return head.item;
    }

    /**
     * Clear the queue
     *
     */
    public void clear() {
        while (!isEmpty()){
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
    public Iterator<Item> iterator()  {
        return new MyQueueIterator<Item>(head);
    }

    private class MyQueueIterator<Item> implements Iterator<Item> {

        private Node<Item> current;

        public MyQueueIterator(Node<Item> first) {
            current = first;
        }

        public boolean hasNext()  {
            return (current != null);
        }

        public void remove(){
            throw new UnsupportedOperationException();
        }

        public Item next() {
            if (!hasNext()){
                throw new NoSuchElementException();
            }
            Item item = current.item;
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
