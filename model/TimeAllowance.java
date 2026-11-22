package gpu.model;

public class TimeAllowance {
    private int allocated;
    private int used;
    private int reserved;

    public TimeAllowance(int allocated) throws ProjectException {
        this(allocated, 0, 0);
    }

    public TimeAllowance(int allocated, int used, int reserved) throws ProjectException {
        if (allocated < 0 || used < 0 || reserved < 0 || (long) used + reserved > allocated) {
            throw new ProjectException("Invalid time allowance.");
        }
        this.allocated = allocated;
        this.used = used;
        this.reserved = reserved;
    }

    public int getAllocated() { return allocated; }
    public int getUsed() { return used; }
    public int getReserved() { return reserved; }
    public int getAvailable() { return allocated - used - reserved; }

    public void reserve(int minutes) throws ProjectException {
        if (minutes <= 0 || minutes > getAvailable()) {
            throw new ProjectException("The job exceeds your available GPU minutes.");
        }
        reserved += minutes;
    }

    public void settle(int duration, int charge) throws ProjectException {
        if (duration <= 0 || duration > reserved || charge < 0 || charge > duration) {
            throw new ProjectException("Invalid job settlement.");
        }
        reserved -= duration;
        used += charge;
    }

    public void grant(int minutes) throws ProjectException {
        if (minutes <= 0 || (long) allocated + minutes > Integer.MAX_VALUE) {
            throw new ProjectException("Enter a positive whole number within the allowance limit.");
        }
        allocated += minutes;
    }
}
