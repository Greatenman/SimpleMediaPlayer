package com.example.simplemediaplayer.data.model.cache

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 简化的视频缓存管理器
 *
 * 架构讲解：
 * 1. 单一职责原则：这个类只负责缓存管理
 * 2. 依赖注入：通过构造函数传入Context，便于测试
 * 3. 错误处理：缓存失败不影响主流程
 *
 * 缓存策略：
 * 1. 本地视频 → 标记为已缓存（实际项目中会复制文件）
 * 2. 网络视频 → 缓存前2MB（边播边缓存的简化实现）
 *
 * 工作中应用场景：
 *   优化用户体验，减少网络请求，节省流量
 */

class SimpleCacheManager(private val context: Context) {
    companion object {
        private const val TAG = "VideoCache"
        private const val CACHE_DIR_NAME = "video_cache"
        private const val PREVIEW_SIZE = 2 * 1024 * 1024  // 2MB预览
    }
    private val cashDir = File(context.cacheDir,CACHE_DIR_NAME).apply {
        if(!exists()) mkdirs()
        Log.d(TAG, "缓存目录: $absolutePath")
    }   // 缓存目录（使用应用缓存目录，系统会自动清理）

    /**
     * 获取缓存文件（如果存在）
     *
     * 架构讲解：
     * 1. 文件名生成策略：使用URL的hashCode，避免特殊字符
     * 2. 文件存在性检查：避免重复下载
     * 3. 文件有效性检查：避免损坏的缓存文件
     */
    private fun getCacheFile(url: String): File {
        val fileName = "cache_${url.hashCode()}.tmp"
        return File(cashDir,fileName)
    }

    /**
     * 检查是否有缓存
     *
     * 工作中为什么这样设计：
     * 1. 先检查文件是否存在
     * 2. 再检查文件大小（避免下载中断的无效文件）
     * 3. 对于网络视频，有预览大小就认为有效
     */
    private fun hasCache(url: String): Boolean {
        val cacheFile = getCacheFile(url)
        return cacheFile.exists() && cacheFile.length() > 0
    }

    /**
     * 获取缓存URI（如果有）
     *
     * 架构讲解：
     * 1. 异步执行：使用suspend函数，不阻塞主线程
     * 2. 错误处理：try-catch包裹，失败返回原始URI
     * 3. 条件判断：只有网络视频才检查缓存
     */
    suspend fun getCacheUri(originalUri: Uri): Uri = withContext(Dispatchers.IO) {
        val originalUrl = originalUri.toString()
        return@withContext when {
            // 本地视频：直接返回（视为已有缓存）
            originalUrl.startsWith("android.resource://") -> {
                Log.d(TAG, "📱 本地视频，直接播放")
                originalUri
            }
            // 网络视频：检查缓存
            originalUrl.startsWith("http") && hasCache(originalUrl) -> {
                val cacheFile = getCacheFile(originalUrl)
                Log.d(TAG, "✅ 使用缓存: ${cacheFile.name} (${cacheFile.length()} bytes)")
                Uri.fromFile(cacheFile)
            }
            else -> {
                Log.d(TAG, "🌐 无缓存，使用原始URL")
                originalUri
            }
        }
    }

    /**
     * 开始缓存视频（后台任务）
     *
     * 架构讲解：
     * 1. 分离关注点：缓存是后台任务，不影响主线程
     * 2. 智能判断：只有网络视频才需要缓存
     * 3. 避免重复：检查是否已有缓存
     *
     * 工作中应用：
     *   在用户观看视频时，后台预缓存相关视频
     */
    fun startCache(uri: Uri) {
        val url = uri.toString()
        // 只缓存网络视频
        if(!url.startsWith("http") || hasCache(url)) {
            return
        }

        // 在后台线程执行缓存
        Thread {
            try {
                Log.d(TAG, "⏬ 开始缓存: ${url.take(50)}...")
                cacheNetworkVideo(url)
            } catch (e: Exception) {
                Log.e(TAG, "缓存失败: ${e.message}")
            }
        }.start()
    }

    /**
     * 缓存网络视频（简化版）
     *
     * 实现思路：
     * 1. 建立HTTP连接
     * 2. 只下载前2MB（支持断点续传的简化版）
     * 3. 保存到缓存文件
     */
    private fun cacheNetworkVideo(url: String) {
        var connection: HttpURLConnection? = null
        var inputStream: FileInputStream? = null
        var outputStream: FileOutputStream? = null
        try {
            val cacheFile = getCacheFile(url)
            // 如果文件已存在且有内容，跳过
            if (cacheFile.exists() && cacheFile.length() > PREVIEW_SIZE) {
                return
            }

            // 创建HTTP连接
            connection = URL(url).openConnection() as HttpURLConnection?
            connection?.connectTimeout = 10000
            connection?.readTimeout = 15000

            // 支持部分下载（Range请求）
            connection?.setRequestProperty("Range", "bytes=0-\${PREVIEW_SIZE - 1}")
            if (connection?.responseCode == 206) {
                connection.inputStream.use { httpStream ->
                    FileOutputStream(cacheFile).use { fileStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytes = 0L

                        // 读取数据并写入文件
                        while (httpStream.read(buffer).also { bytesRead = it } != -1 && totalBytes < PREVIEW_SIZE) {
                            val bytesToWrite = minOf(bytesRead, (PREVIEW_SIZE - totalBytes).toInt())
                            fileStream.write(buffer,0,bytesToWrite)
                            totalBytes += bytesToWrite
                        }

                        // 每512KB记录一次进度
                        if (totalBytes % (512 * 1024) == 0L) {
                            Log.d(TAG, "📥 已缓存: ${totalBytes / 1024}KB")
                        }
                    }
                }

            }

        } catch (e: Exception) {
            // 删除可能不完整的缓存文件
            getCacheFile(url).delete()
            throw e
        } finally {
            connection?.disconnect()
            inputStream?.close()
            outputStream?.close()
        }
    }

    /**
     * 清理所有缓存
     */
    fun clearAllCache() {
        cashDir.listFiles()?.forEach { it.delete() }
        Log.d(TAG, "🗑️ 已清理所有缓存")
    }

    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): String {
        val files = cashDir.listFiles() ?:return "已无缓存"
        val totalSizeKB = files.sumOf { it.length() } / 1024
        return "缓存文件: ${files.size}个, 总大小: ${totalSizeKB}KB"
    }
}