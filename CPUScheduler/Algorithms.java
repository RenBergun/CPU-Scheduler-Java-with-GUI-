import java.util.*;

public class Algorithms {

    // =============================================================
    // ALGORITHM 1 - FCFS: First-Come, First-Served (Non-Preemptive)
    //
    // Concept:
    //   Processes execute in the order they arrive.  No preemption —
    //   once started, a process runs to completion.  Ties in arrival
    //   time are broken by original input order (index).
    //
    // Weakness: "Convoy effect" which is a long process that blocks all shorter
    //   ones that arrive just after it.
    // ===============================================================
    public static SchedulerResult fcfs(Process[] original, StringBuilder log) {

        // Sort a deep copy by arrival time; ties broken by index
        Process[] ps = deepCopy(original);
        Arrays.sort(ps, (a, b) -> a.arrivalTime != b.arrivalTime
                ? a.arrivalTime - b.arrivalTime : a.index - b.index);

        List<GanttEntry> gantt   = new ArrayList<>();
        List<Process>    results = new ArrayList<>();
        int time = 0;

        logHeader(log, "FCFS — First-Come, First-Served (Non-Preemptive)");

        for (Process p : ps) {

            // CPU idle gap: no process has arrived yet
            if (time < p.arrivalTime) {
                log.append(String.format("  [t=%-3d] IDLE  — waiting for %s to arrive at t=%d%n",
                        time, p.id, p.arrivalTime));
                gantt.add(new GanttEntry("IDLE", time, p.arrivalTime));
                time = p.arrivalTime;
            }

            // Dispatch p: it runs for its full burst (non-preemptive)
            int start = time;
            int end = time + p.burstTime;
            log.append(String.format("  [t=%-3d] RUN   %-4s | BT=%d | finishes at t=%d%n",
                    start, p.id, p.burstTime, end));
            time = end;

            gantt.add(new GanttEntry(p.id, start, end));

            // Compute Completion Time, WT, TAT
            p.finishTime     = end;                              // CT  = time when done
            p.turnaroundTime = p.finishTime - p.arrivalTime;    // TAT = CT  - AT
            p.waitingTime    = p.turnaroundTime - p.burstTime;  // WT  = TAT - BT
            results.add(p);

            log.append(String.format("         DONE  %-4s | CT=%d | WT=%d | TAT=%d%n",
                    p.id, p.finishTime, p.waitingTime, p.turnaroundTime));
        }

        return new SchedulerResult(gantt, results);
    }


    // ==========================================================
    // ALGORITHM 2 - SJF: Shortest Job First (Non-Preemptive)
    //
    // Concept:
    //   At each scheduling decision point, all processes that have
    //   already arrived are inspected.  The one with the SMALLEST
    //   burst time is dispatched and runs to completion.
    //
    // Tie-breaking: smaller BT wins; equal BT -> earlier AT -> lower index
    //
    // Optimality: SJF gives the minimum average WT among all
    //   non-preemptive algorithms (proven optimal).
    // ============================================================
    public static SchedulerResult sjf(Process[] original, StringBuilder log) {

        Process[] ps = deepCopy(original);
        List<GanttEntry> gantt   = new ArrayList<>();
        List<Process>    results = new ArrayList<>();
        int time = 0, completed = 0;

        logHeader(log, "SJF — Shortest Job First (Non-Preemptive)");

        while (completed < ps.length) {

            // Find the shortest available process
            Process best = null;
            for (Process p : ps) {
                if (!p.isDone && p.arrivalTime <= time) {
                    if (best == null
                            || p.burstTime < best.burstTime
                            || (p.burstTime == best.burstTime && p.arrivalTime < best.arrivalTime)
                            || (p.burstTime == best.burstTime && p.arrivalTime == best.arrivalTime
                                && p.index < best.index)) {
                        best = p;
                    }
                }
            }

            // No process ready: CPU idles until next arrival
            if (best == null) {
                int next = Integer.MAX_VALUE;
                for (Process p : ps)
                    if (!p.isDone && p.arrivalTime < next) next = p.arrivalTime;
                log.append(String.format("  [t=%-3d] IDLE  — next arrival at t=%d%n", time, next));
                gantt.add(new GanttEntry("IDLE", time, next));
                time = next;
                continue;
            }

            // Dispatch shortest job (non-preemptive)
            int start = time, end = time + best.burstTime;
            log.append(String.format("  [t=%-3d] RUN   %-4s | BT=%d (shortest) | finishes at t=%d%n",
                    start, best.id, best.burstTime, end));

            // Log skipped processes so the user can trace the decision
            for (Process p : ps)
                if (!p.isDone && p.arrivalTime <= time && !p.id.equals(best.id))
                    log.append(String.format("         SKIP  %-4s | BT=%d (longer)%n", p.id, p.burstTime));

            time = end;
            gantt.add(new GanttEntry(best.id, start, end));
            best.isDone = true;
            completed++;

            best.finishTime     = end;
            best.turnaroundTime = best.finishTime - best.arrivalTime;
            best.waitingTime    = best.turnaroundTime - best.burstTime;
            results.add(best);

            log.append(String.format("         DONE  %-4s | CT=%d | WT=%d | TAT=%d%n",
                    best.id, best.finishTime, best.waitingTime, best.turnaroundTime));
        }

        return new SchedulerResult(gantt, results);
    }


    // ============================================================
    // ALGORITHM 3 - SRT: Shortest Remaining Time (Preemptive SJF)
    //
    // Concept:
    //   At every clock tick, the scheduler picks the process with
    //   the LEAST remaining CPU time.  A newly arrived process can
    //   preempt the currently running one if its remaining time
    //   is shorter.
    //
    // Implementation detail:
    //   Simulated one tick at a time.  Consecutive same-PID ticks
    //   are merged into one Gantt block for readability.
    // =============================================================
    public static SchedulerResult srt(Process[] original, StringBuilder log) {

        Process[] ps = deepCopy(original);
        List<GanttEntry> gantt   = new ArrayList<>();
        List<Process>    results = new ArrayList<>();

        int maxTime = Arrays.stream(ps).mapToInt(p -> p.arrivalTime + p.burstTime).max().orElse(0) + 1;
        int time = 0, completed = 0;
        String segPid  = null;   // PID currently in the open Gantt segment
        int    segStart = 0;

        logHeader(log, "SRT — Shortest Remaining Time (Preemptive)");

        while (completed < ps.length && time <= maxTime) {

            // Find the process with minimum remaining time
            Process running = null;
            for (Process p : ps) {
                if (!p.isDone && p.arrivalTime <= time) {
                    if (running == null
                            || p.remainingTime < running.remainingTime
                            || (p.remainingTime == running.remainingTime && p.arrivalTime < running.arrivalTime)
                            || (p.remainingTime == running.remainingTime && p.arrivalTime == running.arrivalTime
                                && p.index < running.index)) {
                        running = p;
                    }
                }
            }

            // CPU idle tick
            if (running == null) {
                if (!"IDLE".equals(segPid)) {
                    if (segPid != null) gantt.add(new GanttEntry(segPid, segStart, time));
                    log.append(String.format("  [t=%-3d] IDLE%n", time));
                    segPid = "IDLE"; segStart = time;
                }
                time++;
                continue;
            }

            // Context switch: a different process now holds the CPU
            if (!running.id.equals(segPid)) {
                if (segPid != null) {
                    gantt.add(new GanttEntry(segPid, segStart, time));
                    if (!"IDLE".equals(segPid)) {
                        Process prev = getProcById(ps, segPid);
                        log.append(String.format("  [t=%-3d] PREEMPT %-4s (rem=%d) → %-4s (rem=%d)%n",
                                time, segPid,
                                prev != null ? prev.remainingTime : 0,
                                running.id, running.remainingTime));
                    }
                }
                log.append(String.format("  [t=%-3d] RUN   %-4s | remaining=%d%n",
                        time, running.id, running.remainingTime));
                segPid = running.id; segStart = time;
            }

            // Execute one tick
            running.remainingTime--;
            time++;

            // Process just finished
            if (running.remainingTime == 0) {
                gantt.add(new GanttEntry(running.id, segStart, time));
                segPid = null;
                running.isDone       = true;
                running.finishTime   = time;
                running.turnaroundTime = running.finishTime - running.arrivalTime;
                running.waitingTime    = running.turnaroundTime - running.burstTime;
                completed++;
                results.add(running);
                log.append(String.format("         DONE  %-4s | CT=%d | WT=%d | TAT=%d%n",
                        running.id, running.finishTime, running.waitingTime, running.turnaroundTime));
            }
        }

        return new SchedulerResult(mergeGantt(gantt), results);
    }


    // ============================================================
    // ALGORITHM 4 - Round Robin (Preemptive)
    //
    // Concept:
    //   Processes share the CPU in a cyclic order.  Each gets at
    //   most "quantum" time units per turn.  If not finished, the
    //   process is placed at the BACK of the ready queue.
    //   New arrivals during a quantum join the queue after the
    //   current process either finishes or is re-enqueued.
    //
    // Fairness: No process waits more than (n−1)×quantum time
    //   units before its next turn which guarantee strong fairness.
    // ==============================================================
    public static SchedulerResult roundRobin(Process[] original, int quantum, StringBuilder log) {

        Process[] ps = deepCopy(original);
        Arrays.sort(ps, (a, b) -> a.arrivalTime != b.arrivalTime
                ? a.arrivalTime - b.arrivalTime : a.index - b.index);

        List<GanttEntry> gantt  = new ArrayList<>();
        List<Process>    results = new ArrayList<>();
        Queue<Process>   queue  = new LinkedList<>();  // FIFO ready queue
        int time = 0, nextIdx = 0;

        // Seed queue with all t=0 arrivals
        while (nextIdx < ps.length && ps[nextIdx].arrivalTime <= time)
            queue.add(ps[nextIdx++]);

        logHeader(log, "Round Robin (Preemptive) — Quantum=" + quantum);

        while (!queue.isEmpty() || nextIdx < ps.length) {

            // CPU idle: queue empty, jump to next arrival
            if (queue.isEmpty()) {
                int nextAT = ps[nextIdx].arrivalTime;
                log.append(String.format("  [t=%-3d] IDLE  — next arrival: %s at t=%d%n",
                        time, ps[nextIdx].id, nextAT));
                gantt.add(new GanttEntry("IDLE", time, nextAT));
                time = nextAT;
                while (nextIdx < ps.length && ps[nextIdx].arrivalTime <= time)
                    queue.add(ps[nextIdx++]);
                continue;
            }

            // Dequeue and execute for up to `quantum` units
            Process p        = queue.poll();
            int     execTime = Math.min(quantum, p.remainingTime);
            int     start    = time;
            int     end      = time + execTime;

            log.append(String.format("  [t=%-3d] RUN   %-4s | rem=%d | exec=%d | ends t=%d%n",
                    start, p.id, p.remainingTime, execTime, end));

            p.remainingTime -= execTime;
            time = end;

            // Enqueue processes that arrived during this slice
            while (nextIdx < ps.length && ps[nextIdx].arrivalTime <= time)
                queue.add(ps[nextIdx++]);

            gantt.add(new GanttEntry(p.id, start, end));

            // Check: done or re-enqueue
            if (p.remainingTime == 0) {
                p.finishTime     = time;
                p.turnaroundTime = p.finishTime - p.arrivalTime;
                p.waitingTime    = p.turnaroundTime - p.burstTime;
                p.isDone         = true;
                results.add(p);
                log.append(String.format("         DONE  %-4s | CT=%d | WT=%d | TAT=%d%n",
                        p.id, p.finishTime, p.waitingTime, p.turnaroundTime));
            } else {
                log.append(String.format("         PREEMPT %-4s | rem=%d → re-enqueued%n",
                        p.id, p.remainingTime));
                queue.add(p);  // goes to back of queue
            }
        }

        return new SchedulerResult(gantt, results);
    }


    // =============================================================
    // ALGORITHM 5 — Priority Scheduling (Non-Preemptive)
    //
    // Concept:
    //   At each scheduling point, the process with the HIGHEST
    //   priority among arrived processes is chosen.  Direction
    //   is user-defined:
    //     • lowerIsBetter=true : smallest number = best priority
    //     • lowerIsBetter=false : largest number = best priority
    //
    //   The chosen process runs to completion (non-preemptive).
    //
    // Risk: Low-priority processes may starve if high-priority
    //   jobs keep arriving (not mitigated here - no aging).
    // ==============================================================
    public static SchedulerResult priorityNP(Process[] original, boolean lowerIsBetter, StringBuilder log) {

        Process[] ps = deepCopy(original);
        List<GanttEntry> gantt   = new ArrayList<>();
        List<Process>    results = new ArrayList<>();
        int time = 0, completed = 0;

        logHeader(log, "Priority Scheduling (Non-Preemptive) — "
                + (lowerIsBetter ? "Lower=Higher Priority" : "Higher=Higher Priority"));

        while (completed < ps.length) {

            // Pick highest-priority ready process
            Process best = null;
            for (Process p : ps) {
                if (!p.isDone && p.arrivalTime <= time) {
                    if (best == null || isBetterPriority(p, best, lowerIsBetter))
                        best = p;
                }
            }

            // CPU idle
            if (best == null) {
                int next = Integer.MAX_VALUE;
                for (Process p : ps)
                    if (!p.isDone && p.arrivalTime < next) next = p.arrivalTime;
                log.append(String.format("  [t=%-3d] IDLE  — next arrival at t=%d%n", time, next));
                gantt.add(new GanttEntry("IDLE", time, next));
                time = next;
                continue;
            }

            // Dispatch highest-priority process
            int start = time, end = time + best.burstTime;
            log.append(String.format("  [t=%-3d] RUN   %-4s | priority=%d | BT=%d | finishes t=%d%n",
                    start, best.id, best.priority, best.burstTime, end));

            for (Process p : ps)
                if (!p.isDone && p.arrivalTime <= time && !p.id.equals(best.id))
                    log.append(String.format("         SKIP  %-4s | priority=%d (lower priority)%n",
                            p.id, p.priority));

            time = end;
            gantt.add(new GanttEntry(best.id, start, end));
            best.isDone = true;
            completed++;

            best.finishTime     = end;
            best.turnaroundTime = best.finishTime - best.arrivalTime;
            best.waitingTime    = best.turnaroundTime - best.burstTime;
            results.add(best);

            log.append(String.format("         DONE  %-4s | CT=%d | WT=%d | TAT=%d%n",
                    best.id, best.finishTime, best.waitingTime, best.turnaroundTime));
        }

        return new SchedulerResult(gantt, results);
    }


    // ===========================================================
    // ALGORITHM 6 — Priority + Round Robin (Preemptive)
    //
    // Concept:
    //   Combines the two strategies:
    //     1. Processes are grouped into priority tiers.
    //     2. The highest-priority tier with ready processes always
    //        runs first (preemptive over lower tiers).
    //     3. Within each tier, processes share the CPU using Round
    //        Robin with the given quantum — preventing starvation
    //        within a tier.
    //
    //   A higher-priority arrival immediately preempts any running
    //   lower-priority process at the next quantum boundary.
    // ============================================================
    public static SchedulerResult priorityRR(Process[] original, int quantum,
                                             boolean lowerIsBetter, StringBuilder log) {

        Process[] ps = deepCopy(original);
        Arrays.sort(ps, (a, b) -> a.arrivalTime != b.arrivalTime
                ? a.arrivalTime - b.arrivalTime : a.index - b.index);

        // Collect unique priority levels, ordered best→worst
        List<Integer> levels = new ArrayList<>();
        for (Process p : ps)
            if (!levels.contains(p.priority)) levels.add(p.priority);
        levels.sort((a, b) -> lowerIsBetter ? a - b : b - a);

        // One FIFO deque per priority level
        Map<Integer, Deque<Process>> queues = new LinkedHashMap<>();
        for (int lv : levels) queues.put(lv, new LinkedList<>());

        List<GanttEntry> gantt   = new ArrayList<>();
        List<Process>    results = new ArrayList<>();
        int time = 0, nextIdx = 0;

        logHeader(log, "Priority + Round Robin (Preemptive) — Quantum=" + quantum
                + " | " + (lowerIsBetter ? "Lower=Higher" : "Higher=Higher") + " Priority");

        // Enqueue t=0 arrivals
        while (nextIdx < ps.length && ps[nextIdx].arrivalTime <= time)
            queues.get(ps[nextIdx].priority).addLast(ps[nextIdx++]);

        while (Arrays.stream(ps).anyMatch(p -> !p.isDone) || nextIdx < ps.length) {

            // Find the best (highest-priority) non-empty tier
            Deque<Process> active = null;
            int activePrio = -1;
            for (int lv : levels) {
                if (!queues.get(lv).isEmpty()) { active = queues.get(lv); activePrio = lv; break; }
            }

            // All queues empty: jump to next arrival
            if (active == null) {
                if (nextIdx >= ps.length) break;
                int nextAT = ps[nextIdx].arrivalTime;
                log.append(String.format("  [t=%-3d] IDLE  — next: %s at t=%d%n",
                        time, ps[nextIdx].id, nextAT));
                gantt.add(new GanttEntry("IDLE", time, nextAT));
                time = nextAT;
                while (nextIdx < ps.length && ps[nextIdx].arrivalTime <= time)
                    queues.get(ps[nextIdx].priority).addLast(ps[nextIdx++]);
                continue;
            }

            // Dequeue from best tier, execute one quantum
            Process p        = active.pollFirst();
            int     execTime = Math.min(quantum, p.remainingTime);
            int     start    = time, end = time + execTime;

            log.append(String.format("  [t=%-3d] RUN   %-4s | prio=%d | rem=%d | exec=%d → t=%d%n",
                    start, p.id, p.priority, p.remainingTime, execTime, end));

            p.remainingTime -= execTime;
            time = end;

            // Enqueue arrivals that came in during this slice
            while (nextIdx < ps.length && ps[nextIdx].arrivalTime <= time) {
                log.append(String.format("         ARRIVE %-4s (prio=%d) → enqueued%n",
                        ps[nextIdx].id, ps[nextIdx].priority));
                queues.get(ps[nextIdx].priority).addLast(ps[nextIdx++]);
            }

            gantt.add(new GanttEntry(p.id, start, end));

            if (p.remainingTime == 0) {
                // Process finished
                p.finishTime     = time;
                p.turnaroundTime = p.finishTime - p.arrivalTime;
                p.waitingTime    = p.turnaroundTime - p.burstTime;
                p.isDone         = true;
                results.add(p);
                log.append(String.format("         DONE  %-4s | CT=%d | WT=%d | TAT=%d%n",
                        p.id, p.finishTime, p.waitingTime, p.turnaroundTime));
            } else {
                //  Re-enqueue to back of its tier
                log.append(String.format("         PREEMPT %-4s | rem=%d → back to prio-%d queue%n",
                        p.id, p.remainingTime, p.priority));
                active.addLast(p);
            }
        }

        return new SchedulerResult(mergeGantt(gantt), results);
    }



    // HELPER METHODS

    /** Deep-copies the process array so algorithms don't mutate originals. */
    private static Process[] deepCopy(Process[] src) {
        Process[] out = new Process[src.length];
        for (int i = 0; i < src.length; i++) out[i] = src[i].copy();
        return out;
    }

    /**
     * Returns true if `candidate` has strictly better priority than `current`.
     * Tie-breaks: earlier arrival time → lower index.
     */
    private static boolean isBetterPriority(Process candidate, Process current, boolean lowerIsBetter) {
        int cp = lowerIsBetter ? candidate.priority : -candidate.priority;
        int cu = lowerIsBetter ? current.priority   : -current.priority;
        if (cp != cu) return cp < cu;
        if (candidate.arrivalTime != current.arrivalTime) return candidate.arrivalTime < current.arrivalTime;
        return candidate.index < current.index;
    }

    /** Finds a process by ID string (used to look up remaining time in SRT trace). */
    private static Process getProcById(Process[] ps, String id) {
        for (Process p : ps) if (p.id.equals(id)) return p;
        return null;
    }

    /**
     * Merges adjacent Gantt entries with the same PID into a single block.
     * Reduces visual noise on the chart for preemptive algorithms.
     */
    public static List<GanttEntry> mergeGantt(List<GanttEntry> raw) {
        List<GanttEntry> out = new ArrayList<>();
        for (GanttEntry e : raw) {
            if (!out.isEmpty()) {
                GanttEntry last = out.get(out.size() - 1);
                if (last.pid.equals(e.pid) && last.end == e.start) { last.end = e.end; continue; }
            }
            out.add(new GanttEntry(e.pid, e.start, e.end));
        }
        return out;
    }

    /** Appends a section header to the trace log. */
    private static void logHeader(StringBuilder log, String title) {
        log.append("┌── ").append(title).append("\n│\n");
    }
}
