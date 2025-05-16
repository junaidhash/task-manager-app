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