import java.util.*;

public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("|══════════════════════════════════════════|");
        System.out.println("|       CPU SCHEDULING SIMULATOR           |");
        System.out.println("|══════════════════════════════════════════|");

        boolean runAgain = true;

        while (runAgain) {
            // Select Algorithm
            System.out.println("\n|─ SELECT ALGORITHM ─────────────────────|");
            System.out.println("│  1. FCFS  — First-Come, First-Served   │");
            System.out.println("│  2. SJF   — Shortest Job First         │");
            System.out.println("│  3. SRT   — Shortest Remaining Time    │");
            System.out.println("│  4. RR    — Round Robin                │");
            System.out.println("│  5. Priority (Non-Preemptive)          │");
            System.out.println("│  6. Priority + Round Robin             │");
            System.out.println("|────────────────────────────────────────|");
            System.out.print("Enter choice [1-6]: ");
            int choice = InputHelper.readInt(sc, 1, 6);

            // Number of processes
            System.out.print("\nEnter number of processes: ");
            int n = InputHelper.readInt(sc, 3, 20);

            // Determine if priority/quantum are needed
            boolean needsPriority = (choice == 5 || choice == 6);
            boolean needsQuantum  = (choice == 4 || choice == 6);

            //  Priority direction
            boolean lowerIsBetter = true; // default
            if (needsPriority) {
                System.out.println("\nPriority Direction:");
                System.out.println("  1. Lower value  = Higher priority (e.g., P1 > P2)");
                System.out.println("  2. Higher value = Higher priority (e.g., P5 > P1)");
                System.out.print("Enter choice [1-2]: ");
                lowerIsBetter = (InputHelper.readInt(sc, 1, 2) == 1);
                System.out.println("  → " + (lowerIsBetter
                    ? "Lower value = Higher priority"
                    : "Higher value = Higher priority"));
            }

            // Time Quantum
            int quantum = 2;
            if (needsQuantum) {
                System.out.print("\nEnter Time Quantum: ");
                quantum = InputHelper.readInt(sc, 1, 1000);
            }

            // Read each process
            Process[] processes = new Process[n];
            System.out.println("\n┌─ PROCESS INPUT ────────────────────────────────────────┐");
            System.out.printf("│  %-6s  %-14s  %-12s%s%n",
                "PID", "Arrival Time", "Burst Time",
                needsPriority ? "  Priority" : "");
            System.out.println("├────────────────────────────────────────────────────────┤");

            for (int i = 0; i < n; i++) {
                String defaultId = "P" + (i + 1);
                System.out.print("│  Process ID [" + defaultId + "]: ");
                String pid = sc.nextLine().trim();
                if (pid.isEmpty()) pid = defaultId;

                System.out.print("│  Arrival Time for " + pid + ": ");
                int at = InputHelper.readInt(sc, 0, Integer.MAX_VALUE);

                System.out.print("│  Burst Time  for " + pid + ": ");
                int bt = InputHelper.readInt(sc, 1, Integer.MAX_VALUE);

                int priority = 0;
                if (needsPriority) {
                    System.out.print("│  Priority    for " + pid + ": ");
                    priority = InputHelper.readInt(sc, 0, Integer.MAX_VALUE);
                }

                processes[i] = new Process(pid, at, bt, priority, i);
                System.out.println("│");
            }
            System.out.println("└────────────────────────────────────────────────────────┘");

            StringBuilder log = new StringBuilder();

            // Run selected algorithm
            SchedulerResult result;
            String algoName;

            switch (choice) {
                case 1:
                    algoName = "First-Come, First-Served (FCFS) — Non-Preemptive";
                    result   = Algorithms.fcfs(processes, log);
                    break;
                case 2:
                    algoName = "Shortest Job First (SJF) — Non-Preemptive";
                    result   = Algorithms.sjf(processes, log);
                    break;
                case 3:
                    algoName = "Shortest Remaining Time (SRT) — Preemptive";
                    result   = Algorithms.srt(processes, log);
                    break;
                case 4:
                    algoName = "Round Robin (RR) — Preemptive | Quantum = " + quantum;
                    result   = Algorithms.roundRobin(processes, quantum, log);
                    break;
                case 5:
                    algoName = "Priority Scheduling — Non-Preemptive | "
                             + (lowerIsBetter ? "Lower = Higher Priority" : "Higher = Higher Priority");
                    result   = Algorithms.priorityNP(processes, lowerIsBetter, log);
                    break;
                case 6:
                    algoName = "Priority + Round Robin — Preemptive | Quantum = " + quantum
                             + " | " + (lowerIsBetter ? "Lower = Higher Priority" : "Higher = Higher Priority");
                    result   = Algorithms.priorityRR(processes, quantum, lowerIsBetter, log);
                    break;
                default:
                    algoName = "FCFS";
                    result   = Algorithms.fcfs(processes, log);
            }


            System.out.println("\n┌─ EXECUTION TRACE " + "─".repeat(46));
            System.out.print(log);
            System.out.println("└" + "─".repeat(64));

            // Display Gantt chart, summary table, and averages
            OutputRenderer.printAll(algoName, result, needsPriority, lowerIsBetter);


            System.out.print("\nRun another simulation? [y/n]: ");
            String again = sc.nextLine().trim().toLowerCase();
            runAgain = again.equals("y") || again.equals("yes");
        }

        System.out.println("\nThank you for using CPU Scheduling Simulator!");
        sc.close();
    }
}
