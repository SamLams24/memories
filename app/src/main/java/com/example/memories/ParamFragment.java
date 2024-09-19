package com.example.memories;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

public class ParamFragment extends Fragment {

    private ImageView backBtn;
    private AppCompatButton editProfileBtn;
    private SwitchCompat notificationSwitch;
    private SwitchCompat nightModeSwitch;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private SwitchCompat privateAccountSwitch;


    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_param, container, false);




    }


}