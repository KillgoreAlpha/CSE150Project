package nachos.threads;

import nachos.machine.*;

/**
 * A KThread is a thread that can be used to execute Nachos kernel code. Nachos
 * allows multiple threads to run concurrently.
 * 
 * To create a new thread of execution, first declare a class that implements
 * the <tt>Runnable</tt> interface. That class then implements the <tt>run</tt>
 * method. An instance of the class can then be allocated, passed as an argument
 * when creating <tt>KThread</tt>, and forked. For example, a thread that
 * computes pi could be written as follows:
 * 
 * <p>
 * <blockquote>
 * 
 * <pre>
 * class PiRun implements Runnable {
 * 	public void run() {
 *         // compute pi
 *         ...
 *     }
 * }
 * </pre>
 * 
 * </blockquote>
 * <p>
 * The following code would then create a thread and start it running:
 * 
 * <p>
 * <blockquote>
 * 
 * <pre>
 * PiRun p = new PiRun();
 * new KThread(p).fork();
 * </pre>
 * 
 * </blockquote>
 */
public class KThread {
	/**
	 * Get the current thread.
	 * 
	 * @return the current thread.
	 */
	public static KThread currentThread() {
		Lib.assertTrue(currentThread != null);
		return currentThread;
	}

	/**
	 * Allocate a new <tt>KThread</tt>. If this is the first <tt>KThread</tt>,
	 * create an idle thread as well.
	 */
	public KThread() {
		if (currentThread != null) {
			tcb = new TCB();
		}
		else {
			readyQueue = ThreadedKernel.scheduler.newThreadQueue(false);
			readyQueue.acquire(this);

			currentThread = this;
			tcb = TCB.currentTCB();
			name = "main";
			restoreState();

			createIdleThread();
		}
	}

	/**
	 * Allocate a new KThread.
	 * 
	 * @param target the object whose <tt>run</tt> method is called.
	 */
	public KThread(Runnable target) {
		this();
		this.target = target;
	}

	/**
	 * Set the target of this thread.
	 * 
	 * @param target the object whose <tt>run</tt> method is called.
	 * @return this thread.
	 */
	public KThread setTarget(Runnable target) {
		Lib.assertTrue(status == statusNew);

		this.target = target;
		return this;
	}

	/**
	 * Set the name of this thread. This name is used for debugging purposes
	 * only.
	 * 
	 * @param name the name to give to this thread.
	 * @return this thread.
	 */
	public KThread setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Get the name of this thread. This name is used for debugging purposes
	 * only.
	 * 
	 * @return the name given to this thread.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the full name of this thread. This includes its name along with its
	 * numerical ID. This name is used for debugging purposes only.
	 * 
	 * @return the full name given to this thread.
	 */
	public String toString() {
		return (name + " (#" + id + ")");
	}

	/**
	 * Deterministically and consistently compare this thread to another thread.
	 */
	public int compareTo(Object o) {
		KThread thread = (KThread) o;

		if (id < thread.id)
			return -1;
		else if (id > thread.id)
			return 1;
		else
			return 0;
	}

	// Self Test Code:
	private static void joinTest1 () {
		KThread child1 = new KThread( new Runnable () {
			public void run() {
				System.out.println("I (heart) Nachos!");
			}
			});
		child1.setName("child1").fork();
	
		// We want the child to finish before we call join.  Although
		// our solutions to the problems cannot busy wait, our test
		// programs can!
	
		for (int i = 0; i < 5; i++) {
			System.out.println ("busy...");
			KThread.currentThread().yield();
		}
	
		child1.join();
		System.out.println("After joining, child1 should be finished.");
		System.out.println("is it? " + (child1.status == statusFinished));
		Lib.assertTrue((child1.status == statusFinished), " Expected child1 to be finished.");
		}

	public static void joinTest2() {

		Lib.debug(dbgThread, "Enter KThread.selfTest");
		/**
		 * Allocate a new thread setting the target to point to a 
		 * PingTest object whose run method will be called when the 
		 * newly created thread executes. 
		 */
		KThread th1 = new KThread(new PingTest(1));
		/**
		 * Set the name of the new thread to "forked thread 1" 
		 */
		th1.setName("forked thread 1");
		/**
		 * Execute fork() to begin execution of the new thread 
		 * (putting it in the ready queue). This new thread will 
		 * eventually execute the PingTest object's run() method when
		 * scheduled. The current thread (that created this new thread) 
		 * will return from the fork() method call resuming its own 
		 * execution, thus, giving us 2 concurrent thread.
		 */
		th1.fork();
		/**
		 * The current thread that returned from fork() (creating the 
		 * new thread th1 above) will then create its own PingTest 
		 * object and execute the run method. So now we have two 
		 * threads running the PingTest's run() method.
		 */
		new PingTest(0).run2();
		/**
		 * Current thread calls join() on the newly created thread 
		 * thus going to sleep till th1 finishes.
		 */
		th1.join(); 
		/**
		 * Try to join with th1 again. This should immediately return
		 * as th1 should have already finished.
		 */
		th1.join();
	}

	public static void joinTest3() {
		System.out.println("joinTest3: A thread calling join() on itself should cause an assertion error.");
	
		KThread self = KThread.currentThread();
		System.out.println("joinTest3: Current thread (" + self.getName() + ") is about to call join() on itself...");
		self.join(); // This should trigger an assertion error in join().
		
		System.out.println("joinTest3 ERROR: Should never reach here if the assertion is working properly.");
	}

	public static void joinTest4() {
		System.out.println("joinTest4: Two separate threads will attempt to join the same target; " +
						   "the second attempt should produce an error assertion.");
	
		// This child will take a little time so that both joiner threads
		// have a chance to attempt to join
		final KThread child = new KThread(new Runnable() {
			public void run() {
				System.out.println("joinTest4 (child): Starting and yielding a few times...");
				for (int i = 0; i < 5; i++) {
					KThread.yield();
				}
				System.out.println("joinTest4 (child): Finishing now...");
			}
		}).setName("child");
		child.fork();
	
		// First "joiner" thread
		KThread joiner1 = new KThread(new Runnable() {
			public void run() {
				System.out.println("joinTest4 (joiner1): Attempting child.join()...");
				child.join();
				System.out.println("joinTest4 (joiner1): child.join() returned normally.");
			}
		}).setName("joiner1");
		joiner1.fork();
	
		// Give joiner1 a moment to call join
		KThread.yield();
	
		// Second "joiner" thread (which will trigger an assertion error)
		KThread joiner2 = new KThread(new Runnable() {
			public void run() {
				System.out.println("joinTest4 (joiner2): Attempting child.join(), expect an error");
				child.join();  // This should fail if your code enforces only one join per target.
				System.out.println("joinTest4 (joiner2): ERROR - We should never see this message if assertion works correctly.");
			}
		}).setName("joiner2");
		joiner2.fork();
	}
		
	

	/**
	 * Causes this thread to begin execution. The result is that two threads are
	 * running concurrently: the current thread (which returns from the call to
	 * the <tt>fork</tt> method) and the other thread (which executes its
	 * target's <tt>run</tt> method).
	 */
	public void fork() {
		Lib.assertTrue(status == statusNew);
		Lib.assertTrue(target != null);

		Lib.debug(dbgThread, "Forking thread: " + toString() + " Runnable: "
				+ target);

		boolean intStatus = Machine.interrupt().disable();

		tcb.start(new Runnable() {
			public void run() {
				runThread();
			}
		});

		ready();

		Machine.interrupt().restore(intStatus);
	}

	private void runThread() {
		begin();
		target.run();
		finish();
	}

	private void begin() {
		Lib.debug(dbgThread, "Beginning thread: " + toString());

		Lib.assertTrue(this == currentThread);

		restoreState();

		Machine.interrupt().enable();
	}

	/**
	 * Finish the current thread and schedule it to be destroyed when it is safe
	 * to do so. This method is automatically called when a thread's
	 * <tt>run</tt> method returns, but it may also be called directly.
	 * 
	 * The current thread cannot be immediately destroyed because its stack and
	 * other execution state are still in use. Instead, this thread will be
	 * destroyed automatically by the next thread to run, when it is safe to
	 * delete this thread.
	 */
	public static void finish() {
		Lib.debug(dbgThread, "Finishing thread: " + currentThread.toString());
		Machine.interrupt().disable();

		Machine.autoGrader().finishingCurrentThread();
 
		Lib.assertTrue(toBeDestroyed == null);
		toBeDestroyed = currentThread;
 
		currentThread.status = statusFinished;
	   
		//pseudo code:
 
		//Check if there is a thread recorded in our "joins" class.
		//If there is, wake it up (put it in the ready queue?)
		//If there isn't, do nothing.
 
		if(currentThread.joinCond != null){

			joinLock.acquire();
			currentThread.joinCond.wake();  // This is our ability to wake up the thread that tried to join us.
			joinLock.release();
		}
 
		//------------
		// is this needed below?
		// boolean interrupt = Machine.interrupt().disable();

		sleep(); // after destruction it takes care of it right?

	}

	/**
	 * Relinquish the CPU if any other thread is ready to run. If so, put the
	 * current thread on the ready queue, so that it will eventually be
	 * rescheuled.
	 * 
	 * <p>
	 * Returns immediately if no other thread is ready to run. Otherwise returns
	 * when the current thread is chosen to run again by
	 * <tt>readyQueue.nextThread()</tt>.
	 * 
	 * <p>
	 * Interrupts are disabled, so that the current thread can atomically add
	 * itself to the ready queue and switch to the next thread. On return,
	 * restores interrupts to the previous state, in case <tt>yield()</tt> was
	 * called with interrupts disabled.
	 */
	public static void yield() {
		Lib.debug(dbgThread, "Yielding thread: " + currentThread.toString());

		Lib.assertTrue(currentThread.status == statusRunning);

		boolean intStatus = Machine.interrupt().disable();

		currentThread.ready();

		runNextThread();

		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Relinquish the CPU, because the current thread has either finished or it
	 * is blocked. This thread must be the current thread.
	 * 
	 * <p>
	 * If the current thread is blocked (on a synchronization primitive, i.e. a
	 * <tt>Semaphore</tt>, <tt>Lock</tt>, or <tt>Condition</tt>), eventually
	 * some thread will wake this thread up, putting it back on the ready queue
	 * so that it can be rescheduled. Otherwise, <tt>finish()</tt> should have
	 * scheduled this thread to be destroyed by the next thread to run.
	 */
	public static void sleep() {
		Lib.debug(dbgThread, "Sleeping thread: " + currentThread.toString());

		Lib.assertTrue(Machine.interrupt().disabled());

		if (currentThread.status != statusFinished)
			currentThread.status = statusBlocked;

		runNextThread();
	}

	/**
	 * Moves this thread to the ready state and adds this to the scheduler's
	 * ready queue.
	 */
	public void ready() {
		Lib.debug(dbgThread, "Ready thread: " + toString());

		Lib.assertTrue(Machine.interrupt().disabled());
		Lib.assertTrue(status != statusReady);

		status = statusReady;
		if (this != idleThread)
			readyQueue.waitForAccess(this);

		Machine.autoGrader().readyThread(this);
	}

	/**
	 * Waits for this thread to finish. If this thread is already finished,
	 * return immediately. This method must only be called once; the second call
	 * is not guaranteed to return. This thread must not be the current thread.
	 */
	public void join() {
		Lib.debug(dbgThread, "Joining to thread: " + toString());
		Lib.assertTrue(this != currentThread, "ERROR: Threads may not join themselves."); // this is a thread can't join itself check
// obtaining the lock prevents 2 threads from attempting to join at the same time. I mean they can both run join but they will be forced to do so atomically.
		joinLock.acquire();
		// boolean interupts = Machine.interrupt().disable();
		if(this.status == statusFinished){ //This is us checking if the thread we are trying to join is already finished. "this" refers to the thread we are trying to join.
			//do i put it in the ready queue? the instructions say "B returns immediately from join without waiting if A has already finished."
			//If i disabled interrupts I should enable them before I return.
			joinLock.release();
			// Machine.interrupt().restore(interupts);
			return;
		}

		Lib.assertTrue(this.joinCond == null, "ERROR: Another thread has already tried to join this thread."); //This will return an error and stop the rest from happening

		this.joinCond = new Condition2(joinLock); //record whos trying to join in the thread we are trying to join's "joins" variable
		// Do i make the thread who is waiting status=blocked? No sleep does that for me.
		this.joinCond.sleep(); //put the current thread to sleep, is this the correct operation to make the thread wait inside of KThread.join()?
		joinLock.release();
		//------------

	}

	/**
	 * Create the idle thread. Whenever there are no threads ready to be run,
	 * and <tt>runNextThread()</tt> is called, it will run the idle thread. The
	 * idle thread must never block, and it will only be allowed to run when all
	 * other threads are blocked.
	 * 
	 * <p>
	 * Note that <tt>ready()</tt> never adds the idle thread to the ready set.
	 */
	private static void createIdleThread() {
		Lib.assertTrue(idleThread == null);

		idleThread = new KThread(new Runnable() {
			public void run() {
				while (true)
					KThread.yield();
			}
		});
		idleThread.setName("idle");

		Machine.autoGrader().setIdleThread(idleThread);

		idleThread.fork();
	}

	/**
	 * Determine the next thread to run, then dispatch the CPU to the thread
	 * using <tt>run()</tt>.
	 */
	private static void runNextThread() {
		KThread nextThread = readyQueue.nextThread();
		if (nextThread == null)
			nextThread = idleThread;

		nextThread.run();
	}

	/**
	 * Dispatch the CPU to this thread. Save the state of the current thread,
	 * switch to the new thread by calling <tt>TCB.contextSwitch()</tt>, and
	 * load the state of the new thread. The new thread becomes the current
	 * thread.
	 * 
	 * <p>
	 * If the new thread and the old thread are the same, this method must still
	 * call <tt>saveState()</tt>, <tt>contextSwitch()</tt>, and
	 * <tt>restoreState()</tt>.
	 * 
	 * <p>
	 * The state of the previously running thread must already have been changed
	 * from running to blocked or ready (depending on whether the thread is
	 * sleeping or yielding).
	 * 
	 * @param finishing <tt>true</tt> if the current thread is finished, and
	 * should be destroyed by the new thread.
	 */
	private void run() {
		Lib.assertTrue(Machine.interrupt().disabled());

		Machine.yield();

		currentThread.saveState();

		Lib.debug(dbgThread, "Switching from: " + currentThread.toString()
				+ " to: " + toString());

		currentThread = this;

		tcb.contextSwitch();

		currentThread.restoreState();
	}

	/**
	 * Prepare this thread to be run. Set <tt>status</tt> to
	 * <tt>statusRunning</tt> and check <tt>toBeDestroyed</tt>.
	 */
	protected void restoreState() {
		Lib.debug(dbgThread, "Running thread: " + currentThread.toString());

		Lib.assertTrue(Machine.interrupt().disabled());
		Lib.assertTrue(this == currentThread);
		Lib.assertTrue(tcb == TCB.currentTCB());

		Machine.autoGrader().runningThread(this);

		status = statusRunning;

		if (toBeDestroyed != null) {
			toBeDestroyed.tcb.destroy();
			toBeDestroyed.tcb = null;
			toBeDestroyed = null;
		}
	}

	/**
	 * Prepare this thread to give up the processor. Kernel threads do not need
	 * to do anything here.
	 */
	protected void saveState() {
		Lib.assertTrue(Machine.interrupt().disabled());
		Lib.assertTrue(this == currentThread);
	}

	private static class PingTest implements Runnable {
		PingTest(int which) {
			this.which = which;
		}

		public void run() {
			for (int i = 0; i < 5; i++) {
				System.out.println("*** the thread " + which + " looped " + i
						+ " times");
				currentThread.yield();
			}
		}
		public void run2() {
			for (int i = 0; i < 50; i++) {
				System.out.println("*** the thread " + which + " looped " + i
						+ " times");
				currentThread.yield();
			}
		}

		private int which;
	}

	/**
	 * Tests whether this module is working.
	 */
	public static void selfTest() {
		Lib.debug(dbgThread, "Enter KThread.selfTest");

		new KThread(new PingTest(1)).setName("forked thread").fork();
		new PingTest(0).run();
		joinTest1();
		joinTest2();
		// joinTest3();
		// joinTest4();
	}

	private static final char dbgThread = 't';

	/**
	 * Additional state used by schedulers.
	 * 
	 * @see nachos.threads.PriorityScheduler.ThreadState
	 */
	public Object schedulingState = null;

	private static final int statusNew = 0;

	private static final int statusReady = 1;

	private static final int statusRunning = 2;

	private static final int statusBlocked = 3;

	private static final int statusFinished = 4;

	/**
	 * The status of this thread. A thread can either be new (not yet forked),
	 * ready (on the ready queue but not running), running, or blocked (not on
	 * the ready queue and not running).
	 */
	private int status = statusNew;

	private String name = "(unnamed thread)";

	private Runnable target;

	private TCB tcb;

	/**
	 * Unique identifer for this thread. Used to deterministically compare
	 * threads.
	 */
	private int id = numCreated++;

	/** Number of times the KThread constructor was called. */
	private static int numCreated = 0;

	private static ThreadQueue readyQueue = null;

	private static KThread currentThread = null;

	private static KThread toBeDestroyed = null;

	private static KThread idleThread = null;

	// private KThread joins = null; // This allows each thread to check if joins is null or a KThread.


	// Condition Implementation (CHANGE FOR CONDITION 2)
	private static Lock joinLock = new Lock();
	private Condition2 joinCond = null;

}