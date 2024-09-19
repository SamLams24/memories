package com.example.memories;

import static java.util.Locale.filter;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SearchView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class MemoriesFragment extends Fragment implements MemoryAdapter.OnMemoryActionListener {

    private FusedLocationProviderClient fusedLocationClient;
    private double currentLatitude;
    private double currentLongitude;

    private SearchView searchView;
    private List<Memory> filteredMemoryList;

    private RecyclerView recyclerView;
    private MemoryAdapter adapter, adapter1;
    private MemoryDatabaseHelper databaseHelper;
    private List<Memory> memoryList;
    private FloatingActionButton fab;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_memories, container, false);

        if (getActivity() != null) {

            //searchView = view.findViewById(R.id.searchView);
            recyclerView = view.findViewById(R.id.recycler_view);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

            databaseHelper = new MemoryDatabaseHelper(getContext());
            memoryList = databaseHelper.getAllMemories();
            filteredMemoryList = new ArrayList<>(memoryList);

            adapter = new MemoryAdapter(getContext(), memoryList, this);
            recyclerView.setAdapter(adapter);

            FloatingActionButton fab = view.findViewById(R.id.fab_add_memory);
            fab.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), AddEditMemoryActivity.class);
                startActivity(intent);
            });
            return view;
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        memoryList.clear();
        memoryList.addAll(databaseHelper.getAllMemories());
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onEdit(Memory memory) {
        Intent intent = new Intent(getActivity(), AddEditMemoryActivity.class);
        intent.putExtra("memory_id", Integer.parseInt(memory.getId()));
        startActivity(intent);
    }

    @Override
    public void onDelete(Memory memory) {
        databaseHelper.deleteMemory(Integer.parseInt(memory.getId()));
        memoryList.remove(memory);
        adapter.notifyDataSetChanged();
    }

    private void filter(String text) {
        Log.d("MemoriesFragment", "Filtering for: " + text);
        filteredMemoryList.clear();
        for (Memory memory : memoryList) {
            if (memory.getTitle().toLowerCase().contains(text.toLowerCase())) {
                filteredMemoryList.add(memory);
            }
        }
        Log.d("MemoriesFragment", "Filtered list size: " + filteredMemoryList.size());
        adapter.notifyDataSetChanged();

    }
}
