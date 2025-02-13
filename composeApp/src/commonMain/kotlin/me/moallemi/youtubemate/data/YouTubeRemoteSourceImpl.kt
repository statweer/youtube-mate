package me.moallemi.youtubemate.data

import com.google.api.services.youtube.YouTube
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import me.moallemi.youtubemate.model.Channel
import me.moallemi.youtubemate.model.Comment
import me.moallemi.youtubemate.model.CommentAuthor
import me.moallemi.youtubemate.model.Stats
import me.moallemi.youtubemate.model.Video
import me.moallemi.youtubemate.model.YouTubeCredential
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.min

class YouTubeRemoteSourceImpl(
  private val youTube: YouTube,
) : YouTubeRemoteSource {
  override suspend fun channel(
    channelId: String,
    youTubeCredential: YouTubeCredential,
  ): Result<Channel, GeneralError> =
    suspendCancellableCoroutine { continuation ->
      try {
        val channel = youTube.channels()
          .list(listOf("snippet", "statistics"))
          .apply {
            this.id = listOf(channelId)
            key = youTubeCredential.apiKey
          }
          .execute()?.items?.get(0)!!.let { youtubeChannel ->
            Channel(
              id = youtubeChannel.id,
              title = youtubeChannel.snippet.title,
              handle = youtubeChannel.snippet.customUrl,
              thumbnail = youtubeChannel.snippet.thumbnails.medium.url,
              stats = Stats(
                videoCount = youtubeChannel.statistics.videoCount,
                subscriberCount = youtubeChannel.statistics.subscriberCount,
              ),
            )
          }
        continuation.resume(Result.Success(channel))
      } catch (e: Exception) {
        continuation.resume(Result.Failure(GeneralError.ApiError(e.message, -1)))
      }
    }

  override suspend fun allVideos(
    channelId: String,
    youTubeCredential: YouTubeCredential,
  ): Result<List<Video>, GeneralError> =
    latestVideos(count = 50, channelId = channelId, youTubeCredential = youTubeCredential)

  override suspend fun latestVideos(
    count: Int,
    channelId: String,
    youTubeCredential: YouTubeCredential
  ): Result<List<Video>, GeneralError> =
    suspendCancellableCoroutine { continuation ->
      val request = youTube.channels().list(listOf("contentDetails"))
        .setKey(youTubeCredential.apiKey)
        .setId(listOf(channelId))
      val response = request.execute()
      val uploadPlaylistId = response.items[0].contentDetails.relatedPlaylists.uploads

      // List playlist items
      val playlistItemsRequest = youTube.playlistItems().list(listOf("snippet"))
        .setPlaylistId(uploadPlaylistId)
        .setMaxResults(max(min(50, count.toLong()), 0))
        .setKey(youTubeCredential.apiKey)

      val videos = mutableListOf<Video>()

      var nextPageToken: String? = null
      do {
        playlistItemsRequest.pageToken = nextPageToken
        val playlistItemsResponse = playlistItemsRequest.execute()
        val ids = playlistItemsResponse.items.map {
          Video(
            id = it.snippet.resourceId.videoId,
            title = it.snippet.title,
            thumbnail = it.snippet.thumbnails.default.url,
          )
        }
        videos.addAll(ids)

        nextPageToken = playlistItemsResponse.nextPageToken
      } while (nextPageToken != null)

      continuation.resume(Result.Success(videos))
    }

  override suspend fun allComments(
    videoIds: List<String>,
    youTubeCredential: YouTubeCredential,
  ): Result<List<Comment>, GeneralError> =
    latestComments(count = Int.MAX_VALUE, videoIds = videoIds, youTubeCredential = youTubeCredential)

  override suspend fun latestComments(
    count: Int,
    videoIds: List<String>,
    youTubeCredential: YouTubeCredential
  ): Result<List<Comment>, GeneralError> {
    return try {
      val comments = mutableListOf<Comment>()
      val videoIdIterator = videoIds.iterator()

      coroutineScope {
        val jobs = mutableListOf<Deferred<List<Comment>>>()

        while (comments.size < count && videoIdIterator.hasNext()) {
          val videoId = videoIdIterator.next()
          val job = async {
            try {
              fetchComments(videoId, youTubeCredential)
            } catch (e: Exception) {
              e.printStackTrace()
              emptyList() // Return an empty list if the request fails
            }
          }
          jobs.add(job)
        }

        // Collect results from the active jobs
        jobs.forEach { job ->
          val result = job.await()
          comments.addAll(result)
          if (comments.size >= count) {
            return@coroutineScope
          }
        }
      }

      Result.Success(comments.take(count))
    } catch (e: Exception) {
      e.printStackTrace()
      Result.Failure(GeneralError.ApiError(e.message, -1))
    }
  }

  private suspend fun fetchComments(
    videoId: String,
    youTubeCredential: YouTubeCredential,
  ): List<Comment> =
    suspendCancellableCoroutine { continuation ->
      try {
        // Make a request to the commentThreads endpoint, filtering by your channel ID
        val request = youTube.commentThreads().list(listOf("snippet", "replies"))
          .setKey(youTubeCredential.apiKey)
          .setVideoId(videoId)

        val commentsList = mutableListOf<Comment>()
        val response = request.execute()
        response.items.forEach { commentThread ->
          val topLevelComment = commentThread.snippet.topLevelComment
          commentsList.add(
            Comment(
              id = topLevelComment.id,
              text = topLevelComment.snippet.textDisplay,
              videoId = videoId,
              date = topLevelComment.snippet.publishedAt.toStringRfc3339(),
              author = CommentAuthor(
                name = topLevelComment.snippet.authorDisplayName,
                avatarUrl = topLevelComment.snippet.authorProfileImageUrl,
                profileLink = commentThread.snippet.topLevelComment.snippet.authorChannelUrl,
              ),
            ),
          )

          if (commentThread.snippet.totalReplyCount != 0L) {
            commentThread.replies.comments.forEach { comment ->
              commentsList.add(
                Comment(
                  id = comment.id,
                  text = comment.snippet.textDisplay,
                  videoId = videoId,
                  date = comment.snippet.publishedAt.toStringRfc3339(),
                  author = CommentAuthor(
                    name = comment.snippet.authorDisplayName,
                    avatarUrl = comment.snippet.authorProfileImageUrl,
                    profileLink = comment.snippet.authorChannelUrl,
                  ),
                ),
              )
            }
          }
        }
        continuation.resume(commentsList)
      } catch (e: Exception) {
        continuation.resumeWithException(e)
      }
    }
}
