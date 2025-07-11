# 🔨 BudgetWise Android Build Guide

Complete guide for building, signing, and distributing the BudgetWise Android application.

## 📋 Overview

BudgetWise is a native Android personal finance management application built with Java. This guide provides comprehensive instructions for setting up the development environment, building the application, and deploying it.

## 🎯 Quick Start

**TL;DR**: Download ready-to-install APKs from [GitHub Releases](https://github.com/hetpatel748/BudgetWise/releases)

### APK Status
✅ **Debug APK**: 8.8MB (signed, installable)  
✅ **Release APK**: 6.4MB (signed, installable)  
✅ **GitHub Actions**: Automated builds working  
✅ **APK Signing**: Fixed and properly configured

## 🛠️ Prerequisites

### System Requirements
- **Operating System**: Windows 10+, macOS 10.14+, or Linux (Ubuntu 18.04+ recommended)
- **RAM**: Minimum 8GB, 16GB recommended
- **Storage**: At least 10GB free space for Android SDK and build artifacts
- **Internet**: Required for downloading dependencies

### Required Software
- **Java Development Kit (JDK)**: Version 17 (recommended) or 11
- **Android SDK**: API Level 34 with build-tools 34.0.0
- **Git**: For version control

## 🚀 Environment Setup

### 1. Install Java JDK

#### Ubuntu/Debian:
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

#### macOS (using Homebrew):
```bash
brew install openjdk@17
```

#### Windows:
Download and install from [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://adoptium.net/)

### 2. Install Android SDK

#### Option A: Android Studio (Recommended)
1. Download [Android Studio](https://developer.android.com/studio)
2. Install and run the setup wizard
3. Install SDK Platform 34 and Build Tools 34.0.0

#### Option B: Command Line Tools Only
```bash
# Download command line tools
wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
unzip commandlinetools-linux-9477386_latest.zip
mkdir -p /opt/android-sdk/cmdline-tools
mv cmdline-tools /opt/android-sdk/cmdline-tools/latest

# Install required components
/opt/android-sdk/cmdline-tools/latest/bin/sdkmanager "platforms;android-34"
/opt/android-sdk/cmdline-tools/latest/bin/sdkmanager "build-tools;34.0.0"
/opt/android-sdk/cmdline-tools/latest/bin/sdkmanager "platform-tools"
```

### 3. Set Environment Variables

Add these to your shell profile (`~/.bashrc`, `~/.zshrc`, etc.):

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64  # Adjust path as needed
export ANDROID_HOME=/opt/android-sdk                  # Adjust path as needed
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/build-tools/34.0.0
```

### 4. Accept Android SDK Licenses
```bash
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
```

## 📁 Project Setup

### 1. Clone the Repository
```bash
git clone https://github.com/hetpatel748/BudgetWise.git
cd BudgetWise
```

### 2. Create local.properties
```bash
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

### 3. Make Gradle Wrapper Executable
```bash
chmod +x gradlew
```

## 🔨 Building the Application

### Debug Build
```bash
./gradlew assembleDebug
```
- **Output**: `app/build/outputs/apk/debug/app-debug.apk`
- **Size**: ~8.3MB
- **Features**: Debugging enabled, no obfuscation
- **Package ID**: `com.budgetwise.debug`

### Release Build
```bash
./gradlew assembleRelease
```
- **Output**: `app/build/outputs/apk/release/app-release-unsigned.apk`
- **Size**: ~3.1MB (optimized with ProGuard)
- **Features**: Optimized, obfuscated, production-ready
- **Package ID**: `com.budgetwise`

### Clean Build
```bash
./gradlew clean
./gradlew assembleDebug assembleRelease
```

## 🔐 Code Signing (Production)

### 1. Generate Keystore
```bash
keytool -genkey -v -keystore budgetwise-release-key.keystore \
        -alias budgetwise -keyalg RSA -keysize 2048 -validity 10000
```

### 2. Configure Signing in build.gradle
```gradle
android {
    signingConfigs {
        release {
            storeFile file('budgetwise-release-key.keystore')
            storePassword 'your_store_password'
            keyAlias 'budgetwise'
            keyPassword 'your_key_password'
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            // ... other configurations
        }
    }
}
```

### 3. Build Signed APK
```bash
./gradlew assembleRelease
```

**⚠️ Security Note**: Never commit keystore files or passwords to version control!

## 🧪 Testing & Quality Assurance

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Run Lint Checks
```bash
./gradlew lint
```

### Run All Checks
```bash
./gradlew check
```

## 📊 Build Analysis

### APK Analysis
```bash
# Analyze APK contents
$ANDROID_HOME/build-tools/34.0.0/aapt dump badging app/build/outputs/apk/debug/app-debug.apk

# Check APK size breakdown
$ANDROID_HOME/build-tools/34.0.0/aapt list -v app/build/outputs/apk/debug/app-debug.apk
```

### Build Performance
- **Debug Build Time**: ~6 seconds
- **Release Build Time**: ~60 seconds (due to ProGuard optimization)
- **Incremental Build**: ~2-3 seconds

## 🚀 CI/CD with GitHub Actions

The project includes automated CI/CD pipeline that:

1. **Builds** both debug and release APKs
2. **Tests** the application with unit tests
3. **Analyzes** code with lint checks
4. **Uploads** APK artifacts
5. **Creates** GitHub releases automatically

### Workflow Triggers
- Push to `main` or `develop` branches
- Pull requests to `main`
- Manual workflow dispatch

### Artifacts Generated
- Debug APK
- Release APK
- Lint reports
- Test reports

## 📱 Installation & Testing

### Install Debug APK
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Install Release APK
```bash
adb install app/build/outputs/apk/release/app-release-unsigned.apk
```

### Enable Unknown Sources
For manual installation on device:
1. Go to Settings > Security
2. Enable "Unknown sources" or "Install unknown apps"
3. Install the APK file

## 🐛 Common Issues & Solutions

### Issue: "SDK location not found"
**Solution**: Ensure `local.properties` exists with correct `sdk.dir` path

### Issue: "Permission denied: ./gradlew"
**Solution**: Run `chmod +x gradlew`

### Issue: "Failed to find Build Tools revision"
**Solution**: Install correct build tools version:
```bash
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "build-tools;34.0.0"
```

### Issue: "R8 compilation failed"
**Solution**: Check ProGuard rules in `proguard-rules.pro` and add missing keep rules

### Issue: "Unsupported Java version"
**Solution**: Ensure Java 17 or 11 is installed and `JAVA_HOME` is set correctly

## 📈 Performance Optimization

### Gradle Performance
Add to `gradle.properties`:
```properties
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.configureondemand=true
org.gradle.jvmargs=-Xmx4g -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
```

### Build Variants
The project supports multiple build variants:
- **Debug**: Development builds with debugging enabled
- **Release**: Production builds with optimization

## 🔄 Version Management

### Update Version
Edit `app/build.gradle`:
```gradle
defaultConfig {
    versionCode 2
    versionName "1.1.0"
}
```

### Semantic Versioning
- **Major**: Breaking changes (e.g., 1.0.0 → 2.0.0)
- **Minor**: New features (e.g., 1.0.0 → 1.1.0)
- **Patch**: Bug fixes (e.g., 1.0.0 → 1.0.1)

## 📚 Additional Resources

- [Android Developer Documentation](https://developer.android.com/docs)
- [Gradle Build Tool](https://gradle.org/guides/)
- [ProGuard Manual](https://www.guardsquare.com/manual/configuration)
- [Material Design Guidelines](https://material.io/design)

## 🔐 APK Signing Troubleshooting

### ❌ Common APK Installation Errors

#### "App not installed as package appears to be invalid"

**Root Cause**: APK is unsigned or corrupted  
**Solution**: 
```bash
# Verify APK is signed
jarsigner -verify -verbose app/build/outputs/apk/release/app-release.apk

# If unsigned, rebuild with proper signing
./gradlew clean assembleRelease
```

#### "INSTALL_FAILED_UPDATE_INCOMPATIBLE"

**Root Cause**: Different signing keys between installed and new APK  
**Solution**: 
```bash
# Uninstall existing app first
adb uninstall com.budgetwise.app

# Then install new APK
adb install app/build/outputs/apk/release/app-release.apk
```

#### "INSTALL_FAILED_INVALID_APK"

**Root Cause**: APK file corruption or invalid format  
**Solution**: 
```bash
# Check APK integrity
aapt dump badging app/build/outputs/apk/release/app-release.apk

# Rebuild if corrupted
./gradlew clean assembleRelease
```

### ✅ APK Verification Commands

```bash
# Check APK signature
jarsigner -verify -verbose -certs app-release.apk

# View APK contents
aapt dump badging app-release.apk

# Check APK size and compression
unzip -l app-release.apk

# Verify APK can be parsed
aapt dump xmltree app-release.apk AndroidManifest.xml
```

### 🔧 Signing Configuration Fix

If you encounter signing issues, ensure your `app/build.gradle` has:

```gradle
android {
    signingConfigs {
        debug {
            storeFile file('debug.keystore')
            storePassword 'android'
            keyAlias 'androiddebugkey'
            keyPassword 'android'
        }
        release {
            storeFile file('debug.keystore')
            storePassword 'android'
            keyAlias 'androiddebugkey'
            keyPassword 'android'
        }
    }

    buildTypes {
        debug {
            signingConfig signingConfigs.debug
        }
        release {
            signingConfig signingConfigs.release
        }
    }
}
```

### 🛡️ Security Best Practices

1. **Never commit keystores** to version control
2. **Use different keystores** for debug and release
3. **Store production keystores** securely
4. **Backup keystores** - losing them means you can't update your app
5. **Use strong passwords** for production keystores

## 🆘 Support

For build issues or questions:
1. Check this documentation
2. Review [GitHub Issues](https://github.com/hetpatel748/BudgetWise/issues)
3. Check [GitHub Actions logs](https://github.com/hetpatel748/BudgetWise/actions)
4. Check Android Developer documentation
5. Create a new issue with detailed error logs

---

**Last Updated**: July 2025  
**Build System**: Gradle 8.2  
**Android Gradle Plugin**: 8.2.0  
**Target SDK**: 34 (Android 14)