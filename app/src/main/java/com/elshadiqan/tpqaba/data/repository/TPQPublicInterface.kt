package com.elshadiqan.tpqaba.data.repository

import com.elshadiqan.tpqaba.data.model.AppConfig
import com.elshadiqan.tpqaba.data.model.Santri
import kotlinx.coroutines.flow.Flow

interface TPQPublicInterface {
    /**
     * Retrieves the current application configuration.
     */
    fun getPublicConfigFlow(): Flow<AppConfig?>

    /**
     * Checks if a student exists using their unique NIS number.
     */
    suspend fun checkSantriByNis(nis: String): Santri?

    /**
     * Searches registered students by name.
     */
    suspend fun searchSantriByName(name: String): List<Santri>

    /**
     * Exposes public counts.
     */
    fun getPublicSantriCountFlow(): Flow<Int>
    fun getPublicUstadzCountFlow(): Flow<Int>
}
