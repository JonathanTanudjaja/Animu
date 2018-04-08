package com.example.jo.animu.model

class Anime(var id: Long?, var title: String?, var score: Long?, var coverImage: String?) {

    init {
        title = title ?: ""
        score = score ?: 0
    }
}