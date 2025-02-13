package me.moallemi.youtubemate.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.moallemi.youtubemate.data.Result.Success
import me.moallemi.youtubemate.di.DispatcherProvider
import me.moallemi.youtubemate.model.Channel
import me.moallemi.youtubemate.model.Comment
import me.moallemi.youtubemate.model.Video
import me.moallemi.youtubemate.model.YouTubeCredential

class DataRepositoryImpl(
  private val youTubeRemoteSource: YouTubeRemoteSource,
  private val localStore: LocalStore,
  private val appScope: CoroutineScope,
  private val dispatcher: DispatcherProvider,
) : DataRepository {
  override suspend fun channel(channelId: String): Result<Channel, GeneralError> =
    withContext(dispatcher.io()) {
      val youTubeCredential = localStore.observeYouTubeCredential().first()
        ?: return@withContext Result.Failure(GeneralError.AppError("No YouTube Credential"))
      val result = youTubeRemoteSource.channel(channelId, youTubeCredential)
      if (result is Success) {
        localStore.storeChannel(result.data)
      }
      result
    }

  override fun observeChannel(): Flow<Channel?> =
    localStore.observeChannel()

  override suspend fun deleteChannel(id: String) {
    withContext(dispatcher.io()) {
      localStore.deleteChannel()
    }
  }

  override fun observeYouTubeCredential(): Flow<YouTubeCredential?> =
    localStore.observeYouTubeCredential()

  override fun storeYouTubeCredential(credential: YouTubeCredential) {
    appScope.launch {
      localStore.storeYouTubeCredential(credential)
    }
  }

  override fun observeVideos(): Flow<List<Video>> =
    localStore.observeVideos()

  override suspend fun allVideos(channelId: String): Result<List<Video>, GeneralError> =
    withContext(dispatcher.io()) {
      val youTubeCredential = localStore.observeYouTubeCredential().first()
        ?: return@withContext Result.Failure(GeneralError.AppError("No YouTube Credential"))

      val result = youTubeRemoteSource.allVideos(channelId, youTubeCredential)
      if (result is Success) {
        localStore.storeVideos(result.data)
      }
      result
    }

  override suspend fun latestVideos(
    count: Int,
    channelId: String
  ): Result<List<Video>, GeneralError> =
    withContext(dispatcher.io()) {
      val youTubeCredential = localStore.observeYouTubeCredential().first()
        ?: return@withContext Result.Failure(GeneralError.AppError("No YouTube Credential"))

      val result = youTubeRemoteSource.latestVideos(count, channelId, youTubeCredential)
      if (result is Success) {
        localStore.storeVideos(result.data)
      }
      result
    }

  override suspend fun deleteAllVideos() {
    withContext(dispatcher.io()) {
      localStore.deleteAllVideos()
    }
  }

  override fun observeComments(): Flow<List<Comment>> =
    localStore.observeComments()

  override suspend fun allComments(videoIds: List<String>): Result<List<Comment>, GeneralError> =
    withContext(dispatcher.io()) {
      val youTubeCredential = localStore.observeYouTubeCredential().first()
        ?: return@withContext Result.Failure(GeneralError.AppError("No YouTube Credential"))

      val result = youTubeRemoteSource.allComments(videoIds, youTubeCredential)
      if (result is Success) {
        localStore.storeComments(result.data)
      }
      result
    }

  override suspend fun latestComments(
    count: Int,
    videoIds: List<String>
  ): Result<List<Comment>, GeneralError> =
    withContext(dispatcher.io()) {
      val youTubeCredential = localStore.observeYouTubeCredential().first()
        ?: return@withContext Result.Failure(GeneralError.AppError("No YouTube Credential"))

      val result = youTubeRemoteSource.latestComments(count, videoIds, youTubeCredential)
      if (result is Success) {
        localStore.storeComments(result.data)
      }
      result
    }

  override suspend fun deleteAllComments() {
    withContext(dispatcher.io()) {
      localStore.deleteAllComments()
    }
  }

  override suspend fun clearEverything() {
    withContext(dispatcher.io()) {
      localStore.clearEverything()
    }
  }
}
