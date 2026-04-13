# Patch Notifier Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a single-screen Android app that tracks estrogen patch placement across 4 thigh locations, auto-suggests rotation, and sends persistent nagging notifications every 7 days until patches are replaced.

**Architecture:** Single-activity Compose app. Room DB stores 4 patch rows (current state only). AlarmManager schedules exact 7-day alarms and repeating nag alarms. A BroadcastReceiver handles alarm firing, notification display, and boot recovery.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Room, AlarmManager, NotificationManager. Min SDK 31. Gradle Kotlin DSL.

---

## File Structure

```
app/
├── build.gradle.kts                          # App-level build config with Room, Compose deps
├── src/main/
│   ├── AndroidManifest.xml                   # Activity, receivers, permissions
│   ├── java/com/patch/notifier/
│   │   ├── PatchApp.kt                       # Application class — creates notification channel
│   │   ├── MainActivity.kt                   # Single activity, hosts Compose content
│   │   ├── data/
│   │   │   ├── Patch.kt                      # Room entity
│   │   │   ├── PatchDao.kt                   # Room DAO
│   │   │   └── PatchDatabase.kt              # Room database singleton
│   │   ├── alarm/
│   │   │   ├── AlarmScheduler.kt             # Schedules/cancels exact alarms and nag alarms
│   │   │   └── AlarmReceiver.kt              # BroadcastReceiver — fires notifications, schedules nags
│   │   ├── boot/
│   │   │   └── BootReceiver.kt               # Re-schedules alarms after reboot
│   │   └── ui/
│   │       ├── PatchScreen.kt                # Main Compose screen (status, grid, button)
│   │       └── Theme.kt                      # Dark theme colors
│   └── res/
│       └── values/
│           └── strings.xml                   # App name
├── src/test/java/com/patch/notifier/
│   ├── RotationLogicTest.kt                  # Unit tests for rotation suggestion
│   └── AlarmSchedulerTest.kt                 # Unit tests for alarm intent creation
├── src/androidTest/java/com/patch/notifier/
│   └── PatchDaoTest.kt                       # Instrumented Room tests
build.gradle.kts                              # Project-level build config
settings.gradle.kts                           # Project settings
gradle.properties                             # Gradle config
```

---

### Task 1: Scaffold Android Project

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts` (project-level)
- Create: `app/build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/patch/notifier/PatchApp.kt`
- Create: `app/src/main/java/com/patch/notifier/MainActivity.kt`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/java/com/patch/notifier/ui/Theme.kt`

- [ ] **Step 1: Create Gradle wrapper**

Run:
```bash
cd /home/jlyons/PycharmProjects/patch-notifier
gradle wrapper --gradle-version 8.7
```

If `gradle` is not installed globally, download the wrapper manually:
```bash
mkdir -p gradle/wrapper
cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF
```

Then fetch the wrapper jar and scripts — or copy from an existing Android project if available.

- [ ] **Step 2: Create settings.gradle.kts**

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolution {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "patch-notifier"
include(":app")
```

- [ ] **Step 3: Create project-level build.gradle.kts**

```kotlin
// build.gradle.kts
plugins {
    id("com.android.application") version "8.4.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("com.google.devtools.ksp") version "2.0.0-1.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
}
```

- [ ] **Step 4: Create gradle.properties**

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 5: Create app/build.gradle.kts**

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.patch.notifier"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.patch.notifier"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.05.00")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Core
    implementation("androidx.core:core-ktx:1.13.1")

    // Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

- [ ] **Step 6: Create AndroidManifest.xml**

```xml
<!-- app/src/main/AndroidManifest.xml -->
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <application
        android:name=".PatchApp"
        android:allowBackup="false"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <receiver
            android:name=".alarm.AlarmReceiver"
            android:exported="false" />

        <receiver
            android:name=".boot.BootReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

- [ ] **Step 7: Create strings.xml**

```xml
<!-- app/src/main/res/values/strings.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Patch Notifier</string>
</resources>
```

- [ ] **Step 8: Create Theme.kt**

```kotlin
// app/src/main/java/com/patch/notifier/ui/Theme.kt
package com.patch.notifier.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF1A1A2E)
val NavyLight = Color(0xFF252547)
val Blue = Color(0xFF3D5AFE)
val BlueBright = Color(0xFF6979F8)
val TextPrimary = Color(0xFFE0E0FF)
val TextSecondary = Color(0xFF8B8BA7)
val TextMuted = Color(0xFF6C6C8A)
val BorderColor = Color(0xFF4A4A7A)

private val DarkColors = darkColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    background = Navy,
    surface = NavyLight,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = BorderColor,
    onSurfaceVariant = TextSecondary,
)

@Composable
fun PatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
```

- [ ] **Step 9: Create PatchApp.kt**

```kotlin
// app/src/main/java/com/patch/notifier/PatchApp.kt
package com.patch.notifier

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes

class PatchApp : Application() {
    companion object {
        const val CHANNEL_ID = "patch_reminders"
    }

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Patch Reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Reminders to replace estrogen patches"
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, audioAttributes)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
```

- [ ] **Step 10: Create stub MainActivity.kt**

```kotlin
// app/src/main/java/com/patch/notifier/MainActivity.kt
package com.patch.notifier

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.patch.notifier.ui.PatchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PatchTheme {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Patch Notifier")
                }
            }
        }
    }
}
```

- [ ] **Step 11: Verify project builds**

Run:
```bash
cd /home/jlyons/PycharmProjects/patch-notifier
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 12: Commit**

```bash
git init
echo -e ".gradle/\nbuild/\napp/build/\n*.iml\n.idea/\nlocal.properties\n.superpowers/" > .gitignore
git add .gitignore settings.gradle.kts build.gradle.kts gradle.properties gradle/ app/build.gradle.kts app/src/
git commit -m "scaffold: Android project with Compose, Room, Material 3"
```

---

### Task 2: Room Database — Patch Entity, DAO, Database

**Files:**
- Create: `app/src/main/java/com/patch/notifier/data/Patch.kt`
- Create: `app/src/main/java/com/patch/notifier/data/PatchDao.kt`
- Create: `app/src/main/java/com/patch/notifier/data/PatchDatabase.kt`
- Create: `app/src/test/java/com/patch/notifier/RotationLogicTest.kt`

- [ ] **Step 1: Write rotation logic tests**

The rotation logic is pure — it takes a list of patches and returns suggested IDs. Test it as a unit test without Room.

```kotlin
// app/src/test/java/com/patch/notifier/RotationLogicTest.kt
package com.patch.notifier

import com.patch.notifier.data.Patch
import org.junit.Assert.assertEquals
import org.junit.Test

class RotationLogicTest {

    private fun suggestLocations(patches: List<Patch>, count: Int = 3): List<Int> {
        return patches
            .sortedBy { it.appliedAt ?: 0L }
            .take(count)
            .map { it.id }
    }

    @Test
    fun `first launch - all null appliedAt - suggests first 3 by id`() {
        val patches = listOf(
            Patch(1, "Left Upper", null, null),
            Patch(2, "Left Lower", null, null),
            Patch(3, "Right Upper", null, null),
            Patch(4, "Right Lower", null, null),
        )
        val suggested = suggestLocations(patches)
        assertEquals(3, suggested.size)
        assertEquals(listOf(1, 2, 3), suggested)
    }

    @Test
    fun `after first use - rests the most recently applied`() {
        val now = System.currentTimeMillis()
        val patches = listOf(
            Patch(1, "Left Upper", now, now + 604800000),
            Patch(2, "Left Lower", now, now + 604800000),
            Patch(3, "Right Upper", now, now + 604800000),
            Patch(4, "Right Lower", null, null),
        )
        val suggested = suggestLocations(patches)
        // id 4 has null appliedAt (sorted as 0, earliest), then 1,2,3 all tied
        // Suggest: 4, 1, 2 (first 3 after sort)
        assertEquals(3, suggested.size)
        assert(4 in suggested) { "Should suggest the unused location" }
    }

    @Test
    fun `rotation cycle - oldest applied gets suggested`() {
        val week1 = 1000L
        val week2 = 604800000L + 1000L
        val patches = listOf(
            Patch(1, "Left Upper", week1, week1 + 604800000),
            Patch(2, "Left Lower", week1, week1 + 604800000),
            Patch(3, "Right Upper", week1, week1 + 604800000),
            Patch(4, "Right Lower", week2, week2 + 604800000),
        )
        val suggested = suggestLocations(patches)
        // 1,2,3 applied at week1 (oldest), 4 applied at week2
        // Sort: 1,2,3,4 — take first 3: 1,2,3
        assertEquals(listOf(1, 2, 3), suggested)
    }

    @Test
    fun `selecting fewer than 3 - works with count 1`() {
        val patches = listOf(
            Patch(1, "Left Upper", null, null),
            Patch(2, "Left Lower", null, null),
            Patch(3, "Right Upper", null, null),
            Patch(4, "Right Lower", null, null),
        )
        val suggested = suggestLocations(patches, count = 1)
        assertEquals(1, suggested.size)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test`
Expected: FAIL — `Patch` class doesn't exist yet.

- [ ] **Step 3: Create Patch entity**

```kotlin
// app/src/main/java/com/patch/notifier/data/Patch.kt
package com.patch.notifier.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patches")
data class Patch(
    @PrimaryKey val id: Int,
    val location: String,
    val appliedAt: Long?,
    val dueAt: Long?,
)

fun suggestLocations(patches: List<Patch>, count: Int = 3): List<Int> {
    return patches
        .sortedBy { it.appliedAt ?: 0L }
        .take(count)
        .map { it.id }
}
```

- [ ] **Step 4: Fix test imports and run**

Update test to import `suggestLocations` from `com.patch.notifier.data`:

```kotlin
// Update imports at top of RotationLogicTest.kt
import com.patch.notifier.data.Patch
import com.patch.notifier.data.suggestLocations
```

Remove the local `suggestLocations` function from the test class body. Each test should call `suggestLocations(patches)` directly.

Run: `./gradlew test`
Expected: ALL PASS

- [ ] **Step 5: Create PatchDao**

```kotlin
// app/src/main/java/com/patch/notifier/data/PatchDao.kt
package com.patch.notifier.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PatchDao {
    @Query("SELECT * FROM patches ORDER BY id")
    fun observeAll(): Flow<List<Patch>>

    @Query("SELECT * FROM patches ORDER BY id")
    suspend fun getAll(): List<Patch>

    @Upsert
    suspend fun upsert(patch: Patch)

    @Upsert
    suspend fun upsertAll(patches: List<Patch>)

    @Query("SELECT * FROM patches WHERE dueAt IS NOT NULL AND dueAt > 0 ORDER BY dueAt")
    suspend fun getActivePatchesByDueDate(): List<Patch>
}
```

- [ ] **Step 6: Create PatchDatabase**

```kotlin
// app/src/main/java/com/patch/notifier/data/PatchDatabase.kt
package com.patch.notifier.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Patch::class], version = 1, exportSchema = false)
abstract class PatchDatabase : RoomDatabase() {
    abstract fun patchDao(): PatchDao

    companion object {
        @Volatile
        private var INSTANCE: PatchDatabase? = null

        fun getInstance(context: Context): PatchDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PatchDatabase::class.java,
                    "patch_db",
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).patchDao().upsertAll(
                                    listOf(
                                        Patch(1, "Left Upper", null, null),
                                        Patch(2, "Left Lower", null, null),
                                        Patch(3, "Right Upper", null, null),
                                        Patch(4, "Right Lower", null, null),
                                    )
                                )
                            }
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
```

- [ ] **Step 7: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/patch/notifier/data/ app/src/test/
git commit -m "feat: Room database with Patch entity, DAO, and rotation logic"
```

---

### Task 3: Alarm Scheduling

**Files:**
- Create: `app/src/main/java/com/patch/notifier/alarm/AlarmScheduler.kt`
- Create: `app/src/main/java/com/patch/notifier/alarm/AlarmReceiver.kt`
- Create: `app/src/main/java/com/patch/notifier/boot/BootReceiver.kt`
- Create: `app/src/test/java/com/patch/notifier/AlarmSchedulerTest.kt`

- [ ] **Step 1: Write AlarmScheduler tests**

```kotlin
// app/src/test/java/com/patch/notifier/AlarmSchedulerTest.kt
package com.patch.notifier

import com.patch.notifier.alarm.AlarmScheduler
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmSchedulerTest {

    @Test
    fun `request code for patch alarm uses patch id`() {
        assertEquals(1001, AlarmScheduler.patchAlarmRequestCode(1))
        assertEquals(1004, AlarmScheduler.patchAlarmRequestCode(4))
    }

    @Test
    fun `request code for nag alarm uses offset`() {
        assertEquals(2001, AlarmScheduler.nagAlarmRequestCode(1))
        assertEquals(2004, AlarmScheduler.nagAlarmRequestCode(4))
    }

    @Test
    fun `nag delay is 1 hour for first nag then 2 hours`() {
        assertEquals(3600000L, AlarmScheduler.nagDelayMs(nagCount = 0))
        assertEquals(7200000L, AlarmScheduler.nagDelayMs(nagCount = 1))
        assertEquals(7200000L, AlarmScheduler.nagDelayMs(nagCount = 5))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test`
Expected: FAIL — `AlarmScheduler` doesn't exist.

- [ ] **Step 3: Create AlarmScheduler**

```kotlin
// app/src/main/java/com/patch/notifier/alarm/AlarmScheduler.kt
package com.patch.notifier.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object AlarmScheduler {
    private const val PATCH_ALARM_BASE = 1000
    private const val NAG_ALARM_BASE = 2000
    private const val FIRST_NAG_DELAY_MS = 3_600_000L   // 1 hour
    private const val REPEAT_NAG_DELAY_MS = 7_200_000L  // 2 hours

    const val EXTRA_PATCH_ID = "patch_id"
    const val EXTRA_PATCH_LOCATION = "patch_location"
    const val EXTRA_NAG_COUNT = "nag_count"
    const val EXTRA_IS_NAG = "is_nag"
    const val ACTION_PATCH_DUE = "com.patch.notifier.PATCH_DUE"

    fun patchAlarmRequestCode(patchId: Int): Int = PATCH_ALARM_BASE + patchId
    fun nagAlarmRequestCode(patchId: Int): Int = NAG_ALARM_BASE + patchId

    fun nagDelayMs(nagCount: Int): Long {
        return if (nagCount == 0) FIRST_NAG_DELAY_MS else REPEAT_NAG_DELAY_MS
    }

    fun schedulePatchAlarm(context: Context, patchId: Int, location: String, triggerAtMs: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_PATCH_DUE
            putExtra(EXTRA_PATCH_ID, patchId)
            putExtra(EXTRA_PATCH_LOCATION, location)
            putExtra(EXTRA_IS_NAG, false)
            putExtra(EXTRA_NAG_COUNT, 0)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            patchAlarmRequestCode(patchId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMs,
            pendingIntent,
        )
    }

    fun scheduleNagAlarm(context: Context, patchId: Int, location: String, nagCount: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val triggerAt = System.currentTimeMillis() + nagDelayMs(nagCount)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_PATCH_DUE
            putExtra(EXTRA_PATCH_ID, patchId)
            putExtra(EXTRA_PATCH_LOCATION, location)
            putExtra(EXTRA_IS_NAG, true)
            putExtra(EXTRA_NAG_COUNT, nagCount + 1)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            nagAlarmRequestCode(patchId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent,
        )
    }

    fun cancelAlarms(context: Context, patchId: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_PATCH_DUE
        }
        // Cancel both patch alarm and nag alarm
        for (requestCode in listOf(patchAlarmRequestCode(patchId), nagAlarmRequestCode(patchId))) {
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }
    }
}
```

- [ ] **Step 4: Run tests**

Run: `./gradlew test`
Expected: ALL PASS

- [ ] **Step 5: Create AlarmReceiver**

```kotlin
// app/src/main/java/com/patch/notifier/alarm/AlarmReceiver.kt
package com.patch.notifier.alarm

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.patch.notifier.MainActivity
import com.patch.notifier.PatchApp

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_PATCH_DUE) return

        val patchId = intent.getIntExtra(AlarmScheduler.EXTRA_PATCH_ID, -1)
        val location = intent.getStringExtra(AlarmScheduler.EXTRA_PATCH_LOCATION) ?: return
        val isNag = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_NAG, false)
        val nagCount = intent.getIntExtra(AlarmScheduler.EXTRA_NAG_COUNT, 0)

        if (patchId == -1) return

        showNotification(context, patchId, location, isNag, nagCount)

        // Schedule next nag
        AlarmScheduler.scheduleNagAlarm(context, patchId, location, nagCount)
    }

    private fun showNotification(
        context: Context,
        patchId: Int,
        location: String,
        isNag: Boolean,
        nagCount: Int,
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPending = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = if (isNag) "Patch Still Needs Replacing!" else "Patch Replacement Due"
        val body = "Time to replace: $location"
        val priority = if (nagCount >= 2) {
            NotificationCompat.PRIORITY_MAX
        } else {
            NotificationCompat.PRIORITY_HIGH
        }

        val notification = NotificationCompat.Builder(context, PatchApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(tapPending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(patchId, notification)
        } catch (_: SecurityException) {
            // Notification permission not granted — nothing we can do
        }
    }
}
```

- [ ] **Step 6: Create BootReceiver**

```kotlin
// app/src/main/java/com/patch/notifier/boot/BootReceiver.kt
package com.patch.notifier.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.patch.notifier.alarm.AlarmScheduler
import com.patch.notifier.data.PatchDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = PatchDatabase.getInstance(context).patchDao()
                val patches = dao.getActivePatchesByDueDate()
                val now = System.currentTimeMillis()

                for (patch in patches) {
                    val dueAt = patch.dueAt ?: continue
                    if (dueAt > now) {
                        AlarmScheduler.schedulePatchAlarm(
                            context, patch.id, patch.location, dueAt,
                        )
                    } else {
                        // Already overdue — fire notification immediately via a nag
                        AlarmScheduler.scheduleNagAlarm(
                            context, patch.id, patch.location, nagCount = 0,
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
```

- [ ] **Step 7: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/patch/notifier/alarm/ app/src/main/java/com/patch/notifier/boot/ app/src/test/java/com/patch/notifier/AlarmSchedulerTest.kt
git commit -m "feat: alarm scheduling with nag repeats and boot recovery"
```

---

### Task 4: Main UI Screen

**Files:**
- Create: `app/src/main/java/com/patch/notifier/ui/PatchScreen.kt`
- Modify: `app/src/main/java/com/patch/notifier/MainActivity.kt`

- [ ] **Step 1: Create PatchScreen composable**

```kotlin
// app/src/main/java/com/patch/notifier/ui/PatchScreen.kt
package com.patch.notifier.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patch.notifier.data.Patch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun PatchScreen(
    patches: List<Patch>,
    selectedIds: Set<Int>,
    onToggle: (Int) -> Unit,
    onConfirm: () -> Unit,
    showConfirmation: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))

        // Status header
        StatusHeader(patches)

        Spacer(Modifier.height(32.dp))

        // Location grid
        LocationGrid(
            patches = patches,
            selectedIds = selectedIds,
            onToggle = onToggle,
        )

        Spacer(Modifier.height(12.dp))

        // Helper text
        Text(
            text = "${selectedIds.size} selected · tap to toggle",
            color = TextSecondary,
            fontSize = 14.sp,
        )

        Spacer(Modifier.weight(1f))

        // Confirm button
        Button(
            onClick = onConfirm,
            enabled = selectedIds.isNotEmpty() && !showConfirmation,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Blue,
                disabledContainerColor = NavyLight,
            ),
        ) {
            Text(
                text = if (showConfirmation) "✓ DONE" else "REPLACED ${selectedIds.size} PATCHES",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun StatusHeader(patches: List<Patch>) {
    val activeDueDates = patches.mapNotNull { it.dueAt }.filter { it > 0 }

    if (activeDueDates.isEmpty()) {
        Text(
            text = "No active patches",
            color = TextSecondary,
            fontSize = 14.sp,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Select locations to get started",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
    } else {
        val earliest = activeDueDates.min()
        val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        val daysAway = TimeUnit.MILLISECONDS.toDays(earliest - System.currentTimeMillis())
        val patchCount = activeDueDates.size

        Text(
            text = "NEXT REPLACEMENT",
            color = TextSecondary,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = dateFormat.format(Date(earliest)),
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "$patchCount patches · ${daysAway}d from now",
            color = TextMuted,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun LocationGrid(
    patches: List<Patch>,
    selectedIds: Set<Int>,
    onToggle: (Int) -> Unit,
) {
    val leftPatches = patches.filter { it.location.startsWith("Left") }
        .sortedBy { if (it.location.contains("Upper")) 0 else 1 }
    val rightPatches = patches.filter { it.location.startsWith("Right") }
        .sortedBy { if (it.location.contains("Upper")) 0 else 1 }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Left thigh
        ThighColumn(
            label = "LEFT THIGH",
            patches = leftPatches,
            selectedIds = selectedIds,
            onToggle = onToggle,
            modifier = Modifier.weight(1f),
        )
        // Right thigh
        ThighColumn(
            label = "RIGHT THIGH",
            patches = rightPatches,
            selectedIds = selectedIds,
            onToggle = onToggle,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ThighColumn(
    label: String,
    patches: List<Patch>,
    selectedIds: Set<Int>,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavyLight),
            border = BorderStroke(2.dp, BorderColor),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (patch in patches) {
                    val selected = patch.id in selectedIds
                    LocationButton(
                        label = patch.location.substringAfter(" "),
                        selected = selected,
                        onClick = { onToggle(patch.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) Blue else Navy,
        label = "bg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) BlueBright else BorderColor,
        label = "border",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) TextPrimary else TextMuted,
        label = "text",
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(2.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (selected) "✓ $label" else label,
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
```

- [ ] **Step 2: Update MainActivity to wire everything together**

```kotlin
// app/src/main/java/com/patch/notifier/MainActivity.kt
package com.patch.notifier

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patch.notifier.alarm.AlarmScheduler
import com.patch.notifier.data.PatchDatabase
import com.patch.notifier.data.suggestLocations
import com.patch.notifier.ui.Navy
import com.patch.notifier.ui.PatchScreen
import com.patch.notifier.ui.PatchTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* granted or not, app still works — just no notifications */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val db = PatchDatabase.getInstance(this)
        val dao = db.patchDao()

        setContent {
            PatchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Navy,
                ) {
                    val patches by dao.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
                    var selectedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
                    var suggestionsApplied by remember { mutableStateOf(false) }
                    var showConfirmation by remember { mutableStateOf(false) }

                    // Apply suggestions when patches load
                    LaunchedEffect(patches) {
                        if (patches.isNotEmpty() && !suggestionsApplied) {
                            selectedIds = suggestLocations(patches).toSet()
                            suggestionsApplied = true
                        }
                    }

                    PatchScreen(
                        patches = patches,
                        selectedIds = selectedIds,
                        onToggle = { id ->
                            selectedIds = if (id in selectedIds) {
                                selectedIds - id
                            } else {
                                selectedIds + id
                            }
                        },
                        onConfirm = {
                            val now = System.currentTimeMillis()
                            val dueAt = now + 7 * 24 * 60 * 60 * 1000L
                            val context = this@MainActivity

                            CoroutineScope(Dispatchers.IO).launch {
                                for (id in selectedIds) {
                                    val patch = patches.find { it.id == id } ?: continue
                                    val updated = patch.copy(appliedAt = now, dueAt = dueAt)
                                    dao.upsert(updated)

                                    // Cancel existing alarms and schedule new ones
                                    AlarmScheduler.cancelAlarms(context, id)
                                    AlarmScheduler.schedulePatchAlarm(
                                        context, id, patch.location, dueAt,
                                    )
                                }
                            }

                            showConfirmation = true
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(1500)
                                showConfirmation = false
                                // Reset suggestions for next time
                                suggestionsApplied = false
                            }
                        },
                        showConfirmation = showConfirmation,
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Install on device and test**

Run:
```bash
./gradlew installDebug
```

Manual test on device:
1. App opens to "No active patches" with all 4 locations, first 3 pre-selected
2. Tap a selected location to deselect — count updates
3. Tap an unselected location to select — count updates
4. Tap "REPLACED 3 PATCHES" — button briefly shows "✓ DONE"
5. Status header updates to show due date 7 days from now
6. Force-close and reopen — state persists

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/patch/notifier/ui/PatchScreen.kt app/src/main/java/com/patch/notifier/MainActivity.kt
git commit -m "feat: main UI screen with location grid, rotation suggestions, confirm flow"
```

---

### Task 5: End-to-End Verification & Polish

**Files:**
- Modify: `app/src/main/java/com/patch/notifier/ui/PatchScreen.kt` (if needed)

- [ ] **Step 1: Run full test suite**

Run: `./gradlew test`
Expected: ALL PASS

- [ ] **Step 2: Build release APK**

Run:
```bash
./gradlew assembleDebug
```

Locate APK:
```bash
ls -la app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 3: Install and full manual test**

Run: `adb install app/build/outputs/apk/debug/app-debug.apk`

Manual test checklist:
1. First launch: 4 locations shown, first 3 pre-selected, "No active patches" header
2. Confirm with 3 selected: header shows due date, button flashes "✓ DONE"
3. Reopen app: state persisted, suggestions updated based on rotation
4. Notification fires after 7 days (to test quickly: temporarily change `7 * 24 * 60 * 60 * 1000L` to `60_000L` in MainActivity for 1-minute testing)
5. Nag fires 1 hour after first notification (test with shortened delay)
6. Tapping notification opens app
7. Confirming replacement cancels pending nags
8. Reboot device: alarms re-scheduled (check via adb shell dumpsys alarm | grep patch)

- [ ] **Step 4: Commit final state**

```bash
git add -A
git commit -m "chore: final build verification"
```
