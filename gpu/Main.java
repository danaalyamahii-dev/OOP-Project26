package gpu;

import javax.swing.JOptionPane;
import gpu.logic.AccountService;
import gpu.logic.Scheduler;
import gpu.model.AppState;
import gpu.model.ProjectException;
import gpu.storage.DataStore;
import gpu.ui.LoginFrame;

public class Main {
    public static void main(String[] args) {
        DataStore dataStore = new DataStore("data");
        AppState state;
        try {
            state = dataStore.loadAll();
        } catch (ProjectException ex) {
            JOptionPane.showMessageDialog(null,
                ex.getMessage() + "\nThe file was not loaded. Fix or remove it, then restart.",
                "File Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        AccountService accountService = new AccountService(state);
        Scheduler scheduler = new Scheduler(state);
        LoginFrame login = new LoginFrame(state, accountService, scheduler, dataStore);
        login.setVisible(true);
    }
}
