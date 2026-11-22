package gpu.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import gpu.logic.*;
import gpu.model.*;
import gpu.storage.DataStore;

public class StudentFrame extends JFrame {
    private AppState state;
    private AccountService accounts;
    private Scheduler scheduler;
    private DataStore store;
    private Student student;
    private JLabel time = new JLabel();
    private JLabel allocatedValue, usedValue, reservedValue, availableValue, durationLabel;
    private JTextField title = new JTextField();
    private JTextField application = new JTextField();
    private JTextField search = new JTextField(20);
    private JTextArea description = new JTextArea(4, 30);
    private JComboBox<String> size = new JComboBox<String>(new String[]{"Small (5)","Medium (15)","Large (30)","Very large (60)"});
    private JComboBox<String> status = new JComboBox<String>(new String[]{"All","Queued","Running","Completed","Cancelled"});
    private DefaultTableModel jobs = new DefaultTableModel(new String[]{"Job","Title","Application","Size","Status","GPU","Submitted","Start","Finish","Wait"},0) {
        public boolean isCellEditable(int r,int c) { return false; }
    };
    private JTable table = new JTable(jobs);
    public StudentFrame(AppState state, AccountService accounts, Scheduler scheduler, DataStore store, Student student) {
        super("GPU Utilization Management - Student");
        this.state = state;
        this.accounts = accounts;
        this.scheduler = scheduler;
        this.store = store;
        this.student = student;
        setSize(1050, 680);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(12, 12));

        JPanel header = new JPanel(new BorderLayout(10, 8));
        JPanel heading = new JPanel(new GridLayout(2, 1));
        JLabel welcome = new JLabel("Welcome, " + student.getName());
        welcome.setFont(new Font("SansSerif", Font.BOLD, 19));
        heading.add(welcome);
        heading.add(new JLabel("Student - " + student.getUsername()));
        header.add(heading, BorderLayout.WEST);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 4));
        controls.add(time);
        JButton logout = new JButton("Log out");
        logout.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { logout(); }
        });
        controls.add(logout);
        header.add(controls, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        JPanel home = new JPanel(new BorderLayout(12, 14));
        JPanel balances = new JPanel(new GridLayout(1, 4, 10, 10));
        allocatedValue = balanceCard(balances, "Allocated");
        usedValue = balanceCard(balances, "Used");
        reservedValue = balanceCard(balances, "Reserved");
        availableValue = balanceCard(balances, "Available");
        home.add(balances, BorderLayout.NORTH);

        JPanel form = new JPanel(new BorderLayout(10, 12));
        form.setBorder(BorderFactory.createTitledBorder("Submit a new job"));
        JPanel formContent = new JPanel(new BorderLayout(10, 12));
        JPanel fields = new JPanel(new GridLayout(2, 2, 12, 5));
        fields.add(new JLabel("Job title"));
        fields.add(new JLabel("Application name"));
        fields.add(title);
        fields.add(application);
        formContent.add(fields, BorderLayout.NORTH);

        JPanel descriptionPanel = new JPanel(new BorderLayout(4, 4));
        descriptionPanel.add(new JLabel("Description"), BorderLayout.NORTH);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        JScrollPane descriptionScroll = new JScrollPane(description);
        descriptionScroll.setPreferredSize(new Dimension(600, 110));
        descriptionPanel.add(descriptionScroll, BorderLayout.CENTER);
        formContent.add(descriptionPanel, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        actions.add(new JLabel("Task size"));
        actions.add(size);
        durationLabel = new JLabel("Duration: 5 minutes");
        actions.add(durationLabel);
        size.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { updateDuration(); }
        });
        JButton submit = new JButton("Submit job");
        submit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { submit(); }
        });
        actions.add(submit);
        formContent.add(actions, BorderLayout.SOUTH);
        form.add(formContent, BorderLayout.CENTER);
        home.add(form, BorderLayout.CENTER);
        tabs.addTab("Home", home);

        JPanel myJobs = new JPanel(new BorderLayout(10, 10));
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        filters.add(new JLabel("Search by job ID or title"));
        filters.add(search);
        filters.add(status);
        JButton find = new JButton("Search");
        find.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { refresh(); }
        });
        filters.add(find);
        status.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { refresh(); }
        });
        JButton cancel = new JButton("Cancel selected job");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { cancel(); }
        });
        filters.add(cancel);
        myJobs.add(filters, BorderLayout.NORTH);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        myJobs.add(new JScrollPane(table), BorderLayout.CENTER);
        myJobs.add(new JLabel("Queued jobs show predicted start, finish and remaining wait."), BorderLayout.SOUTH);
        tabs.addTab("My jobs", myJobs);
        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
        refresh();
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { saveExit(); }
        });
    }

    private JLabel balanceCard(JPanel parent, String heading) {
        JPanel card = new JPanel(new GridLayout(2, 1, 2, 2));
        card.setBorder(BorderFactory.createTitledBorder(heading));
        JLabel value = new JLabel("0", SwingConstants.CENTER);
        value.setFont(new Font("SansSerif", Font.BOLD, 20));
        card.add(new JLabel("GPU minutes", SwingConstants.CENTER));
        card.add(value);
        parent.add(card);
        return value;
    }

    private void updateDuration() {
        int[] values = {5, 15, 30, 60};
        durationLabel.setText("Duration: " + values[size.getSelectedIndex()] + " minutes");
    }
    private void refresh() {
        TimeAllowance allowance = student.getAllowance();
        allocatedValue.setText("" + allowance.getAllocated());
        usedValue.setText("" + allowance.getUsed());
        reservedValue.setText("" + allowance.getReserved());
        availableValue.setText("" + allowance.getAvailable());

        if (state.getClock().isSet()) {
            time.setText("Simulated time: " + SimulationClock.format(state.getClock().getNow()));
        } else {
            time.setText("Simulated time: Not set");
        }

        jobs.setRowCount(0);
        ArrayList<Prediction> predictions = scheduler.predictions();
        String query = search.getText().trim().toLowerCase();
        String selectedStatus = (String) status.getSelectedItem();

        for (Job job : state.getJobs()) {
            if (!job.getStudentId().equals(student.getUsername())) {
                continue;
            }
            if (!job.getId().toLowerCase().contains(query)
                    && !job.getTitle().toLowerCase().contains(query)) {
                continue;
            }
            if (!"All".equals(selectedStatus) && !job.getStatus().equals(selectedStatus)) {
                continue;
            }

            String start = SimulationClock.format(job.getStartTime());
            String finish = SimulationClock.format(job.getFinishTime());
            String wait = "-";
            for (Prediction prediction : predictions) {
                if (prediction.getJobId().equals(job.getId())) {
                    start = SimulationClock.format(prediction.getStart());
                    finish = SimulationClock.format(prediction.getFinish());
                    wait = prediction.getWaitMinutes() + " min";
                    break;
                }
            }
            String gpu = "-";
            if (job.getGpuId() > 0) {
                gpu = "GPU" + job.getGpuId();
            }
            jobs.addRow(new Object[]{job.getId(), job.getTitle(), job.getApplication(),
                    job.getDuration(), job.getStatus(), gpu,
                    SimulationClock.format(job.getSubmissionTime()), start, finish, wait});
        }
    }

    private void submit() {
        try {
            int[] durations = {5, 15, 30, 60};
            int duration = durations[size.getSelectedIndex()];
            int available = student.getAllowance().getAvailable();
            if (!state.getClock().isSet()) {
                throw new ProjectException("The administrator must initialize the clock first.");
            }
            if (duration > available) {
                throw new ProjectException("The job exceeds your available GPU minutes.");
            }
            int choice = JOptionPane.showConfirmDialog(this,
                    "Submit " + title.getText() + "? Duration: " + duration
                    + " min. Available after: " + (available - duration),
                    "Confirm job", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            scheduler.submit(student, title.getText(), application.getText(),
                    description.getText(), duration);
            store.saveAll(state);
            refresh();
            JOptionPane.showMessageDialog(this, "Job submitted successfully.");
        } catch (ProjectException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Submission error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancel() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a job first.");
            return;
        }
        String id = (String) jobs.getValueAt(row, 0);
        try {
            Job job = scheduler.accessibleJob(student, id);
            if (!job.isActive()) {
                throw new ProjectException("Only queued or running jobs can be cancelled.");
            }
            int charge = 0;
            if (job.getStatus().equals(Job.RUNNING)) {
                charge = job.getRuntime(state.getClock().getNow());
            }
            String reason = JOptionPane.showInputDialog(this,
                    "Cancel " + id + "? Charge: " + charge + " minutes. Enter reason:");
            if (reason == null) {
                return;
            }
            scheduler.cancel(student, id, reason);
            store.saveAll(state);
            refresh();
        } catch (ProjectException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Cancellation error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void logout() {
        try {
            store.saveAll(state);
            dispose();
            new LoginFrame(state, accounts, scheduler, store).setVisible(true);
        } catch (ProjectException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    private void saveExit() {
        try {
            store.saveAll(state);
            dispose();
            System.exit(0);
        } catch (ProjectException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }
}
