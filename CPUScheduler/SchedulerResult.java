import java.util.List;

/**
 * GanttEntry — one contiguous CPU block on the Gantt chart.
 * pid = "IDLE" for idle gaps between processes.
 */
class GanttEntry {
    public String pid;   // process ID occupying the CPU, or "IDLE"
    public int    start; // inclusive start time
    public int    end;   // exclusive end time

    public GanttEntry(String pid, int start, int end) {
        this.pid   = pid;
        this.start = start;
        this.end   = end;
    }

    /** Duration of this segment in time units. */
    public int duration() { return end - start; }
}

/**
 * SchedulerResult — output bundle returned by every algorithm.
 *
 * gantt   : ordered list of Gantt blocks (adjacent same-PID blocks merged)
 * results : one Process per input process, with WT/TAT/CT filled in
 */
class SchedulerResult {
    public List<GanttEntry> gantt;
    public List<Process>    results;

    public SchedulerResult(List<GanttEntry> gantt, List<Process> results) {
        this.gantt   = gantt;
        this.results = results;
    }
}
