/**
 * Process.java - Data model for a single CPU process
 *
 * Holds all scheduling attributes and computed result fields.
 * Used by every algorithm; deep-copied before each run so the
 * original input data is never mutated.
 */
public class Process {

    // Identity
    public String  id;       // e.g. "P1", "P2"
    public int     index;    // original input order (0-based), used for tie-breaking

    // Input Attributes
    public int arrivalTime;  // when the process enters the ready queue
    public int burstTime;    // total CPU time required
    public int priority;     // lower or higher = better, depending on user setting

    // Runtime State (modified during simulation)
    public int     remainingTime; // CPU time still needed; starts = burstTime
    public boolean isDone;        // true once the process has finished

    // Computed Results
    public int finishTime;       // wall-clock time when process completes
    public int waitingTime;      // WT  = finishTime − arrivalTime − burstTime
    public int turnaroundTime;   // TAT = finishTime − arrivalTime

    // Constructor
    public Process(String id, int arrivalTime, int burstTime, int priority, int index) {
        this.id            = id;
        this.arrivalTime   = arrivalTime;
        this.burstTime     = burstTime;
        this.priority      = priority;
        this.index         = index;
        this.remainingTime = burstTime;
        this.isDone        = false;
        this.finishTime    = 0;
        this.waitingTime   = 0;
        this.turnaroundTime = 0;
    }

    /** Returns a fresh deep copy so algorithms don't mutate the original. */
    public Process copy() {
        return new Process(id, arrivalTime, burstTime, priority, index);
    }
}
