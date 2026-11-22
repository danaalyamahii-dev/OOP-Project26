package gpu;

import java.awt.GridLayout;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import gpu.logic.DemoData;
import gpu.model.AppState;
import gpu.storage.FileStateStore;
import gpu.ui.AppController;
import gpu.ui.ScreenView;

public class Main extends JFrame implements ScreenView {
    private AppController controller;

    public Main(AppState state, FileStateStore store) {
        setTitle("GPU Utilization Management");
        setLayout(new GridLayout(0, 1));
        setSize(1320, 1000);
        // Every exit goes through the Save and exit button.
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        controller = new AppController(state, store, this);
    }

    public void clearScreen() { getContentPane().removeAll(); }
    public void addLabel(JLabel label) { add(label); }
    public void addButton(JButton button) { add(button); }
    public void addField(JTextField field) { add(field); }
    public void showScreen() { setVisible(true); validate(); repaint(); }
    public void message(String text) { JOptionPane.showMessageDialog(this, text); }
    public String choose(String title, String[] options, String selected) {
        return (String) JOptionPane.showInputDialog(this, title, title,
            JOptionPane.QUESTION_MESSAGE, null, options, selected);
    }
    public String ask(String prompt) { return JOptionPane.showInputDialog(this, prompt); }
    public void closeScreen() { System.exit(0); }

    public static void main(String[] args) {
        try {
            boolean demo = args.length == 1 && args[0].equals("--demo");
            if (args.length > 1 || (args.length == 1 && !demo)) {
                throw new Exception("Use no argument for an empty system or --demo for the sample.");
            }
            String filename = "data/state.txt";
            if (demo) { filename = "data/demo-state.txt"; }
            FileStateStore store = new FileStateStore(filename);
            AppState state;
            if (demo && !store.exists()) {
                state = DemoData.demonstration();
                store.save(state);
            } else { state = store.load(); }
            new Main(state, store);
        } catch (Exception ex) { JOptionPane.showMessageDialog(null, ex.getMessage()); }
    }
}
