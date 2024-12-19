package com.fsb.taskmanager;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

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
    private List<TaskModel> filteredList;
    private TaskAdapter adapter;

    private SearchView searchView;
    private Spinner filterSpinner;
    private String currentFilter = "Toutes Les Tâches"; // Default filter

    private TextView noTasksTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupRecyclerView();
        setupSearchView();
        setupFilterSpinner();
        setupFabButton();
        fetchTasks();
        updateNoTasksMessage();
    }

    private void initializeViews() {
        mRecyclerview = findViewById(R.id.recyclerview);
        fab = findViewById(R.id.fab);
        searchView = findViewById(R.id.searchView);
        filterSpinner = findViewById(R.id.filterSpinner);
        noTasksTextView = findViewById(R.id.noTasksTextView);
        myDB = new DataBaseHelper(MainActivity.this);
        tasksList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new TaskAdapter(myDB, MainActivity.this);
    }

    private void setupRecyclerView() {
        mRecyclerview.setHasFixedSize(true);
        mRecyclerview.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerview.setAdapter(adapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new RecyclerViewTouchHelper(adapter));
        itemTouchHelper.attachToRecyclerView(mRecyclerview);
    }

    private void setupSearchView() {
        fetchTasks();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterTasks(newText, currentFilter);
                return true;
            }
        });
    }

    private void setupFilterSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.filter_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(adapter);

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentFilter = parent.getItemAtPosition(position).toString();
                filterTasks(searchView.getQuery().toString(), currentFilter); // Apply filter after fetching tasks
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentFilter = "Toutes Les Tâches";
                filterTasks(searchView.getQuery().toString(), currentFilter); // Apply filter after fetching tasks
            }
        });

    }

    private void setupFabButton() {
        fab.setOnClickListener(v -> AddNewTask.newInstance().show(getSupportFragmentManager(), AddNewTask.TAG));
    }

    private void fetchTasks() {
        // Always fetch the tasks from the database
        tasksList = myDB.getAllTasks();
        Collections.reverse(tasksList); // Optional if you want them in reverse order
    }



    private void filterTasks(String query, String filterOption) {
        // Fetch the latest tasks from the database
        //fetchTasks();

        // Clear the filtered list to avoid duplication
        filteredList.clear();

        for (TaskModel task : tasksList) {
            boolean matchesQuery = task.getTitre().toLowerCase().contains(query.toLowerCase());
            boolean matchesFilter = filterOption.equals("Toutes Les Tâches") ||
                    (filterOption.equals("Complétées") && task.getStatut() == 1) ||
                    (filterOption.equals("Non Complétées") && task.getStatut() == 0);

            // Only add tasks that match both the query and filter
            if (matchesQuery && matchesFilter) {
                filteredList.add(task);
            }
        }

        // Update the adapter with the filtered list
        adapter.updateTaskList(filteredList);

        // Update the no tasks message visibility
        updateNoTasksMessage();
    }




    private void updateNoTasksMessage() {
        if (filteredList.isEmpty()) {
            noTasksTextView.setVisibility(View.VISIBLE);
            mRecyclerview.setVisibility(View.GONE);
        } else {
            noTasksTextView.setVisibility(View.GONE);
            mRecyclerview.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDialogClose(DialogInterface dialogInterface) {
        fetchTasks();
        filterTasks(searchView.getQuery().toString(), currentFilter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            int taskId = data.getIntExtra("id", -1);
            int taskStatus = data.getIntExtra("statut", -1);

            if (taskId != -1 && taskStatus != -1) {
                // Update the task status in the list and notify the adapter
                for (TaskModel task : adapter.getTasksList()) {
                    if (task.getId() == taskId) {
                        task.setStatut(taskStatus);
                        break;
                    }
                }

                adapter.notifyDataSetChanged(); // Refresh the RecyclerView
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Always fetch tasks and apply the current filter
        fetchTasks();
        filterTasks(searchView.getQuery().toString(), currentFilter);
    }

}