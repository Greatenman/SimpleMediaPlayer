package com.example.simplemediaplayer.data.model

/**
 * 播放器状态密封类
 *
 * 架构讲解：
 * 1. 为什么用密封类而不是枚举？
 *    - 密封类可以有数据（如Error可以带错误信息）
 *    - 编译器会检查是否处理了所有情况
 *    - 扩展性好，可以添加新的状态
 *
 * 2. sealed class vs enum：
 *    enum PlayerState { IDLE, BUFFERING, PLAYING } // 只能表示简单状态
 *    sealed class PlayerState {                     // 可以携带数据
 *        object Idle : PlayerState()
 *        data class Error(val msg: String) : PlayerState()
 *    }
 *
 * 工作中应用场景：
 *   管理UI状态、网络请求状态、播放器状态等
 */

sealed class PlayerState {
    object Idle : PlayerState()   //空闲状态
    object Buffering : PlayerState()   //缓存状态
    object Ready : PlayerState()    //准备状态
    object Ended : PlayerState()   //播放结束状态
    data class Error(var message: String) : PlayerState()  //出现错误的情况(带错误信息)
}