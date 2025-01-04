package me.moallemi.youtubemate

import android.app.Application

class YouTubeMateApplication : Application() {
  override fun onCreate() {
    super.onCreate()

    me.moallemi.youtubemate.di.applicationContext = this
  }
}
