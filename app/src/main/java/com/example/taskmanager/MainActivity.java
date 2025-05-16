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