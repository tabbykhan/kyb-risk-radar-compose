package com.nationwide.kyb.core.di

import android.annotation.SuppressLint
import android.content.Context
import com.nationwide.kyb.data.datasource.remote.KybRemoteDataSource
import com.nationwide.kyb.data.local.DataStoreManager
import com.nationwide.kyb.data.repository.KybRepositoryImpl
import com.nationwide.kyb.domain.repository.KybRepository

/**
 * Simple dependency injection module
 * In a production app, consider using Hilt or Koin
 */
object AppModule {
    @SuppressLint("StaticFieldLeak")
    private var dataStoreManager: DataStoreManager? = null
    private var kybRepository: KybRepository? = null
    private var remoteDataSource: KybRemoteDataSource? = null

    fun provideDataStoreManager(context: Context): DataStoreManager {
        if (dataStoreManager == null) {
            dataStoreManager = DataStoreManager(context.applicationContext)
        }
        return dataStoreManager!!
    }
    
    fun provideRemoteDataSource(): KybRemoteDataSource {
        if (remoteDataSource == null) {
            remoteDataSource = KybRemoteDataSource()
        }
        return remoteDataSource!!
    }

    fun provideKybRepository(context: Context): KybRepository {
        if (kybRepository == null) {
            val remote = provideRemoteDataSource()
            val dataStore = provideDataStoreManager(context)
            kybRepository = KybRepositoryImpl(remote, dataStore)
        }
        return kybRepository!!
    }
}


