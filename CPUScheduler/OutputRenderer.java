import java.util.List;

/**
 * ══════════════════════════════════════════════════════════════
 * OutputRenderer.java — All console output for simulation results
 *
 * Responsibilities:
 *   • Print the algorithm name banner
 *   • Render a text-based Gantt chart
 *   • Print the per-process WT / TAT summary table
 *   • Display average WT and average TAT
 * ══════════════════════════════════════════════════════════════
 */
public class OutputRenderer {

    /**
     * Master output method — calls all sub-renderers in order.
     *
     * @param algoName       Human-readable algorithm label
     * @param result         SchedulerResult from the algorithm
     * @param showPriority   Whether to include the Priority column
     * @param lowerIsBetter  Direction of priority (for the note)
     */
    public static void printAll(String algoName, SchedulerResult result,
                                boolean showPriority, boolean lowerIsBetter) {
        System.out.println("\n\n|====================================================|");
        System.out.println("|                   SIMULATION RESULTS                |");
        System.out.println("|======================================================|");
        System.out.println("  Algorithm : " + algoName);
        if (showPriority)
            System.out.println("  Priority  : " + (lowerIsBetter
                    ? "Lower value = Higher priority"
                    : "Higher value = Higher priority"));

        printGantt(result.gantt);
        printTable(result.results, showPriority);
        printAverages(result.results);
    }

    public static void printGantt(List<GanttEntry> gantt) {
        if (gantt.isEmpty()) {
            System.out.println("\n  [No Gantt data]");
            return;
        }

        System.out.println("\n┌─ GANTT CHART " + "─".repeat(50));

        // ── Build the top border ───────────────────────────────────
        StringBuilder topBorder = new StringBuilder("  ┌");
        StringBuilder midRow    = new StringBuilder("  │");
        StringBuilder botBorder = new StringBuilder("  └");

        for (GanttEntry e : gantt) {
            int width = Math.max(e.pid.length() + 2, e.duration() * 2);  // min width = label+2
            String dash   = "─".repeat(width);
            String padded = center(e.pid, width);

            topBorder.append(dash).append("┬");
            midRow.append(padded).append("│");
            botBorder.append(dash).append("┴");
        }

        // Replace last corner character
        replaceLastChar(topBorder, '┐');
        replaceLastChar(midRow,    '│');
        replaceLastChar(botBorder, '┘');

        System.out.println(topBorder);
        System.out.println(midRow);
        System.out.println(botBorder);

        // ── Build the time-marker row ──────────────────────────────
        // Each time marker is printed at the left edge of its block.
        StringBuilder timeRow = new StringBuilder("  ");
        int cursor = 0;

        for (int i = 0; i < gantt.size(); i++) {
            GanttEntry e   = gantt.get(i);
            int blockWidth = Math.max(e.pid.length() + 2, e.duration() * 2);
            String marker  = String.valueOf(e.start);

            timeRow.append(marker);
            cursor += marker.length();

            // Pad to reach the next block boundary
            int nextEdge = blockWidth + 1;  // +1 for the border char
            int spaces   = nextEdge - marker.length();
            for (int s = 0; s < spaces; s++) timeRow.append(" ");
            cursor += spaces;
        }

        // Append the final end time
        timeRow.append(gantt.get(gantt.size() - 1).end);
        System.out.println(timeRow);
        System.out.println();
    }


    // Summary Table Renderer
    public static void printTable(List<Process> results, boolean showPriority) {
        System.out.println("┌─ PROCESS SUMMARY " + "─".repeat(56));

        // ── Table header ───────────────────────────────────────────
        if (showPriority) {
            System.out.printf("  %-6s  %12s  %10s  %8s  %15s  %12s  %15s%n",
                    "PID", "Arrival Time", "Burst Time", "Priority",
                    "Completion Time", "Waiting Time", "Turnaround Time");
            System.out.println("  " + "─".repeat(85));
        } else {
            System.out.printf("  %-6s  %12s  %10s  %15s  %12s  %15s%n",
                    "PID", "Arrival Time", "Burst Time",
                    "Completion Time", "Waiting Time", "Turnaround Time");
            System.out.println("  " + "─".repeat(75));
        }

        // ── Sort results by original index for readability ─────────
        List<Process> sorted = new java.util.ArrayList<>(results);
        sorted.sort((a, b) -> a.index - b.index);

        // ── One row per process ────────────────────────────────────
        for (Process p : sorted) {
            if (showPriority) {
                System.out.printf("  %-6s  %12d  %10d  %8d  %15d  %12d  %15d%n",
                        p.id, p.arrivalTime, p.burstTime,
                        p.priority, p.finishTime, p.waitingTime, p.turnaroundTime);
            } else {
                System.out.printf("  %-6s  %12d  %10d  %15d  %12d  %15d%n",
                        p.id, p.arrivalTime, p.burstTime,
                        p.finishTime, p.waitingTime, p.turnaroundTime);
            }
        }

        if (showPriority)
            System.out.println("  " + "─".repeat(85));
        else
            System.out.println("  " + "─".repeat(75));
    }


    // Averages Renderer
    public static void printAverages(List<Process> results) {
        if (results.isEmpty()) return;

        double totalCT = 0, totalWT = 0, totalTAT = 0;
        for (Process p : results) {
            totalCT  += p.finishTime;      // Completion Time = finishTime
            totalWT  += p.waitingTime;
            totalTAT += p.turnaroundTime;
        }

        double avgCT  = totalCT  / results.size();
        double avgWT  = totalWT  / results.size();
        double avgTAT = totalTAT / results.size();

        System.out.println("\n┌─ PERFORMANCE METRICS " + "─".repeat(42));
        System.out.printf("  %-30s : %.2f time units%n", "Average Completion Time", avgCT);
        System.out.printf("  %-30s : %.2f time units%n", "Average Waiting Time",    avgWT);
        System.out.printf("  %-30s : %.2f time units%n", "Average Turnaround Time", avgTAT);
        System.out.println("└" + "─".repeat(64));
    }


    // Private Utility Methods

    /**
     * Centers a string within a field of the given total width,
     * padding with spaces on both sides.
     */
    private static String center(String s, int width) {
        if (s.length() >= width) return s;
        int left  = (width - s.length()) / 2;
        int right = width - s.length() - left;
        return " ".repeat(left) + s + " ".repeat(right);
    }

    /**
     * Replaces the last character of a StringBuilder.
     * Used to fix the corner character of the Gantt border.
     */
    private static void replaceLastChar(StringBuilder sb, char c) {
        if (sb.length() > 0) sb.setCharAt(sb.length() - 1, c);
    }
}
