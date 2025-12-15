package com.example.simplemediaplayer

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import com.example.simplemediaplayer.data.VideoRepository
import com.example.simplemediaplayer.databinding.ActivityMainBinding
import com.example.simplemediaplayer.ui.viewmodel.PlayerViewModel
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import androidx.lifecycle.lifecycleScope
import com.example.simplemediaplayer.ui.viewmodel.PlayerViewModelFactory
import com.google.android.exoplayer2.MediaItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.app.AlertDialog
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import kotlin.math.log

/**
 * MainActivity - View层
 *
 * 架构讲解：
 * 1. 单一职责：只负责UI显示和用户交互
 * 2. 观察者模式：观察ViewModel的状态变化
 * 3. 生命周期管理：正确初始化和释放资源
 *
 * Activity的职责：
 * 1. 初始化UI组件
 * 2. 设置事件监听器
 * 3. 观察ViewModel状态并更新UI
 * 4. 处理Android生命周期
 * 5. 执行具体的UI操作（如播放视频）
 */
class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    //==================== 1. 变量声明 ====================

    // ViewBinding（替代findViewById）
    private lateinit var binding: ActivityMainBinding

    // ==================== 2. UI控件 ====================
    private lateinit var playerView: StyledPlayerView
    private lateinit var btnPlayLocal: Button
    private lateinit var btnPlayNetwork: Button
    private lateinit var btnPause: Button
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnStartStory: Button
    private lateinit var btnCacheTest: Button

    //播放器（Activity持有，因为与UI生命周期相关）
    private lateinit var player: ExoPlayer

    // ==================== 4. MVVM组件 ====================
    // ViewModel（通过委托创建，自动管理生命周期
    private val viewModel: PlayerViewModel by viewModels {
        // 提供ViewModel的工厂，传入需要的依赖
        PlayerViewModelFactory(
            VideoRepository(this)
        )
    }
    // 数据仓库（用于缓存操作）
    private lateinit var repository: VideoRepository

    // ==================== 2. 原有故事功能 ====================
    private var clickCount = 0
    private var currentVideoUrl = ""
    private var nextChoice1 = ""
    private var nextChoice2 = ""
    private var nextVideo1 = ""
    private var nextVideo2 = ""

    data class StoryNode(
        val title: String,
        val videoUrl: String,
        val nextChoice1: String,
        val nextChoice2: String,
        val nextVideo1: String,
        val nextVideo2: String,
        val choiceTime: Long = 10000
    )

    // ==================== 3. onCreate ====================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        // 工作中：使用ViewBinding更安全
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //初始化video仓库
        repository = VideoRepository(this)

        // 初始化UI
        initViews()

        // 初始化播放器
        initPlayer()

        // 设置点击监听
        setupClickListeners()

        // 观察ViewModel的状态变化
        observeViewModel()

        // 开始日志
        startLogging()


        Log.d(TAG, "✅ 架构初始化完成")
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG,"onStart")
        if (player == null) {
            initPlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("Lifecycle", "onStop")
        player?.release()
    }

    // ==================== 4. 初始化视图 ====================
    private fun initViews() {
        playerView = binding.playerView
        btnPlayLocal = binding.btnPlayLocal
        btnPlayNetwork = binding.btnPlayNetwork
        btnPause = binding.btnPause
        tvStatus = binding.tvStatus
        progressBar = binding.progressBar
        btnStartStory = binding.btnStartStory

        // 添加缓存测试按钮
        btnCacheTest = Button(this).apply {
            text = "缓存测试"
            setOnClickListener { showCacheMenu() }
        }
        (binding.root as? android.widget.LinearLayout)?.addView(btnCacheTest)
    }

    // ==================== 8. 观察ViewModel ====================
    /**
     * 工作中为什么要观察ViewModel？
     * 因为ViewModel负责管理状态，Activity只需要根据状态更新UI
     * 实现了"数据驱动UI"
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            // 监听UI状态变化
            viewModel.uiState.collect { uiState ->
                // 更新UI
                tvStatus.text = uiState.statusText
                progressBar.progress = uiState.progress

                // 根据状态更新按钮文本
                btnPause.text = if (uiState.playerState == com.example.simplemediaplayer.data.model.PlayerState.Idle) {
                    "继续"
                } else {
                    "暂停"
                }

                // 处理加载状态
                if (uiState.isLoading) {
                    // 可以显示加载动画
                    Log.d("UI", "正在加载...")
                }
            }
        }
    }

    // ==================== 6. 点击监听 ====================
    private fun setupClickListeners() {
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                updatePlayerStatus(state)
            }
        })
        // 按钮1：播放本地视频
        btnPlayLocal.setOnClickListener {
            Log.d(TAG, "点击了【播本地】")
            // 工作中：Activity只做两件事：
            // 1. 调用ViewModel的方法（告诉ViewModel用户做了什么）
            viewModel.playLocalVideo()
           lifecycleScope.launch {
               // 2. 执行UI相关的操作（播放器控制）
               try {
                   var localUri = Uri.parse("android.resource://$packageName/raw/sample")
                   playVideoWithCache(localUri, "本地视频", isLocal = true)
//                viewModel.playVideo(localUri,"本地视频正在播放中")
                   Log.d(TAG, "播放本地视频")
               } catch (e: Exception) {
                   // 错误可以交给ViewModel处理
                   Log.e(TAG, "本地视频错误: ${e.message}")
               }
           }
        }

        // 按钮2：播放网络视频
        btnPlayNetwork.setOnClickListener {
            Log.d("CLICK", "点击了【播网络】")
            // 调用ViewModel
            viewModel.playNetworkVideo()
            val videos = listOf(
                Triple(
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    "MP4格式",
                    "🐰 兔兔视频"
                ),
                Triple(
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    "MP4格式",
                    "🐘 大象之梦"
                )
            )

            // 轮换播放（原有逻辑）
            val (url, format, desc) = videos[clickCount % videos.size]
            clickCount++
            Log.d(TAG, "播放 $format: ${url.take(50)}...  名字 $desc")
            // 播放操作
            if (url.startsWith("http")) {
                var uri = Uri.parse(url)
                lifecycleScope.launch {
                    playVideoWithCache(Uri.parse(url), desc, isLocal = false)
                }
                // 开始更新进度条
                startProgressUpdate()
            }
        }

        btnStartStory.setOnClickListener {
            Log.d("CLICK", "点击了【播互动】")
            // 原有故事功能
            setupStoryButton()
        }

        btnPause.setOnClickListener {
            Log.d("CLICK", "点击了【暂停】")
            val isPlaying = player?.isPlaying == true

            // 通知ViewModel
            viewModel.togglePlayPause(isPlaying)
            if (player?.isPlaying == true) {
                player?.pause()
                btnPause.text = "继续"
                viewModel.updateText("暂停中")
                Log.d("myceshi", "已暂停")
            } else {
                player?.play()
                btnPause.text = "暂停"
                viewModel.updateText("播放中")
                Log.d(TAG, "继续播放")
            }
        }
    }

    // ==================== 7. 视频播放方法 ====================
    /**
     * 带缓存的视频播放方法
     *
     * 架构讲解：
     * 1. 分离关注点：播放操作在Activity中
     * 2. 异步处理：使用协程避免阻塞主线程
     * 3. 降级处理：缓存失败时使用原始URI
     */
    private suspend fun playVideoWithCache(uri: Uri, title: String, isLocal: Boolean = false) {
        Log.d("CACHE", "播放视频: $title (isLocal: $isLocal)")
        try {
            val cacheUri = repository.getCachedVideoUri(uri)
            val finalUri = if (cacheUri.toString() != uri.toString()) {
                Log.d("CACHE", "✅ 使用缓存播放")
                "🎯 [缓存] $title"
                cacheUri
            }  else {
                Log.d("CACHE", "🌐 无缓存，直接播放")
                "🌐 [网络] $title"
                uri
            }
            // 更新UI状态
            viewModel.updateText("播放: $title")

            // 播放视频
            val mediaItem = MediaItem.fromUri(finalUri)
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.play()

            // 更新缓存
            viewModel.refreshCacheInfo()
        } catch (e: Exception) {
            Log.e(TAG, "缓存播放失败: ${e.message}")
            // 降级：直接播放
            mainPlayVideo(uri, title)
        }

    }


    private fun initPlayer() {
        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        player?.addListener(object : Player.Listener{
            override fun onPlaybackStateChanged(state: Int) {
                updateStatus(state)
            }
        })
        viewModel.updateText("播放器就绪，请选择视频")
    }
    private fun updateStatus(state: Int) {
        runOnUiThread {
            when(state) {
                Player.STATE_IDLE -> Log.d("PLAYER", "状态：正在缓冲")
                Player.STATE_READY -> Log.d("PLAYER", "状态：准备就绪")
                Player.STATE_BUFFERING -> Log.d("PLAYER", "状态：播放结束")
                Player.STATE_ENDED -> Log.d("PLAYER", "状态：空闲")
            }
        }
    }

    // ==================== 11. 进度条更新 ====================
    private fun startProgressUpdate() {
        lifecycleScope.launch {
            while (true) {
                delay(200)
                player?.let { p -> {
                    if (p.duration > 0) {
                        var progress = (p.currentPosition.toFloat() / p.duration * 100).toInt()
                        runOnUiThread {
                            progressBar.progress = progress
                            // 通知ViewModel更新进度
                            viewModel.updateProgress(progress)
                        }
                    }
                } }
            }
        }
    }

    private fun setupStoryButton() {
        Log.d("DAY2", "开始互动故事")
        var text: String = "开始播放故事..."
//        tvStatus.text = "开始播放故事..."
        viewModel.updateText(text)
        val startNode = StoryNode(
            title = "冒险开始",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            nextChoice1 = "向左走，探索森林",
            nextChoice2 = "向右走，前往城堡",
            nextVideo1 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            nextVideo2 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"
        )
        currentVideoUrl = startNode.videoUrl
        nextChoice1 = startNode.nextChoice1
        nextChoice2 = startNode.nextChoice2
        nextVideo1 = startNode.nextVideo1
        nextVideo2 = startNode.nextVideo2

//        viewModel.playVideo(currentVideoUrl.toUri(), startNode.title)
        mainPlayVideo(currentVideoUrl.toUri(), startNode.title)
        lifecycleScope.launch {
            delay(10000)
            showStoryChoice()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showStoryChoice() {
        runOnUiThread {
            player.pause()
            AlertDialog.Builder(this)
                .setTitle("请选择")
                .setMessage("故事发展到关键点，你要怎么选择？")
                .setPositiveButton(nextChoice1) { _, _ ->
                    Log.d("CHOICE", "选择了: $nextChoice1")
                    viewModel.updateText("选择了: $nextChoice1")
//                    viewModel.playVideo(nextVideo1.toUri(), "森林结局")
                    mainPlayVideo(nextVideo1.toUri(), "森林结局")
                }
                .setNegativeButton(nextChoice2) { _, _ ->
                    Log.d("CHOICE", "选择了: $nextChoice2")
                    tvStatus.text = "选择了: $nextChoice2"
//                    viewModel.playVideo(nextVideo2.toUri(), "城堡结局")
                    mainPlayVideo(nextVideo2.toUri(), "城堡结局")
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun mainPlayVideo(url: Uri, title: String) {
        tvStatus.text = "播放: $title"

        val mediaItem = MediaItem.fromUri(url)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()
    }

    private fun startLogging() {
        Log.d("LEARNING", "🎬 ========== 开始学习音视频开发 ==========")
        Log.d("LEARNING", "1. ExoPlayer版本: 2.19.1")
        Log.d("LEARNING", "2. 支持格式: MP4, HLS, DASH")
        Log.d("LEARNING", "3. 线程模型: 主线程UI + 后台解码线程")

        // 打印当前线程信息
        Log.d("xiancheng", "主线程: ${Thread.currentThread().name}")
    }

    // ==================== 8. 缓存相关功能 ====================

    private fun showCacheMenu() {
        AlertDialog.Builder(this)
            .setTitle("缓存测试")
            .setItems(arrayOf("查看缓存信息", "清理缓存", "测试重复播放", "测试本地缓存")) { _, choiceNumber ->
                when (choiceNumber) {
                    0 -> showCacheInfo()
                    1 -> clearCache()
                    2 -> testRepeatPlay()
                    3 -> testLocalCache()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showCacheInfo() {
        val stats = repository.getCacheStats()
        AlertDialog.Builder(this)
            .setTitle("缓存信息")
            .setMessage(stats)
            .setPositiveButton("确定", null)
            .show()

        // 更新ViewModel中的缓存信息
        viewModel.refreshCacheInfo()
    }

    private fun clearCache() {
        repository.clearCache()
        Toast.makeText(this, "缓存已清理", Toast.LENGTH_SHORT).show()
        viewModel.refreshCacheInfo()
    }

    private fun testRepeatPlay() {
        // 测试缓存效果：连续播放同一个网络视频
        val testUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"

        lifecycleScope.launch {
            val uri = Uri.parse(testUrl)

            // 第一次播放
            viewModel.updateText("第一次播放（网络）")
            playVideoWithCache(uri, "测试视频")

            delay(3000) // 播放3秒

            player?.pause()
            delay(500)

            // 第二次播放（应该有缓存）
            viewModel.updateText("第二次播放（应该从缓存）")
            playVideoWithCache(uri, "测试视频-缓存")
        }
    }

    private fun testLocalCache() {
        // 测试本地视频缓存
        lifecycleScope.launch {
            val localUri = Uri.parse("android.resource://$packageName/raw/sample")
            viewModel.updateText("测试本地视频缓存")
            playVideoWithCache(localUri, "本地测试视频", isLocal = true)
        }
    }

    // ==================== 10. 辅助方法 ====================

    private fun updatePlayerStatus(state: Int) {
        runOnUiThread {
            when(state) {
                Player.STATE_BUFFERING -> {
                    Log.d("PLAYER", "状态：正在缓冲")
                    viewModel.updateText("缓冲中...")
                }
                Player.STATE_READY -> {
                    Log.d("PLAYER", "状态：准备就绪")
                }
                Player.STATE_ENDED -> {
                    Log.d("PLAYER", "状态：播放结束")
                    viewModel.updateText("播放结束")
                }
                Player.STATE_IDLE -> {
                    Log.d("PLAYER", "状态：空闲")
                }
            }
        }
    }

}







