package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

class MediaProjectionService : Service() {

    private var mImageReader: ImageReader? = null
    private var mWidth: Int = 1080
    private var mHeight: Int = 1920
    private var mProjection: MediaProjection? = null
    private var mVirtualDisplay: VirtualDisplay? = null

    @RequiresApi(Build.VERSION_CODES.Q)

    override fun onCreate() {
        super.onCreate()
        // 服務一建立就先建立通知管道 (只需要做一次)
        createNotificationChannel()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "CHANNEL_ID")
            .setContentTitle("螢幕擷取中")
            .setContentText("MobileMind 正在運行自動化服務")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 確保這個圖片存在
            .build()

        // 啟動前台服務（ID 不可為 0）
        startForeground(1, notification)

        // ... 處理 MediaProjection 邏輯 ...

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "CHANNEL_ID", // 這裡的字串必須跟上面 Builder 用的一模一樣
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?) = null

    // 在 MediaProjectionService 裡 (部分邏輯)
    private fun captureScreen() {
        // 1. 從 ImageReader 獲取最新的影像幀
        val image = mImageReader?.acquireLatestImage() ?: return
        val planes = image.planes
        val buffer = planes[0].buffer

        // 2. 轉換為 Bitmap (資工系基本功：處理 Padding)
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * mWidth
        val bitmap =
            Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()

        // 3. 存入相簿
        saveBitmapToGallery(bitmap)
    }

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        val filename = "Screenshot_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            // Android 10+ 存入 Pictures/MobileMind 資料夾
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MobileMind")
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            contentResolver.openOutputStream(it).use { out ->
                if (out != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    Log.d("MediaProjection", "截圖已儲存: $filename")
                }
            }
        }
    }
}
