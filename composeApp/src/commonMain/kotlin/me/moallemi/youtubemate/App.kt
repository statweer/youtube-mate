package me.moallemi.youtubemate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons.Rounded
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.moallemi.youtubemate.compoentns.AddYouTubeChannelSection
import me.moallemi.youtubemate.compoentns.ChannelSection
import me.moallemi.youtubemate.compoentns.CommentsSection
import me.moallemi.youtubemate.compoentns.TopCommentersSection
import me.moallemi.youtubemate.compoentns.YouTubeApiKeySection
import me.moallemi.youtubemate.data.Result.Failure
import me.moallemi.youtubemate.di.DependencyContainer
import me.moallemi.youtubemate.di.DependencyProvider
import me.moallemi.youtubemate.model.Channel
import me.moallemi.youtubemate.model.Comment
import me.moallemi.youtubemate.model.CommentAuthor
import me.moallemi.youtubemate.model.Video
import me.moallemi.youtubemate.model.YouTubeCredential
import me.moallemi.youtubemate.ui.theme.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
  AppTheme {
    val dependencyContainer = remember { DependencyContainer(DependencyProvider()) }

    val youtubeApiKey by dependencyContainer.dataRepository.observeYouTubeCredential().collectAsState(null)
    val channel by dependencyContainer.dataRepository.observeChannel().collectAsState(null)
    val videos by dependencyContainer.dataRepository.observeVideos().collectAsState(initial = null)
    val comments by dependencyContainer.dataRepository.observeComments().collectAsState(initial = null)
    val commentsByAuthor by remember {
      derivedStateOf {
        comments?.filter { comment ->
          comment.author.name != "@RezaDevs"
        }?.groupBy {
          it.author
        }?.entries
          ?.sortedByDescending { it.value.size }
          ?.associate { it.key to it.value }
      }
    }
    val topComments by remember {
      derivedStateOf {
        comments?.filter { comment ->
          comment.author.name != "@RezaDevs"
        }?.sortedByDescending { it.date }
      }
    }

    if (youtubeApiKey == null) {
      YouTubeApiKeySection(
        youtubeApiKey = youtubeApiKey,
        onSave = { apiKey ->
          dependencyContainer.dataRepository.storeYouTubeCredential(YouTubeCredential(apiKey))
        },
      )
    }
    if (youtubeApiKey != null && channel == null) {
      val scope = rememberCoroutineScope()
      var isLoading by remember { mutableStateOf(false) }
      var error by remember { mutableStateOf<String?>(null) }

      AddYouTubeChannelSection(
        channel = channel,
        isLoading = isLoading,
        error = error,
        saveChannel = { channelId ->
          scope.launch {
            isLoading = true
            val result = dependencyContainer.dataRepository.channel(channelId)
            if (result is Failure) {
              error = "Invalid channel id"
            }
            isLoading = false
          }
        },
      )
    }

    if (youtubeApiKey != null && channel != null) {
      MainContent(
        channel = channel,
        dependencyContainer = dependencyContainer,
        videos = videos,
        comments = comments,
        commentsByAuthor = commentsByAuthor,
        topComments = topComments,
      )
    }
  }
}

@Composable
private fun MainContent(
  channel: Channel?,
  dependencyContainer: DependencyContainer,
  videos: List<Video>?,
  comments: List<Comment>?,
  commentsByAuthor: Map<CommentAuthor, List<Comment>>?,
  topComments: List<Comment>?,
) {
  val coroutineScope = rememberCoroutineScope()
  Column(
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    MainHeaderSection(
      channel = channel,
      coroutineScope = coroutineScope,
      dependencyContainer = dependencyContainer,
    )

    MainBodySection(
      videos = videos,
      dependencyContainer = dependencyContainer,
      channel = channel,
      comments = comments,
      commentsByAuthor = commentsByAuthor,
      topComments = topComments,
    )
  }
}

@Composable
private fun MainBodySection(
  videos: List<Video>?,
  dependencyContainer: DependencyContainer,
  channel: Channel?,
  comments: List<Comment>?,
  commentsByAuthor: Map<CommentAuthor, List<Comment>>?,
  topComments: List<Comment>?,
) {
  var isLoading by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    val cachedVideos: List<Video>
    if (videos?.isEmpty() == true) {
      isLoading = true
      val videoResult = dependencyContainer.dataRepository.latestVideos(20, channel!!.id)
      isLoading = false
      if (videoResult is Failure) {
        // Handle error
      }
      cachedVideos = videoResult.successValue()!!
    } else {
      cachedVideos = videos ?: emptyList()
    }

    if (comments?.isEmpty() == true) {
      isLoading = true
      val commentsResult = dependencyContainer.dataRepository.latestComments(50, cachedVideos.map { it.id })
      if (commentsResult is Failure) {
        // Handle error
      }
      dependencyContainer.dataRepository.allComments(cachedVideos.map { it.id })
    }
    isLoading = false
  }
  val topCommentersSection = remember(commentsByAuthor, isLoading) {
    movableContentOf {
      ElevatedCard(
        modifier = Modifier
          .padding(vertical = 16.dp)
          .padding(start = 16.dp, end = 8.dp),
      ) {
        TopCommentersSection(
          topCommentAuthors = commentsByAuthor ?: emptyMap(),
          isLoading = comments == null || isLoading,
        )
      }
    }
  }

  val topCommentsSection = remember(topComments, isLoading) {
    movableContentOf {
      ElevatedCard(
        modifier = Modifier
          .padding(vertical = 16.dp)
          .padding(start = 8.dp, end = 16.dp),
      ) {
        CommentsSection(
          items = topComments ?: emptyList(),
          isLoading = topComments == null || isLoading,
        )
      }
    }
  }
  BoxWithConstraints {
    if (maxWidth < 840.dp) {
      Column(
        modifier = Modifier,
      ) {
        var selectedTabIndex by remember { mutableStateOf(0) }
        TabRow(
          modifier = Modifier.fillMaxWidth(),
          selectedTabIndex = selectedTabIndex,
          tabs = {
            Tab(
              text = { Text("Top Commenters") },
              selected = selectedTabIndex == 0,
              onClick = { selectedTabIndex = 0 },
            )
            Tab(
              text = { Text("Latest Comments") },
              selected = selectedTabIndex == 1,
              onClick = { selectedTabIndex = 1 },
            )
          },
        )
        when (selectedTabIndex) {
          0 -> topCommentersSection()
          1 -> topCommentsSection()
        }
      }
    } else {
      Row(
        modifier = Modifier
          .padding(top = 16.dp)
          .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(
          modifier = Modifier.weight(0.35f),
        ) {
          Text(
            text = "Top Commenters",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
              .padding(horizontal = 16.dp),
          )
          topCommentersSection()
        }
        Column(
          modifier = Modifier.weight(0.65f),
        ) {
          Text(
            text = "Top Comments",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
              .padding(horizontal = 16.dp),
          )
          topCommentsSection()
        }
      }
    }
  }
}

@Composable
private fun MainHeaderSection(
  channel: Channel?,
  coroutineScope: CoroutineScope,
  dependencyContainer: DependencyContainer,
) {
  val channelSection = remember(channel) {
    movableContentOf {
      ChannelSection(
        modifier = Modifier,
        channel = channel!!,
      )
    }
  }
  val settingsSection = remember {
    movableContentOf {
      Button(
        onClick = {
          // TODO: Add navigation instead of manual deletion
          coroutineScope.launch {
            dependencyContainer.dataRepository.clearEverything()
          }
        },
      ) {
        Icon(
          modifier = Modifier.padding(end = 8.dp),
          imageVector = Rounded.Settings,
          contentDescription = "Refresh",
        )
        Text(text = "Change Channel")
      }
    }
  }

  BoxWithConstraints {
    if (maxWidth < 840.dp) {
      Column(
        modifier = Modifier
          .padding(top = 16.dp)
          .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        channelSection()
        Box(
          modifier = Modifier
            .fillMaxWidth(),
          contentAlignment = Alignment.CenterEnd,
        ) {
          settingsSection()
        }
      }
    } else {
      Row(
        modifier = Modifier
          .padding(top = 16.dp)
          .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier.weight(1f),
        ) {
          channelSection()
        }
        Box {
          settingsSection()
        }
      }
    }
  }
}
