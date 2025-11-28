package com.example.moneymanager.di

import android.util.Log
import com.example.moneymanager.data.remote.OllamaService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Dùng 10.0.2.2 cho Emulator
    private const val BASE_URL = "http://10.0.2.2:11434/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        // 1. Tạo bộ ghi log để xem request/response trong Logcat
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            // Vẫn giữ logic lọc của bạn nếu muốn
            if (message.startsWith("-->") || message.startsWith("<--") || message.contains("FAILED")) {
                Log.d("API_LOG", message)
            }
        }.apply {
            level = if (com.example.moneymanager.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            // 2. QUAN TRỌNG: Đặt thời gian timeout.
            // Nếu không kết nối được sau 10s, nó sẽ tự hủy và báo lỗi.
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS) // Thời gian chờ AI trả lời
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // Sử dụng OkHttpClient đã cấu hình ở trên
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideOllamaService(retrofit: Retrofit): OllamaService {
        return retrofit.create(OllamaService::class.java)
    }
}
