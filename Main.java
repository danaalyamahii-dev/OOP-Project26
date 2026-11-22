import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import logic.AccountService;
import logic.Scheduler;
import model.AppState;
import model.ProjectException;
import storage.DataStore;
import ui.LoginFrame;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DataStore dataStore = new DataStore("data");
            AppState state;
            try {
                state = dataStore.loadAll();
            } catch (ProjectException ex) {
                JOptionPane.showMessageDialog(
                    null,
                    ex.getMessage() + "\nThe file was not loaded. Fix or remove it, then restart.",
                    "File Error",
                    JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            AccountService accountService = new AccountService(state);
            Scheduler scheduler = new Scheduler(state);
            new LoginFrame(state, accountService, scheduler, dataStore).setVisible(true);
        });
    }
}