package gpu.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import gpu.logic.*;
import gpu.model.*;
import gpu.storage.StateStore;

/** Button handling follows the getSource pattern used in Lab 8. */
public class AppController implements ActionListener {
    private AppState state;
    private AccountService accounts;
    private Scheduler scheduler;
    private StateStore store;
    private ScreenView view;
    private User user;
    private String screen = "login";
    private String searchText = "";
    private String studentFilter = "";
    private int page;
    private int balancePage;
    private int completedPage;
    private final int pageSize = 2;
    private String saveMessage = "Use Save and exit to close the application.";
    private JTextField username, password, fullName, email, phone;
    private JTextField selectedId, minutes, reason, search;
    private JTextField jobTitle, application, description;
    private JLabel sizeLabel;
    private JButton signIn, registerScreen, register, back;
    private JButton saveNow, logout, exit, menu, advance;
    private JButton submit, chooseSize, approve, reject, searchStudents, studentJobs, grant;
    private JButton searchJobs, details, cancel, previous, next;
    private JButton previousBalances, nextBalances, previousCompleted, nextCompleted;
    private JButton pickDate, pickTime, initialize;
    private int duration = 5;
    private int day = 16, month = 11, year = 2026, hour = 10, minute;

    public AppController(AppState state, StateStore store, ScreenView view) {
        this.state = state;
        this.store = store;
        this.view = view;
        accounts = new AccountService(state);
        scheduler = new Scheduler(state);
        refresh();
    }

    public User getUser() { return user; }

    private JButton button(String text) {
        JButton result = new JButton(text);
        result.addActionListener(this);
        view.addButton(result);
        return result;
    }

    private void label(String text) { view.addLabel(new JLabel(text)); }

    private JTextField field(String text, String value) {
        label(text);
        JTextField result = new JTextField(value);
        view.addField(result);
        return result;
    }

    private String time(long value) { return SimulationClock.format(value); }

    public void login(String name, String enteredPassword) throws ProjectException {
        user = accounts.login(name, enteredPassword);
        if (user.getRole().equals("Administrator")) { screen = "dashboard"; }
        else { screen = "home"; }
        page = 0; balancePage = 0; completedPage = 0;
        searchText = ""; studentFilter = "";
        refresh();
    }

    private String balance(Student student) {
        TimeAllowance a = student.getAllowance();
        return student.getUsername() + " | " + student.getName() + " | Allocated: " + a.getAllocated()
            + " | Used: " + a.getUsed() + " | Reserved: " + a.getReserved() + " | Available: " + a.getAvailable();
    }

    private void header() {
        label(user.getName() + " | " + user.getRole() + " | Simulation: " + time(state.getClock().getNow()));
        menu = button("Open screen");
        saveNow = button("Save now");
        logout = button("Log out");
        exit = button("Save and exit");
        if (user == state.getAdministrator()) {
            advance = button("Advance Minute");
            advance.setEnabled(state.getClock().isSet());
        } else { label(balance((Student) user)); }
    }

    public void refresh() {
        view.clearScreen();
        // Clear references so a button from a previous screen cannot be handled.
        signIn = null; registerScreen = null; register = null; back = null;
        saveNow = null; logout = null; exit = null; menu = null; advance = null;
        submit = null; chooseSize = null; approve = null; reject = null;
        searchStudents = null; studentJobs = null; grant = null; searchJobs = null;
        details = null; cancel = null; previous = null; next = null;
        previousBalances = null; nextBalances = null; previousCompleted = null; nextCompleted = null;
        pickDate = null; pickTime = null; initialize = null;
        if (screen.equals("login")) {
            label("GPU Utilization Management | Four simulated H100 GPUs");
            username = field("University ID or admin", "");
            password = field("Password", "");
            signIn = button("Sign in"); registerScreen = button("Register"); exit = button("Save and exit");
        } else if (screen.equals("register")) {
            label("Student registration");
            username = field("University ID", ""); fullName = field("Full name", "");
            email = field("Email", ""); phone = field("Phone", ""); password = field("Chosen password", "");
            register = button("Submit request"); back = button("Back to login");
        } else {
            header();
            try {
                if (screen.equals("dashboard")) { dashboard(); }
                else if (screen.equals("home")) { studentHome(); }
                else if (screen.equals("clock")) { clockSetup(); }
                else if (screen.equals("requests")) { requests(); }
                else if (screen.equals("students")) { students(); }
                else if (screen.equals("jobs")) { jobs(); }
            } catch (ProjectException ex) { label(ex.getMessage()); }
        }
        label(saveMessage);
        view.showScreen();
    }

    private void openScreen() {
        String[] choices;
        if (user == state.getAdministrator()) {
            if (state.getClock().isSet()) {
                choices = new String[]{"Dashboard", "Registration requests", "Students", "All jobs"};
            } else {
                choices = new String[]{"Dashboard", "Registration requests", "Students", "All jobs", "Initialize clock"};
            }
        } else { choices = new String[]{"Submit a job", "My jobs"}; }
        String selected = view.choose("Open screen", choices, choices[0]);
        if (selected == null) { return; }
        if (selected.equals("Dashboard")) { screen = "dashboard"; }
        else if (selected.equals("Registration requests")) { screen = "requests"; }
        else if (selected.equals("Students")) { screen = "students"; }
        else if (selected.equals("Initialize clock")) { screen = "clock"; }
        else if (selected.equals("Submit a job")) { screen = "home"; }
        else { screen = "jobs"; }
        page = 0; searchText = ""; studentFilter = "";
        refresh();
    }

    private int count(String status) {
        int total = 0;
        for (Job job : state.getJobs()) { if (job.getStatus().equals(status)) { total++; } }
        return total;
    }

    private int lastPage(int total) {
        if (total == 0) { return 0; }
        return (total - 1) / pageSize;
    }

    private void dashboard() throws ProjectException {
        accounts.requireAdmin(user);
        String utilization = "N/A";
        if (scheduler.utilization() >= 0) { utilization = String.format("%.1f%%", scheduler.utilization()); }
        label("DASHBOARD | Queued: " + count(Job.QUEUED) + " | Running: " + count(Job.RUNNING)
            + " | Completed: " + count(Job.COMPLETED) + " | Cancelled: " + count(Job.CANCELLED)
            + " | Busy GPUs: " + count(Job.RUNNING) + "/4 | Utilization: " + utilization);
        for (int id = 1; id <= 4; id++) {
            GPU gpu = state.getGpu(id);
            if (gpu.isFree()) { label("GPU " + id + " | Available | Job: - | Remaining: 0 | Expected finish: -"); }
            else {
                Job job = gpu.getCurrentJob();
                label("GPU " + id + " | Busy | " + job.getId() + " | Remaining: "
                    + (job.getExpectedFinish() - state.getClock().getNow()) + " | Expected finish: " + time(job.getExpectedFinish()));
            }
        }
        ArrayList<Student> students = state.getStudents();
        if (balancePage > lastPage(students.size())) { balancePage = lastPage(students.size()); }
        label("STUDENT BALANCES | Page " + (balancePage + 1) + " of " + (lastPage(students.size()) + 1));
        for (int i = balancePage * pageSize; i < students.size() && i < (balancePage + 1) * pageSize; i++) {
            label(balance(students.get(i)));
        }
        if (students.size() == 0) { label("No approved students."); }
        previousBalances = button("Previous balances"); previousBalances.setEnabled(balancePage > 0);
        nextBalances = button("Next balances"); nextBalances.setEnabled(balancePage < lastPage(students.size()));
        label("ALL JOBS | ~ means predicted time; all times are simulated.");
        showJobRecords(state.getJobs(), page);
        pagination(state.getJobs().size());
        ArrayList<Job> completed = new ArrayList<Job>();
        for (Job job : state.getJobs()) { if (job.getStatus().equals(Job.COMPLETED)) { completed.add(job); } }
        if (completedPage > lastPage(completed.size())) { completedPage = lastPage(completed.size()); }
        label("COMPLETED RESULTS | Page " + (completedPage + 1) + " of " + (lastPage(completed.size()) + 1));
        showJobRecords(completed, completedPage);
        previousCompleted = button("Previous results"); previousCompleted.setEnabled(completedPage > 0);
        nextCompleted = button("Next results"); nextCompleted.setEnabled(completedPage < lastPage(completed.size()));
    }

    private void studentHome() throws ProjectException {
        accounts.requireStudent(user);
        label("Submit a job");
        jobTitle = field("Job title", ""); application = field("Application name", "");
        description = field("Description", ""); duration = 5;
        chooseSize = button("Choose task size");
        sizeLabel = new JLabel("Small | Workload units: 5 | Duration: 5 minutes"); view.addLabel(sizeLabel);
        submit = button("Submit job");
        submit.setEnabled(state.getClock().isSet() && ((Student) user).getAllowance().getAvailable() > 0);
        if (!state.getClock().isSet()) { label("Waiting for the administrator to initialize the clock."); }
    }

    private int chooseNumber(String title, int minimum, int maximum, int selected) {
        String[] options = new String[maximum - minimum + 1];
        for (int i = 0; i < options.length; i++) { options[i] = "" + (minimum + i); }
        String result = view.choose(title, options, "" + selected);
        if (result == null) { return selected; }
        return Integer.parseInt(result);
    }

    private void clockSetup() throws ProjectException {
        accounts.requireAdmin(user);
        label("Initialize once using the date and time selection lists, then use Advance Minute.");
        pickDate = button("Pick date: " + String.format("%02d/%02d/%04d", day, month, year));
        pickTime = button("Pick time: " + String.format("%02d:%02d", hour, minute));
        initialize = button("Initialize clock");
    }

    private void requests() throws ProjectException {
        accounts.requireAdmin(user);
        label("Registration requests");
        ArrayList<AccessRequest> list = state.getRequests();
        for (int i = page * pageSize; i < list.size() && i < (page + 1) * pageSize; i++) {
            AccessRequest r = list.get(i);
            label(r.getId() + " | " + r.getName() + " | " + r.getStatus());
            label("Email: " + r.getEmail() + " | Phone: " + r.getPhone() + " | Chosen password: " + r.getPassword());
        }
        pagination(list.size());
        selectedId = field("Request university ID", ""); minutes = field("Initial GPU minutes", "100");
        approve = button("Approve"); reject = button("Reject");
    }

    private void students() throws ProjectException {
        accounts.requireAdmin(user);
        search = field("Search university ID or leave empty for all", searchText);
        searchStudents = button("Search students");
        ArrayList<Student> list = new ArrayList<Student>();
        for (Student s : state.getStudents()) { if (searchText.isEmpty() || s.getUsername().equals(searchText)) { list.add(s); } }
        for (int i = page * pageSize; i < list.size() && i < (page + 1) * pageSize; i++) { label(balance(list.get(i))); }
        if (list.size() == 0) { label("No matching students."); }
        pagination(list.size());
        selectedId = field("Student ID for jobs or grant", searchText);
        minutes = field("Extra GPU minutes", ""); reason = field("Grant reason", "");
        studentJobs = button("Show student jobs"); grant = button("Grant time");
    }

    private void jobs() throws ProjectException {
        search = field("Search jobs by ID or title", searchText);
        searchJobs = button("Search jobs");
        ArrayList<Job> list = new ArrayList<Job>();
        for (Job job : scheduler.visibleJobs(user)) {
            boolean owner = studentFilter.isEmpty() || job.getStudentId().equals(studentFilter);
            boolean match = job.getId().toLowerCase().contains(searchText.toLowerCase())
                || job.getTitle().toLowerCase().contains(searchText.toLowerCase());
            if (owner && match) { list.add(job); }
        }
        showJobRecords(list, page); pagination(list.size());
        selectedId = field("Job ID for details or cancellation", "");
        details = button("View details"); cancel = button("Cancel job");
    }

    private void showJobRecords(ArrayList<Job> list, int selectedPage) {
        for (int i = selectedPage * pageSize; i < list.size() && i < (selectedPage + 1) * pageSize; i++) {
            Job job = list.get(i);
            Prediction prediction = scheduler.prediction(job.getId());
            String position = "-", wait = "-", begin = time(job.getStartTime());
            long end = job.getFinishTime();
            if (job.getStatus().equals(Job.RUNNING)) { end = job.getExpectedFinish(); }
            String finish = time(end);
            if (prediction != null) {
                position = "" + prediction.getPosition(); wait = "" + prediction.getWaitMinutes();
                begin = "~ " + time(prediction.getStart()); finish = "~ " + time(prediction.getFinish());
            }
            String charge = "Pending";
            if (!job.isActive()) { charge = "" + job.getCharge(); }
            label(job.getId() + " | Student: " + job.getStudentId() + " | App: " + job.getApplication()
                + " | " + job.getStatus() + " | Queue position: " + position + " | Remaining wait: " + wait);
            label("Title: " + job.getTitle() + " | " + job.getTaskSize() + " | GPU: " + gpuText(job));
            label("Submitted: " + time(job.getSubmissionTime()) + " | Start: " + begin + " | Finish: " + finish);
            label("Runtime: " + job.getRuntime(state.getClock().getNow()) + " | Total queue wait: "
                + queueWaitText(job) + " | Charged: " + charge);
        }
        if (list.size() == 0) { label("No matching jobs."); }
    }

    private String gpuText(Job job) {
        if (job.getGpuId() == 0) { return "-"; }
        return "" + job.getGpuId();
    }

    private String queueWaitText(Job job) {
        if (job.getStartTime() < 0) { return "-"; }
        return "" + job.getQueueWait();
    }

    private void pagination(int total) {
        label("Page " + (page + 1) + " of " + (lastPage(total) + 1) + " | Records: " + total);
        previous = button("Previous jobs/records"); previous.setEnabled(page > 0);
        next = button("Next jobs/records"); next.setEnabled(page < lastPage(total));
    }

    public String jobDetails(String id) throws ProjectException {
        Job job = scheduler.accessibleJob(user, id);
        Prediction prediction = scheduler.prediction(job.getId());
        String text = "Job: " + job.getId() + " | Owner: " + job.getStudentId()
            + "\nTitle: " + job.getTitle() + "\nApplication: " + job.getApplication()
            + "\nDescription: " + job.getDescription() + "\nTask size: " + job.getTaskSize()
            + "\nWorkload units and estimated duration: " + job.getDuration()
            + "\nStatus: " + job.getStatus() + "\nGPU: " + gpuText(job)
            + "\nSubmitted: " + time(job.getSubmissionTime()) + "\nStarted: " + time(job.getStartTime())
            + "\nFinished: " + time(job.getFinishTime())
            + "\nRuntime: " + job.getRuntime(state.getClock().getNow())
            + "\nTotal queue wait: " + queueWaitText(job);
        if (job.isActive()) { text += "\nCharge: pending"; }
        else { text += "\nCharge: " + job.getCharge(); }
        if (job.getStatus().equals(Job.RUNNING)) { text += "\nExpected finish: " + time(job.getExpectedFinish()); }
        if (prediction != null) {
            text += "\nQueue position: " + prediction.getPosition() + "\nPredicted start: " + time(prediction.getStart())
                + "\nPredicted finish: " + time(prediction.getFinish()) + "\nRemaining wait: " + prediction.getWaitMinutes();
        }
        if (job.getStatus().equals(Job.CANCELLED)) {
            text += "\nCancelled: " + time(job.getCancellationTime()) + "\nBy: " + job.getCancelledBy()
                + "\nReason: " + job.getCancellationReason();
        }
        return text;
    }

    private boolean confirm(String text) {
        String[] options = {"Confirm", "Back"};
        String selected = view.choose(text, options, options[1]);
        return selected != null && selected.equals("Confirm");
    }

    public void save() throws IOException {
        try {
            store.save(state);
            saveMessage = "Saved | Simulation: " + time(state.getClock().getNow()) + " | Use Save and exit to close.";
        } catch (IOException ex) {
            saveMessage = "Not saved. Use Save now to retry.";
            throw ex;
        }
    }

    private void saveChange() {
        try { save(); }
        catch (IOException ex) { view.message("File error: " + ex.getMessage()); }
        refresh();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        try {
            if (event.getSource() == signIn) { login(username.getText(), password.getText()); }
            else if (event.getSource() == registerScreen) { screen = "register"; refresh(); }
            else if (event.getSource() == register) {
                accounts.register(username.getText(), fullName.getText(), email.getText(), phone.getText(), password.getText());
                screen = "login"; saveChange(); view.message("Request created. Wait for administrator approval.");
            } else if (event.getSource() == back) { screen = "login"; refresh(); }
            else if (event.getSource() == menu) { openScreen(); }
            else if (event.getSource() == saveNow) { save(); refresh(); }
            else if (event.getSource() == logout) { save(); user = null; screen = "login"; refresh(); }
            else if (event.getSource() == exit) { save(); view.closeScreen(); }
            else if (event.getSource() == advance) { scheduler.advanceMinute(user); saveChange(); }
            else if (event.getSource() == previous) { page--; refresh(); }
            else if (event.getSource() == next) { page++; refresh(); }
            else if (event.getSource() == previousBalances) { balancePage--; refresh(); }
            else if (event.getSource() == nextBalances) { balancePage++; refresh(); }
            else if (event.getSource() == previousCompleted) { completedPage--; refresh(); }
            else if (event.getSource() == nextCompleted) { completedPage++; refresh(); }
            else if (event.getSource() == pickDate) {
                year = chooseNumber("Pick year", 1, 9999, year);
                month = chooseNumber("Pick month", 1, 12, month);
                int maximum = SimulationClock.daysInMonth(month, year);
                if (day > maximum) { day = maximum; }
                day = chooseNumber("Pick day", 1, maximum, day);
                refresh();
            } else if (event.getSource() == pickTime) {
                hour = chooseNumber("Pick hour (24-hour clock)", 0, 23, hour);
                minute = chooseNumber("Pick minute", 0, 59, minute);
                refresh();
            } else if (event.getSource() == initialize) {
                long selected = SimulationClock.toMinutes(day, month, year, hour, minute);
                if (confirm("Initialize simulation to " + time(selected) + "?")) {
                    scheduler.initializeClock(user, day, month, year, hour, minute);
                    screen = "dashboard"; saveChange();
                }
            } else if (event.getSource() == chooseSize) {
                String[] options = {"Small - 5 minutes", "Medium - 15 minutes", "Large - 30 minutes", "Very large - 60 minutes"};
                String selected = view.choose("Select task size", options, options[0]);
                if (selected != null) {
                    int[] values = {5, 15, 30, 60};
                    for (int i = 0; i < options.length; i++) { if (options[i].equals(selected)) { duration = values[i]; } }
                    sizeLabel.setText(selected + " | Workload units: " + duration);
                }
            } else if (event.getSource() == submit) {
                if (confirm("Submit this job and reserve " + duration + " GPU minutes?")) {
                    scheduler.submit(user, jobTitle.getText(), application.getText(), description.getText(), duration);
                    saveChange();
                }
            } else if (event.getSource() == approve) {
                accounts.approve(user, selectedId.getText().trim(), AccountService.wholeMinutes(minutes.getText(), true)); saveChange();
            } else if (event.getSource() == reject) {
                accounts.reject(user, selectedId.getText().trim()); saveChange();
            } else if (event.getSource() == searchStudents || event.getSource() == searchJobs) {
                searchText = search.getText().trim(); page = 0; refresh();
            } else if (event.getSource() == studentJobs) {
                accounts.requireAdmin(user); studentFilter = accounts.student(selectedId.getText().trim()).getUsername();
                screen = "jobs"; page = 0; searchText = ""; refresh();
            } else if (event.getSource() == grant) {
                accounts.grant(user, selectedId.getText().trim(), AccountService.wholeMinutes(minutes.getText(), false), reason.getText());
                saveChange();
            } else if (event.getSource() == details) { view.message(jobDetails(selectedId.getText().trim())); }
            else if (event.getSource() == cancel) {
                Job job = scheduler.accessibleJob(user, selectedId.getText().trim());
                if (!job.isActive()) { throw new ProjectException("Only queued or running jobs can be cancelled."); }
                String explanation = view.ask("Cancel " + job.getId() + "? Charge: " + job.getRuntime(state.getClock().getNow())
                    + " minutes. Release reservation: " + job.getDuration() + " minutes.\nEnter a reason, or Cancel to keep the job:");
                if (explanation != null) { scheduler.cancel(user, job.getId(), explanation); saveChange(); }
            }
        } catch (Exception ex) { view.message("Unable to complete action: " + ex.getMessage()); }
    }
}
