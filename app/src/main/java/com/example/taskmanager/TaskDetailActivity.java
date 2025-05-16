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