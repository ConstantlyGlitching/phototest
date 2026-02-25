package com.fotokayit.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileInputStream

class MainActivity : AppCompatActivity() {

    private var tempImageUri: Uri? = null
    private var tempImageFile: File? = null

    // Kamera sonucu
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            showFileNameDialog()
        } else {
            Toast.makeText(this, "Fotoğraf çekilmedi.", Toast.LENGTH_SHORT).show()
        }
    }

    // Klasör seçici sonucu
    private var pendingFileName: String = ""
    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val folderUri = result.data?.data ?: return@registerForActivityResult
            saveImageToFolder(folderUri, pendingFileName)
        } else {
            Toast.makeText(this, "Klasör seçilmedi.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnTakePhoto).setOnClickListener {
            openCamera()
        }
    }

    private fun openCamera() {
        val imageFile = File(cacheDir, "temp_photo.jpg")
        tempImageFile = imageFile
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            imageFile
        )
        tempImageUri = uri

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
        }
        cameraLauncher.launch(intent)
    }

    private fun showFileNameDialog() {
        val input = TextInputEditText(this).apply {
            hint = "Dosya adı"
            setPadding(48, 32, 48, 16)
        }

        AlertDialog.Builder(this)
            .setTitle("Dosya Adı")
            .setMessage("Fotoğraf için bir isim girin:")
            .setView(input)
            .setPositiveButton("Klasör Seç") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Lütfen bir isim girin.", Toast.LENGTH_SHORT).show()
                    showFileNameDialog()
                } else {
                    pendingFileName = name
                    openFolderPicker()
                }
            }
            .setNegativeButton("İptal") { _, _ ->
                Toast.makeText(this, "İptal edildi.", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun openFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        folderPickerLauncher.launch(intent)
    }

    private fun saveImageToFolder(folderUri: Uri, fileName: String) {
        try {
            val folder = DocumentFile.fromTreeUri(this, folderUri) ?: return
            val newFile = folder.createFile("image/jpeg", "$fileName.jpg") ?: return

            val inputStream = FileInputStream(tempImageFile)
            val outputStream = contentResolver.openOutputStream(newFile.uri)

            outputStream?.use { out ->
                inputStream.use { inp ->
                    inp.copyTo(out)
                }
            }

            Toast.makeText(this, "✅ Kaydedildi: $fileName.jpg", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
