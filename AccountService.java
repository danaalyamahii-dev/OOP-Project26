package gpu.logic;

import gpu.model.*;

public class AccountService {
    private AppState state;

    public AccountService(AppState state) { this.state = state; }

    public static String required(String value, String field) throws ProjectException {
        if (value == null || value.trim().isEmpty()) {
            throw new ProjectException(field + " is required.");
        }
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == '\n' || value.charAt(i) == '\r') {
                throw new ProjectException(field + " must be entered on one line.");
            }
        }
        return value.trim();
    }

    public static boolean digits(String value) {
        if (value.isEmpty()) { return false; }
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) < '0' || value.charAt(i) > '9') { return false; }
        }
        return true;
    }

    public static int wholeMinutes(String text, boolean allowZero) throws ProjectException {
        text = required(text, "Minutes");
        if (!digits(text)) { throw new ProjectException("Minutes must be a whole number."); }
        int value;
        try { value = Integer.parseInt(text); }
        catch (NumberFormatException ex) { throw new ProjectException("Minutes are too large."); }
        if (!allowZero && value == 0) { throw new ProjectException("Minutes must be greater than zero."); }
        return value;
    }

    public static void validateRegistration(String id, String name, String email,
                                             String phone, String password) throws ProjectException {
        required(id, "University ID");
        required(name, "Full name");
        required(email, "Email");
        required(phone, "Phone");
        required(password, "Password");
        if (!digits(id) || id.length() > 20) { throw new ProjectException("University ID must contain 1 to 20 digits."); }
        int at = email.indexOf('@');
        int dot = email.lastIndexOf('.');
        if (at <= 0 || at != email.lastIndexOf('@') || dot <= at + 1 || dot >= email.length() - 1) {
            throw new ProjectException("Enter a valid email address.");
        }
        for (int i = 0; i < email.length(); i++) {
            if (Character.isWhitespace(email.charAt(i))) { throw new ProjectException("Email cannot contain spaces."); }
        }
        String number = phone;
        if (number.charAt(0) == '+') { number = number.substring(1); }
        if (!digits(number) || number.length() < 7 || number.length() > 15) {
            throw new ProjectException("Phone must contain 7 to 15 digits with an optional leading +.");
        }
    }

    public AccessRequest register(String id, String name, String email, String phone,
                                  String password) throws ProjectException {
        id = required(id, "University ID");
        name = required(name, "Full name");
        email = required(email, "Email");
        phone = required(phone, "Phone");
        validateRegistration(id, name, email, phone, password);
        if (state.findRequest(id) != null || state.findStudent(id) != null) {
            throw new ProjectException("This university ID already has a request or account.");
        }
        AccessRequest request = new AccessRequest(id, name, email, phone, password);
        state.addRequest(request);
        return request;
    }

    public User login(String username, String password) throws ProjectException {
        username = required(username, "Username");
        User user = state.getAdministrator();
        if (username.equals(user.getUsername()) && user.checkPassword(password)) { return user; }
        Student student = state.findStudent(username);
        if (student != null && student.checkPassword(password)) { return student; }
        AccessRequest request = state.findRequest(username);
        if (request != null && !request.getStatus().equals(AccessRequest.APPROVED)) {
            throw new ProjectException("Registration status: " + request.getStatus());
        }
        throw new ProjectException("Incorrect username or password.");
    }

    public void requireAdmin(User actor) throws ProjectException {
        if (actor != state.getAdministrator()) { throw new ProjectException("Administrator access is required."); }
    }

    public void requireUser(User actor) throws ProjectException {
        if (actor == null || (actor != state.getAdministrator()
                && state.findStudent(actor.getUsername()) != actor)) {
            throw new ProjectException("Sign in to continue.");
        }
    }

    public Student requireStudent(User actor) throws ProjectException {
        requireUser(actor);
        if (actor == state.getAdministrator()) { throw new ProjectException("A student account is required."); }
        return (Student) actor;
    }

    public Student student(String id) throws ProjectException {
        Student result = state.findStudent(id);
        if (result == null) { throw new ProjectException("Student not found."); }
        return result;
    }

    public Student approve(User actor, String id, int minutes) throws ProjectException {
        requireAdmin(actor);
        AccessRequest request = state.findRequest(id);
        if (request == null) { throw new ProjectException("Registration request not found."); }
        if (state.findStudent(id) != null) { throw new ProjectException("Account already exists."); }
        Student student = request.approve(minutes);
        state.addStudent(student);
        return student;
    }

    public void reject(User actor, String id) throws ProjectException {
        requireAdmin(actor);
        AccessRequest request = state.findRequest(id);
        if (request == null) { throw new ProjectException("Registration request not found."); }
        request.reject();
    }

    public void grant(User actor, String id, int minutes, String reason) throws ProjectException {
        requireAdmin(actor);
        state.getClock().requireSet();
        reason = required(reason, "Grant reason");
        student(id).getAllowance().grant(minutes);
        state.addGrant(new TimeGrant(id, minutes, actor.getUsername(), reason, state.getClock().getNow()));
    }
}
