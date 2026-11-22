package gpu.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import gpu.logic.AccountService;
import gpu.model.*;
import gpu.storage.DataStore;

public class RequestsPanel extends JPanel {
    private final AppState state;
    private final AccountService accountService;
    private final DataStore dataStore;
    private final Runnable onDataChanged;

    private JComboBox<String> filterBox;
    private DefaultTableModel tableModel;
    private JTable table;
    private JLabel selectedLbl;
    private JSpinner initialMinutesSpinner;
    private JButton approveBtn, rejectBtn;

    public RequestsPanel(AppState state, AccountService accountService, DataStore dataStore, Runnable onDataChanged) {
        this.state = state;
        this.accountService = accountService;
        this.dataStore = dataStore;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        // Top filter bar
        JPanel top = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Registration Requests");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        top.add(title, BorderLayout.WEST);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        filterPanel.add(new JLabel("Show:"));
        filterBox = new JComboBox<>(new String[]{"Pending", "Approved", "Rejected", "All"});
        filterBox.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { refresh(); } });
        filterPanel.add(filterBox);
        top.add(filterPanel, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // Requests table
        tableModel = new DefaultTableModel(
            new String[]{"University ID", "Name", "Email", "Phone", "Password", "Status"}, 0
        ) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) { updateSelectionControls(); }
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Bottom action panel
        JPanel bottom = new JPanel(new BorderLayout(10, 8));
        bottom.setBorder(BorderFactory.createTitledBorder("Review Selected Request"));

        selectedLbl = new JLabel("Select a pending request above to approve or reject.");
        selectedLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        bottom.add(selectedLbl, BorderLayout.NORTH);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        controls.add(new JLabel("Initial GPU minutes:"));
        initialMinutesSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 10000, 10));
        controls.add(initialMinutesSpinner);

        approveBtn = new JButton("Approve");
        rejectBtn = new JButton("Reject");
        approveBtn.setEnabled(false);
        rejectBtn.setEnabled(false);

        approveBtn.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { handleApprove(); } });
        rejectBtn.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { handleReject(); } });

        controls.add(approveBtn);
        controls.add(rejectBtn);
        bottom.add(controls, BorderLayout.CENTER);

        add(bottom, BorderLayout.SOUTH);
        refresh();
    }

    public void refresh() {
        String filter = (String) filterBox.getSelectedItem();
        tableModel.setRowCount(0);
        for (AccessRequest req : state.getRequests()) {
            if ("All".equals(filter) || req.getStatus().equalsIgnoreCase(filter)) {
                tableModel.addRow(new Object[]{
                    req.getId(),
                    req.getName(),
                    req.getEmail(),
                    req.getPhone(),
                    "••••••",
                    req.getStatus()
                });
            }
        }
        updateSelectionControls();
    }

    private AccessRequest getSelectedRequest() {
        int row = table.getSelectedRow();
        if (row < 0) { return null; }
        String id = (String) tableModel.getValueAt(row, 0);
        for (AccessRequest req : state.getRequests()) {
            if (req.getId().equals(id)) { return req; }
        }
        return null;
    }

    private void updateSelectionControls() {
        AccessRequest req = getSelectedRequest();
        if (req == null) {
            selectedLbl.setText("Select a pending request above to approve or reject.");
            approveBtn.setEnabled(false);
            rejectBtn.setEnabled(false);
            return;
        }
        selectedLbl.setText("Selected: " + req.getName() + " (" + req.getId() + ") — Status: " + req.getStatus());
        boolean isPending = "Pending".equalsIgnoreCase(req.getStatus());
        approveBtn.setEnabled(isPending);
        rejectBtn.setEnabled(isPending);
        initialMinutesSpinner.setEnabled(isPending);
    }

    private void handleApprove() {
        AccessRequest req = getSelectedRequest();
        if (req == null) { return; }
        int minutes = (Integer) initialMinutesSpinner.getValue();
        try {
            accountService.approveRequest(state.getAdministrator(), req.getId(), minutes);
            dataStore.saveAll(state);
            JOptionPane.showMessageDialog(this,
                "Approved " + req.getName() + " (" + req.getId() + ") with " + minutes + " GPU minutes.",
                "Request Approved", JOptionPane.INFORMATION_MESSAGE);
            onDataChanged.run();
        } catch (ProjectException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Approval Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleReject() {
        AccessRequest req = getSelectedRequest();
        if (req == null) { return; }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Reject registration request for " + req.getName() + " (" + req.getId() + ")?",
            "Confirm Rejection", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) { return; }
        try {
            accountService.rejectRequest(state.getAdministrator(), req.getId());
            dataStore.saveAll(state);
            onDataChanged.run();
        } catch (ProjectException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Rejection Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
