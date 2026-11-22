package gpu.ui;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;

/** The interface operations needed by the JFrame application. */
public interface ScreenView {
    void clearScreen();
    void addLabel(JLabel label);
    void addButton(JButton button);
    void addField(JTextField field);
    void showScreen();
    void message(String text);
    String choose(String title, String[] options, String selected);
    String ask(String prompt);
    void closeScreen();
}
