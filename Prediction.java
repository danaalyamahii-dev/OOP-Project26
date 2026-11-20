package gpu.logic;

public class Prediction {
    private String jobId;
    private int position;
    private int gpuId;
    private long start;
    private long finish;
    private long waitMinutes;

    public Prediction(String jobId, int position, int gpuId, long start, long finish, long waitMinutes) {
        this.jobId = jobId;
        this.position = position;
        this.gpuId = gpuId;
        this.start = start;
        this.finish = finish;
        this.waitMinutes = waitMinutes;
    }

    public String getJobId() { return jobId; }
    public int getPosition() { return position; }
    public int getGpuId() { return gpuId; }
    public long getStart() { return start; }
    public long getFinish() { return finish; }
    public long getWaitMinutes() { return waitMinutes; }
}
