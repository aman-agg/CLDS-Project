import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


class TTASlock {
    AtomicBoolean state = new AtomicBoolean(false);

    void lock() {
        while (true) {
            if (state.get() == false) {
                if (state.getAndSet(true) == false) {
                    break;
                }
            }
        }
    }

    void unlock() {
        state.set(false);
    }
}


class TASLockCounter implements Runnable {
    private TASlock lock;
    private static int counter = 0;
    private static final int limit = 1000;
    private static final int threadPoolSize = 5;

    public TASLockCounter(TASlock lock) {
        this.lock = lock;
    }


    @Override
    public void run() {
        while (counter < limit) {
            increaseCounter();
        }
    }

    private void increaseCounter() {
        lock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " : " + counter);
            counter++;
        } finally {
            lock.unlock();
        }
    }
}