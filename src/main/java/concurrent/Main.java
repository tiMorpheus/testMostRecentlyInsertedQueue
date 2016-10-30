package concurrent;

public class Main {

    static ConcurrentMostRecentlyInsertedQueue queue;

    public static void main(String[] args) throws InterruptedException {

        queue = new ConcurrentMostRecentlyInsertedQueue(5);
        Runnable r = () -> {
            int i = 0;
            while (i < 10) {
                i++;
                queue.offer(1);
                queue.offer((int) (Math.random() * 10));

            }
        };

        Runnable r2 = () -> {
            int i = 0;
            while (i < 7) {
                i++;
                queue.poll();
                System.out.println(queue);
            }
        };
        new Thread(r).start();
        new Thread(r2).start();
        Thread.sleep(1000);

        System.out.println(queue.size());
    }
}
