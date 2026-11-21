package model;

public class GPU {
    private int id;
    private Job currentJob;

    public GPU(int id) { this.id = id; }
    public int getId() { return id; }
    public Job getCurrentJob() { return currentJob; }
    public boolean isFree() { return currentJob == null; }

    public void assign(Job job) throws ProjectException {
        if (!isFree() || job == null || !job.getStatus().equals(Job.RUNNING) || job.getGpuId() != id) {
            throw new ProjectException("Invalid GPU assignment.");
        }
        currentJob = job;
    }

    public void release() { currentJob = null; }
}
