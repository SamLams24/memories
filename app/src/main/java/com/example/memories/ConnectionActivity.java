package com.example.memories;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ConnectionActivity extends AppCompatActivity {

    private EditText emailField;
    private EditText passwordField;
    private Button loginButton;
    private TextView registerText;
    private MemoryDatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connection_activity);

        db = new MemoryDatabaseHelper(this);
        // Configurer les champs et boutons de l'interface utilisateur pour l'authentification
        emailField = findViewById(R.id.mail);
        passwordField = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);
        registerText = findViewById(R.id.registerText);

        loginButton.setOnClickListener(view -> {
            String email = emailField.getText().toString();
            String password = passwordField.getText().toString();
            if (email.equals("") && password.equals("")) {
                Toast.makeText(ConnectionActivity.this, "Veuillez rentrer vos Informations", Toast.LENGTH_SHORT).show();
            }
            else if (email.equals("")) {
                Toast.makeText(ConnectionActivity.this, "Veuillez entrer votre E-mail", Toast.LENGTH_SHORT).show();
            }
            else if (password.equals("")) {
                Toast.makeText(ConnectionActivity.this, "Veuillez entrer un mot de passe", Toast.LENGTH_SHORT).show();
            }
            else if (db.checkUser(email, password)) {
                Toast.makeText(ConnectionActivity.this, "Connexion réussie", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ConnectionActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(ConnectionActivity.this, "Mot de passe ou E-mail Invalide", Toast.LENGTH_SHORT).show();
            }
        });
        registerText.setOnClickListener(view -> {
            Intent intent = new Intent(ConnectionActivity.this, CcompteActivity.class);
            startActivity(intent);
        });
    }
    public void retour(View view) {
        finish(); // Cette ligne permet de fermer l'activité actuelle et de revenir à l'activité précédente
    }

}
