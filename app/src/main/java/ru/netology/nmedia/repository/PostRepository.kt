package ru.netology.nmedia.repository

import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.Post
import java.io.File

interface PostRepository {
    val data: Flow<List<Post>>
    suspend fun getAll()
    fun getNewerCount(id: Long): Flow<Int>
    suspend fun save(post: Post, photo: File?)
    suspend fun removeById(id: Long)
    suspend fun likeById(id: Long)
    suspend fun saveNewPosts()
}

