# BudgetWise - Personal Finance Manager

A secure, offline personal finance manager with AI-powered insights built with Java for Android.

## Features

- 🔒 **AES-256 Encryption** - Bank-level security for your financial data
- 🤖 **Local AI Intelligence** - Smart categorization and spending insights
- 📱 **Material 3 UI** - Premium design with dark/light themes
- 📊 **Advanced Analytics** - Charts and spending pattern analysis
- 💾 **Secure Backup** - Encrypted local backups
- 🔐 **Biometric Auth** - Fingerprint and face unlock
- 📈 **Budget Tracking** - Set and monitor spending limits
- 🔔 **Smart Notifications** - Budget alerts and reminders

## 🚀 Quick Start

### Download APK
- **Latest Release**: [Download from GitHub Releases](https://github.com/hetpatel748/BudgetWise/releases)
- **Debug Build**: Available in GitHub Actions artifacts
- **Minimum Android**: 7.0 (API 24)
- **Target Android**: 14 (API 34)

### Build from Source

#### Prerequisites
- **Java JDK**: 17 (recommended) or 11
- **Android SDK**: API level 34 with build-tools 34.0.0
- **Git**: For cloning the repository

#### Quick Build
```bash
git clone https://github.com/hetpatel748/BudgetWise.git
cd BudgetWise
echo "sdk.dir=/path/to/android-sdk" > local.properties
chmod +x gradlew
./gradlew assembleDebug
```

#### Build Results
- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk` (~8.3MB)
- **Release APK**: `app/build/outputs/apk/release/app-release-unsigned.apk` (~3.1MB)

📖 **Detailed build instructions**: See [BUILD.md](BUILD.md)

## Project Structure

\`\`\`
app/
├── src/main/java/com/budgetwise/
│   ├── data/
│   │   ├── models/          # Data models (Transaction, Budget)
│   │   ├── repository/      # Data repository layer
│   │   └── storage/         # Secure storage implementation
│   ├── security/            # Encryption and biometric auth
│   ├── ml/                  # Local intelligence service
│   ├── ui/
│   │   ├── dashboard/       # Dashboard fragment and ViewModel
│   │   ├── transactions/    # Transaction management
│   │   ├── analytics/       # Charts and analytics
│   │   ├── settings/        # App settings
│   │   ├── adapters/        # RecyclerView adapters
│   │   └── views/           # Custom views (charts)
│   ├── utils/               # Utility classes
│   ├── notifications/       # Notification system
│   └── backup/              # Backup management
└── src/main/res/
    ├── layout/              # XML layouts
    ├── values/              # Strings, colors, themes
    ├── drawable/            # Icons and graphics
    └── xml/                 # Configuration files
\`\`\`

## Security Features

- **AES-256-GCM Encryption** with Android Keystore
- **HMAC verification** for data integrity
- **Biometric authentication** support
- **No network permissions** - completely offline
- **Secure backup** with encrypted exports
- **ProGuard obfuscation** in release builds

## Performance Optimizations

- **Background processing** for all heavy operations
- **RecyclerView with DiffUtil** for efficient updates
- **ViewBinding** throughout the app
- **Memory leak prevention** with proper lifecycle management
- **R8 code shrinking** and optimization

## Testing

\`\`\`bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Generate test coverage report
./gradlew jacocoTestReport
\`\`\`

## Building for Release

1. **Generate signing key**
   \`\`\`bash
   keytool -genkey -v -keystore budgetwise-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias budgetwise
   \`\`\`

2. **Configure signing in `app/build.gradle`**
   ```gradle
   android {
       signingConfigs {
           release {
               storeFile file('budgetwise-release-key.jks')
               storePassword 'your_store_password'
               keyAlias 'budgetwise'
               keyPassword 'your_key_password'
           }
       }
       buildTypes {
           release {
               signingConfig signingConfigs.release
               // ... other config
           }
       }
   }
   \`\`\`

3. **Build release APK**
   \`\`\`bash
   ./gradlew assembleRelease
   \`\`\`

## Troubleshooting

### Common Issues

1. **SDK not found**
   - Ensure `local.properties` has correct SDK path
   - Install required SDK components in Android Studio

2. **Build fails with dependency errors**
   - Clean and rebuild: `./gradlew clean build`
   - Sync project with Gradle files in Android Studio

3. **Biometric authentication not working**
   - Test on physical device (emulator may not support biometrics)
   - Ensure device has biometric authentication set up

4. **Charts not displaying**
   - Check if custom views are properly initialized
   - Verify data is being passed to chart components

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Privacy

BudgetWise is designed with privacy in mind:
- **No data collection** - all data stays on your device
- **No network access** - completely offline operation
- **No analytics** - no tracking or telemetry
- **Open source** - transparent and auditable code
\`\`\`

Finally, let's add a .gitignore file:
