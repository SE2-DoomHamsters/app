package com.doomhamsters

/** Provides the backend host configuration used by the app. */
object BackendConfig {
    val BASE_URL: String get() = BuildConfig.BASE_URL
    val LOCAL_URL: String get() = BuildConfig.LOCAL_URL
}
