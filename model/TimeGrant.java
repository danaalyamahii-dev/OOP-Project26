package gpu.model;

public class TimeGrant {
    private String studentId;
    private int minutes;
    private String administrator;
    private String reason;
    private long time;

    public TimeGrant(String studentId, int minutes, String administrator, String reason, long time) {
        this.studentId = studentId;
        this.minutes = minutes;
        this.administrator = administrator;
        this.reason = reason;
        this.time = time;
    }

    public String getStudentId() { return studentId; }
    public int getMinutes() { return minutes; }
    public String getAdministrator() { return administrator; }
    public String getReason() { return reason; }
    public long getTime() { return time; }
}
