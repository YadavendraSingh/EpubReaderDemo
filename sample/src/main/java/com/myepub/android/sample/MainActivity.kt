package com.myepub.android.sample

import android.Manifest
import android.app.Dialog
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment

import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.myepub.Config
import com.myepub.FolioReader
import com.myepub.FolioReader.OnClosedListener
import com.myepub.android.sample.util.EncryptionDecription
import com.myepub.android.sample.util.EncryptionDecription.encodeFile
import com.myepub.android.sample.util.EncryptionDecription.getNGenerateSecretKey
import com.myepub.android.sample.util.Util.Companion.Url
import com.myepub.model.HighLight
import com.myepub.model.HighLight.HighLightAction
import com.myepub.model.locators.ReadLocator
import com.myepub.model.locators.ReadLocator.Companion.fromJson
import com.myepub.util.AppUtil.Companion.getSavedConfig
import com.myepub.util.OnHighlightListener
import com.myepub.util.ReadLocatorListener
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.net.URL
import java.net.URLConnection
import javax.crypto.SecretKey



class MainActivity : AppCompatActivity(), OnHighlightListener, ReadLocatorListener, OnClosedListener {
    lateinit var folioReader: FolioReader
    var isFileExists = false

    private val LOG_TAG = MainActivity::class.java.simpleName
    // File url to download
    private val file_url = Url

    private val encryptedFileName = "encrypted_chapter13"

   lateinit var yourKey: SecretKey
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        folioReader = FolioReader.get()
            .setOnHighlightListener(this)
            .setReadLocatorListener(this)
            .setOnClosedListener(this)

        // Request camera permissions
        if (allPermissionsGranted()) {
            declaration()
        }
        else{
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }


    }

   fun declaration(){

       button_view.setOnClickListener {
           var file = isFileExists()
           if (isFileExists) {
               decodeFile()
           }
           else{
               Toast.makeText(applicationContext,"Please download first",Toast.LENGTH_SHORT).show()
           }

       }
       btn_download.setOnClickListener {
           isFileExists()
           //download if file not exists
           if (!isFileExists) {
               DownloadFileFromURL().execute(file_url)
           } else {
               Toast.makeText(applicationContext,"File already downloaded",Toast.LENGTH_SHORT).show()
           }

       }
    }

    fun isFileExists():File{
        val file = File(
            Environment.getExternalStorageDirectory(),
            encryptedFileName
        )

        if(file.exists()){
            isFileExists = true
        }

        return file;
    }

    fun openFileForReading(file:File){
        var config = getSavedConfig(applicationContext)
        if (config == null) config = Config()
        config.allowedDirection = Config.AllowedDirection.VERTICAL_AND_HORIZONTAL
        folioReader.setConfig(config, true)
            .openBook(file.absolutePath)
    }

    private fun getLastReadLocator(): ReadLocator? {
        val jsonString =
            loadAssetTextAsString("Locators/LastReadLocators/last_read_locator_1.json")
        return fromJson(jsonString)
    }

    override fun saveReadLocator(readLocator: ReadLocator) {
        Log.i(LOG_TAG, "-> saveReadLocator -> " + readLocator.toJson())
    }


    private fun loadAssetTextAsString(name: String): String? {
        var `in`: BufferedReader? = null
        try {
            val buf = StringBuilder()
            val `is` = assets.open(name)
            `in` = BufferedReader(InputStreamReader(`is`))
            var str: String?
            var isFirst = true
            while (`in`.readLine().also { str = it } != null) {
                if (isFirst) isFirst = false else buf.append('\n')
                buf.append(str)
            }
            return buf.toString()
        } catch (e: IOException) {
            Log.e("HomeActivity", "Error opening asset $name")
        } finally {
            if (`in` != null) {
                try {
                    `in`.close()
                } catch (e: IOException) {
                    Log.e("HomeActivity", "Error closing asset $name")
                }
            }
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        FolioReader.clear()
    }

    override fun onHighlight(highlight: HighLight, type: HighLightAction) {
        Toast.makeText(
            this,
            "highlight id = " + highlight.uuid + " type = " + type,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onFolioReaderClosed() {
        Log.v(LOG_TAG, "-> onFolioReaderClosed")
    }

    // Progress Dialog
    public lateinit var pDialog: ProgressDialog
    val progress_bar_type = 0


    /**
     * Showing Dialog
     */
    override fun onCreateDialog(id: Int): Dialog? {
        return when (id) {
            progress_bar_type -> {
                pDialog = ProgressDialog(this)
                pDialog.setMessage("Downloading file. Please wait...")
                pDialog.setIndeterminate(false)
                pDialog.setMax(100)
                pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                pDialog.setCancelable(true)
                pDialog.show()
                pDialog
            }
            else -> null
        }
    }

    /**
     * Background Async Task to download file
     */
    inner class DownloadFileFromURL :
        AsyncTask<String?, String?, String?>() {
        /**
         * Before starting background thread Show Progress Bar Dialog
         */
        override fun onPreExecute() {
            super.onPreExecute()
            showDialog(progress_bar_type)
        }

        /**
         * Downloading file in background thread
         */

        override fun onProgressUpdate(vararg values: String?) {
            pDialog.setProgress(values[0]!!.toInt())
        }

        /**
         * After completing background task Dismiss the progress dialog
         */
        override fun onPostExecute(file_url: String?) {
            // dismiss the dialog after the file was downloaded
            dismissDialog(progress_bar_type)
            Toast.makeText(applicationContext,"Flie downloaded successfully", Toast.LENGTH_SHORT).show()

        }

        override fun doInBackground(vararg params: String?): String? {
            var count: Int = 1
            try {
                val url = URL(file_url)
                val connection: URLConnection = url.openConnection()
                connection.connect()

                // this will be useful so that you can show a tipical 0-100%
                // progress bar
                val lenghtOfFile: Int = connection.getContentLength()

                // download the file
                val input: InputStream = BufferedInputStream(
                    url.openStream(),
                    8192
                )
                var byteData = ByteArrayOutputStream(lenghtOfFile)

                val data = ByteArray(lenghtOfFile)
                var total: Long = 0
                while (input.read(data).also({ count = it }) != -1) {
                    total += count.toLong()
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + (total * 100 / lenghtOfFile).toInt())

                    // writing data to file
                    byteData.write(data, 0, count)

                }
                saveFile( byteData.toByteArray())

                // closing streams
                input.close()
            } catch (e: Exception) {
                Log.e("Error: ", e.message)
            }
            return null
        }
    }

    //---------------
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                declaration()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }




    fun saveFile(stringToSave: ByteArray?) {
        try {
            val file = File(
                Environment.getExternalStorageDirectory().toString() + File.separator,
                encryptedFileName
            )
            val bos = BufferedOutputStream(FileOutputStream(file))
            //yourKey = generateKey()
            yourKey = getNGenerateSecretKey(applicationContext)
            val filesBytes = encodeFile(yourKey, stringToSave)
            bos.write(filesBytes)
            bos.flush()
            bos.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun decodeFile() {
        try {
            yourKey = getNGenerateSecretKey(applicationContext)
            val decodedData = EncryptionDecription.decodeFile(yourKey, readFile())
            // String str = new String(decodedData);
            //System.out.println("DECODED FILE CONTENTS : " + str);
            getFileFromByte(decodedData!!)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun readFile(): ByteArray? {
        var contents: ByteArray? = null
        val file = File(
            Environment.getExternalStorageDirectory().toString()+File.separator, encryptedFileName
        )
        val size: Int = file.length().toInt()
        contents = ByteArray(size)
        try {
            val buf = BufferedInputStream(
                FileInputStream(file)
            )
            try {
                buf.read(contents)
                buf.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return contents
    }


    private fun getFileFromByte(fileByteArray: ByteArray) {
        try {
            // create temp file that will hold byte array
            val tempFile = File.createTempFile("Chapter13", ".epub", cacheDir)
            tempFile.deleteOnExit()
            val fos = FileOutputStream(tempFile)
            fos.write(fileByteArray)
            fos.close()
           openFileForReading(tempFile)
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }
}