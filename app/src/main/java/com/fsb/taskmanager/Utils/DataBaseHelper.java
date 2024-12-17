package com.fsb.taskmanager.Utils;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.fsb.taskmanager.Model.TaskModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DataBaseHelper extends SQLiteOpenHelper {

    private SQLiteDatabase db;

    private static final String DATABASE_NAME = "TASKMANAGER_DATABASE";
    private static final String TABLE_NAME = "TASK_TABLE";
    private static final String COL_1 = "ID";
    private static final String COL_2 = "TITRE";
    private static final String COL_3 = "DESCRIPTION";
    private static final String COL_4 = "DATE_ECHEANCE";
    private static final String COL_5 = "STATUT";
    private static final String COL_6 = "RAPPEL";

    private static final String DATE_FORMAT = "dd-MM-yyyy"; // Standard Date Format
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());

    public DataBaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    //DataBase Initialization
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                        COL_1 + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_2 + " TEXT NOT NULL, " +
                        COL_3 + " TEXT, " +
                        COL_4 + " TEXT, " +
                        COL_5 + " INTEGER, " +
                        COL_6 + " INTEGER)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    //CRUD Operations
    public void insertTask(TaskModel task) {
        db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_2, task.getTitre());
        values.put(COL_3, task.getDescription());
        values.put(COL_4, dateFormat.format(task.getDate_echeance())); // Format Date
        values.put(COL_5, task.getStatut());
        values.put(COL_6, task.getRappel());
        db.insert(TABLE_NAME, null, values);
    }

    public void updateTitre(int taskId, String newTitre) {
        db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_2, newTitre);
        db.update(TABLE_NAME, values, COL_1 + " = ?", new String[]{String.valueOf(taskId)});
    }

    public void updateDescription(int taskId, String newDescription) {
        db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_3, newDescription);
        db.update(TABLE_NAME, values, COL_1 + " = ?", new String[]{String.valueOf(taskId)});
    }

    public void updateDateEcheance(int taskId, String newDate) {
        db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_4, newDate); // Store date as a string
        db.update(TABLE_NAME, values, COL_1 + " = ?", new String[]{String.valueOf(taskId)});
    }

    public void updateStatut(int taskId, int newStatus) {
        db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_5, newStatus);
        db.update(TABLE_NAME, values, COL_1 + " = ?", new String[]{String.valueOf(taskId)});
    }

    public void updateRappel(int taskId, int newRappel) {
        db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_6, newRappel);
        db.update(TABLE_NAME, values, COL_1 + " = ?", new String[]{String.valueOf(taskId)});
    }

    public void deleteTask(int taskId) {
        db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COL_1 + " = ?", new String[]{String.valueOf(taskId)});
    }

    //Transaction Management
    @SuppressLint("Range") //Suppress warnings related to the use of Cursor.getColumnIndex
    public List<TaskModel> getAllTasks() {
        db = this.getWritableDatabase();
        Cursor cursor = null;
        List<TaskModel> tasksList = new ArrayList<>();

        db.beginTransaction();
        try {
            cursor = db.query(TABLE_NAME, null, null, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    TaskModel task = new TaskModel();
                    task.setId(cursor.getInt(cursor.getColumnIndex(COL_1)));
                    task.setTitre(cursor.getString(cursor.getColumnIndex(COL_2)));
                    task.setDescription(cursor.getString(cursor.getColumnIndex(COL_3)));
                    // Parse Date
                    String dateString = cursor.getString(cursor.getColumnIndex(COL_4));
                    try {
                        task.setDate_echeance(dateFormat.parse(dateString)); // Convert back to Date
                    } catch (ParseException e) {
                        task.setDate_echeance(new Date()); // Fallback to current date
                    }
                    task.setStatut(cursor.getInt(cursor.getColumnIndex(COL_5)));
                    task.setRappel(cursor.getInt(cursor.getColumnIndex(COL_6)));
                    tasksList.add(task);
                } while (cursor.moveToNext());
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            if (cursor != null) cursor.close();
        }
        return tasksList;
    }

    @SuppressLint("Range")
    public TaskModel getTaskById(int taskId) {
        db = this.getReadableDatabase();
        Cursor cursor = null;
        TaskModel task = null;

        try {
            // Query to fetch the task with the specified ID
            cursor = db.query(
                    TABLE_NAME,                 // Table name
                    null,                       // Columns (null = all columns)
                    COL_1 + " = ?",             // WHERE clause
                    new String[]{String.valueOf(taskId)}, // WHERE arguments
                    null,                       // Group by
                    null,                       // Having
                    null                        // Order by
            );

            // Check if the cursor has a result
            if (cursor != null && cursor.moveToFirst()) {
                task = new TaskModel();
                task.setId(cursor.getInt(cursor.getColumnIndex(COL_1)));
                task.setTitre(cursor.getString(cursor.getColumnIndex(COL_2)));
                task.setDescription(cursor.getString(cursor.getColumnIndex(COL_3)));

                // Parse Date
                String dateString = cursor.getString(cursor.getColumnIndex(COL_4));
                try {
                    task.setDate_echeance(dateFormat.parse(dateString));
                } catch (ParseException e) {
                    task.setDate_echeance(new Date()); // Fallback to current date
                }

                task.setStatut(cursor.getInt(cursor.getColumnIndex(COL_5)));
                task.setRappel(cursor.getInt(cursor.getColumnIndex(COL_6)));
            }
        } finally {
            if (cursor != null) cursor.close(); // Close the cursor to prevent memory leaks
        }

        return task;
    }

}
