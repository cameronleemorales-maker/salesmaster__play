package com.yourcompany.salesmaster

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class MainActivity : ComponentActivity() {
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val objectionInput = findViewById<EditText>(R.id.input_objection)
        val askBtn = findViewById<Button>(R.id.btn_ask)
        val answerText = findViewById<TextView>(R.id.text_answer)
        val counterText = findViewById<TextView>(R.id.text_counter)
        val apiField = findViewById<EditText>(R.id.input_api)

        val prefs = getSharedPreferences("salesmaster", Context.MODE_PRIVATE)
        var apiUrl = prefs.getString("apiUrl", "http://127.0.0.1:4242") ?: "http://127.0.0.1:4242"
        apiField.setText(apiUrl)

        var remaining = prefs.getInt("remaining", 4)
        fun updateCounter() {
            counterText.text = if (remaining > 0) "Free answers left: $remaining" else "Limit reached"
        }
        updateCounter()

        apiField.setOnEditorActionListener { _, _, _ ->
            val url = apiField.text.toString().trim()
            if (url.startsWith("http")) {
                prefs.edit().putString("apiUrl", url).apply()
                apiUrl = url
                Toast.makeText(this, "API set to $url", Toast.LENGTH_SHORT).show()
            }
            false
        }

        fun callAi(prompt: String) {
            val body = JSONObject(mapOf("prompt" to prompt)).toString()
            val req = Request.Builder()
                .url("${apiUrl}/ai-answer")
                .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), body))
                .build()
            client.newCall(req).enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        answerText.text = "Demo answer (offline): Acknowledge, restate value/ROI, offer proof, ask a closing question."
                    }
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val text = try {
                            val obj = JSONObject(it.body?.string() ?: "{}")
                            obj.optString("text", "No response")
                        } catch (e: Exception) {
                            "No response"
                        }
                        runOnUiThread {
                            answerText.text = text
                            if (remaining > 0) {
                                remaining -= 1
                                prefs.edit().putInt("remaining", remaining).apply()
                                updateCounter()
                            }
                        }
                    }
                }
            })
        }

        askBtn.setOnClickListener {
            val q = objectionInput.text.toString().trim()
            if (q.isEmpty()) {
                Toast.makeText(this, "Type an objection first", Toast.LENGTH_SHORT).show()
            } else if (remaining <= 0) {
                Toast.makeText(this, "Limit reached", Toast.LENGTH_SHORT).show()
            } else {
                callAi(q)
            }
        }
    }
}
