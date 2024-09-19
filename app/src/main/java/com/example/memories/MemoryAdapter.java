package com.example.memories;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemoryAdapter extends RecyclerView.Adapter<MemoryAdapter.MemoryViewHolder>{
    private Context context;
    private List<Memory> memoryList;
    private final OnMemoryActionListener listener;

    public MemoryAdapter(Context context,List<Memory> memoryList, OnMemoryActionListener listener) {
        this.context = context;
        this.memoryList = memoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MemoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemview = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_memory, parent, false);
        return new MemoryViewHolder(itemview);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MemoryViewHolder holder, int position) {
        Memory memory = memoryList.get(position);
        holder.titleTextView.setText(memory.getTitle());
        holder.descriptionTextView.setText(memory.getDescription());
        holder.date.setText(memory.getDate());
        holder.locationTextView.setText("Lat: " + memory.getLatitude() + ", Long: " + memory.getLongitude());
        // Charge l'image et la vidéo si nécessaire
        //holder.image_View.setImageBitmap(BitmapFactory.decodeByteArray(memory.getImage(), 0, memory.getImage().length));
        byte[] imageBytes = memory.getImageBytes();
        if (imageBytes != null && imageBytes.length > 0) {
            Log.d("MemoryAdapter", "Loading image with byte size: " + imageBytes.length);
            //if (holder.itemView.getContext() != null) {
            Glide.with(holder.itemView.getContext())
                    .load(imageBytes)
                    .into(holder.image_View);
            holder.image_View.setVisibility(View.VISIBLE);
            holder.playerView.setVisibility(View.GONE);
        }else if (memory.getVideoPath() != null) {
            holder.image_View.setVisibility(View.GONE);
            holder.playerView.setVisibility(View.VISIBLE);
            Uri videoUri = Uri.parse(memory.getVideoPath());
            holder.initializePlayer(videoUri);
        }
        else {
            holder.image_View.setVisibility(View.VISIBLE);
            holder.playerView.setVisibility(View.GONE);
            Log.d("MemoryAdapter", "No image found for this memory.");
            holder.image_View.setImageResource(R.drawable.photo_add);
        }
        holder.editButton.setOnClickListener(v -> listener.onEdit(memory));
        holder.deleteButton.setOnClickListener(v -> listener.onDelete(memory));
        holder.shareButton.setOnClickListener(v -> shareMemory(memory));
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull MemoryViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.releasePlayer();

    }

    @Override
    public int getItemCount() {
        return memoryList.size();
    }

    // Méthode pour mettre à jour la liste des souvenirs
    public void setMemories(List<Memory> memories) {
        memoryList = memories;
        notifyDataSetChanged();
    }

    private void shareMemory(Memory memory) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Convertir les images et vidéos en chaînes de caractères Base64
        String imageBase64 = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            imageBase64 = memory.getImageBytes() != null ? Base64.getEncoder().encodeToString(memory.getImageBytes()) : "";
        }
        String videoBase64 = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            videoBase64 = memory.getVideoBytes() != null ? Base64.getEncoder().encodeToString(memory.getVideoBytes()) : "";
        }

        // Créer une map avec les données de memory
        Map<String, Object> memoryMap = new HashMap<>();
        memoryMap.put("id", Integer.parseInt(memory.getId()));
        memoryMap.put("title", memory.getTitle());
        memoryMap.put("description", memory.getDescription());
        memoryMap.put("image", imageBase64);
        memoryMap.put("video", videoBase64);
        memoryMap.put("date", memory.getDate());
        memoryMap.put("category", memory.getCategory());
        memoryMap.put("latitude", memory.getLatitude());
        memoryMap.put("longitude", memory.getLongitude());

        // Pousser le memory dans Firestore
        db.collection("shared_memories").document(memory.getId()).set(memoryMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Memory successfully shared!");
                    Toast.makeText(context, "Memory shared successfully!", Toast.LENGTH_SHORT).show();
                        }
                )
                .addOnFailureListener(e -> {
                    Log.w("Firestore", "Error sharing memory", e);
                    Toast.makeText(context, "Failed to share memory.", Toast.LENGTH_SHORT).show();
                });
    }



    public class MemoryViewHolder extends RecyclerView.ViewHolder implements com.example.memories.MemoryViewHolder {
    public TextView titleTextView;
    public TextView descriptionTextView;
    public ImageView image_View;
    public PlayerView playerView;
    public ExoPlayer player;
    public TextView date;
    public TextView locationTextView;
    public ImageButton editButton;
    public ImageButton deleteButton;
    public ImageButton shareButton;

        public MemoryViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.memory_title);
            descriptionTextView = itemView.findViewById(R.id.memory_description);
            image_View = itemView.findViewById(R.id.memory_image);
            playerView = itemView.findViewById(R.id.player_view);
            date = itemView.findViewById(R.id.memory_date);
            locationTextView = itemView.findViewById(R.id.location_text_view);
            editButton = itemView.findViewById(R.id.edit_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
            shareButton = itemView.findViewById(R.id.share_button);
        }
        public void bind(Memory memory) {
            titleTextView.setText(memory.getTitle());
            descriptionTextView.setText(memory.getDescription());
        }

        public void initializePlayer(Uri videoUri) {
            player = new ExoPlayer.Builder(playerView.getContext()).build();
            playerView.setPlayer(player);

            MediaItem mediaItem = MediaItem.fromUri(videoUri);
            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();
        }
        public void releasePlayer() {
            if (player != null) {
                player.release();
                player = null;
            }
        }

        /**
         *
         */
        @Override
        public void onViewDetachedFromWindow() {

        }

        /**
         * @param holder
         */
        @Override
        public void onViewDetachedFromWindow(@NonNull MemoryViewHolder holder) {

        }

        /**
         *
         */

    }

    public interface OnMemoryActionListener {
        void onEdit(Memory memory);
        void onDelete(Memory memory);
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
        memoryMap.put("id", memory.getId());
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



}


