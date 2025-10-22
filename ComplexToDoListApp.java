import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ComplexToDoListApp extends JFrame {

    private final DefaultListModel<Task> allTasksModel;
    private JList<Task> taskList;
    private JTextField taskField;
    private JComboBox<String> prioritySet;
    private JTextField completionDateField;
    private JTextField taskKeyField;
    private JComboBox<String> statusSelect;
    private JComboBox<String> sort;

    private static final String FILE_NAME = "tasks_complex.ser";

    public ComplexToDoListApp() {
        super("Feature-Rich To-Do List");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        allTasksModel = new DefaultListModel<>();
        // Load tasks first before setting up the UI display
        List<Task> loadedTasks = loadTasksFromFile();
        loadedTasks.forEach(allTasksModel::addElement);

        // --- UI Setup ---
        JPanel inputPanel = createInputPanel();
        add(inputPanel, BorderLayout.NORTH);

        JPanel mainContentPanel = createMainContentPanel();
        add(mainContentPanel, BorderLayout.CENTER);

        // --- Event Registration ---
        registerEvents();

        setSize(850, 600);
        setLocationRelativeTo(null);
        setVisible(true);

        // Apply initial sorting/filtering after loading and setup
        applyFilterAndSort();
    }

    // =================================================================
    // UI CREATION
    // =================================================================

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 1. Task Field
        taskField = new JTextField(30);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        panel.add(new JLabel("Task Description:"), gbc);
        gbc.gridy = 1;
        panel.add(taskField, gbc);

        // 2. Priority Selector
        String[] priorities = { "Low", "Medium", "High" };
        prioritySet = new JComboBox<>(priorities);
        prioritySet.setSelectedIndex(0);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Priority:"), gbc);
        gbc.gridy = 3;
        panel.add(prioritySet, gbc);

        // 3. Completion Date Input
        completionDateField = new JTextField(LocalDate.now().plusDays(1).toString());
        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(new JLabel("Due Date (YYYY-MM-DD):"), gbc);
        gbc.gridy = 3;
        panel.add(completionDateField, gbc);

        // 4. Add Button
        JButton addButton = new JButton("Add Task");
        addButton.addActionListener(this::handleSubmit);
        gbc.gridx = 2;
        gbc.gridy = 3;
        panel.add(addButton, gbc);

        return panel;
    }

    private JPanel createMainContentPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // --- Top Controls (Search, Filter, Sort) ---
        JPanel topControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        taskKeyField = new JTextField(15);
        // ComplexToDoListApp.java:109 - Replacement
        taskKeyField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update(DocumentEvent e) {
                handleSearch();
            }
        });
        topControls.add(new JLabel("Search:"));
        topControls.add(taskKeyField);

        statusSelect = new JComboBox<>(new String[] { "all", "completed", "incomplete" });
        statusSelect.setSelectedItem("all");
        statusSelect.addActionListener(e -> applyFilterAndSort());
        topControls.add(new JLabel("Status:"));
        topControls.add(statusSelect);

        sort = new JComboBox<>(new String[] { "None", "High to Low (P)", "Low to High (P)", "Nearest Date" });
        sort.setSelectedItem("High to Low (P)");
        sort.addActionListener(e -> applyFilterAndSort());
        topControls.add(new JLabel("Sort By:"));
        topControls.add(sort);

        panel.add(topControls, BorderLayout.NORTH);

        // --- Task List ---
        taskList = new JList<>(allTasksModel);
        taskList.setCellRenderer(new TaskCellRenderer());
        taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(taskList);
        panel.add(scrollPane, BorderLayout.CENTER);

        // --- Bottom Buttons (Delete, Clear All) ---
        JPanel bottomButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton removeButton = new JButton("Remove Selected");
        removeButton.addActionListener(this::handleDelete);
        JButton clearAllButton = new JButton("Clear All Tasks");
        clearAllButton.addActionListener(this::handleClearAll);

        bottomButtons.add(removeButton);
        bottomButtons.add(clearAllButton);
        panel.add(bottomButtons, BorderLayout.SOUTH);

        return panel;
    }

    // =================================================================
    // EVENT HANDLERS
    // =================================================================

    private void registerEvents() {
        // Double-click to mark as complete/incomplete
        taskList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    handleMarkAsCompleted();
                }
            }
        });
    }

    private void handleSubmit(ActionEvent evt) {
        String taskValue = taskField.getText().trim();
        String dateString = completionDateField.getText().trim();
        LocalDate dueDate;

        try {
            dueDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid Date Format. Use YYYY-MM-DD.", "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (taskValue.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Task description is required.", "Input Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Create Task Object
        int priority = prioritySet.getSelectedIndex() + 1;
        Task taskObj = new Task(getUniqueId(), taskValue, priority, dueDate);

        // Add to persistence (file)
        List<Task> tasks = getTasksFromLocalStorage();
        tasks.add(taskObj);
        setTasksToLocalStorage(tasks);

        // Update UI
        taskField.setText("");
        applyFilterAndSort();
    }

    private void handleDelete(ActionEvent evt) {
        Task selectedTask = taskList.getSelectedValue();
        if (selectedTask == null) {
            JOptionPane.showMessageDialog(this, "Please select a task to delete.", "No Selection",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Modal/Confirmation Equivalent
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete: \"" + selectedTask.getValue() + "\"?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            removeTaskFromLocalStorage(selectedTask.getId());
            applyFilterAndSort();
        }
    }

    private void handleMarkAsCompleted() {
        Task selectedTask = taskList.getSelectedValue();
        if (selectedTask == null)
            return;

        selectedTask.setCompleted(!selectedTask.isCompleted());

        // Update persistence
        List<Task> tasks = getTasksFromLocalStorage();
        tasks.stream()
                .filter(t -> t.getId() == selectedTask.getId())
                .findFirst()
                .ifPresent(t -> t.setCompleted(selectedTask.isCompleted()));
        setTasksToLocalStorage(tasks);

        // Update UI
        applyFilterAndSort();
    }

    private void handleSearch() {
        applyFilterAndSort();
    }

    private void handleClearAll(ActionEvent evt) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to clear ALL tasks?",
                "Confirm Clear All", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            setTasksToLocalStorage(new ArrayList<>());
            allTasksModel.clear();
        }
    }

    // =================================================================
    // SORTING & FILTERING
    // =================================================================

    private void applyFilterAndSort() {
        List<Task> tasks = getTasksFromLocalStorage();
        String currentStatus = (String) statusSelect.getSelectedItem();
        String currentSort = (String) sort.getSelectedItem();
        String searchKey = taskKeyField.getText().trim().toLowerCase();

        // 1. Filter by Status and Search Key
        List<Task> filteredTasks = tasks.stream()
                .filter(task -> {
                    boolean statusMatch;

                    // --- CORRECTED: Using traditional switch statement ---
                    switch (currentStatus) {
                        case "completed":
                            statusMatch = task.isCompleted();
                            break;
                        case "incomplete":
                            statusMatch = !task.isCompleted();
                            break;
                        default:
                            statusMatch = true; // "all"
                            break;
                    }

                    boolean searchMatch = task.getValue().toLowerCase().contains(searchKey);
                    return statusMatch && searchMatch;
                })
                .collect(Collectors.toList());

        // 2. Sort
        Comparator<Task> comparator;

        // --- CORRECTED: Using traditional switch statement ---
        switch (currentSort) {
            case "High to Low (P)":
                comparator = Comparator.comparing(Task::getPriority).reversed();
                break;
            case "Low to High (P)":
                comparator = Comparator.comparing(Task::getPriority);
                break;
            case "Nearest Date":
                comparator = Task.getComparatorByDate();
                break;
            default:
                comparator = (t1, t2) -> 0; // "None"
                break;
        }

        filteredTasks.sort(comparator);

        // 3. Update List Model
        allTasksModel.clear();
        filteredTasks.forEach(allTasksModel::addElement);
    }

    // =================================================================
    // UTILITIES AND PERSISTENCE
    // =================================================================

    private long getUniqueId() {
        return System.currentTimeMillis() + Math.round(Math.random() * 1000);
    }
    @SuppressWarnings("unchecked")
    private List<Task> loadTasksFromFile() {
        File file = new File(FILE_NAME);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                Object obj = ois.readObject();
                if (obj instanceof List) {
                    // Safe cast using traditional instanceof
                    return (List<Task>) obj;
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error loading tasks: " + e.getMessage());
            }
        }
        return new ArrayList<>();
    }

    private List<Task> getTasksFromLocalStorage() {
        return loadTasksFromFile();
    }

    private void setTasksToLocalStorage(List<Task> tasks) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(tasks);
        } catch (IOException e) {
            System.err.println("Error saving tasks: " + e.getMessage());
        }
    }

    private void removeTaskFromLocalStorage(long taskId) {
        List<Task> tasks = getTasksFromLocalStorage();
        List<Task> filteredTasks = tasks.stream()
                .filter(task -> taskId != task.getId())
                .collect(Collectors.toList());
        setTasksToLocalStorage(filteredTasks);
    }

    // =================================================================
    // CUSTOM RENDERER
    // =================================================================

    private static class TaskCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            // --- CORRECTED: Using traditional instanceof check and cast ---
            if (value instanceof Task) {
                Task task = (Task) value; // Explicit cast

                if (!isSelected) {
                    Color bgColor = getColorForTask(task);
                    label.setBackground(bgColor);
                }

                label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

                if (task.isCompleted()) {
                    label.setText("<html><strike>" + task.toString() + "</strike></html>");
                } else {
                    label.setText(task.toString());
                }
            }
            return label;
        }

        // Mirrors the JS getBgColor logic
        private Color getColorForTask(Task task) {
            LocalDate dueDate = task.getCompletionDate();
            long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);

            if (task.isCompleted()) {
                return new Color(220, 255, 220); // Light green for completed
            } else if (daysUntilDue < 0) {
                return Color.RED.brighter(); // Past Due (Red)
            } else if (daysUntilDue <= 2) {
                return Color.YELLOW.brighter(); // Due in 0-2 days (Yellow)
            } else {
                return Color.GREEN.brighter(); // Safe (Green)
            }
        }
    }

    // Utility to simplify DocumentListener implementation
    @FunctionalInterface
    public interface SimpleDocumentListener extends DocumentListener {
        void update(DocumentEvent e);

        @Override
        default void insertUpdate(DocumentEvent e) {
            update(e);
        }

        @Override
        default void removeUpdate(DocumentEvent e) {
            update(e);
        }

        @Override
        default void changedUpdate(DocumentEvent e) {
            update(e);
        }
    }

    // =================================================================
    // MAIN
    // =================================================================

    public static void main(String[] args) {
        // Set system look and feel for native app appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(ComplexToDoListApp::new);
    }
}