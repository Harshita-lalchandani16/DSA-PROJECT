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
import java.util.Optional;
import java.util.stream.Collectors;

public class ComplexToDoListApp extends JFrame {

    private final DefaultListModel<Task> allTasksModel;
    private JList<Task> taskList;
    private JTextArea taskField;
    private JComboBox<String> prioritySet;
    private JTextField completionDateField;
    private JTextField taskKeyField;
    private JComboBox<String> statusSelect;
    private JComboBox<String> sort;
    
    // NEW FEATURE VARIABLES
    private JTextField taskIdField; // Hidden field to track task being edited (0 = new task)
    private JButton addButton; // Reference to change text/functionality
    private JButton cancelButton; // New button to cancel edit

    // --- Custom Colors and Fonts ---
    private static final Color PRIMARY_COLOR = new Color(52, 73, 94); // Dark Blue/Grey
    private static final Color ACCENT_COLOR = new Color(46, 204, 113); // Emerald Green
    private static final Color ACCENT_HOVER = new Color(39, 174, 96); // Darker Green
    private static final Color WARNING_COLOR = new Color(243, 156, 18); // Flat Orange
    private static final Color WARNING_HOVER = new Color(230, 126, 34); // Darker Orange
    private static final Color BACKGROUND_COLOR = new Color(245, 245, 245); // Light Grey
    private static final Color ERROR_COLOR = new Color(231, 76, 60); // Flat Red
    private static final Font APP_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font LIST_FONT = new Font("Monospaced", Font.PLAIN, 13);
    
    // PRIORITY CUE COLORS (Used for the text background/cue box)
    private static final Color HIGH_PRIORITY_CUE = new Color(231, 76, 60); // Red
    private static final Color MEDIUM_PRIORITY_CUE = new Color(241, 196, 15); // Yellow
    private static final Color LOW_PRIORITY_CUE = new Color(39, 174, 96); // Green

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
            "NEW TASK ENTRY / EDIT TASK",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            TITLE_FONT.deriveFont(Font.BOLD), PRIMARY_COLOR
        );
        panel.setBorder(new CompoundBorder(new EmptyBorder(15, 15, 0, 15), titledBorder));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Hidden ID Field (for tracking state: 0 = new, >0 = editing existing)
        taskIdField = new JTextField("0"); 
        taskIdField.setVisible(false); 
        
        // 1. Task Field (JTextArea)
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
        gbc.gridwidth = 4; // Span across the entire top row
        panel.add(taskLabel, gbc);
        
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        panel.add(taskScrollPane, gbc);
        gbc.weighty = 0; 
        gbc.gridwidth = 1; // Reset grid width for next row

        // 2. Priority Selector (Updated labels)
        String[] priorities = { "Low", "Medium", "High" };
        prioritySet = new JComboBox<>(priorities);
        prioritySet.setSelectedIndex(2); // Default to High
        prioritySet.setFont(APP_FONT);
        prioritySet.setBackground(Color.WHITE);
        
        JLabel priorityLabel = new JLabel("Priority:");
        priorityLabel.setFont(APP_FONT.deriveFont(Font.BOLD));
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.25;
        panel.add(priorityLabel, gbc);
        
        gbc.gridy = 3;
        panel.add(prioritySet, gbc);

        // 3. Completion Date Input with Picker Button
        JPanel datePanel = new JPanel(new BorderLayout(5, 0));
        completionDateField = new JTextField(LocalDate.now().plusDays(1).toString());
        completionDateField.setFont(APP_FONT);
        completionDateField.setBorder(BorderFactory.createCompoundBorder(
            completionDateField.getBorder(), 
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        JButton datePickerButton = createStyledButton("...", Color.LIGHT_GRAY, Color.GRAY, Color.BLACK);
        datePickerButton.setPreferredSize(new Dimension(30, completionDateField.getPreferredSize().height));
        datePickerButton.addActionListener(this::showDatePickerDialog);
        
        datePanel.add(completionDateField, BorderLayout.CENTER);
        datePanel.add(datePickerButton, BorderLayout.EAST);
        
        JLabel dateLabel = new JLabel("Due Date (YYYY-MM-DD):");
        dateLabel.setFont(APP_FONT.deriveFont(Font.BOLD));
        
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 0.45;
        panel.add(dateLabel, gbc);
        
        gbc.gridy = 3;
        panel.add(datePanel, gbc);

        // 4. Action Buttons (Add/Update and Cancel)
        
        // Add/Update Button
        addButton = createStyledButton("Add Task", ACCENT_COLOR, ACCENT_HOVER, Color.WHITE);
        addButton.addActionListener(this::handleSubmit);
        
        // Cancel Button (Only visible when editing)
        cancelButton = createStyledButton("Cancel Edit", WARNING_COLOR, WARNING_HOVER, Color.WHITE);
        cancelButton.addActionListener(this::handleCancelEdit);
        cancelButton.setVisible(false);
        
        // Panel for holding Add/Update and Cancel
        JPanel actionPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        actionPanel.setBackground(BACKGROUND_COLOR);
        actionPanel.add(addButton);
        actionPanel.add(cancelButton);

        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weightx = 0.3;
        panel.add(actionPanel, gbc);

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


        // --- Top Controls (Search, Filter, Sort, Edit Button) ---
        JPanel topControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        topControls.setBackground(BACKGROUND_COLOR);

        // Search Field
        taskKeyField = new JTextField(20);
        taskKeyField.setFont(APP_FONT);
        taskKeyField.setBorder(BorderFactory.createCompoundBorder(
            taskKeyField.getBorder(), 
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
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

        // --- Bottom Buttons (Edit, Delete, Clear All) ---
        JPanel bottomButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 10));
        bottomButtons.setBackground(BACKGROUND_COLOR);
        
        JButton editButton = createStyledButton("Edit Selected Task", WARNING_COLOR, WARNING_HOVER, Color.WHITE);
        editButton.addActionListener(this::handleEdit);
        
        JButton removeButton = createStyledButton("Remove Selected", ERROR_COLOR, ERROR_COLOR.darker(), Color.WHITE);
        removeButton.addActionListener(this::handleDelete);
        
        JButton clearAllButton = createStyledButton("Clear All Tasks", new Color(127, 140, 141), new Color(149, 165, 166), Color.WHITE); // Grey Color
        clearAllButton.addActionListener(this::handleClearAll);

        bottomButtons.add(editButton);
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

    private void showDatePickerDialog(ActionEvent evt) {
        String current = completionDateField.getText();
        String dateInput = (String) JOptionPane.showInputDialog(this, 
                                                       "Enter Due Date (YYYY-MM-DD):\nExample: " + LocalDate.now().plusDays(7).toString(), 
                                                       "Set Due Date", 
                                                       JOptionPane.PLAIN_MESSAGE, 
                                                       null, 
                                                       null, 
                                                       current);
        
        if (dateInput != null && !dateInput.trim().isEmpty()) {
             try {
                // Validate format before setting
                LocalDate.parse(dateInput.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
                completionDateField.setText(dateInput.trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Invalid Date Format. Please use YYYY-MM-DD.", "Date Error", JOptionPane.ERROR_MESSAGE);
            }
        }
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
            completionDateField.requestFocusInWindow();
            return;
        }

        if (taskValue.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Task description is required.", "Input Error",
                    JOptionPane.WARNING_MESSAGE);
            taskField.requestFocusInWindow();
            return;
        }

        // Get priority index (0=Low, 1=Medium, 2=High). Add 1 to get priority level (1-3)
        int priority = prioritySet.getSelectedIndex() + 1;
        long currentTaskId = Long.parseLong(taskIdField.getText());

        List<Task> tasks = getTasksFromLocalStorage();
        
        if (currentTaskId == 0) {
            // --- ADD NEW TASK ---
            Task taskObj = new Task(getUniqueId(), taskValue, priority, dueDate);
            tasks.add(taskObj);
            JOptionPane.showMessageDialog(this, "Task added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            // --- UPDATE EXISTING TASK ---
            Optional<Task> existingTask = tasks.stream()
                .filter(t -> t.getId() == currentTaskId)
                .findFirst();

            if (existingTask.isPresent()) {
                Task taskToUpdate = existingTask.get();
                // Replace the old task with a new, updated instance (as Task is immutable except for completion status)
                tasks.remove(taskToUpdate);
                Task updatedTask = new Task(currentTaskId, taskValue, priority, dueDate);
                updatedTask.setCompleted(taskToUpdate.isCompleted()); // Maintain current completion status
                tasks.add(updatedTask);
                JOptionPane.showMessageDialog(this, "Task updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                handleCancelEdit(null); // Clear the edit state after successful update
            } else {
                 JOptionPane.showMessageDialog(this, "Error: Task ID not found for update.", "Update Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // Update persistence (file)
        setTasksToLocalStorage(tasks);

        // Update UI
        taskField.setText("");
        completionDateField.setText(LocalDate.now().plusDays(1).toString()); // Reset due date
        prioritySet.setSelectedIndex(2); // Reset to High Priority
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
            handleCancelEdit(null); // Clear editing state if the task being edited was deleted
        }
    }
    
    // NEW FEATURE HANDLERS
    private void handleEdit(ActionEvent evt) {
        Task selectedTask = taskList.getSelectedValue();
        if (selectedTask == null) {
            JOptionPane.showMessageDialog(this, "Please select a task to edit.", "No Selection",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // 1. Populate Input Fields
        taskIdField.setText(String.valueOf(selectedTask.getId()));
        taskField.setText(selectedTask.getValue());
        // Priority is 1-based, ComboBox index is 0-based
        prioritySet.setSelectedIndex(selectedTask.getPriority() - 1); 
        completionDateField.setText(selectedTask.getCompletionDate().toString());

        // 2. Change Button/UI State
        addButton.setText("Update Task");
        addButton.setBackground(WARNING_COLOR);
        addButton.setToolTipText("Click to save changes to the selected task.");
        cancelButton.setVisible(true);
        
        // Scroll back to the input panel
        taskField.requestFocusInWindow();
    }

    private void handleCancelEdit(ActionEvent evt) {
        // 1. Reset Internal State
        taskIdField.setText("0");

        // 2. Reset Input Fields
        taskField.setText("");
        completionDateField.setText(LocalDate.now().plusDays(1).toString());
        prioritySet.setSelectedIndex(2);
        
        // 3. Reset Button/UI State
        addButton.setText("Add Task");
        addButton.setBackground(ACCENT_COLOR);
        addButton.setToolTipText("Click to add a new task.");
        cancelButton.setVisible(false);
    }

    private void handleMarkAsCompleted() {
        Task selectedTask = taskList.getSelectedValue();
        if (selectedTask == null)
            return;

        // Toggle completion status
        boolean newStatus = !selectedTask.isCompleted();
        selectedTask.setCompleted(newStatus);

        // Update persistence
        List<Task> tasks = getTasksFromLocalStorage();
        tasks.stream()
                .filter(t -> t.getId() == selectedTask.getId())
                .findFirst()
                .ifPresent(t -> t.setCompleted(newStatus));
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
            handleCancelEdit(null); // Reset edit state
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
        // Ensure ID is greater than any existing ID for uniqueness upon creation
        long maxId = getTasksFromLocalStorage().stream()
                .mapToLong(Task::getId)
                .max()
                .orElse(0L);
        return Math.max(maxId + 1, System.currentTimeMillis() + Math.round(Math.random() * 1000));
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

    private class TaskCellRenderer extends DefaultListCellRenderer {
        // Reduced padding for a tighter, cleaner look
        private static final Border PADDING_BORDER = BorderFactory.createEmptyBorder(6, 10, 6, 10);
        
        /**
         * Returns a JPanel containing two JLabels: one for priority cue and one for task details.
         * This uses a JPanel to handle complex layout/coloring better than a single JLabel with HTML.
         */
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            
            if (!(value instanceof Task)) {
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
            
            Task task = (Task) value;
            
            // --- 1. Determine Colors ---
            
            // Background color based on urgency/completion
            Color bgColor = getColorForTask(task);
            
            // Main text color (usually black, but white if selected)
            Color mainFgColor = isSelected ? Color.WHITE : Color.BLACK;

            // --- 2. Create Panel Container (to hold priority and main text) ---
            
            // Use a simple FlowLayout or BorderLayout for structure
            JPanel panel = new JPanel(new BorderLayout(15, 0)); // 15px gap between elements
            panel.setBackground(bgColor);
            panel.setBorder(PADDING_BORDER);

            // --- 3. Create Priority Cue Label ---
            
            String priorityLabelText;
            Color priorityCueColor;
            Color priorityFgColor;

            switch (task.getPriority()) {
                case 3: 
                    priorityCueColor = HIGH_PRIORITY_CUE; 
                    priorityFgColor = Color.WHITE; // White text on dark red cue
                    priorityLabelText = "HIGH";
                    break;
                case 2:
                    priorityCueColor = MEDIUM_PRIORITY_CUE; 
                    priorityFgColor = Color.BLACK; // Black text on bright yellow cue
                    priorityLabelText = "MEDIUM";
                    break;
                case 1:
                default: 
                    priorityCueColor = LOW_PRIORITY_CUE; 
                    priorityFgColor = Color.WHITE; // White text on dark green cue
                    priorityLabelText = "LOW";
                    break;
            }
            
            JLabel priorityLabel = new JLabel(priorityLabelText);
            priorityLabel.setFont(LIST_FONT.deriveFont(Font.BOLD));
            priorityLabel.setOpaque(true);
            priorityLabel.setBackground(priorityCueColor);
            priorityLabel.setForeground(priorityFgColor);
            
            // Add padding/border to the priority label for the "box" effect
            priorityLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(priorityCueColor.darker(), 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
            ));

            // --- 4. Create Main Task Details Label ---
            
            String dateString = task.getCompletionDate().toString();
            String taskDescription = task.getValue();
            
            String taskText = String.format("%s | %s", dateString, taskDescription);

            JLabel taskDetailsLabel = new JLabel(taskText);
            taskDetailsLabel.setFont(LIST_FONT);
            taskDetailsLabel.setForeground(mainFgColor);
            taskDetailsLabel.setBackground(bgColor);
            taskDetailsLabel.setOpaque(true); 

            // Handle completion (Strikethrough and Gray text)
            if (task.isCompleted()) {
                // Use HTML for strikethrough effect
                taskDetailsLabel.setText(String.format("<html><strike>%s</strike></html>", taskText));
                taskDetailsLabel.setForeground(isSelected ? Color.LIGHT_GRAY : new Color(150, 150, 150));
                priorityLabel.setForeground(isSelected ? Color.LIGHT_GRAY : new Color(150, 150, 150));
            } else {
                taskDetailsLabel.setForeground(mainFgColor);
            }
            
            // --- 5. Final Assembly ---
            
            panel.add(priorityLabel, BorderLayout.WEST);
            panel.add(taskDetailsLabel, BorderLayout.CENTER);

            // --- 6. Handle Selection State ---
            
            if (isSelected) {
                // When selected, set the entire panel background to the selection color
                Color selectionColor = PRIMARY_COLOR.darker();
                panel.setBackground(selectionColor);
                taskDetailsLabel.setBackground(selectionColor);
                
                // Ensure text is white on the dark selection background
                taskDetailsLabel.setForeground(Color.WHITE);
                priorityLabel.setForeground(Color.WHITE);
            }
            
            // Reset the priority label foreground for non-selected completed tasks
            if (task.isCompleted() && !isSelected) {
                 priorityLabel.setForeground(new Color(150, 150, 150));
            }

            return panel;
        }

        private Color getColorForTask(Task task) {
            LocalDate dueDate = task.getCompletionDate();
            long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);

            if (task.isCompleted()) {
                // Completed: Muted success color 
                return new Color(220, 240, 220); // Lighter green
            } else if (daysUntilDue < 0) {
                // Overdue: Clear light red/pink background (high contrast with black text)
                return new Color(255, 180, 180); 
            } else if (daysUntilDue <= 3) {
                // Nearest Due (0 to 3 days away): Warning Orange/Yellow Background (high contrast with black text)
                return new Color(255, 245, 200); 
            } else if (daysUntilDue <= 10) { 
                // Medium Urgency (4 to 10 days away): Soft Light Grey
                return new Color(230, 230, 230);
            } else {
                // Far Away Due (11+ days away): Standard White/Safe
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
