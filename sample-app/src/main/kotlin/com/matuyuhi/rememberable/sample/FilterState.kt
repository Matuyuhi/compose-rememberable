package com.matuyuhi.rememberable.sample

import com.matuyuhi.rememberable.Rememberable

@Rememberable
data class FilterState(
    val query: String,
    val page: Int
)