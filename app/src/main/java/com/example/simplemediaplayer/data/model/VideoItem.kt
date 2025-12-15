package com.example.simplemediaplayer.data.model

/**
 * 视频数据模型类
 *
 * 架构讲解：
 * 1. 为什么需要数据模型？
 *    - 统一数据结构，避免到处传递多个参数
 *    - 类型安全，编译器会检查字段类型
 *    - 易于扩展，添加新字段只需改一处
 *
 * 2. data class的优势：
 *    - 自动生成equals(), hashCode(), toString()
 *    - 自动生成copy()方法（用于不可变数据）
 *    - 支持解构声明
 *
 * 工作中应用场景：
 *   当API返回、数据库存储、UI显示都需要相同数据时，
 *   用数据模型类统一管理
 */

data class VideoItem (
    var Id: String,    //唯一标识
    var url: String,  //唯一地址
    var title: String,   //显示标题
    var format: String  //视频格式
)

