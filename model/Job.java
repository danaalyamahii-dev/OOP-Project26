package gpu.model;

public class Job {
    public static final String QUEUED = "Queued";
    public static final String RUNNING = "Running";
    public static final String COMPLETED = "Completed";
    public static final String CANCELLED = "Cancelled";
    private final int number;
    private final String studentId;
    private final String title;
    private final String application;
    private final String description;
    private final int duration;
    private final long submissionTime;
    private String status;
    private int gpuId;
    private long startTime;
    private long finishTime;
    private long cancellationTime;
    private String cancelledBy;
    private String cancellationReason;

    public Job(int number, String studentId, String title, String application, String description, int duration, long submissionTime) {
        this(number, studentId, title, application, description, duration, submissionTime, QUEUED, 0, -1, -1, -1, "", "");
    }

    public Job(int number, String studentId, String title, String application, String description, int duration, long submissionTime, String status, int gpuId, long startTime, long finishTime, long cancellationTime, String cancelledBy, String cancellationReason) {
        this.number = number;
        this.studentId = studentId;
        this.title = title;
        this.application = application;
        this.description = description;
        this.duration = duration;
        this.submissionTime = submissionTime;
        this.status = status;
        this.gpuId = gpuId;
        this.startTime = startTime;
        this.finishTime = finishTime;
        this.cancellationTime = cancellationTime;
        this.cancelledBy = cancelledBy;
        this.cancellationReason = cancellationReason;
    }

    public int getNumber() { return number; }
    public String getId() { return "J" + number; }
    public String getStudentId() { return studentId; }
    public String getTitle() { return title; }
    public String getApplication() { return application; }
    public String getDescription() { return description; }
    public int getDuration() { return duration; }
    public long getSubmissionTime() { return submissionTime; }
    public String getStatus() { return status; }
    public int getGpuId() { return gpuId; }
    public long getStartTime() { return startTime; }
    public long getFinishTime() { return finishTime; }
    public long getCancellationTime() { return cancellationTime; }
    public String getCancelledBy() { return cancelledBy; }
    public String getCancellationReason() { return cancellationReason; }

    public static boolean validDuration(int minutes) {
        return minutes == 5 || minutes == 15 || minutes == 30 || minutes == 60;
    }

    public String getTaskSize() {
        if (duration == 5) { return "Small"; }
        if (duration == 15) { return "Medium"; }
        if (duration == 30) { return "Large"; }
        return "Very large";
    }

    public boolean isActive() {
        return status.equals(QUEUED) || status.equals(RUNNING);
    }

    public long getExpectedFinish() {
        if (startTime < 0) { return -1; }
        return startTime + duration;
    }

    public int getRuntime(long now) {
        if (startTime < 0) { return 0; }
        long end = finishTime;
        if (end < 0) { end = now; }
        return (int) (end - startTime);
    }

    public int getCharge() {
        if (isActive() || startTime < 0) { return 0; }
        return getRuntime(finishTime);
    }

    public long getQueueWait() {
        if (startTime < 0) { return 0; }
        return startTime - submissionTime;
    }

    public void start(int gpuId, long now) throws ProjectException {
        if (!status.equals(QUEUED) || gpuId < 1 || gpuId > 4 || now < submissionTime) {
            throw new ProjectException("The job cannot start in this state.");
        }
        this.gpuId = gpuId;
        startTime = now;
        status = RUNNING;
    }

    public void complete(long now) throws ProjectException {
        if (!status.equals(RUNNING) || now != getExpectedFinish()) {
            throw new ProjectException("The job is not ready to complete.");
        }
        finishTime = now;
        status = COMPLETED;
    }

    public void cancel(long now, String actor, String reason) throws ProjectException {
        if (!isActive() || now < submissionTime || actor == null || actor.trim().isEmpty()
                || reason == null || reason.trim().isEmpty()) {
            throw new ProjectException("A queued/running job, user and reason are required.");
        }
        if (status.equals(RUNNING)) {
            if (now < startTime || now >= getExpectedFinish()) {
                throw new ProjectException("Invalid cancellation time.");
            }
            finishTime = now;
        }
        status = CANCELLED;
        cancellationTime = now;
        cancelledBy = actor;
        cancellationReason = reason;
    }
}
