Summary of What Was Resolved
1.
Gradle Build System & AGP Upgrade
◦
Upgraded build scripts to Kotlin DSL ( build.gradle.kts &  app/build.gradle.kts).
◦
Upgraded AGP to 9.5.1 and Gradle Wrapper to 9.6.0.
◦
Migrated legacy com.android.support dependencies to AndroidX (androidx.appcompat:appcompat:1.6.1).
2.
IDE Integration
◦
Configured  .gradle/config.properties and  .idea/gradle.xml to point to the valid local JDK (C:/Program Files/Java/jdk-24).
◦
Verified both IDE Sync and IDE Build / Run succeed cleanly.
3.
Android 12+ Permission Fix
◦
Fixed the SecurityException crash when pressing Scan by adding proper Android 12+ (BLUETOOTH_SCAN, BLUETOOTH_CONNECT, ACCESS_FINE_LOCATION) runtime permission requests in  ManagerActivity.java.
4.
Runtime Verification
◦
The app compiles, deploys, launches, and performs BLE/BT device scanning without any crashes.

The project has been upgraded to be fully compatible with Android 15 (API level 35).
Upgrades Implemented
1.
SDK Target Updates
◦
Updated compileSdk = 35 and targetSdk = 35 in  app/build.gradle.kts.
◦
Updated AndroidX dependencies to androidx.appcompat:appcompat:1.7.0 and androidx.core:core:1.13.1.
2.
Android 15 Edge-to-Edge Compliance
◦
Configured ViewCompat.setOnApplyWindowInsetsListener in  ManagerActivity.java and  PeripheralActivity.java to handle system bar insets (WindowInsetsCompat.Type.systemBars()) so content is inset properly and not obscured by status/navigation bars under Android 15's default edge-to-edge enforcement.
3.
Verification
◦
IDE Gradle Sync: Successful
◦
Gradle Build: Successful
◦
Runtime & Deployment: Deployed and verified running on device target API 35 with active Bluetooth device scanning (Wearable_8690i).

Documented Highlights
•
Overview & Maintainer Info: Credited maintainer voryxt@gmail.com and Honeywell 8690i wearable scanner integration.
•
Build System Modernization: Documented migration to Kotlin DSL ( settings.gradle.kts,  build.gradle.kts,  app/build.gradle.kts), AGP 9.5.1, Gradle 9.6.0, AndroidX migration, and Java 17 toolchain.
•
Android 15 & Android 12+ Compatibility: Detailed dynamic runtime permission handling (BLUETOOTH_SCAN, BLUETOOTH_CONNECT, ACCESS_FINE_LOCATION), explicit android:exported activity tags, AGP non-final resource ID refactoring, and Android 15 edge-to-edge window insets handling (ViewCompat.setOnApplyWindowInsetsListener).
•
Build & Run Instructions: Included step-by-step instructions for command-line builds (.\gradlew.bat assembleDebug) and Android Studio IDE execution.
•
Project Structure Map: Included a visual directory tree of the application modules, SDK libraries, and main components.
