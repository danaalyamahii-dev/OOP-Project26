package gpu.model;

public class Student extends User {
    private String email;
    private String phone;
    private TimeAllowance allowance;

    public Student(String id, String name, String email, String phone,
                   String password, TimeAllowance allowance) {
        super(id, name, password);
        this.email = email;
        this.phone = phone;
        this.allowance = allowance;
    }

    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public TimeAllowance getAllowance() { return allowance; }

    @Override
    public String getRole() { return "Student"; }
}
