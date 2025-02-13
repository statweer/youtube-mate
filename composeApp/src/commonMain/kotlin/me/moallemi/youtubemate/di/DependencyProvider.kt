package me.moallemi.youtubemate.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.math.BigInteger

class DependencyProvider {
  fun providesDataStore(): DataStore<Preferences> =
    dataStorePreferences(
      corruptionHandler = null,
      coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
      migrations = emptyList(),
    )

  fun providesYouTube(): YouTube =
    YouTube.Builder(
      GoogleNetHttpTransport.newTrustedTransport(),
      GsonFactory(),
    ) { }.setApplicationName(APPLICATION_NAME)
      .build()

  fun providesAppScope(): CoroutineScope =
    CoroutineScope(Dispatchers.Default + SupervisorJob())

  fun providesJson(): Json {
    val module =
      SerializersModule {
        contextual(BigIntegerSerializer)
      }
    return Json { serializersModule = module }
  }

  fun providesDispatcher(): DispatcherProvider =
    DefaultDispatcherProvider()

  fun providesHttpClient(): HttpClient =
    HttpClient(OkHttp) {
      install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        socketTimeoutMillis = 30_000
        connectTimeoutMillis = 30_000
      }
      install(ContentNegotiation) {
        json(
          Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
          },
        )
      }
      install(Logging) {
        logger = Logger.SIMPLE
        level = LogLevel.BODY
      }
    }

  companion object {
    private const val APPLICATION_NAME = "YoutubeMate"
  }
}

object BigIntegerSerializer : KSerializer<BigInteger> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("BigInteger", PrimitiveKind.STRING)

  override fun serialize(
    encoder: Encoder,
    value: BigInteger,
  ) {
    encoder.encodeString(value.toString())
  }

  override fun deserialize(decoder: Decoder): BigInteger {
    return BigInteger(decoder.decodeString())
  }
}
