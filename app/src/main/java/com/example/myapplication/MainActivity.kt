package com.example.myapplication

import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startForegroundService
import com.example.myapplication.databinding.ActivityMainBinding
import com.google.gson.Gson
//import retrofit2.Call
//import retrofit2.Callback
//import retrofit2.Response
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
import kotlin.jvm.java
import okhttp3.*
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    // 使用 ViewBinding 可以避免 findViewById，程式碼更簡潔且安全
    private lateinit var binding: ActivityMainBinding

    // 定義 後端 服務
    //private lateinit var pythonApiService: PythonApiService
    private lateinit var webSocket: WebSocket
    private val client = OkHttpClient()


    // 宣告 MediaProjection 管理器
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private val REQUEST_CODE = 1000 // 自定義一個請求代碼

    // 新增：註冊螢幕擷取的回傳處理
    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            // 使用者授權後，啟動前台服務進行截圖
            val serviceIntent = Intent(this, MediaProjectionService::class.java).apply {
                putExtra("data", result.data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化 ViewBinding 並設定畫面
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化網路通訊元件 (Retrofit)
        //setupRetrofit()
        // 程式啟動時就建立 WebSocket 連線
        connectToWebSocket()
        //WebSocketManager()
        //初始化發送按鈕點擊
        setupClickListeners()

        // 初始化 Manager
        mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        if (!isAccessibilityServiceEnabled()) {
            AlertDialog.Builder(this)
                .setTitle("需要開啟無障礙權限")
                .setMessage("為了讓 MobileMind 提供更完整的自動化服務，請在接下來的頁面找到本應用並開啟服務。")
                .setPositiveButton("前往設定") { _, _ ->
                    // 跳轉至系統無障礙設定頁面
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    startActivity(intent)
                }
                .setNegativeButton("稍後再說", null)
                .show()
        }

        // 2. 觸發系統授權視窗 (建議放在按鈕點擊事件，否則使用者會覺得莫名其妙)
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        screenCaptureLauncher.launch(captureIntent)


        // 設定鍵盤「發送」鍵監聽 (讓使用者打完字按 Enter 也能發送)
        binding.etCommand.setOnEditorActionListener { v, _, _ ->
            val userInput = v.text.toString()
            if (userInput.isNotBlank()) {
                processCommand(userInput)
                v.text = ""
                true
            } else false
        }
    }// onCreate 結束


    private fun isAccessibilityServiceEnabled(): Boolean {
        val serviceName = "${packageName}/${MyAccessibilityService::class.java.canonicalName}"
        val accessibilityEnabled = Settings.Secure.getInt(
            contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED, 0
        )
        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return settingValue?.contains(serviceName) ?: false
        }
        return false
    }

    //Retrofit 需要 BaseURL 與 Converter (JSON 轉物件)
    /*private fun setupRetrofit() {

        // 後端
        pythonApiService = Retrofit.Builder()
            .baseUrl("wss://unannealed-controllingly-sarai.ngrok-free.dev/ws")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PythonApiService::class.java)

    }*/
    //  建立連線方法
    /*class WebSocketManager {
        private val client = OkHttpClient()
        private var webSocket: WebSocket? = null

        fun connect() {
            val request = Request.Builder()
                .url("wss://unannealed-controllingly-sarai.ngrok-free.dev/ws")
                .addHeader("ngrok-skip-browser-warning", "true")  // 重要！跳過 ngrok 警告頁
                .build()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {

                override fun onOpen(webSocket: WebSocket, response: Response) {
                    // 連線成功後，馬上送出第一條訊息（對應你後端的 initial_data）
                    val initMsg = JSONObject()
                    initMsg.put("first_messages", "使用者的初始指令")
                    webSocket.send(initMsg.toString())
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    // 收到後端訊息
                    val json = JSONObject(text)
                    println("收到訊息: $json")
                    // 根據 type 欄位判斷要做什麼
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    println("連線失敗: ${t.message}")
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    println("連線關閉: $reason")
                }
            })
        }

        // 送訊息給後端（對應你的 handle_user_response）
        fun sendMessage(data: JSONObject) {
            webSocket?.send(data.toString())
        }

        fun disconnect() {
            webSocket?.close(1000, "使用者關閉")
        }
    }*/
    private fun connectToWebSocket() {
        val request = Request.Builder()
        //Ngrok 隨機網址，且路由結尾為 /ws
            .url("wss://much-glisten-educator.ngrok-free.dev/ws")
        //注入 Header 繞過 Ngrok 免費版的 200 OK 網頁攔截，確保 101 握手成功
            .addHeader("ngrok-skip-browser-warning", "true")
            .build()
        //  "Interface"：透過 Listener 處理所有事件
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // 連線成功！
                this@MainActivity.webSocket = webSocket
                runOnUiThread { showResult("WebSocket 已連線成功") }
                // 初始化: 發送 first_messages，喚醒後端的 LangGraph 大腦
                try {
                    val initMsg = org.json.JSONObject()
                    initMsg.put("first_messages", "Hello MobileMind")
                    webSocket.send(initMsg.toString())
                    Log.d("WebSocket", "已成功發送訊號，啟動後端 Agent")
                } catch (e: Exception) {
                    Log.e("WebSocket", "發送初始訊息失敗: ${e.message}")
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // 收到 Python 後端傳來的指令或回覆 ，text 就是後端傳回來的 JSON 字串，需手動解析
                try {
                    // 解析符合後端格式 (source, message, timestamp) 的資料
                    val responseData = Gson().fromJson(text, MyResponseData::class.java)

                    // 安全切換回主執行緒更新介面，顯示後端的 message 內容
                    runOnUiThread {
                        showResult("AI 回應 [${responseData.source}]: ${responseData.message}")
                    }
                } catch (e: Exception) {
                    Log.e("WebSocket", "JSON 解析失敗: ${e.message}，原始文字: $text")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // 連線失敗或中斷[cite: 1]
                runOnUiThread { showResult("連線失敗: ${t.message}") }
            }
        }

        client.newWebSocket(request, listener)
    }
    private fun sendAction(messages: String) {
        if (::webSocket.isInitialized){
        //多輪對話物件
        val request = MyRequestData(prompt = messages, screenshot = null)
        // 手動轉成 JSON 字串
        val jsonString = Gson().toJson(request)
        // 透過長連接送出
        webSocket.send(jsonString)
            Log.d("WebSocket", "已發送多輪對話訊息: $jsonString")
        }
        else {
            Toast.makeText(this, "WebSocket 尚未連線，無法發送", Toast.LENGTH_SHORT).show()
        }
    }


        // 發送按鈕點擊
        private fun setupClickListeners() {
            // 如果你 XML 有 btnVoice (在你的架構中它目前是 ImageButton)
            binding.btnVoice.setOnClickListener {
                val userInput = binding.etCommand.text.toString()
                if (userInput.isNotBlank()) {
                    processCommand(userInput)
                    binding.etCommand.text.clear()
                } else {
                    Toast.makeText(this, "請輸入指令", Toast.LENGTH_SHORT).show()
                }
            }
        }

        //      處理最終指令，決定呼叫哪個 AI 大腦
        private fun processCommand(cmd: String) {
            // 1. 顯示 Toast 讓使用者知道 App 有在動
            Toast.makeText(this, "MobileMind 正在連線 Python 後端...", Toast.LENGTH_SHORT).show()

            /* 2. 直接呼叫 Python 對接函數，不再判斷 mode
            callPython(cmd)*/
            // 2. 改為呼叫 WebSocket 的發送函數
            sendAction(cmd)

            //發送完指令後，讓鍵盤自動收起來
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.etCommand.windowToken, 0)
        }

        //    注意：404 錯誤通常發生在 ApiService 的 @POST 路徑拼錯，請檢查那邊

        /*private fun callPython(userPrompt: String) {
            // 1. 準備發送給 Python 的資料
            val request = MyRequestData(prompt = userPrompt)
            // 2. 使用 pythonService 發送請求
            pythonApiService.sendTestData(request).enqueue(object : Callback<MyResponseData> {
                override fun onResponse(
                    call: Call<MyResponseData>,
                    response: Response<MyResponseData>
                ) {
                    if (response.isSuccessful) {
                        val botReply = response.body()?.reply ?: "Python 回傳空值"
                        showResult(botReply) // 顯示在介面上
                    } else {
                        Log.e("DEBUG_STEP", "錯誤碼: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<MyResponseData>, t: Throwable) {
                    Log.e("DEBUG_STEP", "連線失敗: ${t.message}")
                }
            })
        }*/

        /*統一顯示結果的函數
        包含 runOnUiThread 確保在非同步請求後安全地更新 UI*/
//    private fun showResult(message: String) {
//        runOnUiThread {
//            // 做法 D：將後端回傳訊息加入 RecyclerView
//            taskAdapter.addTask(TaskItem(content = "AI 回覆：$message"))
//
//            // 原有的 Toast 保留作為偵錯用
//            Toast.makeText(this@MainActivity, "收到回覆", Toast.LENGTH_SHORT).show()
//
//            // 讓列表自動捲動到最上方
//            binding.rvRecentTasks.scrollToPosition(0)
//        }
//    }
        private fun showResult(message: String) {
            runOnUiThread {
                // 1. 將後端回傳的訊息設定給 XML 中的 TextView
                binding.tvAiResponse.text = message

                // 2. (選配) 如果你希望每次有新回覆時能自動滾動到最上方
                binding.nestedScrollView.smoothScrollTo(0, 0)

                // 原有的 Toast 可以保留，方便除錯
                Toast.makeText(this, "已更新回覆", Toast.LENGTH_SHORT).show()
            }
        }

        // 在 onActivityResult 接收結果
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data) // 資工系好習慣：呼叫父類別方法

            if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
                // 使用者點擊「立即開始」後，啟動你剛才在 Manifest 宣告的前台服務
                val serviceIntent = Intent(this, MediaProjectionService::class.java)
                serviceIntent.putExtra("data", data)

                // 根據 Android 版本啟動服務 (Android 8.0+ 建議用 startForegroundService)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            }
        }
    }
