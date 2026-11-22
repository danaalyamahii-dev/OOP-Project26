package gpu.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import gpu.logic.*;
import gpu.model.*;
import gpu.storage.DataStore;

public class LoginFrame extends JFrame {
    private AppState state;
    private AccountService accounts;
    private Scheduler scheduler;
    private DataStore store;
    private JTextField username = new JTextField(22);
    private JPasswordField password = new JPasswordField(22);
    private JLabel message = new JLabel(" ", SwingConstants.CENTER);

    public LoginFrame(AppState state, AccountService accounts, Scheduler scheduler, DataStore store) {
        super("GPU Utilization Management - Login");
        this.state = state;
        this.accounts = accounts;
        this.scheduler = scheduler;
        this.store = store;
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(10, 12));
        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 3));
        JLabel title = new JLabel("GPU Utilization Management", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 21));
        heading.add(title);
        heading.add(new JLabel("4 x NVIDIA H100 - simulated", SwingConstants.CENTER));
        root.add(heading, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(6, 1, 0, 6));
        form.add(new JLabel("Username (university ID)"));
        form.add(username);
        form.add(new JLabel("Password"));
        form.add(password);
        JButton sign = new JButton("Sign in");
        sign.setBackground(Color.BLUE);
        sign.setForeground(Color.WHITE);
        sign.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { signIn(); }
        });
        form.add(sign);
        JButton register = new JButton("New student? Request an account");
        register.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { registration(); }
        });
        form.add(register);

        JPanel middle = new JPanel(new BorderLayout());
        middle.add(new JPanel(), BorderLayout.NORTH);
        middle.add(form, BorderLayout.CENTER);
        root.add(middle, BorderLayout.CENTER);
        root.add(new JPanel(), BorderLayout.WEST);
        root.add(new JPanel(), BorderLayout.EAST);

        JPanel bottom = new JPanel(new GridLayout(2, 1, 0, 5));
        message.setForeground(Color.RED);
        bottom.add(message);
        JButton exit = new JButton("Save and exit");
        exit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { saveAndExit(); }
        });
        bottom.add(exit);
        root.add(bottom, BorderLayout.SOUTH);

        // Keep the form compact instead of stretching it across the window.
        JPanel outside = new JPanel(new BorderLayout());
        outside.add(root, BorderLayout.CENTER);
        JPanel left = new JPanel();
        left.setPreferredSize(new Dimension(105, 1));
        JPanel right = new JPanel();
        right.setPreferredSize(new Dimension(105, 1));
        outside.add(left, BorderLayout.WEST);
        outside.add(right, BorderLayout.EAST);
        JPanel top = new JPanel();
        top.setPreferredSize(new Dimension(1, 25));
        outside.add(top, BorderLayout.NORTH);
        JPanel foot = new JPanel();
        foot.setPreferredSize(new Dimension(1, 25));
        outside.add(foot, BorderLayout.SOUTH);
        setContentPane(outside);
        setSize(620, 440);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { saveAndExit(); }
        });
    }

    private void signIn() {
        try {
            User user = accounts.login(username.getText(), new String(password.getPassword()));
            dispose();
            if (user instanceof Administrator) {
                new AdminFrame(state, accounts, scheduler, store).setVisible(true);
            } else {
                new StudentFrame(state, accounts, scheduler, store, (Student) user).setVisible(true);
            }
        } catch (ProjectException e) {
            message.setText(e.getMessage());
        }
    }

    private void registration() {
        JDialog dialog = new JDialog(this, "Request a student account", true);
        JPanel root = new JPanel(new BorderLayout(10, 12));
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 3));
        JLabel title = new JLabel("Request a student account");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        header.add(title);
        header.add(new JLabel("An administrator must approve your request before you can sign in."));
        root.add(header, BorderLayout.NORTH);

        final JTextField id = new JTextField(16);
        final JTextField name = new JTextField(16);
        final JTextField email = new JTextField(16);
        final JTextField phone = new JTextField(16);
        final JPasswordField pass = new JPasswordField(16);
        JPanel fields = new JPanel(new GridLayout(3, 2, 12, 10));
        fields.add(fieldPanel("University ID", id));
        fields.add(fieldPanel("Full name", name));
        fields.add(fieldPanel("Email", email));
        fields.add(fieldPanel("Phone number", phone));
        fields.add(fieldPanel("Password", pass));
        fields.add(new JPanel());
        root.add(fields, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton submit = new JButton("Submit request");
        submit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    accounts.register(id.getText(), name.getText(), email.getText(),
                            phone.getText(), new String(pass.getPassword()));
                    store.saveAll(state);
                    JOptionPane.showMessageDialog(dialog, "Request sent. Status: Pending.");
                    dialog.dispose();
                } catch (ProjectException ex) {
                    JOptionPane.showMessageDialog(dialog, ex.getMessage(),
                            "Registration error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        JButton back = new JButton("Back to login");
        back.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { dialog.dispose(); }
        });
        buttons.add(submit);
        buttons.add(back);
        root.add(buttons, BorderLayout.SOUTH);

        JPanel outside = new JPanel(new BorderLayout());
        outside.add(root, BorderLayout.CENTER);
        JPanel left = new JPanel();
        left.setPreferredSize(new Dimension(22, 1));
        JPanel right = new JPanel();
        right.setPreferredSize(new Dimension(22, 1));
        outside.add(left, BorderLayout.WEST);
        outside.add(right, BorderLayout.EAST);
        JPanel top = new JPanel();
        top.setPreferredSize(new Dimension(1, 15));
        outside.add(top, BorderLayout.NORTH);
        JPanel foot = new JPanel();
        foot.setPreferredSize(new Dimension(1, 15));
        outside.add(foot, BorderLayout.SOUTH);
        dialog.setContentPane(outside);
        dialog.setSize(700, 360);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JPanel fieldPanel(String label, JTextField field) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private void saveAndExit() {
        try {
            store.saveAll(state);
            dispose();
            System.exit(0);
        } catch (ProjectException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Save error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
