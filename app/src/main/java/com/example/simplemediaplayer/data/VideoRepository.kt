package com.example.simplemediaplayer.data

import android.R
import android.content.Context
import android.net.Uri
import androidx.compose.ui.text.LinkAnnotation
import com.example.simplemediaplayer.data.model.VideoItem

/**
 * 数据仓库 - 统一的数据入口
 *
 * 工作中为什么需要：
 * 1. 屏蔽数据来源的复杂性
 * 2. Activity不需要知道数据从哪里来
 * 3. 方便切换数据源（比如测试时用模拟数据）
 * 4. 统一的数据处理逻辑（如错误处理、缓存）
 */

class VideoRepository(private var context: Context) {
    private var networkVideos = listOf<VideoItem>(
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