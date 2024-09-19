package com.example.memories;

import androidx.fragment.app.Fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class SocialFragment extends Fragment {
    private RecyclerView recyclerView;
    private MemoryAdapter memoryAdapter;
    private List<Memory> memoryList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_social, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_social);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        memoryList = new ArrayList<>();
        memoryAdapter = new MemoryAdapter(getContext(),memoryList, new MemoryAdapter.OnMemoryActionListener() {
            @Override
            public void onEdit(Memory memory) {

               // openEditMemoryActivity(memory);
            }


            @Override
            public void onDelete(Memory memory) {

                confirmDeleteMemory(memory);
            }
        });
        recyclerView.setAdapter(memoryAdapter);

        // Fetch memories from Firebase
        fetchMemoriesFromFirebase();

        return view;
    }

    private void fetchMemoriesFromFirebase() {
        FirebaseFirestore db2 = FirebaseFirestore.getInstance();
        db2.collection("shared_memories").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.w("Firestore", "Listen failed.", error);
                    return;
                }

                memoryList.clear();
                assert queryDocumentSnapshots != null;
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    Memory memory = document.toObject(Memory.class);

                    // Décoder l'image Base64
                    String imageBase64 = document.getString("image");
                    if (imageBase64 != null && !imageBase64.isEmpty()) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
                            memory.setImageBytes(imageBytes);
                            Log.d("MemoryAdapter", "Image decoded with byte size: " + imageBytes.length);
                        }
                    }

                    // Décoder la vidéo Base64 (si applicable)
                    String videoBase64 = document.getString("video");
                    if (videoBase64 != null && !videoBase64.isEmpty()) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            byte[] videoBytes = Base64.getDecoder().decode(videoBase64);
                            memory.setVideoBytes(videoBytes);
                            Log.d("MemoryAdapter", "Video decoded with byte size: " + videoBytes.length);
                        }
                    }

                    memoryList.add(memory);
                }
                memoryAdapter.notifyDataSetChanged();
            }
        });
    }
    private void confirmDeleteMemory(final Memory memory) {
        // Example: Show confirmation dialog for deleting memory
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this memory?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Perform delete operation
                        deleteMemoryFromFirebase(memory);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void deleteMemoryFromFirebase(Memory memory) {
        // Example: Delete memory from Firebase
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("shared_memories").document(memory.getId()).delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Memory deleted successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Failed to delete memory", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
