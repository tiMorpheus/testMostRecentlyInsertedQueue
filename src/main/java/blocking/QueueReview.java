package blocking;

import java.util.Queue;

public class QueueReview {
    public static void main(String[] args) {
        Queue<Integer> queue = new MostRecentlyInsertedBlockingQueue<>(3);
        System.out.println("START TEST");
        queue.offer(1);             // queue.size(): 1, contents (head -> tail): [ 1 ]
        System.out.println(queue);
        queue.offer(2);             // queue.size(): 2, contents (head -> tail): [ 1, 2 ]
        System.out.println(queue);
        queue.offer(3);             // queue.size(): 3, contents (head -> tail): [ 1, 2, 3 ]
        System.out.println(queue);
        queue.offer(4);             // queue.size(): 3, contents (head -> tail): [ 2, 3, 4 ]
        System.out.println(queue);
        queue.offer(5);             // queue.size(): 3, contents (head -> tail): [ 3, 4, 5 ]
        System.out.println(queue);
        int poll1 = queue.poll();   // queue.size(): 2, contents (head -> tail): [ 4, 5 ], poll1 = 3
        System.out.println(queue + ", poll1 = " + poll1);
        int poll2 = queue.poll();   // queue.size(): 1, contents (head -> tail): [ 5 ], poll2 = 4
        System.out.println(queue + ", poll2 = " + poll2);
        queue.clear();              // queue.size(): 0, contents (head -> tail): [ ]
        System.out.println(queue);
    }
}
