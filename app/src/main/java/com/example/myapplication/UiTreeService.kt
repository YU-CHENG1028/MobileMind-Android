package com.example.myapplication

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import com.google.gson.GsonBuilder

class UiTreeService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.packageName == "com.wisdomgarden.trpc") {
            val rootNode = rootInActiveWindow ?: return

            // 執行 DFS 遞迴轉換
            val uiTreeData = parseAccessibilityNode(rootNode)

            // 轉成漂亮的 JSON 字串
            val json = GsonBuilder().setPrettyPrinting().create().toJson(uiTreeData)

            // 保留 Log 用於除錯
            Log.d("UiTreeService", "成功抓取 UI 樹，節點文字: ${uiTreeData.text}")

            // 如果你想看完整的漂亮的 JSON，可以使用這個（建議開發完後註釋掉）
            // Log.d("UiTreeService", "TronClass UI Tree JSON:\n$json")

            // 把資料送給 MainActivity 的關鍵
            val intent = Intent("COM_MOBILEMIND_UI_UPDATED")
            intent.putExtra("UI_JSON", json)
            sendBroadcast(intent)

            // 釋放資源 (這行非常重要)
            rootNode.recycle()
        }
    }

    private fun parseAccessibilityNode(node: AccessibilityNodeInfo): UiNode {
        val children = mutableListOf<UiNode>()

        // 深度優先搜尋 (DFS) 遍歷子節點
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                children.add(parseAccessibilityNode(child))
                child.recycle() // 處理完子節點立即釋放
            }
        }

        // 讀取當前節點屬性
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)

        return UiNode(
            text = node.text?.toString() ?: "",
            resourceId = node.viewIdResourceName ?: "",
            className = node.className?.toString() ?: "",
            bounds = rect.toShortString(),
            children = if (children.isNotEmpty()) children else null
        )
    }

    override fun onInterrupt() {}
}

// 定義資料結構
data class UiNode(
    val text: String,
    val resourceId: String,
    val className: String,
    val bounds: String,
    val children: List<UiNode>? = null
)