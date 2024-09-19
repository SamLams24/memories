package com.example.memories;

import android.provider.BaseColumns;
public class MemoryContract {
    // Constructeur privé pour éviter l'instanciation
    private MemoryContract() {}

    // Définit les noms des tables et des colonnes
    public static class MemoryEntry implements BaseColumns {
        public static final String TABLE_NAME = "memory";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_IMAGE = "image";
        public static final String COLUMN_VIDEOPATH = "video_path";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_CATEGORY = "category";
        public static final String COLUMN_LATITUDE = "latitude";
        public static final String COLUMN_LONGITUDE = "longitude";



        // Ajoute d'autres colonnes au besoin
    }
}
