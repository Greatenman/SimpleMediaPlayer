package com.example.simplemediaplayer.data.model

import android.R

//用于给所有video命名规范！！！

data class VideoItem (
    var Id: String,    //唯一标识
    var url: String,  //唯一地址
    var title: String,   //显示标题
    var format: String  //视频格式
)

