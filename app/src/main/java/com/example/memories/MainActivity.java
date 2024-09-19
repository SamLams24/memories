package com.example.memories;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import android.annotation.SuppressLint;
import android.os.Bundle;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.view.Menu;
import android.content.res.ColorStateList;
import android.graphics.Color;


public class MainActivity extends AppCompatActivity implements MemoryAdapter.OnMemoryActionListener {

    private FusedLocationProviderClient fusedLocationClient;
    private double currentLatitude;
    private double currentLongitude;
    private static final int REQUEST_CODE = 1;

    private RecyclerView recyclerView;
    private MemoryAdapter adapter, adapter1;
    private MemoryDatabaseHelper databaseHelper;
    private List<Memory> memoryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        //checkPermissions();
        createNotificationChannel();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        ColorStateList colorStateList = ColorStateList.valueOf(Color.parseColor("#F9B34C"));
        int[][] states = new int[][] {
                new int[] {-android.R.attr.state_checked}, // non sélectionné
                new int[] {android.R.attr.state_checked}  // sélectionné
        };
        int[] colors = new int[] {
                Color.RED,// non sélectionné
                Color.parseColor("#F9B34C") // sélectionné
        };
        colorStateList = new ColorStateList(states, colors);
        bottomNavigationView.setItemTextColor(colorStateList);
        bottomNavigationView.setItemTextColor(colorStateList);

        // Afficher le fragment des mémoires par défaut
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new MemoriesFragment()).commit();
        }

        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                switch (item.getItemId()) {
                    case 2131362203:
                        selectedFragment = new MemoriesFragment();
                        break;
                    case 2131362205:
                        selectedFragment = new SocialFragment();
                        break;
                    case 2131362202:
                        selectedFragment = new MapFragment();
                        break;
                    case 2131362204:
                        selectedFragment = new ParamFragment();
                        break;
                    default:
                        Log.e("MainActivity", "Unexpected value: " + item.getItemId());
                        return false;  // Return false if the item ID is unexpected
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
                }
                else {
                    selectedFragment = new MemoriesFragment();
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
                }
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.bottom_navigation_menu, menu);
        return true;

    }

    @Override
    public void onEdit(Memory memory) {

    }

    @Override
    public void onDelete(Memory memory) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE
            );
        }
    }
    private void addMemoryToFirestore(int id, String title, String description, byte[] imageBytes, String videoPath, String date, String category, double latitude, double longitude) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Memory memory = new Memory();
        memory.setId(id);
        memory.setTitle(title);
        memory.setDescription(description);
        memory.setImageBytes(imageBytes);
        memory.setVideoPath(videoPath);
        memory.setDate(date);
        memory.setCategory(category);
        memory.setLatitude(latitude);
        memory.setLongitude(longitude);

        // Créer une map avec les données de memory
        Map<String, Object> memoryMap = new HashMap<>();
        memoryMap.put("id", Integer.parseInt(memory.getId()));
        memoryMap.put("title", memory.getTitle());
        memoryMap.put("description", memory.getDescription());
        memoryMap.put("imageBytes", memory.getImageBytes());
        memoryMap.put("videoPath", memory.getVideoPath());
        memoryMap.put("date", memory.getDate());
        memoryMap.put("category", memory.getCategory());
        memoryMap.put("latitude", memory.getLatitude());
        memoryMap.put("longitude", memory.getLongitude());

        // Pousser le memory dans Firestore
        db.collection("shared_memories").document(String.valueOf(memory.getId())).set(memoryMap)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Memory successfully added!"))
                .addOnFailureListener(e -> Log.w("Firestore", "Error adding memory", e));
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "ReminderChannel";
            String description = "Channel for Memory Reminders";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("reminderChannel", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

}