import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class CPUSchedulerGUI extends JFrame {

    // Color palette
    private static final Color BG      = new Color(0x05070F);
    private static final Color SURFACE = new Color(0x0B0F1E);
    private static final Color CARD    = new Color(0x0F1526);
    private static final Color BORDER  = new Color(0x1A2240);
    private static final Color ACCENT  = new Color(0x00E5FF);
    private static final Color ACCENT2 = new Color(0x7C4DFF);
    private static final Color ACCENT3 = new Color(0x00FF9D);
    private static final Color WARN    = new Color(0xFF6B35);
    private static final Color TEXT    = new Color(0xC8D8F0);
    private static final Color MUTED   = new Color(0x4A5880);

    // Per-process palette — cycles for > 10 processes
    private static final Color[] PROC_COLORS = {
        new Color(0x00E5FF), new Color(0xB39DFF), new Color(0x00FF9D),
        new Color(0xFF8C5A), new Color(0xFFD700), new Color(0xFF6090),
        new Color(0x40C4FF), new Color(0x69F0AE), new Color(0xFFAB40),
        new Color(0xEA80FC)
    };

    private static final int F_HEADER    = 20;   // top banner title
    private static final int F_SECTION   = 13;   // section labels (01 SELECT ALGO...)
    private static final int F_BUTTON    = 13;   // algorithm buttons
    private static final int F_LABEL     = 13;   // field labels (Processes:, Quantum:)
    private static final int F_COL_HDR   = 12;   // process-input column headers
    private static final int F_FIELD     = 13;   // text-field input values
    private static final int F_NOTE      = 12;   // algorithm description note
    private static final int F_TABLE_HDR = 12;   // results table header
    private static final int F_TABLE     = 13;   // results table body
    private static final int F_METRIC_LBL= 11;   // metric card title
    private static final int F_METRIC_VAL= 30;   // metric card big number
    private static final int F_CT_TITLE  = 13;   // completion-time panel heading
    private static final int F_CT_ROW    = 13;   // completion-time row text
    private static final int F_GANTT_PID = 13;   // process label inside Gantt block
    private static final int F_GANTT_DUR = 10;   // duration sub-label in Gantt block
    private static final int F_GANTT_TIM = 11;   // time-axis numbers below Gantt

    // Application state
    private String selectedAlgo = "FCFS";
    private boolean lowerIsBetter = true;
    private int numProcesses = 3;

    // Input widgets
    private JTextField[] pidFields, atFields, btFields, prioFields;
    private JSpinner numProcSpinner, quantumSpinner;
    private JComboBox<String> prioOrderCombo;
    private JPanel procInputPanel;
    private JButton[] algoButtons;

    // Output widgets
    private GanttPanel ganttPanel;
    private JTable resultTable;
    private DefaultTableModel resultModel;
    private JLabel avgCTLabel, avgWTLabel, avgTATLabel;
    private JPanel ctBreakdownPanel;
    private JLabel algoNoteLabel;

    // Algorithm metadata
    private static final String[] ALGO_NAMES = {
        "FCFS", "SJF", "SRT", "Round Robin", "Priority", "Priority + RR"
    };
    private static final String[] ALGO_DESC = {
        "First-Come, First-Served - Non-Preemptive",
        "Shortest Job First - Non-Preemptive",
        "Shortest Remaining Time - Preemptive",
        "Round Robin - Preemptive (requires Quantum)",
        "Priority Scheduling - Non-Preemptive",
        "Priority + Round Robin - Preemptive"
    };


    // ENTRY POINT
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new CPUSchedulerGUI().setVisible(true));
    }


    // CONSTRUCTOR
    public CPUSchedulerGUI() {
        setTitle("CPU Scheduling Simulator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1300, 820));
        setPreferredSize(new Dimension(1460, 900));
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);

        // Split: wider left panel to accommodate bigger fonts
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildLeftPanel(), buildRightPanel());
        split.setDividerLocation(420);
        split.setDividerSize(4);
        split.setBackground(BORDER);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        updateAlgoSelection("FCFS");
        rebuildProcessRows();
    }

    // HEADER BAR
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CARD);
        p.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        p.setPreferredSize(new Dimension(0, 62));

        // Title — larger, bold
        JLabel title = new JLabel("  ⚡  CPU SCHEDULING SIMULATOR");
        title.setFont(new Font("SansSerif", Font.BOLD, F_HEADER));
        title.setForeground(ACCENT);

        p.add(title, BorderLayout.WEST);
        return p;
    }


    // LEFT PANEL - Algorithm selector + process input
    private JScrollPane buildLeftPanel() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(18, 18, 18, 12));

        // Algorithm buttons
        root.add(sectionLabel("01  SELECT ALGORITHM"));
        root.add(Box.createVerticalStrut(10));

        JPanel algGrid = new JPanel(new GridLayout(3, 2, 8, 8));
        algGrid.setBackground(BG);
        algGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        algoButtons = new JButton[ALGO_NAMES.length];
        for (int i = 0; i < ALGO_NAMES.length; i++) {
            final String name = ALGO_NAMES[i];
            JButton b = makeAlgoButton(name);
            b.addActionListener(e -> updateAlgoSelection(name));
            algoButtons[i] = b;
            algGrid.add(b);
        }
        root.add(algGrid);

        // Algorithm description note
        algoNoteLabel = new JLabel(" ");
        algoNoteLabel.setFont(new Font("SansSerif", Font.ITALIC, F_NOTE));
        algoNoteLabel.setForeground(WARN);
        algoNoteLabel.setBorder(new EmptyBorder(8, 4, 8, 4));
        algoNoteLabel.setAlignmentX(LEFT_ALIGNMENT);
        algoNoteLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        root.add(algoNoteLabel);

        root.add(Box.createVerticalStrut(10));
        root.add(makeSeparator());
        root.add(Box.createVerticalStrut(12));

        // Configuration
        root.add(sectionLabel("02  CONFIGURATION"));
        root.add(Box.createVerticalStrut(10));

        // Number of processes
        JPanel configRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        configRow.setBackground(BG);
        configRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        configRow.add(fieldLabel("Processes:"));
        numProcSpinner = styledSpinner(new SpinnerNumberModel(3, 3, 10, 1));
        numProcSpinner.addChangeListener(e -> rebuildProcessRows());
        configRow.add(numProcSpinner);
        root.add(configRow);

        // Time quantum (RR-based only)
        JPanel quantumRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        quantumRow.setBackground(BG);
        quantumRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        quantumRow.add(fieldLabel("Quantum:"));
        quantumSpinner = styledSpinner(new SpinnerNumberModel(2, 1, 100, 1));
        quantumRow.add(quantumSpinner);
        quantumRow.setName("quantumRow");
        root.add(quantumRow);

        // Priority direction (priority-based only)
        JPanel prioRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        prioRow.setBackground(BG);
        prioRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        prioRow.add(fieldLabel("Priority:"));
        prioOrderCombo = new JComboBox<>(new String[]{
            "Lower value = Higher priority",
            "Higher value = Higher priority"
        });
        styleCombo(prioOrderCombo);
        prioOrderCombo.addActionListener(e -> lowerIsBetter = prioOrderCombo.getSelectedIndex() == 0);
        prioRow.add(prioOrderCombo);
        prioRow.setName("prioRow");
        root.add(prioRow);

        root.add(Box.createVerticalStrut(12));
        root.add(makeSeparator());
        root.add(Box.createVerticalStrut(12));

        // Process input table
        root.add(sectionLabel("03  PROCESS INPUT"));
        root.add(Box.createVerticalStrut(10));

        procInputPanel = new JPanel();
        procInputPanel.setLayout(new BoxLayout(procInputPanel, BoxLayout.Y_AXIS));
        procInputPanel.setBackground(BG);
        root.add(procInputPanel);

        root.add(Box.createVerticalStrut(18));

        // Run button
        JButton runBtn = new JButton("▶   RUN SIMULATION");
        runBtn.setFont(new Font("SansSerif", Font.BOLD, 15));
        runBtn.setForeground(Color.WHITE);
        runBtn.setBackground(ACCENT2);
        runBtn.setBorder(new EmptyBorder(14, 28, 14, 28));
        runBtn.setFocusPainted(false);
        runBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        runBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        runBtn.addActionListener(e -> runSimulation());
        runBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { runBtn.setBackground(new Color(0x9C6EFF)); }
            public void mouseExited(MouseEvent e)  { runBtn.setBackground(ACCENT2); }
        });
        root.add(runBtn);

        JScrollPane scroll = new JScrollPane(root);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }


    // RIGHT PANEL - Gantt + Table + Metrics + CT Breakdown
    private JPanel buildRightPanel() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);

        // Gantt chart
        ganttPanel = new GanttPanel();
        ganttPanel.setBackground(CARD);
        JScrollPane ganttScroll = new JScrollPane(ganttPanel);
        ganttScroll.setBorder(new CompoundBorder(
            new EmptyBorder(14, 14, 8, 14),
            new LineBorder(BORDER, 1)
        ));
        ganttScroll.getViewport().setBackground(CARD);
        ganttScroll.setPreferredSize(new Dimension(0, 150));
        ganttScroll.setMinimumSize(new Dimension(0, 150));

        // Results table
        // Columns: PID | AT | BT | Priority | CT | WT | TAT
        String[] cols = {
            "Process", "Arrival Time", "Burst Time", "Priority",
            "Completion Time", "Waiting Time", "Turnaround Time"
        };
        resultModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        resultTable = new JTable(resultModel);
        styleTable(resultTable);
        JScrollPane tableScroll = new JScrollPane(resultTable);
        tableScroll.setBorder(new CompoundBorder(
            new EmptyBorder(8, 14, 8, 14),
            new LineBorder(BORDER, 1)
        ));
        tableScroll.getViewport().setBackground(SURFACE);
        tableScroll.setPreferredSize(new Dimension(0, 200));

        // Metrics row
        JPanel metricsPanel = buildMetricsPanel();
        metricsPanel.setBorder(new EmptyBorder(8, 14, 8, 14));

        // Completion Time breakdown
        // Shows each process's completion time in a readable card grid
        ctBreakdownPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        ctBreakdownPanel.setBackground(SURFACE);
        ctBreakdownPanel.setBorder(new EmptyBorder(4, 6, 4, 6));

        // Placeholder message before first simulation
        JLabel placeholder = new JLabel("  Run a simulation to see per-process completion times.");
        placeholder.setFont(new Font("SansSerif", Font.ITALIC, F_CT_ROW));
        placeholder.setForeground(MUTED);
        ctBreakdownPanel.add(placeholder);

        JScrollPane ctScroll = new JScrollPane(ctBreakdownPanel);
        ctScroll.setBorder(new CompoundBorder(
            new EmptyBorder(0, 14, 14, 14),
            new LineBorder(BORDER, 1)
        ));
        ctScroll.setPreferredSize(new Dimension(0, 140));
        ctScroll.getViewport().setBackground(SURFACE);

        // Wrap each section with a labelled header
        JPanel ganttSec   = wrapWithLabel("GANTT CHART",              ganttScroll);
        JPanel tableSec   = wrapWithLabel("PROCESS SUMMARY",          tableScroll);
        JPanel metricsSec = wrapWithLabel("PERFORMANCE METRICS",      metricsPanel);
        JPanel ctSec      = wrapWithLabel("COMPLETION TIME BREAKDOWN", ctScroll);

        // Stack vertically inside an outer scroll pane
        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setBackground(BG);
        stack.add(ganttSec);
        stack.add(tableSec);
        stack.add(metricsSec);
        stack.add(ctSec);

        JScrollPane outerScroll = new JScrollPane(stack);
        outerScroll.setBorder(null);
        outerScroll.getViewport().setBackground(BG);

        root.add(outerScroll, BorderLayout.CENTER);
        return root;
    }

    /** Wraps a component with a coloured section-title label above it. */
    private JPanel wrapWithLabel(String label, JComponent content) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(4, 0, 0, 0));
        JLabel lbl = sectionLabel(label);
        lbl.setBorder(new EmptyBorder(10, 16, 4, 16));
        p.add(lbl, BorderLayout.NORTH);
        p.add(content, BorderLayout.CENTER);
        return p;
    }


    // METRICS PANEL - three stat cards: Avg CT, Avg WT, Avg TAT

    private JPanel buildMetricsPanel() {
        JPanel p = new JPanel(new GridLayout(1, 3, 14, 0));
        p.setBackground(BG);

        avgCTLabel  = metricCard("AVG COMPLETION TIME", "—");
        avgWTLabel  = metricCard("AVG WAITING TIME",    "—");
        avgTATLabel = metricCard("AVG TURNAROUND TIME", "—");

        p.add(avgCTLabel.getParent());
        p.add(avgWTLabel.getParent());
        p.add(avgTATLabel.getParent());
        return p;
    }

    /**
     * Builds one metric card.
     * Returns the value JLabel so renderMetrics() can update it.
     */
    private JLabel metricCard(String title, String initVal) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(CARD);
        card.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1),
            new EmptyBorder(16, 18, 16, 18)
        ));

        // Label above the number
        JLabel lbl = new JLabel(title, SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.BOLD, F_METRIC_LBL));
        lbl.setForeground(MUTED);

        // Large numeric value
        JLabel val = new JLabel(initVal, SwingConstants.CENTER);
        val.setFont(new Font("Monospaced", Font.BOLD, F_METRIC_VAL));
        val.setForeground(ACCENT3);

        // Unit sub-label
        JLabel unit = new JLabel("time units", SwingConstants.CENTER);
        unit.setFont(new Font("SansSerif", Font.PLAIN, 11));
        unit.setForeground(MUTED);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(CARD);
        bottom.add(val,  BorderLayout.CENTER);
        bottom.add(unit, BorderLayout.SOUTH);

        card.add(lbl,    BorderLayout.NORTH);
        card.add(bottom, BorderLayout.CENTER);
        return val;
    }


    // PROCESS INPUT ROW BUILDER

    private void rebuildProcessRows() {
        numProcesses = (int) numProcSpinner.getValue();
        boolean needsPrio = selectedAlgo.equals("Priority") || selectedAlgo.equals("Priority + RR");

        procInputPanel.removeAll();

        // Column headers
        int cols = needsPrio ? 4 : 3;
        JPanel header = new JPanel(new GridLayout(1, cols, 6, 0));
        header.setBackground(BG);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        header.add(colHeader("PID"));
        header.add(colHeader("Arrival"));
        header.add(colHeader("Burst"));
        if (needsPrio) header.add(colHeader("Priority"));
        procInputPanel.add(header);
        procInputPanel.add(Box.createVerticalStrut(5));

        // Input fields
        pidFields  = new JTextField[numProcesses];
        atFields   = new JTextField[numProcesses];
        btFields   = new JTextField[numProcesses];
        prioFields = new JTextField[numProcesses];

        for (int i = 0; i < numProcesses; i++) {
            JPanel row = new JPanel(new GridLayout(1, cols, 6, 0));
            row.setBackground(BG);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

            pidFields[i]  = styledField("P" + (i + 1));
            atFields[i]   = styledField(String.valueOf(i == 0 ? 0 : i));
            btFields[i]   = styledField(String.valueOf((i + 1) * 2));
            prioFields[i] = styledField(String.valueOf(i + 1));

            row.add(pidFields[i]);
            row.add(atFields[i]);
            row.add(btFields[i]);
            if (needsPrio) row.add(prioFields[i]);

            procInputPanel.add(row);
            procInputPanel.add(Box.createVerticalStrut(5));
        }

        procInputPanel.revalidate();
        procInputPanel.repaint();
    }


    // ALGORITHM SELECTION
    private void updateAlgoSelection(String algo) {
        selectedAlgo = algo;

        // Highlight active button
        for (int i = 0; i < ALGO_NAMES.length; i++) {
            boolean active = ALGO_NAMES[i].equals(algo);
            algoButtons[i].setBackground(active ? new Color(0x1A0A3A) : SURFACE);
            algoButtons[i].setForeground(active ? ACCENT : MUTED);
            algoButtons[i].setBorder(new LineBorder(active ? ACCENT2 : BORDER, active ? 2 : 1));
        }

        boolean needsQ    = algo.equals("Round Robin") || algo.equals("Priority + RR");
        boolean needsPrio = algo.equals("Priority")    || algo.equals("Priority + RR");

        // Toggle quantum / priority-direction rows by name
        for (Component c : ((JPanel) quantumSpinner.getParent()).getParent().getComponents()) {
            if (c instanceof JPanel panel) {
                String name = panel.getName();
                if ("quantumRow".equals(name)) panel.setVisible(needsQ);
                if ("prioRow".equals(name))    panel.setVisible(needsPrio);
            }
        }

        // Update algorithm description note
        for (int i = 0; i < ALGO_NAMES.length; i++) {
            if (ALGO_NAMES[i].equals(algo)) {
                algoNoteLabel.setText("<html><i>" + ALGO_DESC[i] + "</i></html>");
                break;
            }
        }

        rebuildProcessRows();
    }


    // SIMULATION RUNNER
    // Reads input -> validates -> calls algorithm -> renders all panels

    private void runSimulation() {

        // Validate and collect process data
        Process[] processes = new Process[numProcesses];
        boolean needsPrio = selectedAlgo.equals("Priority") || selectedAlgo.equals("Priority + RR");

        for (int i = 0; i < numProcesses; i++) {
            try {
                String pid = pidFields[i].getText().trim();
                if (pid.isEmpty()) pid = "P" + (i + 1);
                int at   = Integer.parseInt(atFields[i].getText().trim());
                int bt   = Integer.parseInt(btFields[i].getText().trim());
                int prio = needsPrio ? Integer.parseInt(prioFields[i].getText().trim()) : 0;
                if (bt < 1 || at < 0) throw new NumberFormatException();
                processes[i] = new Process(pid, at, bt, prio, i);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                    "Invalid input for process " + (i + 1) + ".\n"
                    + "Arrival Time ≥ 0, Burst Time ≥ 1 (integers only).",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        int quantum = (int) quantumSpinner.getValue();

        // Run chosen algorithm
        StringBuilder log = new StringBuilder();
        SchedulerResult result;

        switch (selectedAlgo) {
            case "FCFS"-> result = Algorithms.fcfs(processes, log);
            case "SJF" -> result = Algorithms.sjf(processes, log);
            case "SRT" -> result = Algorithms.srt(processes, log);
            case "Round Robin" -> result = Algorithms.roundRobin(processes, quantum, log);
            case "Priority" -> result = Algorithms.priorityNP(processes, lowerIsBetter, log);
            case "Priority + RR" -> result = Algorithms.priorityRR(processes, quantum, lowerIsBetter, log);
            default -> result = Algorithms.fcfs(processes, log);
        }

        // Render all output sections
        renderGantt(result.gantt, processes);
        renderTable(result.results, needsPrio);
        renderMetrics(result.results);
        renderCTBreakdown(result.results);   // new panel replaces trace log
    }


    // RENDER - Gantt Chart
    private void renderGantt(List<GanttEntry> gantt, Process[] processes) {
        Map<String, Integer> colorMap = new HashMap<>();
        for (Process p : processes) colorMap.put(p.id, p.index % PROC_COLORS.length);
        ganttPanel.setData(gantt, colorMap);
    }


    // RENDER - Results Table
    // Columns: PID | AT | BT | Priority | CT | WT | TAT
    private void renderTable(List<Process> results, boolean showPriority) {
        resultModel.setRowCount(0);
        List<Process> sorted = new ArrayList<>(results);
        sorted.sort(Comparator.comparingInt(p -> p.index));

        for (Process p : sorted) {
            resultModel.addRow(new Object[]{
                p.id,
                p.arrivalTime,
                p.burstTime,
                showPriority ? p.priority : "—",
                p.finishTime,       // Completion Time
                p.waitingTime,
                p.turnaroundTime
            });
        }
    }


    // RENDER - Performance Metrics (Avg CT, WT, TAT)
    private void renderMetrics(List<Process> results) {
        if (results.isEmpty()) return;
        double sumCT = 0, sumWT = 0, sumTAT = 0;
        for (Process p : results) {
            sumCT  += p.finishTime;
            sumWT  += p.waitingTime;
            sumTAT += p.turnaroundTime;
        }
        int n = results.size();
        avgCTLabel .setText(String.format("%.2f", sumCT  / n));
        avgWTLabel .setText(String.format("%.2f", sumWT  / n));
        avgTATLabel.setText(String.format("%.2f", sumTAT / n));
    }


    // RENDER - Completion Time Breakdown
    private void renderCTBreakdown(List<Process> results) {
        ctBreakdownPanel.removeAll();

        List<Process> sorted = new ArrayList<>(results);
        sorted.sort(Comparator.comparingInt(p -> p.index));

        for (Process p : sorted) {
            int colorIdx = p.index % PROC_COLORS.length;
            Color procColor = PROC_COLORS[colorIdx];

            //  Outer card
            JPanel card = new JPanel(new BorderLayout(0, 6));
            card.setBackground(CARD);
            card.setBorder(new CompoundBorder(
                new LineBorder(procColor, 2),
                new EmptyBorder(10, 14, 10, 14)
            ));
            card.setPreferredSize(new Dimension(180, 130));

            // Process name
            JLabel pidLbl = new JLabel(p.id, SwingConstants.CENTER);
            pidLbl.setFont(new Font("Monospaced", Font.BOLD, 16));
            pidLbl.setForeground(procColor);

            // Stats grid
            JPanel stats = new JPanel(new GridLayout(4, 1, 0, 2));
            stats.setBackground(CARD);

            stats.add(ctStatRow("CT", p.finishTime,      ACCENT3));  // Completion Time — highlighted green
            stats.add(ctStatRow("WT", p.waitingTime,     WARN));     // Waiting Time — orange
            stats.add(ctStatRow("TAT", p.turnaroundTime, TEXT));     // Turnaround Time
            stats.add(ctStatRow("BT", p.burstTime,       MUTED));    // Burst Time (reference)

            card.add(pidLbl, BorderLayout.NORTH);
            card.add(stats,  BorderLayout.CENTER);

            ctBreakdownPanel.add(card);
        }

        ctBreakdownPanel.revalidate();
        ctBreakdownPanel.repaint();
    }

    /**
     * Creates one labelled stat row inside a CT breakdown card.
     * Format: "CT  =  14"  — label left, value right, coloured.
     */
    private JPanel ctStatRow(String label, int value, Color valueColor) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(CARD);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Monospaced", Font.PLAIN, F_CT_ROW));
        lbl.setForeground(MUTED);

        JLabel val = new JLabel(String.valueOf(value), SwingConstants.RIGHT);
        val.setFont(new Font("Monospaced", Font.BOLD, F_CT_ROW));
        val.setForeground(valueColor);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        return row;
    }


    // UI FACTORY HELPERS
    /** Section title label (e.g. "01  SELECT ALGORITHM") */
    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, F_SECTION));
        l.setForeground(ACCENT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    /** Muted label for configuration fields (e.g. "Processes:") */
    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, F_LABEL));
        l.setForeground(TEXT);
        return l;
    }

    /** Column header for the process input grid */
    private JLabel colHeader(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("SansSerif", Font.BOLD, F_COL_HDR));
        l.setForeground(ACCENT2);
        return l;
    }

    /** Dark styled text field used in the process input rows */
    private JTextField styledField(String val) {
        JTextField f = new JTextField(val);
        f.setFont(new Font("Monospaced", Font.PLAIN, F_FIELD));
        f.setForeground(TEXT);
        f.setBackground(SURFACE);
        f.setCaretColor(ACCENT);
        f.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1),
            new EmptyBorder(5, 7, 5, 7)
        ));
        f.setHorizontalAlignment(SwingConstants.CENTER);
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                f.setBorder(new CompoundBorder(new LineBorder(ACCENT, 1), new EmptyBorder(5, 7, 5, 7)));
            }
            public void focusLost(FocusEvent e) {
                f.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(5, 7, 5, 7)));
            }
        });
        return f;
    }


    private JButton makeAlgoButton(String name) {
        JButton b = new JButton("<html><center>" + name + "</center></html>");
        b.setFont(new Font("SansSerif", Font.BOLD, F_BUTTON));
        b.setForeground(MUTED);
        b.setBackground(SURFACE);
        b.setBorder(new LineBorder(BORDER, 1));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (!b.getForeground().equals(ACCENT)) b.setBorder(new LineBorder(ACCENT2, 1));
            }
            public void mouseExited(MouseEvent e) {
                if (!b.getForeground().equals(ACCENT)) b.setBorder(new LineBorder(BORDER, 1));
            }
        });
        return b;
    }


    private JSpinner styledSpinner(SpinnerModel model) {
        JSpinner s = new JSpinner(model);
        s.setFont(new Font("Monospaced", Font.PLAIN, F_FIELD));
        s.getEditor().getComponent(0).setBackground(SURFACE);
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setForeground(TEXT);
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setCaretColor(ACCENT);
        s.setBorder(new LineBorder(BORDER, 1));
        s.setPreferredSize(new Dimension(80, 32));
        return s;
    }


    private void styleCombo(JComboBox<String> cb) {
        cb.setFont(new Font("SansSerif", Font.PLAIN, F_LABEL));
        cb.setForeground(TEXT);
        cb.setBackground(SURFACE);
        cb.setBorder(new LineBorder(BORDER, 1));
    }

    /** Applies dark-theme styling to the results JTable.*/
    private void styleTable(JTable t) {
        t.setBackground(SURFACE);
        t.setForeground(TEXT);
        t.setFont(new Font("Monospaced", Font.PLAIN, F_TABLE));
        t.setRowHeight(32);       // taller rows for readability
        t.setGridColor(BORDER);
        t.setShowGrid(true);
        t.setSelectionBackground(new Color(0x1A1040));
        t.setSelectionForeground(ACCENT);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        t.setFillsViewportHeight(true);
        t.setIntercellSpacing(new Dimension(0, 1));

        // Table header
        JTableHeader h = t.getTableHeader();
        h.setBackground(CARD);
        h.setForeground(ACCENT2);
        h.setFont(new Font("SansSerif", Font.BOLD, F_TABLE_HDR));
        h.setBorder(new MatteBorder(0, 0, 2, 0, BORDER));
        h.setPreferredSize(new Dimension(0, 34));

        // Custom cell renderer — centre-aligned, colour-coded by column
        DefaultTableCellRenderer centre = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable tbl, Object val, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, focus, row, col);
                setHorizontalAlignment(CENTER);
                // Alternate row shading
                setBackground(sel ? new Color(0x1A1040)
                                  : (row % 2 == 0 ? SURFACE : CARD));
                // Column colour coding
                if (col == 4)      setForeground(ACCENT3);  // CT  → green
                else if (col >= 5) setForeground(WARN);     // WT, TAT → orange
                else               setForeground(TEXT);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return this;
            }
        };
        for (int i = 0; i < t.getColumnCount(); i++)
            t.getColumnModel().getColumn(i).setCellRenderer(centre);
    }

    /** Thin horizontal divider line used between sections */
    private JSeparator makeSeparator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }


    // INNER CLASS - GanttPanel
    class GanttPanel extends JPanel {

        private List<GanttEntry>    gantt    = new ArrayList<>();
        private Map<String,Integer> colorMap = new HashMap<>();

        GanttPanel() {
            setBackground(CARD);
            setPreferredSize(new Dimension(900, 110));
        }

        void setData(List<GanttEntry> gantt, Map<String,Integer> colorMap) {
            this.gantt    = gantt;
            this.colorMap = colorMap;
            // Widen so all blocks stay readable without squishing
            int w = Math.max(900, gantt.size() * 72);
            setPreferredSize(new Dimension(w, 110));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Placeholder
            if (gantt.isEmpty()) {
                g.setColor(MUTED);
                g.setFont(new Font("SansSerif", Font.ITALIC, 14));
                g.drawString("Run a simulation to see the Gantt chart.", 20, 55);
                return;
            }

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,  RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int totalTime = gantt.get(gantt.size() - 1).end;
            int W = getWidth();

            int blockH = 62;
            int topY   = 12;
            int timeY  = topY + blockH + 16;

            double scale = (double)(W - 24) / totalTime;
            int x = 12;

            for (GanttEntry e : gantt) {
                int bw = Math.max(2, (int)(e.duration() * scale));

                // Fill
                if (e.pid.equals("IDLE")) {
                    g2.setColor(new Color(0x1A2240));
                } else {
                    Color base = PROC_COLORS[colorMap.getOrDefault(e.pid, 0)];
                    g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 60));
                }
                g2.fillRect(x, topY, bw, blockH);

                // Border
                g2.setColor(e.pid.equals("IDLE")
                    ? BORDER
                    : PROC_COLORS[colorMap.getOrDefault(e.pid, 0)]);
                g2.drawRect(x, topY, bw, blockH);

                // PID label
                Font pidFont = new Font("Monospaced", Font.BOLD, F_GANTT_PID);
                g2.setFont(pidFont);
                g2.setColor(e.pid.equals("IDLE")
                    ? MUTED
                    : PROC_COLORS[colorMap.getOrDefault(e.pid, 0)]);
                FontMetrics fm = g2.getFontMetrics();
                int lx = x + (bw - fm.stringWidth(e.pid)) / 2;
                int ly = topY + blockH / 2 - fm.getHeight() / 2 + fm.getAscent();
                if (lx >= x) g2.drawString(e.pid, lx, ly);

                // Duration sub-label
                if (e.duration() > 1) {
                    String dur = e.duration() + "t";
                    Font durFont = new Font("Monospaced", Font.PLAIN, F_GANTT_DUR);
                    g2.setFont(durFont);
                    FontMetrics dfm = g2.getFontMetrics();
                    int dx = x + (bw - dfm.stringWidth(dur)) / 2;
                    Color c = g2.getColor();
                    g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 170));
                    if (dx >= x) g2.drawString(dur, dx, ly + 14);
                }

                // Start-time marker below block
                g2.setFont(new Font("Monospaced", Font.PLAIN, F_GANTT_TIM));
                g2.setColor(MUTED);
                g2.drawString(String.valueOf(e.start), x, timeY);

                x += bw;
            }

            // Final end-time marker
            g2.setFont(new Font("Monospaced", Font.PLAIN, F_GANTT_TIM));
            g2.setColor(MUTED);
            String endStr = String.valueOf(gantt.get(gantt.size() - 1).end);
            g2.drawString(endStr, x - g2.getFontMetrics().stringWidth(endStr), timeY);
        }
    }
}
