package com.budgetwise.security;

import android.content.Context;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;

public class BiometricManager {
    private final Context context;
    private BiometricPrompt biometricPrompt;

    public interface BiometricCallback {
        void onSuccess();
        void onError(String error);
        void onCancel();
    }

    public BiometricManager(Context context) {
        this.context = context;
    }

    public boolean isBiometricAvailable() {
        androidx.biometric.BiometricManager biometricManager = 
            androidx.biometric.BiometricManager.from(context);
        
        switch (biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            case androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS:
                return true;
            case androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
            case androidx.biometric.BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
            case androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
            default:
                return false;
        }
    }

    public void authenticate(FragmentActivity activity, BiometricCallback callback) {
        if (!isBiometricAvailable()) {
            callback.onError("Biometric authentication not available");
            return;
        }

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authenticate to access BudgetWise")
            .setSubtitle("Use your fingerprint or face to unlock")
            .setNegativeButtonText("Cancel")
            .build();

        biometricPrompt = new BiometricPrompt(activity, 
            ContextCompat.getMainExecutor(context),
            new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                        callback.onCancel();
                    } else {
                        callback.onError(errString.toString());
                    }
                }

                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    callback.onSuccess();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    callback.onError("Authentication failed");
                }
            });

        biometricPrompt.authenticate(promptInfo);
    }
}
