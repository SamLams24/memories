package com.example.memories;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.primitives.Bytes;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;

import android.app.DatePickerDialog;
import android.widget.DatePicker;
import android.widget.VideoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class AddEditMemoryActivity extends AppCompatActivity {
    private Spinner categorySpinner;
    private static final int REQUEST_CODE = 1;

    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int REQUEST_LOCATION_PERMISSION = 3;
    private static final int REQUEST_SELECT_IMAGE = 4;
    private static final int REQUEST_SELECT_VIDEO = 5;
    private static final int REQUEST_SELECT_MEDIA = 6;

    private EditText titleEditText, dateEditText, descriptionEditText;
    private ImageView imageView;
    private PlayerView playerView;
    private ExoPlayer player;
    private Button addLocationButton, saveButton, buttonSelectVideo, buttonSelectImage, setReminderButton;
    private byte[] imageBytes, videoBytes;
    private Memory memory;
    private MemoryDatabaseHelper databaseHelper;
    private int memoryId = -1;
    private FusedLocationProviderClient fusedLocationClient;
    private double latitude, longitude;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_memory);
        checkPermissions();
        // Initialize views
        categorySpinner = findViewById(R.id.categorySpinner);
        dateEditText = findViewById(R.id.date_edit_text);
        dateEditText.setOnClickListener(v -> showDatePickerDialog());

        addLocationButton = findViewById(R.id.location_button);
        titleEditText = findViewById(R.id.edit_text_title);
        descriptionEditText = findViewById(R.id.edit_text_description);
        imageView = findViewById(R.id.image_view);
        playerView = findViewById(R.id.video_View);
        saveButton = findViewById(R.id.button_save);
        buttonSelectVideo = findViewById(R.id.button_select_video);
        buttonSelectImage = findViewById(R.id.button_select_image);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        databaseHelper = new MemoryDatabaseHelper(this);

        // Set up category spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.categories_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        Intent intent = getIntent();
        if (intent.hasExtra("memory_id")) {
            memoryId = intent.getIntExtra("memory_id", -1);
            memory = databaseHelper.getMemoryById(memoryId);
            if (memory != null) {
                loadMemory(memory);
            }
        } else {
            memory = new Memory();
        }

        // Set listeners
        Button setReminderButton = findViewById(R.id.set_reminder_button);
        setReminderButton.setOnClickListener(v -> {
            // Ouvrir un DatePickerDialog pour choisir la date de rappel
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        addReminder(memory, calendar);
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });
        imageView.setOnClickListener(v -> showImageVideoPickerDialog());
        addLocationButton.setOnClickListener(v -> requestLocationPermission());
        saveButton.setOnClickListener(v -> saveMemory());
        buttonSelectVideo.setOnClickListener(v -> openGallery(REQUEST_SELECT_VIDEO));
        buttonSelectImage.setOnClickListener(v -> openGallery(REQUEST_SELECT_IMAGE));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            switch (requestCode) {
                case REQUEST_IMAGE_CAPTURE:
                    handleImageCapture(data);
                    break;
                case REQUEST_SELECT_IMAGE:
                    handleImageSelection(data);
                    break;
                case REQUEST_SELECT_VIDEO:
                    handleVideoSelection(data);
                    break;
                default:
                    Log.e("AddEditMemoryActivity", "Unexpected requestCode: " + requestCode);
                    break;
            }
        }
    }

    private void handleImageCapture(Intent data) {
        Bundle extras = data.getExtras();
        Bitmap imageBitmap = (Bitmap) extras.get("data");
        if (imageBitmap != null) {
            imageView.setImageBitmap(imageBitmap);
            imageView.setVisibility(View.VISIBLE);
            playerView.setVisibility(View.GONE);
            imageBytes = bitmapToByteArray(imageBitmap);
            videoBytes = null; // Reset videoBytes if an image is captured
        }
    }

    private void handleImageSelection(Intent data) {
        Uri selectedImageUri = data.getData();
        if (selectedImageUri != null) {
            imageView.setImageURI(selectedImageUri);
            imageView.setVisibility(View.VISIBLE);
            playerView.setVisibility(View.GONE);
            imageBytes = uriToByteArray(selectedImageUri);
            videoBytes = null; // Reset videoBytes if an image is selected
        }
    }

    private void handleVideoSelection(Intent data) {
        Uri selectedVideoUri = data.getData();
        if (selectedVideoUri != null) {
            String videoPath = saveVideoToExternalStorage(selectedVideoUri);
            if (videoPath != null) {
                initializePlayer(Uri.parse(videoPath));
                playerView.setVisibility(View.VISIBLE);
                imageView.setVisibility(View.GONE);
                videoBytes = null; // Reset videoBytes if a video is selected
                memory.setVideoPath(videoPath); // Store the path instead of the byte array
                imageBytes = null; // Reset imageBytes if a video is selected
            }
        }
    }

    private byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    private byte[] uriToByteArray(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, len);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year1, month1, dayOfMonth) -> {
                    String date = year1 + "-" + (month1 + 1) + "-" + dayOfMonth;
                    dateEditText.setText(date);
                },
                year, month, day);
        datePickerDialog.show();
    }

    private void saveMemory() {
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String category = categorySpinner.getSelectedItem().toString();
        String date = dateEditText.getText().toString();

        memory.setTitle(title);
        memory.setDescription(description);
        memory.setCategory(category);
        memory.setDate(date);
        memory.setImageBytes(imageBytes);
        memory.setVideoBytes(videoBytes);
        memory.setLatitude(latitude);
        memory.setLongitude(longitude);

        if (memoryId == -1) {
            databaseHelper.insertMemory(memory);
            Toast.makeText(this, "Memory ajoutée avec Succès", Toast.LENGTH_SHORT).show();
        } else {
            memory.setId(memoryId);
            databaseHelper.updateMemory(memory);
            Toast.makeText(this, "Memory Modifié avec Succès", Toast.LENGTH_SHORT).show();
        }

        finish();
    }

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            getLocation();
        }
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        Toast.makeText(AddEditMemoryActivity.this, "Localisation Ajoutée avec Succès", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AddEditMemoryActivity.this, "Impossible d'ajouter La Localisation", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadMemory(Memory memory) {
        titleEditText.setText(memory.getTitle());
        descriptionEditText.setText(memory.getDescription());
        dateEditText.setText(memory.getDate());

        if (memory.getImageBytes() != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(memory.getImageBytes(), 0, memory.getImageBytes().length);
            imageView.setImageBitmap(bitmap);
            imageView.setVisibility(View.VISIBLE);
            playerView.setVisibility(View.GONE);
        } else if (memory.getVideoPath() != null) {
            Uri videoUri = Uri.parse(memory.getVideoPath());
            initializePlayer(videoUri);
            playerView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
        }
        latitude = memory.getLatitude();
        longitude = memory.getLongitude();
        imageBytes = memory.getImageBytes();
        videoBytes = memory.getVideoBytes();
    }

    private Uri byteArrayToUri(byte[] bytes) {
        String path = Environment.getExternalStorageDirectory() + "/temp_video.mp4";
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Uri.parse(path);
    }

    private void initializePlayer(Uri videoUri) {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        MediaItem mediaItem = MediaItem.fromUri(videoUri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    private void captureImage() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void showImageVideoPickerDialog() {
        String[] options = {"Prendre une Photo", "Choisir une Image", "Selectionner une Vidéo"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Que voulez vous faire ?")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        captureImage();
                    }else if (which == 1) {
                        openGallery(REQUEST_SELECT_IMAGE);
                    } else if (which == 2) {
                        openGallery(REQUEST_SELECT_VIDEO);
                    }
                });
        builder.show();
    }

    private void openGallery(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (requestCode == REQUEST_SELECT_VIDEO) {
            intent.setType("video/*");
        } else {
            intent.setType("image/*");
        }
        startActivityForResult(intent, requestCode);
    }
    private Uri getVideoUri(byte[] videoBytes) {
        String path = getFilesDir() + "/temp_video.mp4";
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(videoBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Uri.parse(path);
    }

    private String saveVideoToExternalStorage(Uri videoUri) {
        String fileName = "video_" + System.currentTimeMillis() + ".mp4";
        File externalFilesDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        File videoFile = new File(externalFilesDir, fileName);

        try (InputStream inputStream = getContentResolver().openInputStream(videoUri);
             OutputStream outputStream = new FileOutputStream(videoFile)) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            return videoFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
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

    private void addReminder(Memory memory, Calendar reminderTime) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> reminderMap = new HashMap<>();
        reminderMap.put("memoryId", memory.getId());
        reminderMap.put("reminderTime", reminderTime.getTimeInMillis());

        db.collection("reminders").add(reminderMap)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Firestore", "Reminder successfully added!");

                    // Planifier une notification
                    scheduleNotification(memory, reminderTime);
                })
                .addOnFailureListener(e -> Log.w("Firestore", "Error adding reminder", e));
    }
    private void scheduleNotification(Memory memory, Calendar reminderTime) {
        Intent intent = new Intent(getApplicationContext(), ReminderBroadcastReceiver.class);
        intent.putExtra("memoryTitle", memory.getTitle());
        intent.putExtra("memoryDescription", memory.getDescription());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime.getTimeInMillis() - TimeUnit.DAYS.toMillis(1), pendingIntent);
    }
}


/* debut public class AddEditMemoryActivity extends AppCompatActivity {
    private Spinner categorySpinner;
    private static final int REQUEST_CODE = 1;

    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int REQUEST_LOCATION_PERMISSION = 3;
    private static final int REQUEST_SELECT_MEDIA = 6;
    private PlayerView playerView;
    private ExoPlayer player;
    private static final int REQUEST_SELECT_IMAGE = 4;
    private static final int REQUEST_SELECT_VIDEO = 5;

    private EditText titleEditText, dateEditText;
    private EditText descriptionEditText;
    private ImageView imageView;
    private VideoView videoView;
    private Button addLocationButton, saveButton, buttonSelectVideo, buttonSelectImage;
    private byte[] imageBytes, videoBytes;
    private Memory memory;

    private MemoryDatabaseHelper databaseHelper;
    private int memoryId = -1;
    private FusedLocationProviderClient fusedLocationClient;
    private double latitude, longitude;

    @SuppressLint("WrongThread")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_memory);
        checkPermissions();
        // Initialise le spinner avec les données de la catégorie
        categorySpinner = findViewById(R.id.categorySpinner);

        // Initialisation des autres vues
        dateEditText = findViewById(R.id.date_edit_text);
        dateEditText.setOnClickListener(v -> showDatePickerDialog());

        addLocationButton = findViewById(R.id.location_button);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.categories_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
        titleEditText = findViewById(R.id.edit_text_title);
        descriptionEditText = findViewById(R.id.edit_text_description);
        imageView = findViewById(R.id.image_view);
        playerView = findViewById(R.id.video_View);
        saveButton = findViewById(R.id.button_save);
        buttonSelectVideo = findViewById(R.id.button_select_video);
        buttonSelectImage = findViewById(R.id.button_select_image);
        //locationButton = findViewById(R.id.location_button);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        databaseHelper = new MemoryDatabaseHelper(this);

        Intent intent = getIntent();
        if (intent.hasExtra("memory_id")) {
            int memoryId = intent.getIntExtra("memory_id", -1);
            System.out.println(memoryId);
            Log.d("AddEditMemoryActivity", "Memory ID: " + memoryId); // Log pour vérifier l'ID
            memory = databaseHelper.getMemoryById(memoryId);
            if (memory != null) {
                titleEditText.setText(memory.getTitle());
                descriptionEditText.setText(memory.getDescription());
                System.out.println(memoryId);
                memory.setId(memoryId);
                System.out.println(memoryId);
                // Créer un ArrayAdapter en utilisant le tableau de ressources
                ArrayAdapter<CharSequence> adapter_new = ArrayAdapter.createFromResource(this,
                        R.array.categories_array, android.R.layout.simple_spinner_item); // ou mettre  android.R.layout.simple_spinner_item a tester après
                // Spécifier la disposition à utiliser lorsque la liste d'options apparaît
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                // Appliquer l'adaptateur au Spinner
                categorySpinner.setAdapter(adapter);
                dateEditText.setText(memory.getDate());
                if (memory.getImageBytes() != null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(memory.getImageBytes(), 0, memory.getImageBytes().length);
                    imageView.setImageBitmap(bitmap);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    imageBytes = stream.toByteArray();

                }
                latitude = memory.getLatitude();
                longitude = memory.getLongitude();
            }
        } else {
            memory = new Memory();
        }

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageVideoPickerDialog();
            }
        });

        addLocationButton.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            } else {
                getLocation();
            }
        });

        saveButton.setOnClickListener(v -> {
            saveMemory();
        });

        Intent intent_new = getIntent();
        if (intent.hasExtra("memory_id")) {
            memoryId = intent_new.getIntExtra("memory_id", -1);
            loadMemory(memoryId);
        }
        buttonSelectVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery(false);
            }
        });
        buttonSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery(true);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            switch (requestCode) {
                case REQUEST_IMAGE_CAPTURE:
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    if (imageBitmap != null) {
                        Glide.with(this)
                                .load(imageBitmap)
                                .into(imageView);
                        imageView.setVisibility(View.VISIBLE);
                        playerView.setVisibility(View.GONE);
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        imageBytes = stream.toByteArray();
                        Log.d("MainActivity", "Image capture byte size: " + imageBytes.length);
                        videoBytes = null; // Reset videoBytes if an image is captured
                    }
                    break;

                case REQUEST_SELECT_IMAGE:
                    Uri selectedImageUri = data.getData();
                    if (selectedImageUri != null) {
                        // Utilisez Glide pour charger l'image
                        Glide.with(this)
                                .load(selectedImageUri)
                                .into(imageView);
                        imageView.setVisibility(View.VISIBLE);
                        playerView.setVisibility(View.GONE);
                        try {
                            imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                            imageBytes = stream.toByteArray();
                            Log.d("MainActivity", "Selected image byte size: " + imageBytes.length);
                            videoBytes = null; // Reset videoBytes if an image is selected
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;

                case REQUEST_SELECT_VIDEO:
                    Uri selectedVideoUri = data.getData();
                    if (selectedVideoUri != null) {
                        initializePlayer(selectedVideoUri);
                        playerView.setVisibility(View.VISIBLE);
                        imageView.setVisibility(View.GONE);
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(selectedVideoUri);
                            assert inputStream != null;
                            videoBytes = getBytes(inputStream);
                            Log.d("MainActivity", "Selected video byte size: " + videoBytes.length);
                            imageBytes = null; // Reset imageBytes if a video is selected
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;

                default:
                    Log.e("MainActivity", "Unexpected requestCode: " + requestCode);
                    break;
            }
        }
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year1, month1, dayOfMonth) -> {
                    String date = year1 + "-" + (month1 + 1) + "-" + dayOfMonth;
                    dateEditText.setText(date);
                },
                year, month, day);
        datePickerDialog.show();
    }

    private void saveMemory() {
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String category = categorySpinner.getSelectedItem().toString();
        String date = dateEditText.getText().toString();
        Memory memory = new Memory();
        memory.setTitle(title);
        memory.setDescription(description);
        memory.setCategory(category);
        memory.setImageBytes(imageBytes);
        memory.setVideoBytes(videoBytes);
        memory.setLatitude(latitude);
        memory.setLongitude(longitude);
        memory.setDate(date);
        /*double currentLatitude = m.getCurrentLatitude();
        double currentLongitude = m.getCurrentLongitude();
        memory.setLatitude(currentLatitude);
        memory.setLongitude(currentLongitude);*/

       /* debut if (memoryId == -1) {
            databaseHelper.insertMemory(memory);
        } else {
            memory.setId(memoryId);
            databaseHelper.updateMemory(memory);
        }

        finish();
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            Toast.makeText(AddEditMemoryActivity.this, "Localisation Ajoutée", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(AddEditMemoryActivity.this, "Impossible d'ajouter la Localisation", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void loadMemory(int id) {
        Memory memory = databaseHelper.getMemoryById(id);
        if (memory != null) {
            titleEditText.setText(memory.getTitle());
            descriptionEditText.setText(memory.getDescription());
            if (memory.getImageBytes() != null) {
                byte[] imageBytes = memory.getImageBytes();
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Permission non acquise", Toast.LENGTH_SHORT).show();
            }
        }

    }

    private void openGallery(boolean isImage) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(isImage ? "image/*" : "video/*");
        startActivityForResult(intent, isImage ? REQUEST_SELECT_IMAGE : REQUEST_SELECT_VIDEO);
    }

    private void showImageVideoPickerDialog() {
        String[] options = {"Prendre une Photo", "Choisir une Image", "Choisir une Vidéo"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Que voulez-vous faire ?");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // Prendre une Photo
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                        }
                        break;
                    case 1: // Choisir une Photo
                        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(pickPhotoIntent, REQUEST_SELECT_IMAGE);
                        break;
                    case 2: // Choisir une Vidéo
                        Intent pickVideoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(pickVideoIntent, REQUEST_SELECT_VIDEO);
                        break;
                }
            }
        });
        builder.show();
    }

    private byte[] toByteArray(List<Byte> byteList) {
        byte[] bytes = new byte[byteList.size()];
        for (int i = 0; i < byteList.size(); i++) {
            bytes[i] = byteList.get(i);
        }
        return bytes;
    }

    private List<Byte> toByteList(byte[] bytes) {
        List<Byte> byteList = new ArrayList<>();
        for (byte b : bytes) {
            byteList.add(b);
        }
        return byteList;
    }

    private String getPath(Uri uri) {
        String[] projection = {MediaStore.MediaColumns.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        return null;
    }

    private byte[] getBytesFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, len);
        }
        return byteArrayOutputStream.toByteArray();
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private void initializePlayer(Uri videoUri) {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        MediaItem mediaItem = MediaItem.fromUri(videoUri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
            player = null;
        }
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
} fin */
