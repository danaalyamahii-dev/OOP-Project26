package gpu.ui;

import java.awt.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import gpu.logic.Prediction;
import gpu.logic.Scheduler;
import gpu.logic.SimulationClock;
import gpu.model.*;

public class DashboardPanel extends JPanel {
    private final AppState state;
    private final Scheduler scheduler;

    private JLabel runningCountLbl, queuedCountLbl, completedCountLbl, cancelledCountLbl, busyGpuLbl, utilizationLbl;
    private JLabel[] gpuStatusLbls = new JLabel[4];
    private JLabel[] gpuJobLbls = new JLabel[4];
    private JLabel[] gpuRemLbls = new JLabel[4];
    private JLabel[] gpuFinLbls = new JLabel[4];

    private DefaultTableModel waitingModel, completedModel, allowanceModel;

    public DashboardPanel(AppState state, Scheduler scheduler) {
        this.state = state;
        this.scheduler = scheduler;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JPanel content = new JPanel();
        content.setLayout(new BorderLayout(8, 8));
        JPanel topSections = new JPanel(new GridLayout(2, 1, 8, 8));

        JPanel summaryPanel = new JPanel(new GridLayout(1, 6, 8, 8));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("System Summary"));
        runningCountLbl = createMetricCard(summaryPanel, "Running");
        queuedCountLbl = createMetricCard(summaryPanel, "Queued");
        completedCountLbl = createMetricCard(summaryPanel, "Completed");
        cancelledCountLbl = createMetricCard(summaryPanel, "Cancelled");
        busyGpuLbl = createMetricCard(summaryPanel, "Busy GPUs");
        utilizationLbl = createMetricCard(summaryPanel, "Utilization");
        topSections.add(summaryPanel);

        JPanel gpusPanel = new JPanel(new GridLayout(1, 4, 8, 8));
        gpusPanel.setBorder(BorderFactory.createTitledBorder("GPU Status (4 x NVIDIA H100)"));
        for (int i = 0; i < 4; i++) {
            JPanel card = new JPanel(new GridLayout(4, 1, 2, 2));
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
            ));
            gpuStatusLbls[i] = new JLabel("GPU" + (i + 1) + ": Free");
            gpuStatusLbls[i].setFont(new Font("SansSerif", Font.BOLD, 13));
            gpuJobLbls[i] = new JLabel("Current job: -");
            gpuRemLbls[i] = new JLabel("Remaining: -");
            gpuFinLbls[i] = new JLabel("Finishes: -");
            card.add(gpuStatusLbls[i]);
            card.add(gpuJobLbls[i]);
            card.add(gpuRemLbls[i]);
            card.add(gpuFinLbls[i]);
            gpusPanel.add(card);
        }
        topSections.add(gpusPanel);
        content.add(topSections, BorderLayout.NORTH);

        waitingModel = new DefaultTableModel(
            new String[]{"Job", "Student", "Application", "Dur.", "Pos.", "Pred. Start", "Pred. Finish", "Wait"}, 0
        ) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable waitingTable = new JTable(waitingModel);
        JScrollPane waitingScroll = new JScrollPane(waitingTable);
        waitingScroll.setPreferredSize(new Dimension(800, 130));
        waitingScroll.setBorder(BorderFactory.createTitledBorder("Waiting Jobs (FCFS Queue)"));
        content.add(waitingScroll, BorderLayout.CENTER);

        JPanel bottomSplit = new JPanel(new GridLayout(1, 2, 10, 10));

        completedModel = new DefaultTableModel(
            new String[]{"Job", "Student", "App", "GPU", "Submitted", "Start", "Finish", "Run / Wait", "Charged"}, 0
        ) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable completedTable = new JTable(completedModel);
        JScrollPane completedScroll = new JScrollPane(completedTable);
        completedScroll.setPreferredSize(new Dimension(450, 160));
        completedScroll.setBorder(BorderFactory.createTitledBorder("Completed Jobs"));

        allowanceModel = new DefaultTableModel(
            new String[]{"Student ID", "Name", "Alloc.", "Used", "Reserved", "Avail."}, 0
        ) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable allowanceTable = new JTable(allowanceModel);
        JScrollPane allowanceScroll = new JScrollPane(allowanceTable);
        allowanceScroll.setPreferredSize(new Dimension(350, 160));
        allowanceScroll.setBorder(BorderFactory.createTitledBorder("Student Allowances"));

        bottomSplit.add(completedScroll);
        bottomSplit.add(allowanceScroll);
        content.add(bottomSplit, BorderLayout.SOUTH);

        add(new JScrollPane(content), BorderLayout.CENTER);
        refresh();
    }

    private JLabel createMetricCard(JPanel parent, String title) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        JLabel top = new JLabel(title, SwingConstants.CENTER);
        top.setForeground(Color.DARK_GRAY);
        JLabel val = new JLabel("0", SwingConstants.CENTER);
        val.setFont(new Font("SansSerif", Font.BOLD, 18));
        card.add(top, BorderLayout.NORTH);
        card.add(val, BorderLayout.CENTER);
        parent.add(card);
        return val;
    }

    public void refresh() {
        long now = state.getClock().getNow();
        int running = 0, queued = 0, completed = 0, cancelled = 0;
        for (Job j : state.getJobs()) {
            switch (j.getStatus()) {
                case Job.RUNNING: running++; break;
                case Job.QUEUED: queued++; break;
                case Job.COMPLETED: completed++; break;
                case Job.CANCELLED: cancelled++; break;
            }
        }

        int busyGpus = 0;
        for (int i = 0; i < 4; i++) {
            GPU gpu = state.getGpu(i + 1);
            if (gpu.isFree()) {
                gpuStatusLbls[i].setText("GPU" + (i + 1) + " — Free");
                gpuStatusLbls[i].setForeground(Color.GREEN.darker());
                gpuJobLbls[i].setText("Current job: -");
                gpuRemLbls[i].setText("Remaining: -");
                gpuFinLbls[i].setText("Finishes: -");
            } else {
                busyGpus++;
                Job cur = gpu.getCurrentJob();
                long rem = cur.getExpectedFinish() - now;
                gpuStatusLbls[i].setText("GPU" + (i + 1) + " — Busy");
                gpuStatusLbls[i].setForeground(Color.RED.darker());
                gpuJobLbls[i].setText("Current job: " + cur.getId() + " (" + cur.getStudentId() + ")");
                gpuRemLbls[i].setText("Remaining: " + rem + " min");
                gpuFinLbls[i].setText("Finishes: " + SimulationClock.format(cur.getExpectedFinish()));
            }
        }

        runningCountLbl.setText(String.valueOf(running));
        queuedCountLbl.setText(String.valueOf(queued));
        completedCountLbl.setText(String.valueOf(completed));
        cancelledCountLbl.setText(String.valueOf(cancelled));
        busyGpuLbl.setText(busyGpus + "/4");

        double util = scheduler.utilization();
        if (util < 0) {
            utilizationLbl.setText("N/A");
        } else {
            utilizationLbl.setText(String.format("%.1f%%", util));
        }

        waitingModel.setRowCount(0);
        ArrayList<Prediction> preds = scheduler.predictions();
        for (Job j : scheduler.getQueue()) {
            Prediction p = null;
            for (Prediction item : preds) {
                if (item.getJobId().equals(j.getId())) { p = item; break; }
            }
            String pos = (p != null) ? String.valueOf(p.getPosition()) : "-";
            String pStart = (p != null) ? SimulationClock.format(p.getStart()) : "-";
            String pFin = (p != null) ? SimulationClock.format(p.getFinish()) : "-";
            String wait = (p != null) ? p.getWaitMinutes() + " min" : "-";
            waitingModel.addRow(new Object[]{
                j.getId(), j.getStudentId(), j.getApplication(), j.getDuration() + " min", pos, pStart, pFin, wait
            });
        }

        completedModel.setRowCount(0);
        for (Job j : state.getJobs()) {
            if (Job.COMPLETED.equals(j.getStatus())) {
                completedModel.addRow(new Object[]{
                    j.getId(),
                    j.getStudentId(),
                    j.getApplication(),
                    "GPU" + j.getGpuId(),
                    SimulationClock.format(j.getSubmissionTime()),
                    SimulationClock.format(j.getStartTime()),
                    SimulationClock.format(j.getFinishTime()),
                    j.getRuntime(now) + "m / " + j.getQueueWait() + "m",
                    j.getCharge()
                });
            }
        }

        allowanceModel.setRowCount(0);
        for (Student s : state.getStudents()) {
            TimeAllowance a = s.getAllowance();
            allowanceModel.addRow(new Object[]{
                s.getUsername(), s.getName(), a.getAllocated(), a.getUsed(), a.getReserved(), a.getAvailable()
            });
        }
    }
}
