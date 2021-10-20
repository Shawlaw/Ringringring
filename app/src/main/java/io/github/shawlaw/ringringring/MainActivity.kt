package io.github.shawlaw.ringringring


import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.github.shawlaw.ringringring.databinding.ActivityMainBinding
import java.io.*
import java.net.URLDecoder

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"

        private const val REQ_CODE_TO_GET_CONTENT = 123
        private const val REQ_CODE_TO_GET_PERMISSION = 124

    }

    private var selectedFile: SelectedFile? = null

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initWidget()
    }

    private fun initWidget() {
        binding.btnSelectFile.setOnClickListener {
            val pickFileIntent = Intent(Intent.ACTION_GET_CONTENT)
            pickFileIntent.type = "audio/*"
            startActivityForResult(pickFileIntent, REQ_CODE_TO_GET_CONTENT)
            showLoading("Parsing file, please wait……")
        }
        binding.btnSetRing.setOnClickListener {
            onClickSetRingtone()
        }
    }

    private fun onClickSetRingtone() {
        val isPhoneRing = binding.cbPhoneRing.isChecked
        val isNotificationRing = binding.cbNotificationRing.isChecked
        val isAlarmRing = binding.cbAlarmRing.isChecked
        if (!isPhoneRing && !isNotificationRing && !isAlarmRing) {
            return
        }
        selectedFile?.apply {
            if (isValid()) {
                if (checkMustPermission()) {
                    showLoading("Setting ringtone, please wait...")
                    Thread {
                        val destRingFile =
                            File(getExternalFilesDir(Environment.DIRECTORY_RINGTONES), name)
                        if (destRingFile.isFile) {
                            destRingFile.delete()
                        }
                        FileOutputStream(destRingFile).let {
                            it.write(bytes)
                            it.close()
                        }

                        Log.d(TAG, "setRingtone to ${destRingFile.absolutePath}")
                        val values = ContentValues()
                        values.put(MediaStore.MediaColumns.DATA, destRingFile.absolutePath)
                        values.put(MediaStore.MediaColumns.TITLE, title)
                        values.put(MediaStore.MediaColumns.SIZE, size)
                        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        values.put(MediaStore.Audio.Media.ARTIST, artist)
                        values.put(MediaStore.Audio.Media.DURATION, duration)
                        values.put(
                            MediaStore.Audio.Media.IS_RINGTONE,
                            isPhoneRing
                        )
                        values.put(
                            MediaStore.Audio.Media.IS_NOTIFICATION,
                            isNotificationRing
                        )
                        values.put(MediaStore.Audio.Media.IS_ALARM, isAlarmRing)
                        values.put(MediaStore.Audio.Media.IS_MUSIC, false)

                        val uri =
                            MediaStore.Audio.Media.getContentUriForPath(destRingFile.absolutePath)
                        val newUri = contentResolver.insert(uri!!, values)

                        if (isPhoneRing) {
                            RingtoneManager.setActualDefaultRingtoneUri(
                                applicationContext,
                                RingtoneManager.TYPE_RINGTONE,
                                newUri
                            )
                        }
                        if (isNotificationRing) {
                            RingtoneManager.setActualDefaultRingtoneUri(
                                applicationContext,
                                RingtoneManager.TYPE_NOTIFICATION,
                                newUri
                            )
                        }
                        if (isAlarmRing) {
                            RingtoneManager.setActualDefaultRingtoneUri(
                                applicationContext,
                                RingtoneManager.TYPE_ALARM,
                                newUri
                            )
                        }
                        Log.d(TAG, "Ringtone set")
                        runOnUiThread {
                            Toast.makeText(application, "Ringtone set", Toast.LENGTH_LONG).show()
                        }
                    }.start()
                } else {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.WRITE_SETTINGS
                        ),
                        REQ_CODE_TO_GET_PERMISSION
                    )
                }
            }
        }
    }

    private fun checkMustPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showLoading(reason: String) {

    }

    private fun dismissLoading() {

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQ_CODE_TO_GET_PERMISSION) {
            var granted = true
            grantResults.forEach {
                granted = granted.and(it == PackageManager.PERMISSION_GRANTED)
            }
            if (granted) {
                Log.d(
                    TAG,
                    "All permissions granted"
                )
                onClickSetRingtone()
            } else {
                Log.d(
                    TAG,
                    "Not enough permissions"
                )
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, returnIntent: Intent?) {
        if (requestCode == REQ_CODE_TO_GET_CONTENT) {
            handleGetContentResult(resultCode, returnIntent)
        } else {
            super.onActivityResult(requestCode, resultCode, returnIntent)
        }

    }

    private fun handleGetContentResult(
        resultCode: Int,
        returnIntent: Intent?
    ) {
        if (resultCode != RESULT_OK) {
            dismissLoading()
            return
        }
        Thread {
            returnIntent?.data?.also { returnUri ->
                val inputPFD = try {
                    contentResolver.openFileDescriptor(returnUri, "r")
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                    Log.e(TAG, "File not found.")
                    return@Thread
                }

                val fd = inputPFD?.fileDescriptor
                val decodedUri = URLDecoder.decode("$returnUri", "UTF-8")
                Log.d(
                    TAG,
                    "onActivityResult() called with: returnUri = $returnUri, decoded = $decodedUri, fd = $fd"
                )
                val pathStartIdx = decodedUri.lastIndexOf(":") + 1
                val nameStartIdx = decodedUri.lastIndexOf("/") + 1
                val path = decodedUri.substring(pathStartIdx)
                val fileName = if (nameStartIdx > pathStartIdx) {
                    decodedUri.substring(nameStartIdx)
                } else {
                    path
                }
                val fis = FileInputStream(fd)
                val size = fis.available()
                Log.d(
                    TAG,
                    "extractFileInfo() called with: path = $path, fileName = $fileName, size = $size"
                )
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(fd)
                val mimeType = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                val title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                Log.d(
                    TAG,
                    "extraMetadata() called with: mimeType = $mimeType, title = $title, artist = $artist, duration = $duration"
                )
                val selectedFileBytes = fis.readBytes()
                selectedFile = SelectedFile(
                    path, fileName, size,
                    mimeType ?: "", title ?: "", artist ?: "",
                    (duration ?: "-1").toLong(), selectedFileBytes
                )
                runOnUiThread {
                    dismissLoading()
                    renderSelectedFileInfo()
                }
            }
        }.start()
    }

    private fun renderSelectedFileInfo() {
        selectedFile?.apply {
            binding.tvSelectedFile.append("\n${this.toReadeableString()}")
        }
    }
}