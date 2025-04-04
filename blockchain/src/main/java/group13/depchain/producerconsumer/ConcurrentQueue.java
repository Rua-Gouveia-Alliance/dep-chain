package group13.depchain.producerconsumer;

import java.util.ArrayDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentQueue<E> {
    private final ReentrantLock mutex = new ReentrantLock();
    private final ArrayDeque<E> queue = new ArrayDeque<>();
    public final Condition cond = mutex.newCondition();

    public void lock() {
        mutex.lock();
    }

    public void unlock() {
        mutex.unlock();
    }

    public void waitChangeTimeout(long timeout) throws InterruptedException {
        cond.wait(timeout);
    }

    public void waitChange() throws InterruptedException {
        cond.wait();
    }

    public void notifyChange() {
        cond.notifyAll();
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

    public E top() {
        mutex.lock();
        E result = queue.isEmpty() ? null : queue.getLast();
        mutex.unlock();
        return result;
    }

    public void remove(E e) {
        mutex.lock();
        queue.remove(e);
        mutex.unlock();
    }

    public int length() {
        return this.queue.size();
    }

    public ArrayDeque<E> getContainer() {
        return this.queue;
    }
}
