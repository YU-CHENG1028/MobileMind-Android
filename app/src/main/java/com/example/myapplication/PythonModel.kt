package com.example.myapplication

// 1. 傳給後端的初始訊息格式 (啟動 Agent 用)
data class InitialMessage(
    val first_messages: String
)

// 2. 多輪對話發送格式
data class MyRequestData(
    val prompt: String,
    val screenshot: String? = null
)

// 3. 接收後端回傳內容的類別 (與後端 source, message, timestamp 完全對齊)
data class MyResponseData(
    val source: String,
    val message: String,
    val timestamp: String
)