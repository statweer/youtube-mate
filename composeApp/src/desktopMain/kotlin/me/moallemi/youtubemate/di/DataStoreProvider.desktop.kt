package me.moallemi.youtubemate.di

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope

actual fun dataStorePreferences(
  corruptionHandler: ReplaceFileCorruptionHandler<Preferences>?,
  coroutineScope: CoroutineScope,
  migrations: List<DataMigration<Preferences>>,
): DataStore<Preferences> =
  createDataStoreWithDefaults(
    corruptionHandler = corruptionHandler,
    migrations = migrations,
    coroutineScope = coroutineScope,
    path = { YOUTUBE_MATE_DATASTORE_FILE_NAME },
  )
