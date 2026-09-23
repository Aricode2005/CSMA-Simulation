package csma;

public class SimClock {
    private long tick = 0;
    private int activeThreads = 0;
    private int waitingThreads = 0;
    public static boolean isLiveMode = false;
    
    public synchronized void registerThread() { 
        activeThreads++; 
    }
    
    public synchronized void deregisterThread() { 
        activeThreads--; 
        if (waitingThreads == activeThreads && activeThreads > 0) {
            advanceTick();
        } else if (activeThreads == 0) {
            notifyAll();
        }
    }
    
    public synchronized void waitForNextTick() {
        long currentTick = tick;
        waitingThreads++;
        if (waitingThreads == activeThreads) {
            advanceTick();
        } else {
            while (tick == currentTick && activeThreads > 0) {
                try { wait(); } catch (InterruptedException e) {}
            }
        }
    }
    
    private void advanceTick() {
        tick++;
        waitingThreads = 0;
        
        // If we are showing this visually on the Web UI, slow down time!
        if (isLiveMode) {
            try { Thread.sleep(100); } catch (InterruptedException e) {}
        }
        
        notifyAll();
    }
    
    public synchronized long getTick() { 
        return tick; 
    }
}
