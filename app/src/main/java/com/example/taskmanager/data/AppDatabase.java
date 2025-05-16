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
