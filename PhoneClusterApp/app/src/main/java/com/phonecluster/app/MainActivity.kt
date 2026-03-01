package com.phonecluster.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.phonecluster.app.ml.EmbeddingEngine
import com.phonecluster.app.screens.FileBrowserScreen
import com.phonecluster.app.screens.ModeSelectionScreen
import com.phonecluster.app.screens.RegistrationScreen
import com.phonecluster.app.screens.SearchScreen
import com.phonecluster.app.screens.StorageModeScreen
import com.phonecluster.app.screens.UserModeScreen
import com.phonecluster.app.ui.theme.CloudStorageAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Navigation destinations
sealed class Screen {
    object Registration : Screen()
    object ModeSelection : Screen()
    object UserMode : Screen()
    object StorageMode : Screen()
    object Search : Screen()
    object FileBrowser : Screen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // =========================
        // 1) QUICK BYTE ARRAY TEST
        // =========================
        try {
            val key = ByteArray(16) { it.toByte() }
            val nonce = ByteArray(16) { (50 + it).toByte() }
            val ad = "meta".encodeToByteArray()
            val msg = "hello ascon".encodeToByteArray()

            val ct = AsconNative.encrypt(key, nonce, ad, msg)
                ?: throw IllegalStateException("encrypt() returned null")

            val pt = AsconNative.decrypt(key, nonce, ad, ct)
                ?: throw IllegalStateException("decrypt() returned null")

            Log.d("ASCON_TEST", "ok=${pt.contentEquals(msg)} pt=${String(pt)} ctLen=${ct.size}")
        } catch (e: Exception) {
            Log.e("ASCON_TEST", "Error: ${e.message}", e)
        }

        // ======================================
        // 2) FILE ENCRYPT/DECRYPT (VISIBLE PATH)
        // ======================================
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // TEMP hardcoded key (16 bytes). Use Keystore later.
                    val fileKey = ByteArray(16) { 1 }

                    // /sdcard/Android/data/com.phonecluster.app/files/
                    val base = getExternalFilesDir(null)
                        ?: throw IllegalStateException("getExternalFilesDir(null) returned null")

                    val plainRepo = File(base, "plain_repo").apply { mkdirs() }
                    val encRepo = File(base, "encrypted_repo").apply { mkdirs() }
                    val decRepo = File(base, "decrypted_repo").apply { mkdirs() }

                    val inFile = File(plainRepo, "input.txt")
                    val encFile = File(encRepo, "input.txt.ascon")
                    val decFile = File(decRepo, "input_decrypted.txt")

                    inFile.writeText("Hello Wasssuupp File Encryption using ASCON in Kotlin!")

                    // Functions from AsconFileCrypto.kt
                    encryptFile(inFile, encFile, fileKey)
                    decryptFile(encFile, decFile, fileKey)

                    Log.d("FILE_TEST", "Plain:     ${inFile.absolutePath}")
                    Log.d("FILE_TEST", "Encrypted: ${encFile.absolutePath} size=${encFile.length()}")
                    Log.d("FILE_TEST", "Decrypted: ${decFile.absolutePath}")
                    Log.d("FILE_TEST", "Text:      ${decFile.readText()}")
                } catch (e: Exception) {
                    Log.e("FILE_TEST", "Error: ${e.message}", e)
                }
            }
        }

        // =========================
        // 3) COMPOSE UI + ENGINE
        // =========================
        setContent {
            val engine = remember { EmbeddingEngine(this) }

            CloudStorageAppTheme {
                AppNavigation(engine = engine)
            }
        }
    }
}

@Composable
fun AppNavigation(engine: EmbeddingEngine) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Registration) }

    when (currentScreen) {
        Screen.Registration -> {
            RegistrationScreen(
                onRegistered = { currentScreen = Screen.ModeSelection }
            )
        }

        Screen.ModeSelection -> {
            ModeSelectionScreen(
                onUserModeClick = { currentScreen = Screen.UserMode },
                onStorageModeClick = { currentScreen = Screen.StorageMode }
            )
        }

        Screen.UserMode -> {
            UserModeScreen(
                engine = engine,
                onBackClick = { currentScreen = Screen.ModeSelection },
                onSearchClick = { currentScreen = Screen.Search },
                onBrowseClick = { currentScreen = Screen.FileBrowser }
            )
        }

        Screen.StorageMode -> {
            StorageModeScreen(
                onBackClick = { currentScreen = Screen.ModeSelection }
            )
        }

        Screen.Search -> {
            SearchScreen(
                engine = engine,
                onBackClick = { currentScreen = Screen.UserMode }
            )
        }

        Screen.FileBrowser -> {
            FileBrowserScreen(
                onBackClick = { currentScreen = Screen.UserMode }
            )
        }
    }
}