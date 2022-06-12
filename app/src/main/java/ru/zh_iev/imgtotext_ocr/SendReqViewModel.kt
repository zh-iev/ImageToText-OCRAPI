package ru.zh_iev.imgtotext_ocr

import android.app.Application
import android.graphics.Bitmap
import android.util.Base64
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.android.volley.toolbox.StringRequest
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.HashMap
import com.android.volley.toolbox.Volley

class SendReqViewModel(application: Application) : AndroidViewModel(application) {

    val liveData = MutableLiveData<String>()

    fun init(photo: Bitmap, language: String) {
        sendRequest(photo, language)
    }

    private fun sendRequest(photo: Bitmap, language: String) {
        val requestQueue = Volley.newRequestQueue(getApplication())
        val strB64: String = getStringImage(photo)
        val url = "https://api.ocr.space/parse/image"
        val stringRequest = object: StringRequest(Method.POST, url, {
                response ->
            val jsonResp = JSONObject(response)
            val jsonArray = jsonResp.getJSONArray("ParsedResults")
            val result = jsonArray.getJSONObject(0)
            liveData.value = result.getString("ParsedText")

        }, {
                error ->
            liveData.value = "При распознавании текста возникла ошибка"
            Toast.makeText(getApplication(), "Возникла ошибка: $error", Toast.LENGTH_LONG).show()
        }) {
            val boundary = "AS24adije32MDJHEM9oMaGnKUXtfHq"
            override fun getBodyContentType(): String {
                return "multipart/form-data;boundary=${boundary}"
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
                        (key, value) -> "--${boundary}\n" +
                        "Content-Disposition: form-data; " +
                        "name=\"$key\"\n\n$value\n"
                }
                val endResult = "${map.joinToString("")}\n" +
                        "--${boundary}--\n"
                return endResult.toByteArray()
            }
        }
        requestQueue.add(stringRequest)
    }
    private fun getStringImage(bm: Bitmap): String {
        val ba = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 100, ba)
        val imageByte: ByteArray = ba.toByteArray()
        return "data:image/jpeg;base64," + Base64.encodeToString(imageByte, Base64.DEFAULT)
    }
}