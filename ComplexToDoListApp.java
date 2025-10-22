import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
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
    private JTextArea taskField; // Changed to JTextArea for multi-line input
    private JComboBox<String> prioritySet;
    private JTextField completionDateField;
    private JTextField taskKeyField;
    private JComboBox<String> statusSelect;
    private JComboBox<String> sort;

    // --- Custom Colors and Fonts ---
    private static final Color PRIMARY_COLOR = new Color(52, 73, 94); // Dark Blue/Grey
    private static final Color ACCENT_COLOR = new Color(46, 204, 113); // Emerald Green
    private static final Color ACCENT_HOVER = new Color(39, 174, 96); // Darker Green
    private static final Color BACKGROUND_COLOR = new Color(245, 245, 245); // Light Grey
    private static final Color ERROR_COLOR = new Color(231, 76, 60); // Flat Red
    private static final Font APP_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font LIST_FONT = new Font("Monospaced", Font.PLAIN, 13);


    private static final String FILE_NAME = "tasks_complex.ser";

    public ComplexToDoListApp() {
        super("Productivity Hub - Task Manager");
        // Use a modern look and feel if available
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            // Fallback to default L&F
        }
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Set main background color
        getContentPane().setBackground(BACKGROUND_COLOR);
        setLayout(new BorderLayout(15, 15)); // Increased gaps

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

        setSize(900, 700); // Slightly larger window
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
        panel.setBackground(BACKGROUND_COLOR);
        
        // Titled Border for Input Section
        Border titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(PRIMARY_COLOR, 1, true),
            "NEW TASK ENTRY",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            TITLE_FONT.deriveFont(Font.BOLD), PRIMARY_COLOR
        );
        panel.setBorder(new CompoundBorder(new EmptyBorder(15, 15, 0, 15), titledBorder));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 1. Task Field (Now JTextArea)
        taskField = new JTextArea(3, 30);
        taskField.setFont(APP_FONT);
        taskField.setLineWrap(true);
        taskField.setWrapStyleWord(true);
        JScrollPane taskScrollPane = new JScrollPane(taskField);
        taskScrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        JLabel taskLabel = new JLabel("Task Description:");
        taskLabel.setFont(APP_FONT.deriveFont(Font.BOLD));
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        panel.add(taskLabel, gbc);
        
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        panel.add(taskScrollPane, gbc);
        gbc.weighty = 0; // Reset weight

        // 2. Priority Selector
        String[] priorities = { "Low (1)", "Medium (2)", "High (3)" };
        prioritySet = new JComboBox<>(priorities);
        prioritySet.setSelectedIndex(2);
        prioritySet.setFont(APP_FONT);
        prioritySet.setBackground(Color.WHITE);
        
        JLabel priorityLabel = new JLabel("Priority:");
        priorityLabel.setFont(APP_FONT.deriveFont(Font.BOLD));
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0.3;
        panel.add(priorityLabel, gbc);
        
        gbc.gridy = 3;
        panel.add(prioritySet, gbc);

        // 3. Completion Date Input
        completionDateField = new JTextField(LocalDate.now().plusDays(1).toString());
        completionDateField.setFont(APP_FONT);
        completionDateField.setBorder(BorderFactory.createCompoundBorder(
            completionDateField.getBorder(), 
            BorderFactory.createEmptyBorder(5, 5, 5, 5) // Internal padding
        ));
        
        JLabel dateLabel = new JLabel("Due Date (YYYY-MM-DD):");
        dateLabel.setFont(APP_FONT.deriveFont(Font.BOLD));
        
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 0.4;
        panel.add(dateLabel, gbc);
        
        gbc.gridy = 3;
        panel.add(completionDateField, gbc);

        // 4. Add Button
        JButton addButton = createStyledButton("Add Task", ACCENT_COLOR, ACCENT_HOVER, Color.WHITE);
        addButton.addActionListener(this::handleSubmit);
        
        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.weightx = 0.3;
        panel.add(addButton, gbc);

        return panel;
    }

    private JPanel createMainContentPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BACKGROUND_COLOR);
        
        // Titled Border for Controls and List Section
        Border titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(PRIMARY_COLOR, 1, true),
            "TASK MANAGEMENT & LIST",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            TITLE_FONT.deriveFont(Font.BOLD), PRIMARY_COLOR
        );
        panel.setBorder(new CompoundBorder(new EmptyBorder(0, 15, 15, 15), titledBorder));


        // --- Top Controls (Search, Filter, Sort) ---
        JPanel topControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        topControls.setBackground(BACKGROUND_COLOR);

        // Search Field
        taskKeyField = new JTextField(20);
        taskKeyField.setFont(APP_FONT);
        taskKeyField.setBorder(BorderFactory.createCompoundBorder(
            taskKeyField.getBorder(), 
            BorderFactory.createEmptyBorder(5, 5, 5, 5) // Internal padding
        ));
        taskKeyField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update(DocumentEvent e) {
                handleSearch();
            }
        });
        topControls.add(new JLabel("Search Keyword:"));
        topControls.add(taskKeyField);

        // Status Filter
        statusSelect = new JComboBox<>(new String[] { "all", "completed", "incomplete" });
        statusSelect.setFont(APP_FONT);
        statusSelect.setSelectedItem("incomplete"); // Default to incomplete
        statusSelect.addActionListener(e -> applyFilterAndSort());
        topControls.add(new JLabel("Status:"));
        topControls.add(statusSelect);

        // Sort Selector
        sort = new JComboBox<>(new String[] { "None", "High to Low (P)", "Low to High (P)", "Nearest Date" });
        sort.setFont(APP_FONT);
        sort.setSelectedItem("Nearest Date"); // Default to nearest date
        sort.addActionListener(e -> applyFilterAndSort());
        topControls.add(new JLabel("Sort By:"));
        topControls.add(sort);

        panel.add(topControls, BorderLayout.NORTH);

        // --- Task List ---
        taskList = new JList<>(allTasksModel);
        taskList.setCellRenderer(new TaskCellRenderer());
        taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskList.setFont(LIST_FONT); // Use Monospaced for cleaner list view

        JScrollPane scrollPane = new JScrollPane(taskList);
        scrollPane.setBorder(BorderFactory.createLineBorder(PRIMARY_COLOR.brighter(), 1, true));
        panel.add(scrollPane, BorderLayout.CENTER);

        // --- Bottom Buttons (Delete, Clear All) ---
        JPanel bottomButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 10));
        bottomButtons.setBackground(BACKGROUND_COLOR);
        
        JButton removeButton = createStyledButton("Remove Selected", ERROR_COLOR, ERROR_COLOR.darker(), Color.WHITE);
        removeButton.addActionListener(this::handleDelete);
        
        JButton clearAllButton = createStyledButton("Clear All Tasks", new Color(127, 140, 141), new Color(149, 165, 166), Color.WHITE); // Grey Color
        clearAllButton.addActionListener(this::handleClearAll);

        bottomButtons.add(removeButton);
        bottomButtons.add(clearAllButton);
        panel.add(bottomButtons, BorderLayout.SOUTH);

        return panel;
    }
    
    // Custom button creator utility for enhanced look
    private JButton createStyledButton(String text, Color bgColor, Color hoverColor, Color fgColor) {
        JButton button = new JButton(text);
        button.setFont(APP_FONT.deriveFont(Font.BOLD));
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bgColor.darker(), 1, true),
            BorderFactory.createEmptyBorder(8, 15, 8, 15) // Padding
        ));

        // Simple Hover effect using MouseListener (limited without custom painting)
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
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
        // Use getText() for JTextArea
        String taskValue = taskField.getText().trim(); 
        String dateString = completionDateField.getText().trim();
        LocalDate dueDate;

        try {
            dueDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid Date Format. Use YYYY-MM-DD.", "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            completionDateField.requestFocusInWindow();
            return;
        }

        if (taskValue.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Task description is required.", "Input Error",
                    JOptionPane.WARNING_MESSAGE);
            taskField.requestFocusInWindow();
            return;
        }

        // Create Task Object
        // Get priority index (0=Low, 1=Medium, 2=High). Add 1 to get priority level (1-3)
        int priority = prioritySet.getSelectedIndex() + 1; 
        Task taskObj = new Task(getUniqueId(), taskValue, priority, dueDate);

        // Update persistence (file)
        List<Task> tasks = getTasksFromLocalStorage();
        tasks.add(taskObj);
        setTasksToLocalStorage(tasks);

        // Update UI
        taskField.setText("");
        completionDateField.setText(LocalDate.now().plusDays(1).toString()); // Reset due date
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
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            removeTaskFromLocalStorage(selectedTask.getId());
            applyFilterAndSort();
        }
    }

    private void handleMarkAsCompleted() {
        Task selectedTask = taskList.getSelectedValue();
        if (selectedTask == null)
            return;

        // Toggle completion status
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
                "WARNING: This will permanently clear ALL tasks! Are you sure?",
                "Confirm Clear All", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            setTasksToLocalStorage(new ArrayList<>());
            // Update UI by clearing the model and reapplying filter/sort logic
            allTasksModel.clear(); 
            applyFilterAndSort();
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

        switch (currentSort) {
            case "High to Low (P)":
                // Highest priority (3) first
                comparator = Comparator.comparing(Task::getPriority).reversed(); 
                break;
            case "Low to High (P)":
                // Lowest priority (1) first
                comparator = Comparator.comparing(Task::getPriority);
                break;
            case "Nearest Date":
                // Use the custom comparator from Task.java
                comparator = Task.getComparatorByDate();
                break;
            default:
                comparator = (t1, t2) -> 0; // "None" (maintain insertion order)
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
    
    // Suppress the unchecked cast warning related to file deserialization
    @SuppressWarnings("unchecked") 
    private List<Task> loadTasksFromFile() {
        File file = new File(FILE_NAME);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                Object obj = ois.readObject();
                if (obj instanceof List) {
                    return (List<Task>) obj;
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error loading tasks: " + e.getMessage());
            }
        }
        return new ArrayList<>();
    }

    private List<Task> getTasksFromLocalStorage() {
        // Reads from the file every time to ensure consistency, though typically a cache is faster
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
    // CUSTOM RENDERER (Visually Enhanced)
    // =================================================================

    private static class TaskCellRenderer extends DefaultListCellRenderer {
        private static final Border PADDING_BORDER = BorderFactory.createEmptyBorder(8, 10, 8, 10);
        
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            
            // Call superclass method to handle default selection coloring
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setBorder(PADDING_BORDER);
            label.setFont(LIST_FONT);

            if (value instanceof Task) {
                Task task = (Task) value;

                // 1. Background Color based on urgency/completion (Only when not selected)
                if (!isSelected) {
                    Color bgColor = getColorForTask(task);
                    label.setBackground(bgColor);
                } else {
                    // Use a slightly darker color for selected item background
                    label.setBackground(PRIMARY_COLOR.darker());
                    label.setForeground(Color.WHITE);
                }

                // 2. Text Formatting (Strikethrough)
                if (task.isCompleted()) {
                    // Use a muted text color for completed tasks
                    label.setForeground(isSelected ? Color.LIGHT_GRAY : new Color(150, 150, 150)); 
                    label.setText("<html><strike>" + task.toString() + "</strike></html>");
                } else {
                    // Use black or white text for incomplete tasks
                    label.setForeground(isSelected ? Color.WHITE : Color.BLACK);
                    label.setText(task.toString());
                }
            }
            return label;
        }

        private Color getColorForTask(Task task) {
            LocalDate dueDate = task.getCompletionDate();
            long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);

            if (task.isCompleted()) {
                // Muted success color for completed
                return new Color(200, 230, 200); 
            } else if (daysUntilDue < 0) {
                // Overdue: Dark Red
                return new Color(255, 200, 200); 
            } else if (daysUntilDue <= 2) {
                // Critical/Soon-Due: Warning Orange/Yellow
                return new Color(255, 255, 180); 
            } else {
                // Safe: Light Blue/Grey background for standard tasks
                return Color.WHITE; 
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
        // The L&F is now set in the constructor for maximum effect
        SwingUtilities.invokeLater(ComplexToDoListApp::new);
    }
}
