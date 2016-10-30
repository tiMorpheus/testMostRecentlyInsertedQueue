package blocking;

public class Main {

    static MostRecentlyInsertedBlockingQueue<Integer> queue;

    public static void main(String[] args) throws InterruptedException {

        queue = new MostRecentlyInsertedBlockingQueue(100);

        Runnable r = () -> {
            int i = 0;
            while (i < 100) {
                i++;
                queue.offer((int) (Math.random()*123));
                System.out.println("offer " + queue);
            }
        };
        Runnable r2 = () -> {

            int i = 0;
            while (i < 90) {
                i++;

                try {
                    queue.poll();
                    System.out.println("poll " + queue);
                } catch (Exception e) {
                    System.out.println("Empty");
                }
            }
        };
        new Thread(r).start();
        new Thread(r2).start();
        Thread.sleep(10000);

        System.out.println(queue.size());

    }

}
