package com.budgetwise.data.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.budgetwise.security.EncryptionManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SecurePreferences {
    private static final String TAG = "SecurePreferences";
    private static final String PREFS_NAME = "BudgetWiseSecurePrefs";
    
    private final SharedPreferences preferences;
    private final EncryptionManager encryptionManager;
    private final Gson gson;

    public SecurePreferences(Context context, EncryptionManager encryptionManager) {
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.encryptionManager = encryptionManager;
        this.gson = new Gson();
    }

    public void putString(String key, String value) {
        try {
            String encryptedValue = encryptionManager.encrypt(value);
            preferences.edit().putString(key, encryptedValue).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to store encrypted string", e);
        }
    }

    public String getString(String key, String defaultValue) {
        try {
            String encryptedValue = preferences.getString(key, null);
            if (encryptedValue == null) {
                return defaultValue;
            }
            return encryptionManager.decrypt(encryptedValue);
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve encrypted string", e);
            return defaultValue;
        }
    }

    public <T> void putObject(String key, T object) {
        try {
            String json = gson.toJson(object);
            putString(key, json);
        } catch (Exception e) {
            Log.e(TAG, "Failed to store object", e);
        }
    }

    public <T> T getObject(String key, Class<T> classType, T defaultValue) {
        try {
            String json = getString(key, null);
            if (json == null) {
                return defaultValue;
            }
            return gson.fromJson(json, classType);
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve object", e);
            return defaultValue;
        }
    }

    public <T> void putList(String key, List<T> list) {
        try {
            String json = gson.toJson(list);
            putString(key, json);
        } catch (Exception e) {
            Log.e(TAG, "Failed to store list", e);
        }
    }

    public <T> List<T> getList(String key, Type listType) {
        try {
            String json = getString(key, null);
            if (json == null) {
                return new ArrayList<>();
            }
            return gson.fromJson(json, listType);
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve list", e);
            return new ArrayList<>();
        }
    }

    public void putBoolean(String key, boolean value) {
        putString(key, String.valueOf(value));
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    public void putLong(String key, long value) {
        putString(key, String.valueOf(value));
    }

    public long getLong(String key, long defaultValue) {
        String value = getString(key, String.valueOf(defaultValue));
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void putDouble(String key, double value) {
        putString(key, String.valueOf(value));
    }

    public double getDouble(String key, double defaultValue) {
        String value = getString(key, String.valueOf(defaultValue));
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void remove(String key) {
        preferences.edit().remove(key).apply();
    }

    public void clear() {
        preferences.edit().clear().apply();
    }

    public boolean contains(String key) {
        return preferences.contains(key);
    }
}
