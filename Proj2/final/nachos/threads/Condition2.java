package nachos.threads;

import java.util.LinkedList;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see nachos.threads.Condition
 */
public class Condition2 {
	/**
	 * Allocate a new condition variable.
	 *
	 * @param conditionLock the lock associated with this condition variable.
	 * The current thread must hold this lock whenever it uses <tt>sleep()</tt>,
	 * <tt>wake()</tt>, or <tt>wakeAll()</tt>.
	 */

	private LinkedList<KThread> queue; //yiwen
	private static final int N = 20; // yiwen

	public Condition2(Lock conditionLock) {
		this.conditionLock = conditionLock;
		queue = new LinkedList<KThread>(); //yiwen
	}

	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The current
	 * thread must hold the associated lock. The thread will automatically
	 * reacquire the lock before <tt>sleep()</tt> returns.
	 */
	//yiwen
	public void sleep() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		conditionLock.release();

		boolean status = Machine.interrupt().disable();

		queue.add(KThread.currentThread());
		KThread.sleep();
		System.out.println("current thread goes into sleep");

		Machine.interrupt().restore(status);

		conditionLock.acquire();
	}

	/**
	 * Wake up at most one thread sleeping on this condition variable. The
	 * current thread mus t hold the associated lock.
	 */
	//yiwen
	public void wake() {
		System.out.println("call wake()");
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		boolean status = Machine.interrupt().disable();
		if(!queue.isEmpty())
			((KThread)queue.removeFirst()).ready();
		Machine.interrupt().restore(status);
	}

	/**
	 * Wake up all threads sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 */
	//yiwen
	public void wakeAll() {
		System.out.println("call wakeAll");
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		while (queue.isEmpty() != true)
			wake();
	}

	private static class TestThread implements Runnable {
		private Lock lock;
		private Condition2 c2;
		TestThread(Lock lock, Condition2 c2) {
			this.c2 = c2;
			this.lock = lock;
		}

		public void run() {
			lock.acquire();
			System.out.println(KThread.currentThread().getName() + " get the lock");
			c2.sleep();
			System.out.println(KThread.currentThread().getName() + " get the lock second");
			lock.release();
			System.out.println(KThread.currentThread().getName() + " lose the lock");
		}

	}

	public static void selfTest() {

		Lock lock = new Lock();
		Condition2 c2 = new Condition2(lock);

		KThread t[] = new KThread[N];
		for (int i=0; i< N; i++) {
			t[i] = new KThread(new TestThread(lock, c2));
			t[i].setName("T" + i);
			t[i].fork();
		}

		KThread.yield();
		lock.acquire();
		c2.wake();
		c2.wakeAll();

		lock.release();

		System.out.println("Finished");

		t[N-1].join();

	}

	private Lock conditionLock;
}
