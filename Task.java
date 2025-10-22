import java.io.Serializable;
import java.time.LocalDate;
import java.util.Comparator;

class Task implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private String value;
    private LocalDate completionDate;
    private int priority; // 1=Low, 2=Medium, 3=High
    private LocalDate completedAt;

    public Task(long id, String value, int priority, LocalDate completionDate) {
        this.id = id;
        this.value = value;
        this.priority = priority;
        this.completionDate = completionDate;
        this.completedAt = null;
    }

    // --- Getters and Setters ---
    public long getId() { return id; }
    public String getValue() { return value; }
    public int getPriority() { return priority; }
    public LocalDate getCompletionDate() { return completionDate; }
    public LocalDate getCompletedAt() { return completedAt; }
    
    public boolean isCompleted() {
        return completedAt != null;
    }

    public void setCompleted(boolean completed) {
        this.completedAt = completed ? LocalDate.now() : null;
    }
    
    public String getPriorityString() {
        switch (priority) {
            case 3:
                return "High";
            case 2:
                return "Medium";
            default:
                return "Low";
        }
    }
    
    // Custom Comparator for the nearest completion date sorting
    public static Comparator<Task> getComparatorByDate() {
        return Comparator.comparing(Task::getCompletionDate);
    }
    
    // The text shown in the JList
    @Override
    public String toString() {
        String status = isCompleted() ? " [DONE]" : "";
        return String.format("[%s] %s (Due: %s)%s", 
            getPriorityString(), value, completionDate, status);
    }
}