package ru.netology.nmedia.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nmedia.api.*
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.toDto
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.AppError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError
import java.io.File
import java.io.IOException

class PostRepositoryImpl(private val dao: PostDao) : PostRepository {
    override val data = dao.getAll()
        .map(List<PostEntity>::toDto) //{ it.map { it.toDto() } }
        .flowOn(Dispatchers.Default)

    override suspend fun getAll() {
        try {
            val response = PostsApi.service.getAll()
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(body.toEntity())
        } catch (_: IOException) {
            throw NetworkError
        } catch (_: Exception) {
            throw UnknownError
        }
    }

    override fun getNewerCount(id: Long): Flow<Int> = flow {
        while (true) {
            val maxIdInDb = dao.getMaxId() ?: 0
            val response = PostsApi.service.getNewer(maxIdInDb)

            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            if (body.isNotEmpty()) {
                emit(body.size)
                newPostsBuffer.addAll(body)
            } else {
                emit(0)
            }
        }
    }
        .catch { e -> throw AppError.from(e) }
        .flowOn(Dispatchers.Default)

    private val newPostsBuffer = mutableListOf<Post>()

    override suspend fun saveNewPosts() {
        dao.insert(newPostsBuffer.toEntity())
        newPostsBuffer.clear()
    }

    override suspend fun save(post: Post, photo: File?) {
        try {
            val media = photo?.let { saveMedia(it) }

            val postWithAttachment = media?.let {
                post.copy(
                    attachment = Attachment(it.id, AttachmentType.IMAGE)
                )
            } ?: post

            val response = PostsApi.service.save(postWithAttachment)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(PostEntity.fromDto(body))
        } catch (_: IOException) {
            throw NetworkError
        } catch (_: Exception) {
            throw UnknownError
        }
    }

    private suspend fun saveMedia(file: File): Media =
        PostsApi.service.uploadFile(
            MultipartBody.Part.createFormData(
                "file",
                file.name,
                file.asRequestBody()
            )
        )

    override suspend fun removeById(id: Long) {
        try {
            val response = PostsApi.service.removeById(id)
            dao.removeById(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
        } catch (_: IOException) {
            throw NetworkError
        } catch (_: Exception) {
            throw UnknownError
        }
    }

    override suspend fun likeById(id: Long) {

        val postEntity = dao.getById(id) ?: throw IllegalArgumentException("Post not found")
        val post = postEntity.toDto()

        val newPost = post.copy(likedByMe = !post.likedByMe, likes = if (post.likedByMe) post.likes - 1 else post.likes + 1)
        val newPostEntity = PostEntity.fromDto(newPost)
        dao.insert(newPostEntity)

        try {
            val response = if (post.likedByMe) {
                PostsApi.service.dislikeById(id)
            } else {
                PostsApi.service.likeById(id)
            }
            if (!response.isSuccessful) {
                val originalPostEntity = postEntity
                dao.insert(originalPostEntity)
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(PostEntity.fromDto(body))

        } catch (_: IOException) {
            dao.insert(postEntity)
            throw NetworkError
        } catch (_: Exception) {
            dao.insert(postEntity)
            throw UnknownError
        }
    }
}
