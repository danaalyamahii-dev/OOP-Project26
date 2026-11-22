package gpu.model;

import java.util.ArrayList;

public class AppState {
    private SimulationClock clock;
    private GPU[] gpus;
    private Administrator administrator;
    private ArrayList<Student> students;
    private ArrayList<AccessRequest> requests;
    private ArrayList<Job> jobs;
    private ArrayList<String> timeGrantLogs;
    private int nextJobNumber;

    public AppState() {
        this.clock = new SimulationClock();
        this.gpus = new GPU[4];
        for (int i = 0; i < 4; i++) {
            this.gpus[i] = new GPU(i + 1);
        }
        this.administrator = new Administrator();
        this.students = new ArrayList<>();
        this.requests = new ArrayList<>();
        this.jobs = new ArrayList<>();
        this.timeGrantLogs = new ArrayList<>();
        this.nextJobNumber = 101;
    }

    public SimulationClock getClock() { return clock; }
    public void setClock(SimulationClock clock) { this.clock = clock; }

    public GPU getGpu(int id) {
        if (id < 1 || id > 4) { return null; }
        return gpus[id - 1];
    }

    public GPU[] getGpus() { return gpus; }
    public Administrator getAdministrator() { return administrator; }
    public ArrayList<Student> getStudents() { return students; }
    public ArrayList<AccessRequest> getRequests() { return requests; }
    public ArrayList<Job> getJobs() { return jobs; }
    public ArrayList<String> getTimeGrantLogs() { return timeGrantLogs; }

    public int getNextJobNumber() { return nextJobNumber; }
    public void setNextJobNumber(int nextJobNumber) { this.nextJobNumber = nextJobNumber; }

    public void addJob(Job job) {
        jobs.add(job);
        if (job.getNumber() >= nextJobNumber) {
            nextJobNumber = job.getNumber() + 1;
        }
    }

public void addStudent(Student student) {
    students.add(student);
}

public void addRequest(AccessRequest request) {
    requests.add(request);
}

public Student findStudent(String id) {
    for (Student student : students) {
        if (student.getUsername().equals(id)) {
            return student;
        }
    }
    return null;
}

public AccessRequest findRequest(String id) {
    for (AccessRequest request : requests) {
        if (request.getId().equals(id)) {
            return request;
        }
    }
    return null;
}

private ArrayList<TimeGrant> grants = new ArrayList<TimeGrant>();

public void addGrant(TimeGrant grant) {
    grants.add(grant);
}

public ArrayList<TimeGrant> getGrants() {
    return grants;
}

public void addTimeGrantLog(String logEntry) {
    timeGrantLogs.add(logEntry);
}
}


