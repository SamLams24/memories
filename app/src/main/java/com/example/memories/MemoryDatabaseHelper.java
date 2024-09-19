package com.example.memories;

import static java.lang.Integer.parseInt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MemoryDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "memory.db";
    private static final int DATABASE_VERSION = 1;
    private DatabaseReference databaseReference;

    //@Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version
    public MemoryDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        databaseReference = FirebaseDatabase.getInstance().getReference("memories");
    }

    // Définis la requête SQL pour créer la table des souvenirs
    private static final String SQL_CREATE_TABLE_MEMORY =
            "CREATE TABLE " + MemoryContract.MemoryEntry.TABLE_NAME + " (" +
                    MemoryContract.MemoryEntry._ID + " INTEGER PRIMARY KEY," +
                    MemoryContract.MemoryEntry.COLUMN_TITLE + " TEXT," +
                    MemoryContract.MemoryEntry.COLUMN_DESCRIPTION + " TEXT," +
                    MemoryContract.MemoryEntry.COLUMN_IMAGE + " BLOB," +
                    MemoryContract.MemoryEntry.COLUMN_VIDEOPATH + " TEXT," +
                    MemoryContract.MemoryEntry.COLUMN_CATEGORY + " TEXT," +
                    MemoryContract.MemoryEntry.COLUMN_LATITUDE + " REAL," + // Nouvelle colonne
                    MemoryContract.MemoryEntry.COLUMN_LONGITUDE + " REAL," +
                    MemoryContract.MemoryEntry.COLUMN_DATE + " DATETIME)"; // Nouvelle colonne// Nouvelle colonne
    //MemoryContract.MemoryEntry.COLUMN_LATITUDE + " REAL," + Nouvelle colonne
    //                    MemoryContract.MemoryEntry.COLUMN_LONGITUDE + " REAL ...MemoryContract.MemoryEntry.COLUMN_CATEGORY + " TEXT

    // Méthode appelée lors de la création de la base de données
    private static final String SQL_CREATE_TABLE_USERS =
            "CREATE TABLE users (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "EMAIL TEXT," +
                    "PASSWORD TEXT," +
                        "NAME," +
                    "PSEUDO)";

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(SQL_CREATE_TABLE_MEMORY);
        db.execSQL(SQL_CREATE_TABLE_USERS);
    }

    // Méthode appelée lors de la mise à jour de la base de données
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + MemoryContract.MemoryEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }

    // Méthode pour insérer un utilisateur
    public boolean insertUser(String email, String password,String name,String pseudo) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("EMAIL", email);
        contentValues.put("PASSWORD", password);
        contentValues.put("NAME", name);
        contentValues.put("PSEUDO", pseudo);
        long result = db.insert("users", null, contentValues);
        return result != -1;
    }
    // Méthode pour vérifier un utilisateur
    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE EMAIL=? AND PASSWORD=?", new String[]{email, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }


    // Méthode pour insérer un nouveau souvenir
    public long insertMemory(Memory memory) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(MemoryContract.MemoryEntry.COLUMN_TITLE, memory.getTitle());
        values.put(MemoryContract.MemoryEntry.COLUMN_DESCRIPTION, memory.getDescription());
        values.put(MemoryContract.MemoryEntry.COLUMN_IMAGE, memory.getImageBytes());
        values.put(MemoryContract.MemoryEntry.COLUMN_VIDEOPATH, memory.getVideoPath());
        values.put(MemoryContract.MemoryEntry.COLUMN_DATE, memory.getDate());
        values.put(MemoryContract.MemoryEntry.COLUMN_CATEGORY, memory.getCategory());
        values.put(MemoryContract.MemoryEntry.COLUMN_LATITUDE, memory.getLatitude());
        values.put(MemoryContract.MemoryEntry.COLUMN_LONGITUDE, memory.getLongitude());

        long newRowId = db.insert(MemoryContract.MemoryEntry.TABLE_NAME, null, values);
        Log.d("DatabaseHelper", "Memory inserted with ID: " +  MemoryContract.MemoryEntry._ID);

        db.close();
        return newRowId;
    }

    // Méthode pour lire tous les souvenirs
    public List<Memory> getAllMemories() {
        List<Memory> memoryList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT  * FROM " + MemoryContract.MemoryEntry.TABLE_NAME;
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Memory memory = new Memory();
                memory.setId(cursor.getInt(cursor.getColumnIndexOrThrow(MemoryContract.MemoryEntry._ID)));
                memory.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(MemoryContract.MemoryEntry.COLUMN_TITLE)));
                memory.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(MemoryContract.MemoryEntry.COLUMN_DESCRIPTION)));
                int imageColumnIndex = cursor.getColumnIndexOrThrow(MemoryContract.MemoryEntry.COLUMN_IMAGE);
                if (!cursor.isNull(imageColumnIndex)) {
                    byte[] imageBytes = cursor.getBlob(imageColumnIndex);
                    memory.setImageBytes(imageBytes);
                }
                memory.setVideoPath(cursor.getString(cursor.getColumnIndexOrThrow(MemoryContract.MemoryEntry.COLUMN_VIDEOPATH)));
                memory.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(MemoryContract.MemoryEntry.COLUMN_CATEGORY)));
                if (!cursor.isNull(cursor.getColumnIndexOrThrow(MemoryContract.MemoryEntry.COLUMN_LATITUDE))) {
                    memory.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(MemoryContract.MemoryEntry.COLUMN_LATITUDE)));
                }
                if (!cursor.isNull(cursor.getColumnIndexOrThrow(MemoryContract.MemoryEntry.COLUMN_LONGITUDE))) {
                    memory.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(MemoryContract.MemoryEntry.COLUMN_LONGITUDE)));
                }
                memory.setDate(cursor.getString(cursor.getColumnIndexOrThrow(MemoryContract.MemoryEntry.COLUMN_DATE)));
                memoryList.add(memory);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return memoryList;
    }
    // Méthode pour mettre à jour un souvenir
    public int updateMemory(Memory memory) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(MemoryContract.MemoryEntry.COLUMN_TITLE, memory.getTitle());
        values.put(MemoryContract.MemoryEntry.COLUMN_DESCRIPTION, memory.getDescription());
        values.put(MemoryContract.MemoryEntry.COLUMN_IMAGE, memory.getImageBytes());
        values.put(MemoryContract.MemoryEntry.COLUMN_VIDEOPATH, memory.getVideoPath());
        values.put(MemoryContract.MemoryEntry.COLUMN_DATE, memory.getDate());

        int count = db.update(MemoryContract.MemoryEntry.TABLE_NAME, values, MemoryContract.MemoryEntry._ID + " = ?",
                new String[]{String.valueOf(memory.getId())});
        db.close();
        return count;
    }
    // Méthode pour supprimer un souvenir
    public void deleteMemory(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(MemoryContract.MemoryEntry.TABLE_NAME, MemoryContract.MemoryEntry._ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
    }

    // Ajout de la méthode pour récupérer un souvenir par son identifiant
    public Memory getMemoryById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT  * FROM " + MemoryContract.MemoryEntry.TABLE_NAME + " WHERE " +
                MemoryContract.MemoryEntry._ID + " = ?";
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(id)});

        if (cursor != null) {
                cursor.moveToFirst();
                Memory memory = new Memory();
                memory.setId(cursor.getInt(cursor.getColumnIndexOrThrow(MemoryContract.MemoryEntry._ID)));
                memory.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(MemoryContract.MemoryEntry.COLUMN_TITLE)));
                memory.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(MemoryContract.MemoryEntry.COLUMN_DESCRIPTION)));
                memory.setImageBytes(cursor.getBlob(cursor.getColumnIndexOrThrow(MemoryContract.MemoryEntry.COLUMN_IMAGE)));
                memory.setVideoPath(cursor.getString(cursor.getColumnIndexOrThrow(MemoryContract.MemoryEntry.COLUMN_VIDEOPATH)));
                memory.setDate(cursor.getString(cursor.getColumnIndexOrThrow(MemoryContract.MemoryEntry.COLUMN_DATE)));
                cursor.close();
                db.close();
                return memory;
        } else {
            return null;
        }
    }
    public void addMemory(@NonNull Memory memory) {
        String memoryId = databaseReference.push().getKey();
        memory.setId(Integer.parseInt(memoryId));
        assert memoryId != null;
        databaseReference.child(memoryId).setValue(memory);
    }
    public void getAllMemories(ValueEventListener listener) {
        databaseReference.addValueEventListener(listener);
    }
}
