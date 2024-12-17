package com.fsb.taskmanager.Adapter;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fsb.taskmanager.AddNewTask;
import com.fsb.taskmanager.MainActivity;
import com.fsb.taskmanager.Model.TaskModel;
import com.fsb.taskmanager.NotificationReceiver;
import com.fsb.taskmanager.R;
import com.fsb.taskmanager.TaskDetailsActivity;
import com.fsb.taskmanager.Utils.DataBaseHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.MyViewHolder> {

    private List<TaskModel> tasksList;
    private MainActivity activity;
    private DataBaseHelper myDB;
    private List<TaskModel> tasksListFull; // Backup list for tasks


    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

    public TaskAdapter(DataBaseHelper myDB, MainActivity activity) {
        this.activity = activity;
        this.myDB = myDB;
        this.tasksList = new ArrayList<>();
        this.tasksListFull = new ArrayList<>();
    }

    //Inner class that holds references to the views for each item
    public static class MyViewHolder extends RecyclerView.ViewHolder {

        CheckBox mCheckBox;
        TextView mDeadlineTextView;  // TextView to show the deadline
        ImageButton btnShare, btnAlert;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            mCheckBox = itemView.findViewById(R.id.mcheckbox);
            mDeadlineTextView = itemView.findViewById(R.id.deadline);  // Initialize the deadline TextView
            btnShare = itemView.findViewById(R.id.btn_share); // Initialize share button
            btnAlert = itemView.findViewById(R.id.btn_alert); // Initialize reminder button
        }
    }

    //Inflates the layout for individual items
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_layout, parent, false);
        return new MyViewHolder(v);
    }

    //Binds task data to the views in task_layout.xml
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        final TaskModel item = tasksList.get(position);

        //Titre
        holder.mCheckBox.setText(item.getTitre());

        //Deadline
        holder.mDeadlineTextView.setText("Date d’échéance: " + dateFormat.format(item.getDate_echeance()));

        //Statut
        holder.mCheckBox.setChecked(toBoolean(item.getStatut()));
        holder.mCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                myDB.updateStatut(item.getId(), 1);
            } else {
                myDB.updateStatut(item.getId(), 0);
            }
        });

        // Set item click listener to navigate to TaskDetailsActivity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(activity, TaskDetailsActivity.class);
            intent.putExtra("taskId", item.getId());
            intent.putExtra("titre", item.getTitre());
            intent.putExtra("description", item.getDescription());
            intent.putExtra("date_echeance", item.getDate_echeance().getTime());
            intent.putExtra("rappel", item.getRappel());
            activity.startActivityForResult(intent, 1);  // Start Activity for result
        });

        // Share Button Click
        holder.btnShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Tâche: " + item.getTitre());
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Détails de la tâche:\n" +
                    "Titre: " + item.getTitre() + "\n" +
                    "Description: " + item.getDescription() + "\n" +
                    "Deadline: " + dateFormat.format(item.getDate_echeance()));
            activity.startActivity(Intent.createChooser(shareIntent, "Partagez cette tâche via"));
        });

        // Reminder Button Click
        holder.btnAlert.setOnClickListener(v -> {
            // Show a time picker or date picker to set a reminder
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(activity, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                TimePickerDialog timePickerDialog = new TimePickerDialog(activity, (timeView, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);

                    // Schedule a notification (or any action you prefer)
                    scheduleNotification(item.getTitre(), calendar.getTimeInMillis());

                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
                timePickerDialog.show();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();

            // Increment the reminder count
            myDB.updateRappel(item.getId(),item.getRappel()+1);
            item.setRappel(item.getRappel() + 1);
            notifyDataSetChanged(); // Notifie l'adaptateur pour les modifications
        });

    }

    public boolean toBoolean(int num) {
        return num != 0;
    }

    public Context getContext() {
        return activity;
    }

    public void setTasks(List<TaskModel> mList) {
        this.tasksList = mList;
        this.tasksListFull = new ArrayList<>(mList); // Copy the list for backup
        notifyDataSetChanged();
    }

    public void deleteTask(int position) {
        TaskModel item = tasksList.get(position);
        myDB.deleteTask(item.getId());
        tasksList.remove(position);
        notifyItemRemoved(position);
    }

    public void editTask(int position) {
        TaskModel item = tasksList.get(position);

        Bundle bundle = new Bundle();
        bundle.putInt("id", item.getId());
        bundle.putString("titre", item.getTitre());
        bundle.putString("description", item.getDescription());
        bundle.putLong("date_echeance", item.getDate_echeance().getTime());
        bundle.putInt("rappel", item.getRappel());

        AddNewTask task = new AddNewTask();
        task.setArguments(bundle);
        task.show(activity.getSupportFragmentManager(), task.getTag());
    }

    @Override
    public int getItemCount() {
        return tasksList.size();
    }

    public void filter(String query) {
        if (query == null || query.isEmpty()) {
            tasksList = new ArrayList<>(tasksListFull); // Reset to original list
        } else {
            List<TaskModel> filteredList = new ArrayList<>();
            for (TaskModel task : tasksListFull) {
                if (task.getTitre().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(task);
                }
            }
            tasksList = filteredList;
        }
        notifyDataSetChanged();
    }

    // Method to schedule a notification (example implementation)
    private void scheduleNotification(String taskTitle, long triggerTime) {
        Intent intent = new Intent(activity, NotificationReceiver.class);
        intent.putExtra("taskTitle", taskTitle);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

    public void updateTaskList(List<TaskModel> updatedTaskList) {
        this.tasksList = updatedTaskList;
        notifyDataSetChanged(); // Notify the adapter that the data has changed
    }



}
