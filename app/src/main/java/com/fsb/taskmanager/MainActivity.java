package com.fsb.taskmanager;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fsb.taskmanager.Adapter.TaskAdapter;
import com.fsb.taskmanager.Model.TaskModel;
import com.fsb.taskmanager.Utils.DataBaseHelper;
import com.fsb.taskmanager.interfaces.OnDialogCloseListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnDialogCloseListener {

    private RecyclerView mRecyclerview;
    private FloatingActionButton fab;
    private DataBaseHelper myDB;

    private List<TaskModel> tasksList;
    private TaskAdapter adapter;

    private SearchView searchView;

    // Activity Initialization
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerview = findViewById(R.id.recyclerview);
        fab = findViewById(R.id.fab);
        myDB = new DataBaseHelper(MainActivity.this);
        tasksList = new ArrayList<>();
        adapter = new TaskAdapter(myDB, MainActivity.this);

        // RecyclerView Setup
        mRecyclerview.setHasFixedSize(true);
        mRecyclerview.setLayoutManager(new LinearLayoutManager(this)); // Sets a LinearLayoutManager to arrange the list
        // items vertically
        mRecyclerview.setAdapter(adapter); // Sets the adapter to manage the data in the RecyclerView

        // Fetch Tasks
        tasksList = myDB.getAllTasks();
        Collections.reverse(tasksList);
        adapter.setTasks(tasksList);

        // FloatingActionButton Click
        fab.setOnClickListener(v -> AddNewTask.newInstance().show(getSupportFragmentManager(), AddNewTask.TAG));

        // Swipe gestures on tasks
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new RecyclerViewTouchHelper(adapter));
        itemTouchHelper.attachToRecyclerView(mRecyclerview);

        // Initialize SearchView
        searchView = findViewById(R.id.searchView);
        setupSearchView();
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false; // We handle the filtering in real-time
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText); // Filter tasks dynamically
                return true;
            }
        });
    }

    // Update the task list after closing the dialog
    @Override
    public void onDialogClose(DialogInterface dialogInterface) {
        tasksList = myDB.getAllTasks();
        Collections.reverse(tasksList);
        adapter.setTasks(tasksList);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Task was deleted, refresh the RecyclerView to remove it
            refreshTaskList();
        }
    }

    private void refreshTaskList() {
        // Fetch the updated task list from the database
        List<TaskModel> updatedTaskList = myDB.getAllTasks();
        // Notify your RecyclerView adapter to update the UI
        adapter.updateTaskList(updatedTaskList);
    }


}