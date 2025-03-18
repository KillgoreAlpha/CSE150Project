package nachos.threads;

import nachos.machine.*;

import java.util.LinkedList;

public class Condition2 {
    private Lock conditionLock;
    private LinkedList<KThread> waitQueue;

    public Condition2(Lock conditionLock) {
        this.conditionLock = conditionLock;
        this.waitQueue = new LinkedList<KThread>();
    }

    public void sleep() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        boolean intStatus = Machine.interrupt().disable();
        
        KThread currentThread = KThread.currentThread();
        waitQueue.add(currentThread);
        
        conditionLock.release();
        currentThread.sleep();
        
        Machine.interrupt().restore(intStatus);
        conditionLock.acquire();
    }

    public void wake() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        boolean intStatus = Machine.interrupt().disable();
        
        if (!waitQueue.isEmpty()) {
            KThread thread = waitQueue.removeFirst();
            if (thread != null) {
                thread.ready();
            }
        }
        
        Machine.interrupt().restore(intStatus);
    }

    public void wakeAll() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        boolean intStatus = Machine.interrupt().disable();
        
        while (!waitQueue.isEmpty()) {
            wake();
        }
        
        Machine.interrupt().restore(intStatus);
    }

    public void sleepFor(long timeout) {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        boolean intStatus = Machine.interrupt().disable();
        
        KThread currentThread = KThread.currentThread();
        waitQueue.add(currentThread);
        
        conditionLock.release();
        
        ThreadedKernel.alarm.waitUntil(timeout);
        
        Machine.interrupt().restore(intStatus);
        conditionLock.acquire();
    }
}
