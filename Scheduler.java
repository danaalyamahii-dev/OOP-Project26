package logic;

import java.util.ArrayList;
import gpu.model.*;

public class Scheduler {
    private AppState state;
    private AccountService accounts;

    public Scheduler(AppState state) {
        this.state = state;
        accounts = new AccountService(state);
    }

    public void initializeClock(User actor, int day, int month, int year, int hour, int minute) throws ProjectException {
        accounts.requireAdmin(actor);
        state.getClock().initialize(day, month, year, hour, minute);
    }

    public Job submit(User actor, String title, String application, String description, int duration) throws ProjectException {
        Student student = accounts.requireStudent(actor);
        state.getClock().requireSet();
        title = AccountService.required(title, "Job title");
        application = AccountService.required(application, "Application");
        description = AccountService.required(description, "Description");
        if (!Job.validDuration(duration)) { throw new ProjectException("Select a valid task size."); }
        if (state.getNextJobNumber() == Integer.MAX_VALUE) { throw new ProjectException("Job number limit reached."); }
        
        long[] freeAt = copiedAvailability();
        for (Job waiting : getQueue()) {
            int gpu = earliest(freeAt);
            freeAt[gpu] += waiting.getDuration();
        }
        if (freeAt[earliest(freeAt)] + duration > SimulationClock.maximum()) {
            throw new ProjectException("This job would finish beyond year 9999.");
        }
        student.getAllowance().reserve(duration);
        Job job = new Job(state.getNextJobNumber(), student.getUsername(), title, application, description, duration, state.getClock().getNow());
        state.addJob(job);
        dispatch();
        return job;
    }

    public ArrayList<Job> getQueue() {
        ArrayList<Job> queue = new ArrayList<Job>();
        for (Job job : state.getJobs()) {
            if (job.getStatus().equals(Job.QUEUED)) { queue.add(job); }
        }

        ArrayList<Job> ordered = new ArrayList<Job>();
        while (queue.size() > 0) {
            int first = 0;
            for (int i = 1; i < queue.size(); i++) {
                Job a = queue.get(i);
                Job b = queue.get(first);
                if (a.getSubmissionTime() < b.getSubmissionTime() || (a.getSubmissionTime() == b.getSubmissionTime() && a.getNumber() < b.getNumber())) {
                    first = i;
                }
            }
            ordered.add(queue.remove(first));
        }
        return ordered;
    }

    private void dispatch() throws ProjectException {
        ArrayList<Job> queue = getQueue();
        for (int id = 1; id <= 4; id++) {
            GPU gpu = state.getGpu(id);
            if (gpu.isFree() && queue.size() > 0) {
                Job job = queue.remove(0);
                job.start(id, state.getClock().getNow());
                gpu.assign(job);
            }
        }
    }

    public void advanceMinute(User actor) throws ProjectException {
        accounts.requireAdmin(actor);
        state.getClock().advance();
        for (int id = 1; id <= 4; id++) {
            GPU gpu = state.getGpu(id);
            if (!gpu.isFree()) {
                Job job = gpu.getCurrentJob();
                if (job.getExpectedFinish() == state.getClock().getNow()) {
                    job.complete(state.getClock().getNow());
                    accounts.student(job.getStudentId()).getAllowance().settle(job.getDuration(), job.getCharge());
                    gpu.release();
                }
            }
        }
        dispatch();
    }

    public void cancel(User actor, String id, String reason) throws ProjectException {
        Job job = accessibleJob(actor, id);
        reason = AccountService.required(reason, "Cancellation reason");
        job.cancel(state.getClock().getNow(), actor.getUsername(), reason);
        accounts.student(job.getStudentId()).getAllowance().settle(job.getDuration(), job.getCharge());
        if (job.getGpuId() > 0) { state.getGpu(job.getGpuId()).release(); }
        dispatch();
    }

    public ArrayList<Job> visibleJobs(User actor) throws ProjectException {
        accounts.requireUser(actor);
        ArrayList<Job> result = new ArrayList<Job>();
        for (Job job : state.getJobs()) {
            if (actor == state.getAdministrator() || job.getStudentId().equals(actor.getUsername())) {
                result.add(job);
            }
        }
        return result;
    }

    public Job accessibleJob(User actor, String id) throws ProjectException {
        for (Job job : visibleJobs(actor)) {
            if (job.getId().equalsIgnoreCase(id)) { return job; }
        }
        throw new ProjectException("Job not found or access not permitted.");
    }

    private long[] copiedAvailability() {
        long[] freeAt = new long[4];
        for (int i = 0; i < 4; i++) {
            GPU gpu = state.getGpu(i + 1);
            freeAt[i] = state.getClock().getNow();
            if (!gpu.isFree()) { freeAt[i] = gpu.getCurrentJob().getExpectedFinish(); }
        }
        return freeAt;
    }

    private int earliest(long[] freeAt) {
        int first = 0;
        for (int i = 1; i < 4; i++) {
            if (freeAt[i] < freeAt[first]) { first = i; }
        }
        return first;
    }

    public ArrayList<Prediction> predictions() {
        ArrayList<Prediction> result = new ArrayList<Prediction>();
        if (!state.getClock().isSet()) { return result; }
        long[] freeAt = copiedAvailability();
        int position = 1;
        for (Job job : getQueue()) {
            int first = earliest(freeAt);
            long start = freeAt[first];
            long finish = start + job.getDuration();
            result.add(new Prediction(job.getId(), position, first + 1, start, finish, start - state.getClock().getNow()));
            freeAt[first] = finish;
            position++;
        }
        return result;
    }

    public Prediction prediction(String jobId) {
        for (Prediction prediction : predictions()) {
            if (prediction.getJobId().equals(jobId)) { return prediction; }
        }
        return null;
    }

    public double utilization() {
        if (state.getClock().getElapsed() == 0) { return -1; }
        long busy = 0;
        for (Job job : state.getJobs()) { busy += job.getRuntime(state.getClock().getNow()); }
        return 100.0 * busy / (4.0 * state.getClock().getElapsed());
    }
}
