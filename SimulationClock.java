package logic;

import gpu.model.ProjectException;


public class SimulationClock {
    private long initial = -1;
    private long now = -1;

    public SimulationClock() { }

    public SimulationClock(long initial, long now) {
        this.initial = initial;
        this.now = now;
    }

    public long getInitial() { return initial; }
    public long getNow() { return now; }
    public boolean isSet() { return initial >= 0; }
    public long getElapsed() {
        if (!isSet()) { return 0; }
        return now - initial;
    }

    public static boolean isLeapYear(int year) {
        return year % 400 == 0 || (year % 4 == 0 && year % 100 != 0);
    }

    public static int daysInMonth(int month, int year) {
        int[] days = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        if (month == 2 && isLeapYear(year)) { return 29; }
        return days[month - 1];
    }

    public static long toMinutes(int day, int month, int year, int hour, int minute)
            throws ProjectException {
        if (year < 1 || year > 9999 || month < 1 || month > 12 || day < 1 || day > daysInMonth(month, year) || hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            throw new ProjectException("Choose a valid date and time.");
        }
        long days = 0;
        for (int y = 1; y < year; y++) {
            if (isLeapYear(y)) { days += 366; }
            else { days += 365; }
        }
        for (int m = 1; m < month; m++) { days += daysInMonth(m, year); }
        return (days + day - 1) * 1440 + hour * 60 + minute;
    }

    public static long maximum() {
        return 5258964959L;
    }

    public void initialize(int day, int month, int year, int hour, int minute) throws ProjectException {
        if (isSet()) { throw new ProjectException("The clock has already been initialized."); }
        long value = toMinutes(day, month, year, hour, minute);
        initial = value;
        now = value;
    }

    public void requireSet() throws ProjectException {
        if (!isSet()) { throw new ProjectException("The administrator must initialize the clock first."); }
    }

    public void advance() throws ProjectException {
        requireSet();
        if (now >= maximum()) { throw new ProjectException("The supported calendar ends at year 9999."); }
        now++;
    }

    public static String format(long value) {
        if (value < 0) { return "-"; }
        long days = value / 1440;
        int hour = (int) (value % 1440) / 60;
        int minute = (int) (value % 60);
        int year = 1;
        int yearDays = 365;
        while (true) {
            if (isLeapYear(year)) { yearDays = 366; }
            else { yearDays = 365; }
            if (days < yearDays) { break; }
            days -= yearDays;
            year++;
        }
        int month = 1;
        while (days >= daysInMonth(month, year)) {
            days -= daysInMonth(month, year);
            month++;
        }
        return String.format("%02d/%02d/%04d %02d:%02d", days + 1, month, year, hour, minute);
    }
}
