// ===== Task.java =====
package com.example.taskmanager.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class Task {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String title;
    private String description;
    private long dueDate; // Store as timestamp

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public long getDueDate() {
        return dueDate;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDueDate(long dueDate) {
        this.dueDate = dueDate;
    }

}

// ===== TaskDao.java =====
package com.example.taskmanager.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TaskDao {
    @Insert
    void insert(Task task);

    @Update
    void update(Task task);

    @Delete
    void delete(Task task);

    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    LiveData<List<Task>> getAllTasks();

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    Task getTaskById(int taskId);
}

// ===== AppDatabase.java =====
package com.example.taskmanager.data;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
@Database(entities = {Task.class}, version = 1, exportSchema = false)

public abstract class AppDatabase extends RoomDatabase {
    public abstract TaskDao taskDao();

    private static AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            try {
                INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "task_database"
                ).allowMainThreadQueries().build();
            } catch (Exception e) {
                Log.e("DATABASE", "Database initialization failed: " + e.getMessage());
                throw new RuntimeException("Database initialization failed", e);
            }
        }
        return INSTANCE;
    }
}


// ===== MainActivity.java =====
package com.example.taskmanager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.taskmanager.data.AppDatabase;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {
    private AppDatabase database;
    private TaskAdapter adapter;
    private RecyclerView rvTasks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            database = AppDatabase.getInstance(this);
            setupRecyclerView();
            setupFAB();
            observeTasks();
        } catch (Exception e) {
            Log.e("MAIN_ACTIVITY", "Initialization error: " + e.getMessage());
            Toast.makeText(this, "App initialization failed", Toast.LENGTH_LONG).show();
            finish();
        }
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_add) {
                startActivity(new Intent(this, AddEditTaskActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupFAB() {
        try {
            FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
            if (fabAdd != null) {
                fabAdd.setOnClickListener(v -> {
                    try {
                        startActivity(new Intent(MainActivity.this, AddEditTaskActivity.class));
                    } catch (Exception e) {
                        Log.e("FAB_CLICK", "Error starting activity: " + e.getMessage());
                        Toast.makeText(this, "Error creating new task", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Exception e) {
            Log.e("SETUP_FAB", "FAB setup failed: " + e.getMessage());
        }
    }

    private void setupRecyclerView() {
        try {
            rvTasks = findViewById(R.id.rvTasks);
            if (rvTasks != null) {
                // Initialize adapter with empty list
                adapter = new TaskAdapter();
                rvTasks.setLayoutManager(new LinearLayoutManager(this));
                rvTasks.setAdapter(adapter);

                // Set click listener for items
                adapter.setOnItemClickListener(task -> {
                    try {
                        if(task != null && task.getId() > 0) {
                            Intent intent = new Intent(MainActivity.this, TaskDetailActivity.class);
                            intent.putExtra("TASK_ID", task.getId());
                            startActivity(intent);
                        } else {
                            Toast.makeText(MainActivity.this, "Invalid task", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("ITEM_CLICK", "Error: " + e.getMessage());
                        Toast.makeText(MainActivity.this, "Couldn't open task", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Exception e) {
            Log.e("SETUP_RECYCLER", "RecyclerView setup failed: " + e.getMessage());
            Toast.makeText(this, "Error initializing task list", Toast.LENGTH_SHORT).show();
        }
    }

    private void observeTasks() {
        try {
            if (database != null && database.taskDao() != null) {
                database.taskDao().getAllTasks().observe(this, tasks -> {
                    try {
                        if (tasks != null && adapter != null) {
                            adapter.setTasks(tasks);
                            if (tasks.isEmpty()) {
                                Toast.makeText(MainActivity.this,
                                        "No tasks found. Add your first task!",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Exception e) {
                        Log.e("TASK_UPDATE", "Error updating tasks: " + e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            Log.e("TASK_OBSERVER", "Observation error: " + e.getMessage());
            Toast.makeText(this, "Failed to load tasks", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh task list
        if (adapter != null) {
            observeTasks();
        }
    }



}

// ===== AddEditTaskActivity.java =====
package com.example.taskmanager;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.taskmanager.data.AppDatabase;
import com.example.taskmanager.data.Task;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputLayout;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddEditTaskActivity extends AppCompatActivity {
    private EditText etTitle, etDescription;
    private TextView tvSelectedDate;
    private long selectedDate = -1;
    private AppDatabase database;
    private int existingTaskId = -1;
    private TextInputLayout tilTitle, tilDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        initializeViews();
        setupInputValidation();
        checkForExistingTask();
        setupDatePicker();
        setupSaveButton();
    }

    private void initializeViews() {
        database = AppDatabase.getInstance(this);
        tilTitle = findViewById(R.id.tilTitle);
        tilDescription = findViewById(R.id.tilDescription);
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
    }

    private void setupInputValidation() {
        etTitle.addTextChangedListener(new ClearErrorTextWatcher(tilTitle));
        etDescription.addTextChangedListener(new ClearErrorTextWatcher(tilDescription));
    }

    private void checkForExistingTask() {
        existingTaskId = getIntent().getIntExtra("TASK_ID", -1);
        if (existingTaskId != -1) {
            loadExistingTaskData();
        }
    }

    private void loadExistingTaskData() {
        showLoading(true);
        new Thread(() -> {
            try {
                Task task = database.taskDao().getTaskById(existingTaskId);
                runOnUiThread(() -> {
                    showLoading(false);
                    if (task != null) {
                        populateFields(task);
                    } else {
                        showError("Task not found", null);
                        finish();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showError("Error loading task", e);
                });
            }
        }).start();
    }

    private void populateFields(Task task) {
        etTitle.setText(task.getTitle());
        etDescription.setText(task.getDescription());
        if (task.getDueDate() > 0) {
            selectedDate = task.getDueDate();
            updateDateDisplay(selectedDate);
        }
    }

    private void setupDatePicker() {
        findViewById(R.id.btnPickDate).setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        try {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Due Date")
                    .setSelection(selectedDate != -1 ? selectedDate : MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                selectedDate = selection;
                updateDateDisplay(selection);
            });

            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        } catch (Exception e) {
            showError("Failed to open date picker", e);
        }
    }

    private void updateDateDisplay(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        tvSelectedDate.setText(sdf.format(new Date(timestamp)));
    }

    private void setupSaveButton() {
        findViewById(R.id.btnSave).setOnClickListener(v -> saveTask());
    }

    private void saveTask() {
        try {
            if (!validateInputs() || !validateDate()) return;

            Task task = createTaskFromInput();
            showLoading(true);

            new Thread(() -> {
                try {
                    if (existingTaskId == -1) {
                        database.taskDao().insert(task);
                    } else {
                        database.taskDao().update(task);
                    }
                    runOnUiThread(() -> {
                        showLoading(false);
                        finish();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        showError("Database operation failed", e);
                    });
                }
            }).start();

        } catch (Exception e) {
            showError("Error saving task", e);
        }
    }

    private boolean validateInputs() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        boolean isValid = true;

        // Title validation
        if (title.isEmpty()) {
            tilTitle.setError("Title is required");
            isValid = false;
        } else if (title.length() > 100) {
            tilTitle.setError("Maximum 100 characters allowed");
            isValid = false;
        }

        // Description validation
        if (description.length() > 500) {
            tilDescription.setError("Maximum 500 characters allowed");
            isValid = false;
        }

        // Date validation
        if (selectedDate == -1) {
            tvSelectedDate.setError("Please select a due date");
            isValid = false;
        }

        return isValid;
    }

    private boolean validateDate() {
        if (selectedDate < System.currentTimeMillis()) {
            tvSelectedDate.setError("Date cannot be in the past");
            return false;
        }
        return true;
    }

    private Task createTaskFromInput() {
        Task task = new Task();
        task.setTitle(etTitle.getText().toString().trim());
        task.setDescription(etDescription.getText().toString().trim());
        task.setDueDate(selectedDate);
        if (existingTaskId != -1) {
            task.setId(existingTaskId);
        }
        return task;
    }

    private void showError(String message, Exception e) {
        Log.e("APP_ERROR", message + ": " + (e != null ? e.getMessage() : ""));
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showLoading(boolean show) {
        findViewById(R.id.progressBar).setVisibility(show ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnSave).setEnabled(!show);
    }

    private static class ClearErrorTextWatcher implements TextWatcher {
        private final TextInputLayout inputLayout;

        ClearErrorTextWatcher(TextInputLayout inputLayout) {
            this.inputLayout = inputLayout;
        }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            inputLayout.setError(null);
        }
    }

    private void performDatabaseOperation(Task task) {
        new Thread(() -> {
            try {
                if (existingTaskId == -1) {
                    database.taskDao().insert(task);
                } else {
                    database.taskDao().update(task);
                }
                // Set result to notify TaskDetailActivity
                setResult(RESULT_OK);
                runOnUiThread(this::finish);
            } catch (Exception e) {
                runOnUiThread(() -> showError("Database operation failed", e));
            }
        }).start();
    }
}

// ===== TaskDetailActivity.java =====
package com.example.taskmanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.taskmanager.data.AppDatabase;
import com.example.taskmanager.data.Task;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TaskDetailActivity extends AppCompatActivity {
    private AppDatabase database;
    private Task currentTask;
    private TextView tvTitle, tvDescription, tvDueDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        // Initialize views
        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvDueDate = findViewById(R.id.tvDueDate);
        Button btnEdit = findViewById(R.id.btnEdit);

        database = AppDatabase.getInstance(this);
        loadTaskDetails();

        btnEdit.setOnClickListener(v -> {
            if(currentTask != null) {
                Intent intent = new Intent(this, AddEditTaskActivity.class);
                intent.putExtra("TASK_ID", currentTask.getId());
                startActivity(intent);
            }
        });

        Button btnDelete = findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(v -> {
            new Thread(() -> {
                database.taskDao().delete(currentTask);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }).start();
        });
    }

    private void loadTaskDetails() {
        int taskId = getIntent().getIntExtra("TASK_ID", -1);
        if(taskId == -1) {
            showError("Invalid task selected");
            return;
        }

        new Thread(() -> {
            try {
                currentTask = database.taskDao().getTaskById(taskId);
                runOnUiThread(() -> {
                    if(currentTask != null) {
                        tvTitle.setText(currentTask.getTitle());
                        tvDescription.setText(currentTask.getDescription());

                        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                        tvDueDate.setText(sdf.format(new Date(currentTask.getDueDate())));
                    } else {
                        showError("Task not found");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> showError("Error loading task"));
            }
        }).start();
    }

    private void setupEditButton() {
        findViewById(R.id.btnEdit).setOnClickListener(v -> {
            if(currentTask != null) {
                Intent intent = new Intent(this, AddEditTaskActivity.class);
                intent.putExtra("TASK_ID", currentTask.getId()); // Ensure ID is passed
                startActivity(intent);
            }
        });
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTaskDetails();
    }
}




// ===== TaskAdapter.java =====
package com.example.taskmanager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.example.taskmanager.data.Task;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {
    private List<Task> tasks = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Task task);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDueDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);

            // Fixed deprecated getAdapterPosition()
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(tasks.get(position));
                }
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return tasks == null ? 0 : tasks.size();
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            if (tasks != null && position < tasks.size()) {
                Task task = tasks.get(position);
                holder.tvTitle.setText(task.getTitle());

                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                if(task.getDueDate() > 0) {
                    holder.tvDueDate.setText(sdf.format(new Date(task.getDueDate())));
                } else {
                    holder.tvDueDate.setText("No date set");
                }
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e("RECYCLERVIEW", "Invalid position: " + position);
        } catch (Exception e) {
            Log.e("BIND_VIEW", "Error binding data: " + e.getMessage());
        }
    }

    public void submitList(List<Task> newTasks) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new TaskDiffCallback(tasks, newTasks));
        tasks.clear();
        tasks.addAll(newTasks);
        result.dispatchUpdatesTo(this);
    }

    static class TaskDiffCallback extends DiffUtil.Callback {
        private final List<Task> oldTasks, newTasks;

        TaskDiffCallback(List<Task> oldTasks, List<Task> newTasks) {
            this.oldTasks = oldTasks;
            this.newTasks = newTasks;
        }

        @Override public int getOldListSize() { return oldTasks.size(); }
        @Override public int getNewListSize() { return newTasks.size(); }
        @Override public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldTasks.get(oldPos).getId() == newTasks.get(newPos).getId();
        }
        @Override public boolean areContentsTheSame(int oldPos, int newPos) {
            return oldTasks.get(oldPos).equals(newTasks.get(newPos));
        }
    }
}

