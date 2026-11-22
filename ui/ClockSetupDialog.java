package ui;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.swing.*;
import logic.Scheduler;
import model.AppState;
import model.ProjectException;
import storage.DataStore;

public class ClockSetupDialog extends JDialog {
    private boolean confirmed = false;

    public ClockSetupDialog(Frame owner, AppState state, Scheduler scheduler, DataStore dataStore) {
        super(owner, "Start the simulation", true);
        setSize(440, 230);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JPanel header = new JPanel(new GridLayout(2, 1, 4, 4));
        JLabel title = new JLabel("Set the starting date and time");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        JLabel subtitle = new JLabel("This is done once. Afterwards, time moves only with +1 minute.");
        subtitle.setForeground(Color.DARK_GRAY);
        header.add(title);
        header.add(subtitle);
        root.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(2, 2, 12, 6));
        center.add(new JLabel("Date (dd/MM/yyyy)"));
        center.add(new JLabel("Time (24-hour HH:mm)"));

        Calendar defaultCal = Calendar.getInstance();
        defaultCal.set(2026, Calendar.NOVEMBER, 16, 10, 0, 0);
        Date initialDate = defaultCal.getTime();

        JSpinner dateSpinner = new JSpinner(new SpinnerDateModel(initialDate, null, null, Calendar.DAY_OF_MONTH));
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy"));

        JSpinner timeSpinner = new JSpinner(new SpinnerDateModel(initialDate, null, null, Calendar.MINUTE));
        timeSpinner.setEditor(new JSpinner.DateEditor(timeSpinner, "HH:mm"));

        center.add(dateSpinner);
        center.add(timeSpinner);
        root.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton startBtn = new JButton("Start clock");
        startBtn.addActionListener(e -> {
            try {
                Date d = (Date) dateSpinner.getValue();
                Date t = (Date) timeSpinner.getValue();
                String[] dParts = new SimpleDateFormat("dd/MM/yyyy").format(d).split("/");
                String[] tParts = new SimpleDateFormat("HH:mm").format(t).split(":");

                int day = Integer.parseInt(dParts[0]);
                int month = Integer.parseInt(dParts[1]);
                int year = Integer.parseInt(dParts[2]);
                int hour = Integer.parseInt(tParts[0]);
                int minute = Integer.parseInt(tParts[1]);

                scheduler.initializeClock(state.getAdministrator(), day, month, year, hour, minute);
                dataStore.saveAll(state);
                confirmed = true;
                dispose();
            } catch (ProjectException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Clock Setup Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid date or time selected.", "Clock Setup Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        bottom.add(startBtn);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}