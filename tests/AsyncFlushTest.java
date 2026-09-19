import jdbc.DbAsyncTask;
import java.util.concurrent.*;

public class AsyncFlushTest {
    public static void main(String[] args) throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        DbAsyncTask db = DbAsyncTask.gI();
        db.submit(() -> {
            started.countDown();
            try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        if (!started.await(5, TimeUnit.SECONDS)) throw new AssertionError("Worker did not start");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> flush = executor.submit(db::forceFlush);
            try {
                flush.get(300, TimeUnit.MILLISECONDS);
                throw new AssertionError("Flush returned while a database write was still running");
            } catch (TimeoutException expected) {
                // Empty queue does not mean the active write is complete.
            }
            release.countDown();
            flush.get(5, TimeUnit.SECONDS);
            System.out.println("PASS: flush waits for active database writes");
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }
}
