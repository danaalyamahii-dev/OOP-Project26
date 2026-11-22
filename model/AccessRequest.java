package gpu.model;

public class AccessRequest {
    public static final String PENDING = "Pending";
    public static final String APPROVED = "Approved";
    public static final String REJECTED = "Rejected";
    private String id;
    private String name;
    private String email;
    private String phone;
    private String password;
    private String status;

    public AccessRequest(String id, String name, String email, String phone, String password) {
        this(id, name, email, phone, password, PENDING);
    }

    public AccessRequest(String id, String name, String email, String phone,
                         String password, String status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.status = status;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPassword() { return password; }
    public String getStatus() { return status; }

    public Student approve(int minutes) throws ProjectException {
        requirePending();
        TimeAllowance allowance = new TimeAllowance(minutes);
        Student student = new Student(id, name, email, phone, password, allowance);
        status = APPROVED;
        return student;
    }

    public void reject() throws ProjectException {
        requirePending();
        status = REJECTED;
    }

    private void requirePending() throws ProjectException {
        if (!status.equals(PENDING)) {
            throw new ProjectException("This request has already been processed.");
        }
    }
}
