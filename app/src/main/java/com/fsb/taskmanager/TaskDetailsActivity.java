package com.fsb.taskmanager;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
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

public class TaskDetailsActivity extends AppCompatActivity {

    private Button completeButton;
    private Button setReminderButton;
    private Button shareButton;
    private Button editButton;
    private Button deleteButton;
    private DataBaseHelper myDB;
    private TextView titleTextView, descriptionTextView, deadlineTextView, reminderTextView;
    private TaskModel currentTask;

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

        completeButton = findViewById(R.id.completeButton);
        setReminderButton = findViewById(R.id.setReminderButton);
        shareButton = findViewById(R.id.shareButton);
        editButton = findViewById(R.id.editButton);
        deleteButton = findViewById(R.id.deleteButton);

        // Load task details
        loadTaskDetails();

        completeButton.setOnClickListener(v -> toggleTaskCompletion());
        setReminderButton.setOnClickListener(v -> setReminder());
        shareButton.setOnClickListener(v -> shareTask());
        editButton.setOnClickListener(v -> editTask());
        deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());

    }

    private void loadTaskDetails() {
        int taskId = getIntent().getIntExtra("taskId", -1);
        if (taskId == -1) {
            Toast.makeText(this, "Invalid task ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentTask = myDB.getTaskById(taskId);
        if (currentTask == null) {
            Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        titleTextView.setText(currentTask.getTitre());
        descriptionTextView.setText(currentTask.getDescription());
        deadlineTextView.setText("Date d’échéance: " + new SimpleDateFormat("dd-MM-yyyy").format(currentTask.getDate_echeance()));
        reminderTextView.setText("Nombre de Rappels: " + currentTask.getRappel());

        // Update the complete button appearance
        updateCompleteButton();

    }

    private void toggleTaskCompletion() {
        int newStatus = (currentTask.getStatut() == 0) ? 1 : 0;

        // Update status in the database
        myDB.updateStatut(currentTask.getId(), newStatus);

        // Update the current task object
        currentTask.setStatut(newStatus);

        // Prepare the result intent
        Intent resultIntent = new Intent();
        resultIntent.putExtra("taskId", currentTask.getId());
        resultIntent.putExtra("taskStatus", newStatus);
        setResult(Activity.RESULT_OK, resultIntent);

        // Reload task details to reflect changes
        loadTaskDetails();

        // Show a toast message for user feedback
        String message = (newStatus == 1) ? "Tâche marquée comme Complète" : "Tâche marquée comme Incomplète";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void updateCompleteButton() {
        if (currentTask.getStatut() == 1) {
            completeButton.setText("Marquer comme Incomplète");

        } else {
            completeButton.setText("Marquer comme Complète");
        }
    }

    private void setReminder() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);

                // Open the clock app to set the alarm
                openClockApp(calendar);

                scheduleNotification(calendar.getTimeInMillis(), currentTask.getId(), currentTask.getTitre());

                // Increment the reminder count
                myDB.updateRappel(currentTask.getId(),currentTask.getRappel()+1);
                currentTask.setRappel(currentTask.getRappel() + 1);

                loadTaskDetails(); // Refresh details to update "Nombre de Rappels"
                Toast.makeText(this, "Rappel défini avec succès", Toast.LENGTH_SHORT).show();

            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
            timePickerDialog.show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void scheduleNotification(long triggerTime, int taskId, String taskTitle) {
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("taskId", taskId);
        intent.putExtra("taskTitle", taskTitle);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, taskId, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

    private void openClockApp(Calendar calendar) {
        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setType("vnd.android.cursor.item/event");
        intent.putExtra("beginTime", calendar.getTimeInMillis());
        intent.putExtra("allDay", false);
        startActivity(intent);
    }


    private void shareTask() {
        String shareMessage = "Tâche: " + currentTask.getTitre() + "\n" +
                "Description: " + currentTask.getDescription() + "\n" +
                "Date d'échéance: " + new SimpleDateFormat("dd-MM-yyyy").format(currentTask.getDate_echeance());

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Détails de la tâche:\n");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
        startActivity(Intent.createChooser(shareIntent, "Partagez cette tâche via"));
    }

    private void editTask() {
        Bundle bundle = new Bundle();
        bundle.putInt("id", currentTask.getId());
        bundle.putString("titre", currentTask.getTitre());
        bundle.putString("description", currentTask.getDescription());
        bundle.putLong("date_echeance", currentTask.getDate_echeance().getTime());
        bundle.putInt("rappel", currentTask.getRappel());

        AddNewTask addNewTaskFragment = new AddNewTask();
        addNewTaskFragment.setArguments(bundle);

        // Set a listener on the fragment to handle task updates after editing
        addNewTaskFragment.setOnTaskUpdatedListener(() -> {
            // After the task is updated in the fragment, refresh task details
            loadTaskDetails(); // This should be called to refresh the task info
        });

        addNewTaskFragment.show(getSupportFragmentManager(), addNewTaskFragment.getTag());
    }




    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer la tâche")
                .setMessage("Êtes-vous sûr ?")
                .setPositiveButton("Oui", (dialog, which) -> deleteTask())
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void deleteTask() {
        myDB.deleteTask(currentTask.getId());
        Toast.makeText(this, "Tâche supprimée", Toast.LENGTH_SHORT).show();
        // Send a result back to MainActivity to indicate the task has been deleted
        Intent resultIntent = new Intent();
        setResult(RESULT_OK, resultIntent);
        finish();
    }

}
