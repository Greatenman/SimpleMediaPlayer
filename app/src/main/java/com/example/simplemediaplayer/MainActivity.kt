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
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel


class MainActivity : ComponentActivity() {
    // ==================== 1. 视图绑定 ====================
    private lateinit var binding: ActivityMainBinding

    // ==================== 2. UI控件 ====================
    private lateinit var playerView: StyledPlayerView
    private lateinit var btnPlayLocal: Button
    private lateinit var btnPlayNetwork: Button
    private lateinit var btnPause: Button
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnStartStory: Button

    // ==================== 3. 播放器 ====================
    private lateinit var player: ExoPlayer

    // ==================== 4. MVVM组件 ====================
    private val viewModel: PlayerViewModel by viewModels {
        // 提供ViewModel的工厂，传入需要的依赖
        PlayerViewModelFactory(
            VideoRepository(this)
        )
    }

    // ==================== 5. 原有故事功能 ====================
    // （完全保持不变）
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

    // ==================== 6. onCreate ====================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 工作中：使用ViewBinding更安全
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
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


        Log.d("MVVM", "✅ 架构初始化完成")
    }

    // ==================== 7. 初始化视图 ====================
    private fun initViews() {
        playerView = binding.playerView
        btnPlayLocal = binding.btnPlayLocal
        btnPlayNetwork = binding.btnPlayNetwork
        btnPause = binding.btnPause
        tvStatus = binding.tvStatus
        progressBar = binding.progressBar
        btnStartStory = binding.btnStartStory
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

                // 工作中：可以在这里添加更多UI更新逻辑
                // 比如：显示/隐藏加载动画
                // 比如：更新视频标题显示
                // 比如：处理错误状态
            }
        }
    }

    // ==================== 9. 点击监听 ====================
    private fun setupClickListeners() {

        // 按钮1：播放本地视频
        btnPlayLocal.setOnClickListener {
            Log.d("CLICK", "点击了【播本地】")
            // 工作中：Activity只做两件事：
            // 1. 调用ViewModel的方法（告诉ViewModel用户做了什么）
            viewModel.playLocalVideo("android.resource://$packageName/raw/sample")
            // 2. 执行UI相关的操作（播放器控制）
            try {
                var localUri = Uri.parse("android.resource://$packageName/raw/sample")
                mainPlayVideo(localUri,"本地视频正在播放中")
//                viewModel.playVideo(localUri,"本地视频正在播放中")
                Log.d("PLAYER", "播放本地视频")
            } catch (e: Exception) {
                // 错误可以交给ViewModel处理
                Log.e("ERROR", "本地视频错误: ${e.message}")
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
            Log.d("myceshi", "播放 $format: ${url.take(50)}...  名字 $desc")
            // 播放操作
            if (url.startsWith("http")) {
                var uri = Uri.parse(url)
                mainPlayVideo(uri,"轮换视频中")
//                viewModel.playVideo(uri,"轮换视频中")

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
            if (player?.isPlaying == true) {
                player?.pause()
                btnPause.text = "继续"
                viewModel.updateText("暂停中")
                Log.d("myceshi", "已暂停")
            } else {
                player?.play()
                btnPause.text = "暂停"
                viewModel.updateText("播放中")
                Log.d("myceshi", "继续播放")
            }
        }
    }

    // ==================== 10. 播放器相关 ====================
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

}







