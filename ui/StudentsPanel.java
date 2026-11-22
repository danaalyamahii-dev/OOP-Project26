package gpu.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import gpu.logic.AccountService;
import gpu.logic.Scheduler;
import gpu.logic.SimulationClock;
import gpu.model.*;
import gpu.storage.DataStore;

public class StudentsPanel extends JPanel {
    private final AppState state;
    private final AccountService accountService;
    private final Scheduler scheduler;
    private final DataStore dataStore;
    private final Runnable onDataChanged;

    private JTextField searchField;
    private JLabel studentInfoLbl, allocLbl, usedLbl, resLbl, availLbl;
    private JTextField grantMinutesField, grantReasonField;
    private JButton grantBtn, cancelJobBtn;
    private DefaultTableModel jobsModel;
    private JTable jobsTable;
    private Student currentStudent;

    public StudentsPanel(AppState state, AccountService accountService, Scheduler scheduler, DataStore dataStore, Runnable onDataChanged) {
        this.state = state;
        this.accountService = accountService;
        this.scheduler = scheduler;
        this.dataStore = dataStore;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JPanel top = new JPanel(new BorderLayout(8, 8));
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        searchBar.add(new JLabel("University ID:"));
        searchField = new JTextField(16);
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { searchStudent(); } });
        searchBar.add(searchField);
        searchBar.add(searchBtn);
        top.add(searchBar, BorderLayout.NORTH);

        studentInfoLbl = new JLabel("Search a student by University ID to view balance and jobs.");
        studentInfoLbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        top.add(studentInfoLbl, BorderLayout.CENTER);

        JPanel balancePanel = new JPanel(new GridLayout(1, 4, 8, 8));
        allocLbl = createCard(balancePanel, "Allocated");
        usedLbl = createCard(balancePanel, "Used");
        resLbl = createCard(balancePanel, "Reserved");
        availLbl = createCard(balancePanel, "Available");
        top.add(balancePanel, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(10, 10));

        JPanel grantPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        grantPanel.setBorder(BorderFactory.createTitledBorder("Grant Extra Time"));
        grantPanel.add(new JLabel("Minutes:"));
        grantMinutesField = new JTextField(6);
        grantPanel.add(grantMinutesField);
        grantPanel.add(new JLabel("Reason (required):"));
        grantReasonField = new JTextField(24);
        grantPanel.add(grantReasonField);
        grantBtn = new JButton("Grant");
        grantBtn.setEnabled(false);
        grantBtn.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { handleGrant(); } });
        grantPanel.add(grantBtn);
        center.add(grantPanel, BorderLayout.NORTH);

        jobsModel = new DefaultTableModel(
            new String[]{"Job ID", "Title", "App", "Size", "Status", "GPU", "Start", "Finish", "Charged"}, 0
        ) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        jobsTable = new JTable(jobsModel);
        jobsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane jobsScroll = new JScrollPane(jobsTable);
        jobsScroll.setBorder(BorderFactory.createTitledBorder("Student Jobs"));
        center.add(jobsScroll, BorderLayout.CENTER);

        JPanel bottomActions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        cancelJobBtn = new JButton("Cancel Selected Job...");
        cancelJobBtn.setEnabled(false);
        cancelJobBtn.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { handleCancelJob(); } });
        bottomActions.add(cancelJobBtn);
        center.add(bottomActions, BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);
    }

    private JLabel createCard(JPanel parent, String title) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setForeground(Color.DARK_GRAY);
        JLabel v = new JLabel("-", SwingConstants.CENTER);
        v.setFont(new Font("SansSerif", Font.BOLD, 16));
        card.add(t, BorderLayout.NORTH);
        card.add(v, BorderLayout.CENTER);
        parent.add(card);
        return v;
    }

    private void searchStudent() {
        String id = searchField.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a University ID to search.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            currentStudent = accountService.student(id);
            refresh();
        } catch (ProjectException ex) {
            currentStudent = null;
            refresh();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Student Not Found", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refresh() {
        jobsModel.setRowCount(0);
        if (currentStudent == null) {
            studentInfoLbl.setText("Search a student by University ID to view balance and jobs.");
            allocLbl.setText("-");
            usedLbl.setText("-");
            resLbl.setText("-");
            availLbl.setText("-");
            grantBtn.setEnabled(false);
            cancelJobBtn.setEnabled(false);
            return;
        }

        studentInfoLbl.setText(currentStudent.getName() + " (" + currentStudent.getUsername() + ") | " + currentStudent.getEmail());
        TimeAllowance a = currentStudent.getAllowance();
        allocLbl.setText(String.valueOf(a.getAllocated()));
        usedLbl.setText(String.valueOf(a.getUsed()));
        resLbl.setText(String.valueOf(a.getReserved()));
        availLbl.setText(String.valueOf(a.getAvailable()));
        grantBtn.setEnabled(true);
        cancelJobBtn.setEnabled(true);

        for (Job j : state.getJobs()) {
            if (j.getStudentId().equals(currentStudent.getUsername())) {
                String gpuStr = j.getGpuId() > 0 ? "GPU" + j.getGpuId() : "-";
                String chargeStr = j.isActive() ? "-" : String.valueOf(j.getCharge());
                jobsModel.addRow(new Object[]{
                    j.getId(), j.getTitle(), j.getApplication(), j.getTaskSize() + " (" + j.getDuration() + ")",
                    j.getStatus(), gpuStr,
                    SimulationClock.format(j.getStartTime()),
                    SimulationClock.format(j.getFinishTime()),
                    chargeStr
                });
            }
        }
    }

    private void handleGrant() {
        if (currentStudent == null) { return; }
        String minText = grantMinutesField.getText().trim();
        String reason = grantReasonField.getText().trim();
        int minutes;
        try {
            minutes = Integer.parseInt(minText);
            if (minutes <= 0) { throw new NumberFormatException(); }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Minutes must be a positive whole number.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (reason.isEmpty()) {
            JOptionPane.showMessageDialog(this, "A reason is required to grant extra time.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            accountService.grantTime(state.getAdministrator(), currentStudent.getUsername(), minutes, reason);
            dataStore.saveAll(state);
            grantMinutesField.setText("");
            grantReasonField.setText("");
            JOptionPane.showMessageDialog(this, "Granted +" + minutes + " GPU minutes to " + currentStudent.getName() + ".", "Time Granted", JOptionPane.INFORMATION_MESSAGE);
            onDataChanged.run();
        } catch (ProjectException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Grant Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleCancelJob() {
        int row = jobsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a queued or running job to cancel.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String jobId = (String) jobsModel.getValueAt(row, 0);
        try {
            Job job = scheduler.accessibleJob(state.getAdministrator(), jobId);
            if (!job.isActive()) {
                JOptionPane.showMessageDialog(this, "Only Queued or Running jobs can be cancelled.", "Cannot Cancel", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String chargeNote;
            if (Job.RUNNING.equals(job.getStatus())) {
                int ran = job.getRuntime(state.getClock().getNow());
                int returned = job.getDuration() - ran;
                chargeNote = job.getId() + " is running on GPU" + job.getGpuId() + " and has run " + ran + " min.\n"
                           + ran + " min will be charged; " + returned + " min return to the balance.";
            } else {
                chargeNote = job.getId() + " is currently Queued.\nNo charge will be applied; " + job.getDuration() + " min return to the balance.";
            }

            String reason = JOptionPane.showInputDialog(this, chargeNote + "\n\nEnter cancellation reason (required):", "Cancel Job " + jobId, JOptionPane.WARNING_MESSAGE);
            if (reason == null) { return; } // user pressed Cancel
            if (reason.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Cancellation reason cannot be blank.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            scheduler.cancel(state.getAdministrator(), jobId, reason.trim());
            dataStore.saveAll(state);
            onDataChanged.run();
        } catch (ProjectException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cancellation Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
