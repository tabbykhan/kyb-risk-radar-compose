package com.nationwide.kyb.core.di

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nationwide.kyb.data.datasource.MockDataSource
import com.nationwide.kyb.data.local.DataStoreManager
import com.nationwide.kyb.data.repository.KybRepositoryImpl
import com.nationwide.kyb.data.repository.RiskBandDeserializer
import com.nationwide.kyb.domain.model.RiskBand
import com.nationwide.kyb.domain.repository.KybRepository
import java.io.InputStream
import java.lang.reflect.Type

/**
 * Simple dependency injection module
 * In a production app, consider using Hilt or Koin
 */
object AppModule {
    private var dataStoreManager: DataStoreManager? = null
    private var kybRepository: KybRepository? = null
    
    fun provideDataStoreManager(context: Context): DataStoreManager {
        if (dataStoreManager == null) {
            dataStoreManager = DataStoreManager(context.applicationContext)
        }
        return dataStoreManager!!
    }
    
    fun provideKybRepository(context: Context, assetsInputStream: InputStream): KybRepository {
        if (kybRepository == null) {
            val mockDataSource = MockDataSource(assetsInputStream)
            val dataStoreManager = provideDataStoreManager(context)
            kybRepository = KybRepositoryImpl(mockDataSource, dataStoreManager)
        }
        return kybRepository!!
    }
    
    fun provideGson(): Gson {
        return GsonBuilder()
            .setFieldNamingPolicy(com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(RiskBand::class.java, RiskBandDeserializer())
            .create()
    }
}
