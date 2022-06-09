package ru.zh_iev.imgtotext_ocr

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private var indicator: Boolean = true

    private var currentImageUri: Uri? = null

    private lateinit var btnOpenGallery: Button
    private lateinit var btnOpenCamera: Button
    private lateinit var sendData: Button
    private lateinit var ivPhoto: ImageView
    private lateinit var chiprus: Chip
    private lateinit var chipeng: Chip
    private lateinit var photo: Bitmap


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val pB: ProgressBar = findViewById(R.id.loadingPB)

        btnOpenGallery = findViewById(R.id.btnGallery)
        btnOpenCamera = findViewById(R.id.takePhoto)
        sendData = findViewById(R.id.sendData)
        ivPhoto = findViewById(R.id.ivImage)
        chiprus = findViewById(R.id.chiprus)
        chipeng = findViewById(R.id.chipeng)
        val textView: TextView = findViewById(R.id.textView)

        resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK && indicator) {
                    Glide.with(this).load(currentImageUri).into(ivPhoto)
                    photo = MediaStore.Images.Media.getBitmap(this.contentResolver, currentImageUri)
                } else {
                    if (result.resultCode == Activity.RESULT_OK && !indicator) {
                        result.data?.let {
                            ivPhoto.setImageURI(it.data)
                            photo = MediaStore.Images.Media.getBitmap(this.contentResolver, it.data)
                        }
                    }
                }
            }

        btnOpenCamera.setOnClickListener {
            openCamera()
        }
        btnOpenGallery.setOnClickListener {
            openGallery()
        }
        sendData.setOnClickListener {
            pB.visibility = View.VISIBLE
            if (!this::photo.isInitialized) {
                Toast.makeText(this@MainActivity, "Пожалуйста, выберите изображение",
                    Toast.LENGTH_SHORT).show()
                pB.visibility = View.INVISIBLE
                return@setOnClickListener
            }
            val language: String
            if (chipeng.isChecked) {
                language = "eng"
            } else {
                if (chiprus.isChecked) {
                    language = "rus"
                } else {
                    Toast.makeText(this@MainActivity, "Пожалуйста, выберите язык",
                        Toast.LENGTH_SHORT).show()
                    pB.visibility = View.INVISIBLE
                    return@setOnClickListener
                }
            }

            val url = "https://api.ocr.space/parse/image"
            val strB64: String = getStringImage(photo)

            val requestQueue = Volley.newRequestQueue(this)
            val stringRequest = object: StringRequest(Method.POST, url, {
                response ->
                val jsonResp = JSONObject(response)
                val jsonArray = jsonResp.getJSONArray("ParsedResults")
                val result = jsonArray.getJSONObject(0)
                val parsedText = result.getString("ParsedText")
                val s: String = parsedText
                Toast.makeText(this@MainActivity, s, Toast.LENGTH_LONG).show()
                textView.text = s
            }, {
                error ->
                Toast.makeText(this@MainActivity, error.toString(), Toast.LENGTH_SHORT).show()
            }) {
                override fun getBodyContentType(): String {
                    return "multipart/form-data;boundary=AS24adije32MDJHEM9oMaGnKUXtfHq"
                }
                override fun getHeaders(): MutableMap<String, String> {
                    val params = HashMap<String, String>()
                    params.put("apikey", "eb4518fbf288957")
                    return params
                }
                override fun getBody(): ByteArray {
                    val params = HashMap<String, String>()

                    params.put("language", language)
                    params.put("base64image", strB64)

                    val map: List<String> = params.map {
                            (key, value) -> "--AS24adije32MDJHEM9oMaGnKUXtfHq\n" +
                            "Content-Disposition: form-data; " +
                            "name=\"$key\"\n\n$value\n"
                    }
                    val endResult = "${map.joinToString("")}\n" +
                            "--AS24adije32MDJHEM9oMaGnKUXtfHq--\n"
                    return endResult.toByteArray()
                }
            }
            requestQueue.add(stringRequest)
            pB.visibility = View.INVISIBLE
        }
    }

    private fun getStringImage(bm: Bitmap): String {
        val ba = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 100, ba)
        val imageByte: ByteArray = ba.toByteArray()
        return "data:image/jpeg;base64," + Base64.encodeToString(imageByte, Base64.DEFAULT)
    }

    private fun openGallery() {
        indicator = false
        val galleryIntent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        resultLauncher.launch(galleryIntent)
    }
    private fun openCamera() {
        indicator = true

        val permissionRequests = mutableListOf<String>()
        permissionRequests.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissionRequests.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        Dexter.withContext(this)
            .withPermissions(
                permissionRequests
            ).withListener(object: MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        val values = ContentValues()
                        values.put(MediaStore.Images.Media.TITLE, "New Picture")
                        values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera for OCR")
                        currentImageUri = contentResolver.insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                        )
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, currentImageUri)
                        resultLauncher.launch(intent)
                    }
                }
                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>,
                    token: PermissionToken
                ) {}
            }).check()
    }
}