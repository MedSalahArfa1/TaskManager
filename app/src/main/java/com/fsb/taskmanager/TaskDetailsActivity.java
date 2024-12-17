package com.fsb.taskmanager;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fsb.taskmanager.Model.TaskModel;
import com.fsb.taskmanager.Utils.DataBaseHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TaskDetailsActivity extends AppCompatActivity {

    private Button completeButton;
    private Button setReminderButton;
    private Button shareButton;
    private Button editButton;
    private Button deleteButton;
    private DataBaseHelper myDB;
    private TextView titleTextView, descriptionTextView, deadlineTextView, reminderTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details);

        // Initialize database helper
        myDB = new DataBaseHelper(this);

        // Initialize views
        titleTextView = findViewById(R.id.titleTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        deadlineTextView = findViewById(R.id.deadlineTextView);
        reminderTextView = findViewById(R.id.reminderTextView);

        // Load task details
        loadTaskDetails();

        // Initialize the complete button
        completeButton = findViewById(R.id.completeButton);
        // Set the complete button action
        completeButton.setOnClickListener(v -> {
            markTaskAsComplete();
        });

        // Initialize the set reminder button
        setReminderButton = findViewById(R.id.setReminderButton);
        // Set reminder button action
        setReminderButton.setOnClickListener(v -> {
            setReminder();
        });

        // Initialize share button
        shareButton = findViewById(R.id.shareButton);
        // Set share button action
        shareButton.setOnClickListener(v -> {
            shareTask();
        });

        // Initialize the edit button
        editButton = findViewById(R.id.editButton);
        // Set edit button action
        editButton.setOnClickListener(v -> {
            editTask();
        });

        // Initialize delete button
        deleteButton = findViewById(R.id.deleteButton);
        // Set delete button action
        deleteButton.setOnClickListener(v -> {
            deleteTask();
        });

    }

    private void loadTaskDetails() {
        // Retrieve task ID from the intent
        int taskId = getIntent().getIntExtra("taskId", -1);
        if (taskId == -1) {
            Toast.makeText(this, "Invalid task ID", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity if no valid ID is provided
            return;
        }

        // Fetch the task from the database
        TaskModel task = myDB.getTaskById(taskId);
        if (task == null) {
            Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity if the task doesn't exist
            return;
        }

        // Update UI with task details
        titleTextView.setText(task.getTitre());
        descriptionTextView.setText(task.getDescription());
        deadlineTextView.setText("Date d’échéance: " + new SimpleDateFormat("dd-MM-yyyy").format(task.getDate_echeance()));
        reminderTextView.setText("Nombre de Rappels: " + task.getRappel());
    }

    private void markTaskAsComplete() {
        int taskId = getIntent().getIntExtra("taskId", -1);
        if (taskId != 0) {
            myDB.updateStatut(taskId, 1); // Mark as completed (status 1)
            Toast.makeText(this, "Task marked as complete", Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(this, "Task already done", Toast.LENGTH_SHORT).show();
        }
        // Load task details
        loadTaskDetails();
    }

    private void setReminder() {
        int taskId = getIntent().getIntExtra("taskId", -1);
        if (taskId != -1) {
            // Open a date and time picker to set the reminder (use your existing reminder logic)
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                TimePickerDialog timePickerDialog = new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);

                    // Schedule the reminder notification
                    scheduleNotification(calendar.getTimeInMillis(), taskId);

                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
                timePickerDialog.show();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        }
        // Load task details
        loadTaskDetails();
    }

    private void scheduleNotification(long triggerTime, int taskId) {
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("taskId", taskId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, taskId, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

    private void shareTask() {
        int taskId = getIntent().getIntExtra("taskId", -1);
        if (taskId != -1) {
            TaskModel task = myDB.getTaskById(taskId);
            if (task != null) {
                String shareMessage = "Task: " + task.getTitre() + "\n" +
                        "Description: " + task.getDescription() + "\n" +
                        "Deadline: " + new SimpleDateFormat("dd-MM-yyyy").format(task.getDate_echeance());

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Task Details");
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                startActivity(Intent.createChooser(shareIntent, "Share Task"));
            }
        }
        // Load task details
        loadTaskDetails();
    }

    private void editTask() {
        int taskId = getIntent().getIntExtra("taskId", -1);
        if (taskId != -1) {
            TaskModel task = myDB.getTaskById(taskId);
            if (task != null) {
                // Pass the task details to the AddNewTask fragment
                Bundle bundle = new Bundle();
                bundle.putInt("id", task.getId());
                bundle.putString("titre", task.getTitre());
                bundle.putString("description", task.getDescription());
                bundle.putLong("date_echeance", task.getDate_echeance().getTime());
                bundle.putInt("rappel", task.getRappel());

                AddNewTask addNewTaskFragment = new AddNewTask();
                addNewTaskFragment.setArguments(bundle);
                addNewTaskFragment.show(getSupportFragmentManager(), addNewTaskFragment.getTag());
            }
        }
        // Load task details
        loadTaskDetails();
    }

    private void deleteTask() {
        int taskId = getIntent().getIntExtra("taskId", -1);
        if (taskId != -1) {
            myDB.deleteTask(taskId);
            Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity
        }
        // Load task details
        loadTaskDetails();
    }



}
