package gpu.ui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import gpu.logic.AccountService;
import gpu.logic.Scheduler;
import gpu.logic.SimulationClock;
import gpu.model.*;
import gpu.storage.DataStore;

public class AdminFrame extends JFrame {
    private final AppState state;
    private final AccountService accountService;
    private final Scheduler scheduler;
    private final DataStore dataStore;

    private JLabel clockLbl;
    private JButton advanceBtn;
    private JTabbedPane tabs;
    private DashboardPanel dashboardPanel;
    private RequestsPanel requestsPanel;
    private StudentsPanel studentsPanel;

    private JTextField allJobsSearchField;
    private JComboBox<String> allJobsStatusFilter;
    private DefaultTableModel allJobsModel;

    public AdminFrame(AppState state, AccountService accountService, Scheduler scheduler, DataStore dataStore) {
        super("GPU Utilization Management — Administrator");
        this.state = state;
        this.accountService = accountService;
        this.scheduler = scheduler;
        this.dataStore = dataStore;

        setSize(1100, 760);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    dataStore.saveAll(state);
                } catch (ProjectException ex) {
                    JOptionPane.showMessageDialog(AdminFrame.this, ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
                }
                dispose();
                System.exit(0);
            }
        });

        setLayout(new BorderLayout());

        JPanel topBar = new JPanel(new BorderLayout(10, 10));


        JLabel titleLbl = new JLabel("GPU Utilization Dashboard — Administrator");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 18));
        topBar.add(titleLbl, BorderLayout.WEST);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        clockLbl = new JLabel("Simulated time: Not set");
        clockLbl.setFont(new Font("SansSerif", Font.BOLD, 14));

        advanceBtn = new JButton("+1 minute");
        advanceBtn.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { handleAdvanceMinute(); } });

        JButton logoutBtn = new JButton("Log out");
        logoutBtn.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { handleLogout(); } });

        rightControls.add(clockLbl);
        rightControls.add(advanceBtn);
        rightControls.add(logoutBtn);
        topBar.add(rightControls, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        tabs = new JTabbedPane();
        dashboardPanel = new DashboardPanel(state, scheduler);
        requestsPanel = new RequestsPanel(state, accountService, dataStore, new Runnable() { public void run() { refreshAll(); } });
        studentsPanel = new StudentsPanel(state, accountService, scheduler, dataStore, new Runnable() { public void run() { refreshAll(); } });
        JPanel allJobsPanel = buildAllJobsPanel();

        tabs.addTab("Dashboard", dashboardPanel);
        tabs.addTab("Requests", requestsPanel);
        tabs.addTab("Students", studentsPanel);
        tabs.addTab("All Jobs", allJobsPanel);

        add(tabs, BorderLayout.CENTER);

        if (!state.getClock().isSet()) {
            ClockSetupDialog dialog = new ClockSetupDialog(this, state, scheduler, dataStore);
            dialog.setVisible(true);
        }

        refreshAll();
    }

    private JPanel buildAllJobsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));


        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        filterBar.add(new JLabel("Search (Job ID, Student ID, Title):"));
        allJobsSearchField = new JTextField(18);
        allJobsStatusFilter = new JComboBox<>(new String[]{"All", Job.QUEUED, Job.RUNNING, Job.COMPLETED, Job.CANCELLED});
        JButton applyBtn = new JButton("Filter");
        applyBtn.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { refreshAllJobsTable(); } });
        allJobsStatusFilter.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { refreshAllJobsTable(); } });

        filterBar.add(allJobsSearchField);
        filterBar.add(new JLabel("Status:"));
        filterBar.add(allJobsStatusFilter);
        filterBar.add(applyBtn);
        panel.add(filterBar, BorderLayout.NORTH);

        allJobsModel = new DefaultTableModel(
            new String[]{"Job ID", "Student", "Title", "App", "Size", "Status", "GPU", "Submitted", "Start", "Finish", "Charged", "Cancel Reason"}, 0
        ) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(allJobsModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void refreshAllJobsTable() {
        allJobsModel.setRowCount(0);
        String query = allJobsSearchField.getText().trim().toLowerCase();
        String statusFilter = (String) allJobsStatusFilter.getSelectedItem();

        for (Job j : state.getJobs()) {
            if (!"All".equals(statusFilter) && !j.getStatus().equals(statusFilter)) {
                continue;
            }
            if (!query.isEmpty()) {
                boolean match = j.getId().toLowerCase().contains(query)
                             || j.getStudentId().toLowerCase().contains(query)
                             || j.getTitle().toLowerCase().contains(query);
                if (!match) { continue; }
            }
            String gpuStr = j.getGpuId() > 0 ? "GPU" + j.getGpuId() : "-";
            String chargeStr = j.isActive() ? "-" : String.valueOf(j.getCharge());
            String reason = j.getCancellationReason() == null ? "" : j.getCancellationReason();
            allJobsModel.addRow(new Object[]{
                j.getId(), j.getStudentId(), j.getTitle(), j.getApplication(),
                j.getTaskSize() + " (" + j.getDuration() + ")",
                j.getStatus(), gpuStr,
                SimulationClock.format(j.getSubmissionTime()),
                SimulationClock.format(j.getStartTime()),
                SimulationClock.format(j.getFinishTime()),
                chargeStr, reason
            });
        }
    }

    private void handleAdvanceMinute() {
        try {
            scheduler.advanceMinute(state.getAdministrator());
            dataStore.saveAll(state);
            refreshAll();
        } catch (ProjectException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Clock Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleLogout() {
        try {
            dataStore.saveAll(state);
        } catch (ProjectException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
        }
        dispose();
        new LoginFrame(state, accountService, scheduler, dataStore).setVisible(true);
    }

    public void refreshAll() {
        boolean clockSet = state.getClock().isSet();
        clockLbl.setText("Simulated time: " + (clockSet ? SimulationClock.format(state.getClock().getNow()) : "Not set"));
        advanceBtn.setEnabled(clockSet);

        int pendingCount = 0;
        for (AccessRequest r : state.getRequests()) {
            if ("Pending".equalsIgnoreCase(r.getStatus())) { pendingCount++; }
        }
        tabs.setTitleAt(1, "Requests (" + pendingCount + ")");

        dashboardPanel.refresh();
        requestsPanel.refresh();
        studentsPanel.refresh();
        refreshAllJobsTable();
    }
}
