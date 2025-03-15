package nachos.threads;

import nachos.machine.*;
import java.util.PriorityQueue;
import java.util.Comparator;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
		
		waitQueue = new PriorityQueue<WaitingThread>(new WaitTimeComparator());
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		boolean intStatus = Machine.interrupt().disable();
		
		long currentTime = Machine.timer().getTime();
		
		// Check if any threads need to be woken up
		while (!waitQueue.isEmpty() && waitQueue.peek().wakeTime <= currentTime) {
			WaitingThread thread = waitQueue.poll();
			thread.thread.ready();
		}
		
		Machine.interrupt().restore(intStatus);
		
		KThread.currentThread().yield();
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// If x is zero or negative, return immediately
		if (x <= 0)
			return;
			
		boolean intStatus = Machine.interrupt().disable();
		
		long wakeTime = Machine.timer().getTime() + x;
		
		// Create a new waiting thread entry and add it to the queue
		WaitingThread waitingThread = new WaitingThread(KThread.currentThread(), wakeTime);
		waitQueue.add(waitingThread);
		
		// Put the thread to sleep
		KThread.sleep();
		
		Machine.interrupt().restore(intStatus);
	}
	
	/**
	 * Class representing a thread waiting to be awakened at a specific time.
	 */
	private class WaitingThread {
		KThread thread;
		long wakeTime;
		
		WaitingThread(KThread thread, long wakeTime) {
			this.thread = thread;
			this.wakeTime = wakeTime;
		}
	}
	
	/**
	 * Comparator for sorting waiting threads by wake time.
	 */
	private class WaitTimeComparator implements Comparator<WaitingThread> {
		public int compare(WaitingThread a, WaitingThread b) {
			if (a.wakeTime < b.wakeTime) return -1;
			else if (a.wakeTime > b.wakeTime) return 1;
			else return 0;
		}
	}
	
	// Queue of threads waiting to be awakened, sorted by wake time
	private PriorityQueue<WaitingThread> waitQueue;
}
