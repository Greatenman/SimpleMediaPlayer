package com.example.simplemediaplayer

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.simplemediaplayer.ui.theme.SimpleMediaPlayerTheme
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
//    1.声明变量
    private lateinit var playerView: StyledPlayerView
    private lateinit var btnPlayLocal: Button
    private lateinit var btnPlayNetwork: Button
    private lateinit var btnPause: Button
    private lateinit var tvStatus: TextView
    lateinit var progressBar: ProgressBar
    private lateinit var btnCheckThreads: Button

    //    2.设置播放器对象(！)
    var player: ExoPlayer? = null

    //    3.设置计数器来切换视频
    private var clickCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        1.初始化界面控件
        initViews()
//        2.初始化播放器
        initPlayer()
//        3.设置按钮点击
        setupClickListeners()
//        4.开始监控日志
        startLogging()
    }

    override fun onStart() {
        super.onStart()
        Log.d("LIFECYCLE", "onStart: 恢复播放器")
        if (player == null) {
            initPlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("LIFECYCLE", "onStop: 释放播放器，节省资源")
        player?.release()
        player = null
    }

    private fun startLogging() {
        Log.d("LEARNING", "🎬 ========== 开始学习音视频开发 ==========")
        Log.d("LEARNING", "1. ExoPlayer版本: 2.19.1")
        Log.d("LEARNING", "2. 支持格式: MP4, HLS, DASH")
        Log.d("LEARNING", "3. 线程模型: 主线程UI + 后台解码线程")

        // 打印当前线程信息
        Log.d("xiancheng", "主线程: ${Thread.currentThread().name}")
    }

    private fun setupClickListeners() {
//        按钮1：播放本地视频
        btnPlayLocal.setOnClickListener {
            Log.d("myceshi", "点击了【播本地】")
            try {
                val localUri = Uri.parse("android.resource://$packageName/raw/sample")
                val mediaItem = MediaItem.fromUri(localUri)
                player?.setMediaItem(mediaItem)
                player?.prepare()
                player?.play()
                tvStatus.text = "📱 播放本地视频"
                Log.d("myceshi", "使用 ProgressiveMediaSource（本地文件）")
            } catch (e: Exception) {
                tvStatus.text = "❌ 找不到本地视频"
                Log.e("myceshi", "本地视频错误: ${e.message}")
            }
        }
//        按钮2：播放网络视频（核心！）
        btnPlayNetwork.setOnClickListener {
            Log.d("myceshi", "点击了【播网络】")
            val videos = listOf(
                // 1. HLS格式（保证可用）
                Triple(
                    "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_ts/master.m3u8",
                    "HLS格式",
                    "🍎 Apple测试流"
                ),
                // 2. 普通MP4（保证可用）
                Triple(
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    "MP4格式",
                    "🐰 兔兔视频"
                ),
                // 3. 备用mp4视频
                Triple(
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    "MP4格式",
                    "🐘 大象之梦"
                ),
                // 4. 另一个HLS流（备用）
                Triple(
                    "http://qthttp.apple.com.edgesuite.net/1010qwoeiuryfg/sl.m3u8",
                    "HLS格式",
                    "📺 Apple直播测试"
                )
            )

            // 轮换播放三种视频
            val (url, format, desc) = videos[clickCount % videos.size]
            clickCount++

            tvStatus.text = "🌐 加载: $desc"
            Log.d("myceshi", "播放 $format: ${url.take(50)}...")

            if (url.startsWith("http")) {
                val mediaItem = MediaItem.fromUri(url)
                player?.setMediaItem(mediaItem)
                player?.prepare()
                player?.play()

                // 开始更新进度条
                startProgressUpdate()
                // 添加格式检测
                when {
                    url.contains(".mp4") -> Log.d("myceshi", "✅ 播放普通MP4文件")
                    url.contains(".m3u8") -> Log.d("myceshi", "✅ 播放HLS流媒体（分片）")
                    url.contains(".mpd") -> Log.d("myceshi", "✅ 播放DASH流媒体（自适应）")
                    else -> Log.d("FORMAT", "🔍 未知格式，ExoPlayer自动检测")
                }
            } else {
                tvStatus.text = "❌ 无效的URL"
                Log.e("ERROR", "URL格式错误: $url")
            }
        }
//        按钮3:暂停/继续播放
        btnPause.setOnClickListener {
            if (player?.isPlaying == true) {
                player?.pause()
                btnPause.text = "继续"
                Log.d("myceshi", "已暂停")
            } else {
                player?.play()
                btnPause.text = "暂停"
                Log.d("myceshi", "继续播放")
            }
        }
    }

    private fun initPlayer() {
//        创建播放器对象（就像创建Retrofit对象）
        player = ExoPlayer.Builder(this).build()
//        把播放器绑定到PlayerView（就像Adapter绑定到RecyclerView）
        playerView.player = player
//        监听播放器状态（就像监听网络请求
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                updateStatus(state)
            }
        })
        Log.d("LEARNING", "✅ 播放器创建成功！")
        tvStatus.text = "播放器就绪，请选择视频"
    }

//    更新状态显示
    private fun updateStatus(state: Int) {
//        必须在主线程更新
        runOnUiThread {
            when(state) {
                Player.STATE_BUFFERING -> {
                    tvStatus.text = "🔄 缓冲中..."
                    Log.d("PLAYER", "状态：正在缓冲")
                }

                Player.STATE_READY -> {
                    tvStatus.text = "▶️ 播放中"
                    Log.d("PLAYER", "状态：准备就绪，开始播放")
                }
                Player.STATE_ENDED -> {
                    tvStatus.text = "⏹️ 播放结束"
                    Log.d("PLAYER", "状态：播放结束")
                }
                Player.STATE_IDLE -> {
                    tvStatus.text = "⏸️ 暂停中"
                    Log.d("PLAYER", "状态：空闲")
                }
            }
        }
    }

    private fun initViews() {
        playerView = findViewById(R.id.playerView)
        btnPlayLocal = findViewById(R.id.btnPlayLocal)
        btnPlayNetwork = findViewById(R.id.btnPlayNetwork)
        btnPause = findViewById(R.id.btnPause)
        tvStatus = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)
        btnCheckThreads = findViewById(R.id.btnCheckThreads)
    }

}

private fun MainActivity.startProgressUpdate() {
    lifecycleScope.launch {
        while (true) {
            delay(200) // 每200ms更新一次

            player?.let { p -> {
                if (p.duration >0) {
                    val progress = (p.currentPosition.toFloat() / p.duration * 100).toInt()
                    runOnUiThread {
                        progressBar.progress = progress
                    }
                }
            } }
        }
    }
}
