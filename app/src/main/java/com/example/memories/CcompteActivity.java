package com.example.memories;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CcompteActivity extends AppCompatActivity {
    private EditText registerEmailField;
    private EditText registerPasswordField;
    private EditText name_c;
    private EditText pseudo_c;
    private Button registerButton;
    private MemoryDatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ccompte_activity);

        db = new MemoryDatabaseHelper(this);
        registerEmailField = findViewById(R.id.edit_email);
        registerPasswordField = findViewById(R.id.password);
        name_c = findViewById(R.id.name);
        pseudo_c = findViewById(R.id.edit_pseudo);
        registerButton = findViewById(R.id.inscrit);

        registerButton.setOnClickListener(view -> {
            String email = registerEmailField.getText().toString();
            String password = registerPasswordField.getText().toString();
            String name = name_c.getText().toString();
            String pseudo = pseudo_c.getText().toString();
            if (name.equals("") & pseudo.equals("") & email.equals("")) {
                Toast.makeText(CcompteActivity.this, "Veuillez remplir les Champs", Toast.LENGTH_SHORT).show();
            }
            else if (name.equals("")) {
                Toast.makeText(CcompteActivity.this, "Veuillez rentrer votre nom Complet", Toast.LENGTH_SHORT).show();
            }
            else if (pseudo.equals("")) {
                Toast.makeText(CcompteActivity.this, "Veuillez rentrer un pseudo", Toast.LENGTH_SHORT).show();
            }
            else if (email.equals("")) {
                Toast.makeText(CcompteActivity.this, "Veuillez rentrer un E-mail", Toast.LENGTH_SHORT).show();
            }
            else if (password.equals("")) {
                Toast.makeText(CcompteActivity.this, "Veuillez rentrer un Mot de passe", Toast.LENGTH_SHORT).show();
            }
            else if (db.insertUser(email, password, name, pseudo)) {
                Toast.makeText(CcompteActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(CcompteActivity.this, ConnectionActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(CcompteActivity.this, "Registration Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void retour(View view) {
        finish(); // Cette ligne permet de fermer l'activité actuelle et de revenir à l'activité précédente
    }
}