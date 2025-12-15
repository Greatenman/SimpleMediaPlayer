package com.example.simplemediaplayer.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.simplemediaplayer.data.VideoRepository
import com.example.simplemediaplayer.data.model.PlayerState
import com.example.simplemediaplayer.data.model.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel - 管理UI相关的数据和逻辑
 *
 * 架构讲解：
 * 1. 生命周期感知：ViewModel在配置变更时不会重建
 * 2. 业务逻辑集中：所有UI相关的逻辑都在这里
 * 3. 数据驱动UI：通过StateFlow/LiveData通知UI更新
 *
 * ViewModel的职责：
 * 1. 准备UI需要的数据
 * 2. 处理用户输入
 * 3. 执行业务逻辑
 * 4. 管理UI状态
 *
 * 重要：ViewModel不应该持有View/Activity的引用！
 */

class PlayerViewModel(
    private var videoRepository: VideoRepository
): ViewModel() {

    companion object {
        private const val TAG = "PlayerViewModel"
    }

    // ==================== UI状态管理 ====================

    /**
     * UI状态数据类
     *
     * 架构讲解：
     * 1. 不可变数据：使用data class + copy()更新状态
     * 2. 状态集中：所有UI相关的状态都在这里
     * 3. 类型安全：编译器会检查状态更新
     */
    data class UiState(
        val statusText: String = "准备就绪",   //状态
        val currentVideo: VideoItem? = null,     //video视频
        val playerState: PlayerState = PlayerState.Idle,    //video状态
        val progress: Int = 0,     //进度条
        val isLoading: Boolean = false,      //是否加载中
        val cacheInfo: String = ""   //缓存状态
    )

    // 使用StateFlow管理状态
    private var _uiState = MutableStateFlow(UiState())

    var  uiState: StateFlow<UiState> = _uiState

    /**
     * 播放本地视频
     *
     * 架构讲解：
     * 1. 状态更新：先更新为Loading状态
     * 2. 业务逻辑：调用Repository获取数据
     * 3. 状态更新：根据结果更新UI状态
     * 4. 错误处理：捕获异常并更新错误状态
     */
    fun playLocalVideo() {
        viewModelScope.launch {
            Log.d(TAG, "播放本地视频")

            // 更新为加载状态
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                statusText = "加载本地视频中..."
            )

            try {
                // 获取本地视频URI
                val uri = videoRepository.getLocalVideoUri()

                // 创建视频对象
                val video = VideoItem(
                    Id = "local_${System.currentTimeMillis()}",
                    url = uri.toString(),
                    title = "本地视频",
                    format = "MP4"
                )

                // 更新状态
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentVideo = video,
                    playerState = PlayerState.Ready,
                    statusText = "准备播放本地视频",
                    cacheInfo = "📱 本地视频（完整缓存）"
                )

            } catch (e: Exception) {
                Log.e(TAG, "播放本地视频失败: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    playerState = PlayerState.Error("加载失败: ${e.message}"),
                    statusText = "本地视频加载失败"
                )
            }
        }
    }

    /**
     * 播放网络视频
     *
     * 工作中为什么这样设计：
     * 1. 分离关注点：ViewModel只管理状态，不操作播放器
     * 2. 状态驱动：UI根据状态变化自动更新
     * 3. 易于测试：可以mock Repository进行单元测试
     */
    fun playNetworkVideo(video: VideoItem? = null) {
        viewModelScope.launch {
            Log.d(TAG, "播放网络视频")
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                statusText = "加载网络视频.."
            )
            try {
                var targetVideo = videoRepository.getNetworkVideos().firstOrNull()
                if(targetVideo != null){
                    val cacheStats = videoRepository.getCacheStats()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentVideo = video,
                        playerState = PlayerState.Ready,
                        statusText = "播放网络视频: ${video?.title}",
                        cacheInfo = "🌐 网络视频 ($cacheStats)"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        playerState = PlayerState.Error("没有可用的视频"),
                        statusText = "没有找到视频"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "播放网络视频失败: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    playerState = PlayerState.Error("网络错误: ${e.message}"),
                    statusText = "网络视频加载失败"
                )
            }
        }
    }

    /**
     * 切换播放/暂停
     */
    fun togglePlayPause(isPlaying: Boolean) {
        var newState = if (isPlaying) {
            PlayerState.Idle to "已暂停"
        } else {
            PlayerState.Ready to "播放中"
        }
        _uiState.value = _uiState.value.copy(
            playerState = newState.first,
            statusText = newState.second
        )
        Log.d(TAG, "切换播放状态: ${newState.second}")
    }

    /**
     * 更新播放进度
     */
    fun updateProgress(progress: Int) {
        _uiState.value = _uiState.value.copy(progress = progress)
    }

    /**
     * 更新状态文本
     */
    fun updateText(text: String) {
        _uiState.value = _uiState.value.copy(statusText = text)
    }

    /**
     * 获取缓存信息
     */
    fun refreshCacheInfo() {
        viewModelScope.launch {
            val cacheStats = videoRepository.getCacheStats()
            _uiState.value = _uiState.value.copy(cacheInfo = cacheStats)
        }
    }

}