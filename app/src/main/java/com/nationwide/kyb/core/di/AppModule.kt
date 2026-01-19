package com.nationwide.kyb.core.di

import android.annotation.SuppressLint
import android.content.Context
import com.nationwide.kyb.data.datasource.MockDataSource
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
    
    fun provideDataStoreManager(context: Context): DataStoreManager {
        if (dataStoreManager == null) {
            dataStoreManager = DataStoreManager(context.applicationContext)
        }
        return dataStoreManager!!
    }
    
    fun provideKybRepository(context: Context): KybRepository {
    if (kybRepository == null) {
        val mockDataSource = MockDataSource(context.assets)
        val dataStoreManager = provideDataStoreManager(context)
        kybRepository = KybRepositoryImpl(mockDataSource, dataStoreManager)
    }
    return kybRepository!!
}

}
