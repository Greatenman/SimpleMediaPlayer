package com.example.simplemediaplayer.ui.viewmodel

import android.content.Context
import android.net.Uri
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
 * 工作中为什么需要：
 * 1. 分离业务逻辑和UI代码
 * 2. 生命周期感知（屏幕旋转数据不丢失）
 * 3. 方便单元测试（不依赖Android组件）
 * 4. 统一的状态管理
 */

class PlayerViewModel(
    private var videoRepository: VideoRepository
): ViewModel() {
    // UI状态数据类
    // 工作中：把所有UI相关的状态放在一个地方，一目了然
    data class UiState(
        val statusText: String = "准备就绪",   //状态
        val currentVideo: VideoItem? = null,     //video视频
        val playerState: PlayerState = PlayerState.Idle,    //video状态
        val progress: Int = 0,     //进度条
        val isLoading: Boolean = false      //是否加载中
    )

    // 使用StateFlow管理状态（工作中推荐）
    private var _uiState = MutableStateFlow(UiState())

    var  uiState: StateFlow<UiState> = _uiState

    /**
     * 播放本地视频
     */
    fun playLocalVideo(uriString: String) {
        viewModelScope.launch {
                // 工作中：ViewModel处理所有业务逻辑

                // 1. 更新状态：加载中
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    statusText = "加载本地视频.."
                )
            try {
                // 2. 从Repository获取数据
                var uri = uriString
                videoRepository.setLocalVideoUri(uriString.toUri())
                var video = VideoItem(
                    Id = "local",
                    url = uri.toString(),
                    title = "本地视频",
                    format = "MP4"
                )
                // 3. 更新状态：成功
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentVideo = video,
                    playerState = PlayerState.Ready,
                    statusText = "播放本地视频"
                )
            } catch (e: Exception) {
                // 4. 更新状态：错误
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    playerState = PlayerState.Error(e.message ?: "未知错误"),
                    statusText = "加载失败"
                )
            }
        }
    }

    /**
     * 播放网络视频
     */
    fun playNetworkVideo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                statusText = "加载本地视频.."
            )
            try {
                var video = videoRepository.getNetworkVideos()
                if(video.isNotEmpty()){
                    var video = video.first()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentVideo = video,
                        playerState = PlayerState.Ready,
                        statusText = "播放网络视频: ${video.title}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    playerState = PlayerState.Error(e.message ?: "未知错误"),
                    statusText = "加载失败"
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
    }

    /**
     * 更新播放进度
     */
    fun updateProgress(progress: Int) {
        _uiState.value = _uiState.value.copy(progress = progress)
    }

    fun updateText(text: String) {
        _uiState.value = _uiState.value.copy(statusText = text)
    }

}