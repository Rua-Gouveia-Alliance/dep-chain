package group13.depchain.producerconsumer;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentQueue<E> {
    private ReentrantLock mutex = new ReentrantLock();
    private ReentrantLock sem = new ReentrantLock();
    private final ArrayDeque<E> queue = new ArrayDeque<>();

    public void lock() {
        mutex.lock();
    }

    public void unlock() {
        mutex.unlock();
    }

    public void waitChange() throws InterruptedException {
        sem.wait();
    }

    public void notifyChange() {
        sem.notifyAll();
    }

    public void push(E e) {
        mutex.lock();
        queue.offer(e);
        mutex.unlock();
    }

    public E pop() {
        mutex.lock();
        E result = queue.pop();
        mutex.unlock();
        return result;
    }

    public void remove(E e) {
        mutex.lock();
        queue.remove(e);
        mutex.unlock();
    }

    public ArrayDeque<E> getContainer() {
        return this.queue;
    }
}
