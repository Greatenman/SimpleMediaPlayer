package com.example.simplemediaplayer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.simplemediaplayer.data.VideoRepository



//* 工作中为什么需要：
//* 1. ViewModel可能有构造函数参数
//* 2. 统一管理ViewModel的创建
//* 3. 方便依赖注入
//*/

class PlayerViewModelFactory(private val repository: VideoRepository): ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            return PlayerViewModel(repository) as T
        }
        throw IllegalArgumentException("未知的ViewModel类: ${modelClass.name}")
    }
}