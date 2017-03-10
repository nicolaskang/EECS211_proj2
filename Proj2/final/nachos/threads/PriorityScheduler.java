package nachos.threads;

import nachos.machine.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Comparator;
//import java.util.PriorityQueue;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the thread
 * that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fashion to
 * all the highest-priority threads, and ignores all other threads. This has the
 * potential to starve a thread if there's always a thread waiting with higher
 * priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
		
	}

	/**
	 * Allocate a new priority thread queue.
	 *
	 * @param transferPriority <tt>true</tt> if this queue should transfer
	 * priority from waiting threads to the owning thread.
	 * @return a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}

	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}

	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}

	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum
				&& priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();
		boolean ret = true;

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			ret = false;
		else
			setPriority(thread, priority + 1);

		Machine.interrupt().restore(intStatus);
		return ret;
	}

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();
		boolean ret = true;

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			ret = false;
		else
			setPriority(thread, priority - 1);

		Machine.interrupt().restore(intStatus);
		return ret;
	}

	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;
	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 0;
	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = 7;

	/**
	 * Return the scheduling state of the specified thread.
	 *
	 * @param thread
	 *            the thread whose scheduling state to return.
	 * @return the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue extends ThreadQueue {
		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
            /* Tao's implementation starts*/
			if (!waitQueue.isEmpty()) {
				//Get the thread but keep it in the waitQueue
				acquire(waitQueue.poll().thread);
				return lockingThread;
			} else {
				return null;
			}
            /* Tao's implementation ends*/
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 *
		 * @return the next thread that <tt>nextThread()</tt> would return.
		 */
		protected ThreadState pickNextThread() {
            /* Tao's implementation starts*/
			// Get the thread and remove it from waitQueue
			return waitQueue.peek();
            /* Tao's implementation ends*/
		}

		public void print() {
            Lib.assertTrue(Machine.interrupt().disabled());
            // implement me (if you want)
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		boolean transferPriority;
        
        /* Tao's implementation starts*/
		/**
		 * Use the priorityQueue in Java directly
		 */
		public java.util.PriorityQueue<ThreadState> waitQueue = new java.util.PriorityQueue<ThreadState>(8,new ThreadStateComparator<ThreadState>(this));
		/**
		 * The <tt>KThread</tt> that locks this PriorityQueue, the initial value is null.
		 */
		public KThread lockingThread = null;

		protected class ThreadStateComparator<T extends ThreadState> implements Comparator<T> {
			protected ThreadStateComparator(nachos.threads.PriorityScheduler.PriorityQueue pq) {
				priorityQueue = pq;
			}

			@Override
			public int compare(T o1, T o2) {
				//Comparing effective priority first
				int o1_effectivePriority = o1.getEffectivePriority(), o2_effectivePriority = o2.getEffectivePriority();
				if (o1_effectivePriority < o2_effectivePriority) {
					return 1;
					} else if (o1_effectivePriority > o2_effectivePriority) {
					return -1;
					} else {
					//Then compare by the time of threads waiting in this queue
					long o1_waitingTime = o1.waiting.get(priorityQueue), o2_waitingTime = o2.waiting.get(priorityQueue);
					/*
					 * Note: the waiting maps should contain THIS at this point. If they don't then there is an error
					 */
					if (o1_waitingTime < o2_waitingTime) {
						return -1;
					} else if (o1_waitingTime > o2_waitingTime) {
						return 1;
					} else {
						return 0; // Should be the same thread(same priority and waiting time)
					}
				}
			}
			public PriorityQueue priorityQueue;
		}
        /* Tao's implementation ends*/
	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue it's
	 * waiting for, if any.
	 *
	 * @see nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState {
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 *
		 * @param thread
		 *            the thread this state belongs to.
		 */
		ThreadState(KThread thread) {
			this.thread = thread;
            /* Tao's implementation starts*/
            // assign the temporary priority to effectivePriority
			effectivePriority = priorityDefault;
            /* Tao's implementation ends*/
			setPriority(priorityDefault);
		}
        
        /* Tao's implementation starts*/
		/**
		 * Release this priority queue from the resources this ThreadState has locked.
		 * This is the only time the effective priority of a thread can go down and needs a full recalculation.
		 * We can detect if this exists if the top effective priority of the queue we are release is equal to this current effective priority.
		 * If it is less than (it cannot be greater by definition), then we know that something else is contributing to the effective priority of <tt>this</tt>.
		 * @param priorityQueue
		 */
		private void release(PriorityQueue priorityQueue) {
			// if there are priorityQueues exist, remove priorityQueue from my acquired set
			if (acquired.remove(priorityQueue)) {
				priorityQueue.lockingThread = null;
				//update the status of effective priority
				updateEP();
			}
		}
        /* Tao's implementation ends*/

		/**
		 * Return the priority of the associated thread.
		 *
		 * @return the priority of the associated thread.
		 */
		int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 *
		 * @return the effective priority of the associated thread.
		 */
		int getEffectivePriority() {
            /* Tao's implementation starts*/
            // Return the effectivePriority
			return effectivePriority;
            /* Tao's implementation ends*/
		}

		/**
		 * Set the priority of the associated thread to the specified value. <p>
		 * This method assumes the priority has changed. Protection is from PriorityScheduler class calling this.
		 * @param priority the new priority.
		 */
		void setPriority(int priority) {
        /* Tao's implementation starts*/
			this.priority = priority;
			// Always refresh Effective priority when Thread state/waitQueue changes
			updateEP();
		}

		protected void updateEP() {
			for (PriorityQueue pq : waiting.keySet())
				pq.waitQueue.remove(this);

			int tempPriority = priority;

			for (PriorityQueue pq : acquired) {
				if (pq.transferPriority) {
					ThreadState queueHolder = pq.waitQueue.peek();
					if (queueHolder != null) {
						int queueHolder_EP = queueHolder.getEffectivePriority();
						//Always get the maximum priority of threads in queue.
						tempPriority = Math.max(queueHolder_EP,tempPriority);
					}
				}
			}
			// If the tempPriority not equal to effectivePriority, there is no need to transfer(donate) the priority
			boolean needToTransfer = (tempPriority != effectivePriority);

			effectivePriority = tempPriority;

			/*
			 * Add this back and update all the results
			 */
			for (PriorityQueue pq : waiting.keySet())
				pq.waitQueue.add(this);

            if (needToTransfer) {
				for (PriorityQueue pq : waiting.keySet()) {
					if (pq.transferPriority && pq.lockingThread != null)
						getThreadState(pq.lockingThread).updateEP();
				}
            }
            /* Tao's implementation ends*/
		}
        

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the resource
		 * guarded by <tt>waitQueue</tt>. This method is only called if the
		 * associated thread cannot immediately obtain access.
		 *
		 * @param priorityQ
		 *            the queue that the associated thread is now waiting on.
		 *
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
		void waitForAccess(PriorityQueue priorityQ) {
            /* Tao's implementation starts*/
			if (!waiting.containsKey(priorityQ)) {
				//release this wait queue
				release(priorityQ);

				//Put it on the waiting set
				waiting.put(priorityQ, Machine.timer().getTime());
				priorityQ.waitQueue.add(this);

				if (priorityQ.lockingThread != null) {
					getThreadState(priorityQ.lockingThread).updateEP();
				}
			}
            /* Tao's implementation ends*/
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 *
		 * @see nachos.threads.ThreadQueue#acquire
		 * @see nachos.threads.ThreadQueue#nextThread
		 */
		void acquire(PriorityQueue priorityQ) {
            /* Tao's implementation starts*/
			//Release the current locking thread
			if (priorityQ.lockingThread != null) {
				getThreadState(priorityQ.lockingThread).release(priorityQ);
			}

			// If it exists, remove the older thread state from queues.
			priorityQ.waitQueue.remove(this);

			//Acquire the thread, add Priority Queue to acquired, remove it from waiting set
			priorityQ.lockingThread = this.thread;
			acquired.add(priorityQ);
			waiting.remove(priorityQ);

			updateEP();
            /* Tao's implementation ends*/
		}

		/** The thread with which this object is associated. */
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;
		protected int effectivePriority;

		// A set of the PriorityQueues this ThreadState has acquired
		public HashSet<PriorityQueue> acquired = new HashSet<PriorityQueue>();
		// A map of all the PriorityQueues this ThreadState: key is PriorityQueues and value is waiting time indicates the sequence
		public HashMap<PriorityQueue, Long> waiting= new HashMap<PriorityQueue, Long>();
	}

	/**
	 * Self-test.
	 *
	 * XXX: After finish it, delete this code snippet.
	 */
	public static void selfTest() {
		boolean oldP;
		final Lock lock1 = new Lock();
		final Lock lock2 = new Lock();

		// low thread
		KThread lowKt1 = new KThread(new Runnable() {
			public void run() {
				lock1.acquire();

				System.out.println("--------Low thread 1 acquired lock1");

				for(int i=1; i <=3; i++) {
					System.out.println("--------Low thread 1 running "+i+" times ...");
					KThread.yield();
				}

				System.out.println("--------Low thread 1 releasing lock1 ...");

				lock1.release();
				KThread.yield();

				System.err.println("--------Low thread 1 running AFTER releasing the lock 1...");
			}
		}).setName("Low Thread 1");
		oldP = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(lowKt1, 1);
		Machine.interrupt().restore(oldP);

		// middle thread
		KThread midKt1 = new KThread(new Runnable() {
			public void run() {
				lock2.acquire();

				System.out.println("--------Middle thread 1 has acquired lock2 ...");

				for (int i = 0; i < 3; i++) {
					System.out.println("--------Middle thread 1 running "+i+" times ...");
					KThread.yield();
				}

				System.out.println("--------Middle thread 1 releasing lock2 ...");

				lock2.release();
				KThread.yield();

				System.err.println("--------Middle thread 1 running AFTER releasing the lock 2...");
			}
		}).setName("Middle Thread 1");
		oldP = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(midKt1, 1);
		Machine.interrupt().restore(oldP);

		// high thread
		KThread highKt1 = new KThread(new Runnable() {
			public void run() {
				lock1.acquire();
				lock2.acquire();

				System.out.println("--------High thread 1 get lock 1, 2, now yield");
				KThread.yield();

				for (int i = 0; i < 3; i++) {
					System.out.println("--------High thread 1 running "+i+" times ...");
					KThread.yield();
				}

				System.out.println("--------High thread 1 releasing lock 1, 2, now yield");
				lock2.release();
				lock1.release();
				KThread.yield();

				System.err.println("--------High thread 1 running AFTER releasing the lock 1,2...");
			}
		}).setName("High Thread 1");
		oldP = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(highKt1, 5);
		Machine.interrupt().restore(oldP);

		// high thread
		KThread highKt2 = new KThread(new Runnable() {
			public void run() {
				lock2.acquire();

				System.out.println("--------High thread 2 get lock 2");

				for (int i = 0; i < 3; i++) {
					System.out.println("--------High thread 1 running "+i+" times ...");
					KThread.yield();
				}
				System.out.println("--------High thread 2 releasing lock 2, now yield");

				lock2.release();
				KThread.yield();

				System.err.println("--------High thread 2 running AFTER releasing the lock 2...");
			}
		}).setName("High Thread 2");
		oldP = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(highKt2, 5);
		Machine.interrupt().restore(oldP);

		lowKt1.fork();
		KThread.yield();

		midKt1.fork();
		KThread.yield();

		highKt1.fork();
		highKt2.fork();
		KThread.yield();

		highKt2.join();
		highKt1.join();
		midKt1.join();
		lowKt1.join();
	}

	public static void selfTest1() {
		System.out.println("---------PriorityScheduler test---------------------");
		PriorityScheduler s = new PriorityScheduler();
		ThreadQueue queue = s.newThreadQueue(true);
		ThreadQueue queue2 = s.newThreadQueue(true);
		ThreadQueue queue3 = s.newThreadQueue(true);

		KThread thread1 = new KThread();
		KThread thread2 = new KThread();
		KThread thread3 = new KThread();
		KThread thread4 = new KThread();
		thread1.setName("thread1");
		thread2.setName("thread2");
		thread3.setName("thread3");
		thread4.setName("thread4");


		boolean intStatus = Machine.interrupt().disable();

		queue3.acquire(thread1);
		queue.acquire(thread1);
		queue.waitForAccess(thread2);
		queue2.acquire(thread4);
		queue2.waitForAccess(thread1);
		System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority());
		System.out.println("thread2 EP="+s.getThreadState(thread2).getEffectivePriority());
		System.out.println("thread4 EP="+s.getThreadState(thread4).getEffectivePriority());

		s.getThreadState(thread2).setPriority(3);

		System.out.println("After setting thread2's EP=3:");
		System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority());
		System.out.println("thread2 EP="+s.getThreadState(thread2).getEffectivePriority());
		System.out.println("thread4 EP="+s.getThreadState(thread4).getEffectivePriority());

		queue.waitForAccess(thread3);
		s.getThreadState(thread3).setPriority(5);

		System.out.println("After adding thread3 with EP=5:");
		System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority());
		System.out.println("thread2 EP="+s.getThreadState(thread2).getEffectivePriority());
		System.out.println("thread3 EP="+s.getThreadState(thread3).getEffectivePriority());
		System.out.println("thread4 EP="+s.getThreadState(thread4).getEffectivePriority());

		s.getThreadState(thread3).setPriority(2);

		System.out.println("After setting thread3 EP=2:");
		System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority());
		System.out.println("thread2 EP="+s.getThreadState(thread2).getEffectivePriority());
		System.out.println("thread3 EP="+s.getThreadState(thread3).getEffectivePriority());
		System.out.println("thread4 EP="+s.getThreadState(thread4).getEffectivePriority());

		System.out.println("Thread1 acquires queue and queue3");

		Machine.interrupt().restore(intStatus);
		System.out.println("--------End PriorityScheduler test------------------");
	}

	public static void selfTest2() {
		boolean oldP;
		final Lock lock1 = new Lock();
		final Lock lock2 = new Lock();

		// low priority thread
		KThread lowKt1 = new KThread(new Runnable() {
			public void run() {
				lock1.acquire();

				System.out.println("!!!Low thread 1 acquired lock1");

				for(int i=1; i <=3; i++) {
					System.out.println("!!!Low thread 1 running "+i+" times ...");
					KThread.yield();
				}

				System.out.println("!!!Low thread 1 releasing lock1 ...");

				lock1.release();
				KThread.yield();

				System.err.println("!!!Low thread 1 running AFTER releasing the lock ...");
			}
		}).setName("Low Thread 1");

		oldP = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(lowKt1, 1);
		Machine.interrupt().restore(oldP);

		// low priority thread
		KThread lowKt2 = new KThread(new Runnable() {
			public void run() {
				lock2.acquire();

				System.out.println("!!!Low thread 2 acquired lock2");

				for(int i=1; i <=3; i++) {
					System.out.println("!!!Low thread 2 running "+i+" times ...");
					KThread.yield();
				}

				System.out.println("!!!Low thread 2 releasing lock2 ...");

				lock2.release();
				KThread.yield();

				System.err.println("!!!Low thread 2 running AFTER releasing the lock ...");
			}
		}).setName("Low Thread 2");

		oldP = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(lowKt2, 1);
		Machine.interrupt().restore(oldP);

		// high priority thread
		KThread highKt = new KThread(new Runnable() {
			public void run() {
				lock1.acquire();

				System.out.println("!!!High thread acquired lock1");

				lock1.release();

				System.out.println("!!!High thread released lock1");

				lock2.acquire();

				System.out.println("!!!High thread acquired lock2");

				lock2.release();

				System.out.println("!!!High thread released lock2");
			}
		}).setName("High Thread");

		oldP = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(highKt, 6);
		Machine.interrupt().restore(oldP);

		// middle priority thread
		KThread middleKt = new KThread(new Runnable() {
			public void run() {
				for(int i=1;i<=3;i++) {
					System.out.println("!!!Middle thread running "+i+" times ...");

					KThread.yield();
				}
			}
		}).setName("Middle Thread");

		oldP = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(middleKt, 4);
		Machine.interrupt().restore(oldP);

		lowKt1.fork();
		lowKt2.fork();

		//start low thread, let it acquire lock1
		KThread.yield();

		middleKt.fork();
		highKt.fork();

		KThread.yield();

		highKt.join();
		middleKt.join();
		lowKt1.join();
		lowKt2.join();
	}
}

