package com.example.myapplication

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class MyAccessibilityService : AccessibilityService() {
    // 當服務成功啟動時觸發
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("Accessibility", "無障礙服務已連接")
    }

    // 當系統偵測到 UI 變動（如切換視窗、點擊）時觸發
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 自動化邏輯
        val eventType = event?.eventType

        // 監聽：點擊 或 視窗切換
        if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
            eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {

            Log.d("Accessibility", "偵測到操作，準備截圖...")

            // 發送指令給 MediaProjectionService 進行截圖
            val intent = Intent("ACTION_TAKE_SCREENSHOT")
            sendBroadcast(intent)
        }

        // 當服務中斷時觸發
        fun onInterrupt() {
            Log.e("Accessibility", "服務中斷")
        }
    }

    override fun onInterrupt() {
        TODO("Not yet implemented")
    }
}
