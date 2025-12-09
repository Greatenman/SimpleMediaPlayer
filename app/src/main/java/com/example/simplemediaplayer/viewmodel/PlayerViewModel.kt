package com.example.simplemediaplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.simplemediaplayer.manager.CacheManager

class PlayerViewModel(application: Application): AndroidViewModel(application) {
    companion object {
        private const val TAG = "PlayerViewModel"
    }

    // ==================== 播放器实例 ====================
    private var player: ExoPlayer? = null

    // ==================== LiveData状态定义 ====================

    // 播放状态
    private val _playerState = MutableLiveData<Int>(Player.STATE_IDLE)
    val playerState: LiveData<Int> = _playerState

    private val _isPlaying = MutableLiveData<Boolean>(false)
    val isPlaying: LiveData<Boolean> = _isPlaying

    // 视频信息
    private val _currentVideoInfo = MutableLiveData<String>("Day 3: MVVM架构就绪")
    val currentVideoInfo: LiveData<String> = _currentVideoInfo

    // 故事状态
    private val _currentStoryNode = MutableLiveData<String>("start")
    val currentStoryNode: LiveData<String> = _currentStoryNode

    private val _showDecisionDialog = MutableLiveData<Boolean>(false)
    val showDecisionDialog: LiveData<Boolean> = _showDecisionDialog

    private val _decisionChoices = MutableLiveData<Pair<String, String>>()
    val decisionChoices: LiveData<Pair<String, String>> = _decisionChoices

    // 缓存信息
    private val _cacheInfo = MutableLiveData<String>("")
    val cacheInfo: LiveData<String> = _cacheInfo

    // ==================== 业务逻辑变量 ====================
    private var decisionTimerJob: Job? = null
    private var isInStoryMode = false

    // ==================== 初始化 ====================
    init {
        Log.d(TAG, "PlayerViewModel 初始化")
        initializePlayer()
        updateCacheInfo()
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(getApplication())
            .build()
            .also { exoPlayer ->
                // 监听播放器状态
                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        Log.d(TAG, "播放状态变化: $playbackState")
                        _playerState.value = playbackState
                        _isPlaying.value = exoPlayer.isPlaying

                        // 播放结束时处理
                        if (playbackState == Player.STATE_ENDED && isInStoryMode) {
                            handleStoryEnded()
                        }
                    }

                    override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
                        Log.e(TAG, "播放错误: ${error.message}")
                        _currentVideoInfo.value = "❌ 播放错误"
                    }
                })
            }

        Log.d(TAG, "播放器初始化完成")
    }

    // ==================== Day 1 方法 ====================

    /** 播放本地视频 */
    fun playLocalVideo() {
        Log.d(TAG, "播放本地视频")

        try {
            val uri = Uri.parse("android.resource://${getApplication<Application>().packageName}/raw/sample")
            playVideoWithCache(uri, "📱 本地视频播放")

            // 退出故事模式
            exitStoryMode()

        } catch (e: Exception) {
            Log.e(TAG, "本地播放失败", e)
            _currentVideoInfo.value = "❌ 本地视频错误: 请检查raw文件夹"
        }
    }

    /** 播放网络视频 */
    fun playNetworkVideo(url: String, title: String) {
        Log.d(TAG, "播放网络视频: $title")

        try {
            val uri = Uri.parse(url)
            playVideoWithCache(uri, "🌐 网络视频: $title")

            // 退出故事模式
            exitStoryMode()

        } catch (e: Exception) {
            Log.e(TAG, "网络播放失败", e)
            _currentVideoInfo.value = "❌ 网络播放失败"
        }
    }

    /** 暂停/播放切换 */
    fun togglePlayPause() {
        player?.let { exoPlayer ->
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
                Log.d(TAG, "播放暂停")
                _currentVideoInfo.value = "⏸️ 已暂停"

                // 暂停时取消决策计时器
                if (isInStoryMode) {
                    decisionTimerJob?.cancel()
                }
            } else {
                exoPlayer.play()
                Log.d(TAG, "继续播放")
                _currentVideoInfo.value = "▶️ 继续播放"

                // 恢复时重新开始决策计时器
                if (isInStoryMode && _currentStoryNode.value == "start") {
                    startDecisionTimer()
                }
            }
        }
    }

    // ==================== Day 2 方法 ====================

    /** 开始互动故事 */
    fun startInteractiveStory() {
        Log.d(TAG, "开始互动故事")

        isInStoryMode = true
        _currentStoryNode.value = "start"
        _currentVideoInfo.value = "🎬 故事开始: 冒险启程"

        // 播放开始节点
        val startUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
        val uri = Uri.parse(startUrl)
        playVideoWithCache(uri, "冒险开始")

        // 启动决策计时器
        startDecisionTimer()
    }

    /** 用户做出选择 */
    fun makeStoryChoice(choice: String) {
        Log.d(TAG, "用户选择: $choice")

        _showDecisionDialog.value = false
        decisionTimerJob?.cancel()

        when (choice) {
            "left" -> {
                _currentStoryNode.value = "forest"
                val url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
                playVideoWithCache(Uri.parse(url), "🌳 森林结局")
            }
            "right" -> {
                _currentStoryNode.value = "castle"
                val url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"
                playVideoWithCache(Uri.parse(url), "🏰 城堡结局")
            }
        }
    }

    /** 取消故事模式 */
    fun cancelStoryMode() {
        isInStoryMode = false
        _currentStoryNode.value = "start"
        _showDecisionDialog.value = false
        decisionTimerJob?.cancel()
        Log.d(TAG, "退出故事模式")
    }

    // ==================== 私有方法 ====================

    /** 使用缓存的播放方法 */
    private fun playVideoWithCache(uri: Uri, title: String) {
        player?.let { exoPlayer ->
            // 获取缓存管理器
            val cache = CacheManager.getInstance(getApplication()).cache

            // 创建带缓存的数据源工厂
            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(
                    DefaultHttpDataSource.Factory()
                        .setUserAgent(Util.getUserAgent(getApplication(), "VideoPlayer"))
                )
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            // 创建媒体源
            val mediaSourceFactory = com.google.android.exoplayer2.source.DefaultMediaSourceFactory(cacheDataSourceFactory)

            // 创建并播放媒体项
            val mediaItem = MediaItem.fromUri(uri)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()

            _currentVideoInfo.value = title
            Log.d(TAG, "开始播放: $title")
        }
    }

    /** 启动决策计时器 */
    private fun startDecisionTimer() {
        decisionTimerJob?.cancel()

        decisionTimerJob = viewModelScope.launch {
            Log.d(TAG, "决策计时器启动，10秒后触发")
            delay(10000) // 10秒

            // 检查是否还在播放开始节点
            if (isInStoryMode &&
                _currentStoryNode.value == "start" &&
                (player?.isPlaying == true)) {

                Log.d(TAG, "触发决策点")
                _showDecisionDialog.value = true
                _decisionChoices.value = Pair("向左走，探索森林", "向右走，前往城堡")

                // 暂停播放
                player?.pause()
                _currentVideoInfo.value = "🤔 请做出选择..."
            }
        }
    }

    /** 处理故事结束 */
    private fun handleStoryEnded() {
        Log.d(TAG, "故事播放结束")
        _currentVideoInfo.value = "🎉 故事结束"
        isInStoryMode = false
    }

    /** 退出故事模式 */
    private fun exitStoryMode() {
        if (isInStoryMode) {
            cancelStoryMode()
            _currentVideoInfo.value = "已退出故事模式"
        }
    }

    // ==================== 缓存管理方法 ====================

    /** 更新缓存信息 */
    private fun updateCacheInfo() {
        viewModelScope.launch {
            val cacheInfo = CacheManager.getInstance(getApplication()).getCacheInfo()
            _cacheInfo.value = cacheInfo
        }
    }

    /** 清空缓存 */
    fun clearCache() {
        CacheManager.getInstance(getApplication()).clearCache()
        updateCacheInfo()
        _currentVideoInfo.value = "🗑️ 缓存已清空"
        Log.d(TAG, "缓存已清空")
    }

    // ==================== 获取播放器 ====================

    fun getPlayer(): ExoPlayer? {
        return player
    }

    // ==================== 清理资源 ====================

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "PlayerViewModel 被销毁，清理资源")

        decisionTimerJob?.cancel()
        player?.release()
        player = null

        Log.d(TAG, "资源清理完成")
    }
}