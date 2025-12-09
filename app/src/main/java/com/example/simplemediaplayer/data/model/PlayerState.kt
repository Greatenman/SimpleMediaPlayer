package com.example.simplemediaplayer.data.model

sealed class PlayerState {
    object Idle : PlayerState()   //空闲状态
    object Buffering : PlayerState()   //缓存状态
    object Ready : PlayerState()    //准备状态
    object Ended : PlayerState()   //播放结束状态
    data class Error(var message: String) : PlayerState()  //出现错误的情况(带错误信息)
}