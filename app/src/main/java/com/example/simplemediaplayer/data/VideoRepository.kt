package com.example.simplemediaplayer.data

import android.R
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.text.LinkAnnotation
import com.example.simplemediaplayer.data.model.VideoItem
import com.example.simplemediaplayer.data.model.cache.SimpleCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 数据仓库（Repository）
 *
 * 架构讲解：
 * 1. 单一数据源：所有数据都通过Repository获取
 * 2. 数据抽象：ViewModel不需要知道数据来自网络还是本地
 * 3. 数据转换：将原始数据转换为业务需要的数据
 *
 * Repository模式的优势：
 * 1. 统一错误处理
 * 2. 统一缓存策略
 * 3. 便于数据源切换（如从API切换到本地数据库）
 *
 * 工作中应用：
 *   当应用需要从多个数据源获取数据时（API、数据库、文件等），
 *   用Repository统一管理
 */

class VideoRepository(private var context: Context) {

    // 依赖注入：缓存管理器
    private val cacheManager = SimpleCacheManager(context)

    private val networkVideos = listOf<VideoItem>(
        VideoItem(
            Id = "1",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            title = "兔兔视频",
            format = "MP4"
        ),
        VideoItem(
            Id = "2",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            title = "大象视频",
            format = "MP4"
        )
    )

    // ==================== 缓存相关方法 ====================

    /**
     * 获取带缓存的视频URI
     *
     * 架构讲解：
     * 1. 业务逻辑封装：判断视频类型、检查缓存、触发缓存
     * 2. 异步处理：使用suspend函数，避免阻塞
     * 3. 降级处理：缓存失败时返回原始URI
     */
    suspend fun getCachedVideoUri(originalUri: Uri): Uri = withContext(Dispatchers.IO) {
        try {// 1. 获取缓存URI（如果有）
            val cachedUri = cacheManager.getCacheUri(originalUri)
            // 2. 如果是网络视频且没有缓存，开始后台缓存
            val originalUrl = originalUri.toString()
            if (originalUrl.startsWith("http") && originalUri == cachedUri) {
                cacheManager.startCache(originalUri)
            }
            return@withContext cachedUri
        } catch (e: Exception) {
            Log.e("Repository", "获取缓存失败: ${e.message}")
            return@withContext originalUri
        }
    }

    /**
     * 清理缓存
     */
    fun clearCache() {
        cacheManager.clearAllCache()
    }

    /**
     * 获取缓存统计
     */
    fun getCacheStats(): String {
        return cacheManager.getCacheStats()
    }



    // ==================== 数据获取方法 ====================

    private lateinit var localVideoUri: Uri

    /**
     * 获取本地视频URI
     * 工作中：这里可以处理各种异常情况
     */

    fun setLocalVideoUri(newLocalUri: Uri) {
        localVideoUri = newLocalUri
    }

    fun getLocalVideoUri(): Uri {
        return localVideoUri
    }

    /**
     * 获取网络视频
     * 工作中：这里可以添加网络错误处理、重试逻辑等
     */

    fun getNetworkVideos(): List<VideoItem> {
        // 实际工作中这里可能是网络请求
        return networkVideos
    }

    /**
     * 根据ID获取视频
     * 工作中：统一的数据获取接口，方便后续扩展
     */

    fun getVideoById(id: String): VideoItem? {
        return networkVideos.find { it.Id == id }
    }

    /**
     * 根据标题获取视频
     * 工作中：统一的数据获取接口，方便后续扩展
     */
    fun getVideoByTitle(titile: String): VideoItem? {
        return networkVideos.find { it.title == titile }
    }
}