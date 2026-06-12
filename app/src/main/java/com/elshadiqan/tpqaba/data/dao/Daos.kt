package com.elshadiqan.tpqaba.data.dao

import androidx.room.*
import com.elshadiqan.tpqaba.data.model.Kelas
import com.elshadiqan.tpqaba.data.model.Santri
import com.elshadiqan.tpqaba.data.model.User
import com.elshadiqan.tpqaba.data.model.Ustadz
import com.elshadiqan.tpqaba.data.model.AppConfig
import com.elshadiqan.tpqaba.data.model.Absensi
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Query("DELETE FROM users")
    suspend fun clearAllUsers()
}

@Dao
interface SantriDao {
    @Query("SELECT * FROM santri ORDER BY nama ASC")
    fun getAllSantriFlow(): Flow<List<Santri>>

    @Query("SELECT * FROM santri WHERE id = :id LIMIT 1")
    suspend fun getSantriById(id: Int): Santri?

    @Query("SELECT * FROM santri WHERE nis = :nis LIMIT 1")
    suspend fun getSantriByNis(nis: String): Santri?

    @Query("SELECT * FROM santri WHERE kelasId = :kelasId ORDER BY nama ASC")
    fun getSantriByKelasFlow(kelasId: Int): Flow<List<Santri>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSantri(santri: Santri)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSantriList(list: List<Santri>)

    @Query("DELETE FROM santri")
    suspend fun clearAllSantri()

    @Update
    suspend fun updateSantri(santri: Santri)

    @Delete
    suspend fun deleteSantri(santri: Santri)

    @Query("SELECT COUNT(*) FROM santri")
    fun getSantriCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM santri WHERE status = 'Aktif'")
    fun getActiveSantriCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM santri WHERE status = 'Lulus'")
    fun getGraduatedSantriCountFlow(): Flow<Int>
}

@Dao
interface KelasDao {
    @Query("SELECT * FROM kelas ORDER BY namaKelas ASC")
    fun getAllKelasFlow(): Flow<List<Kelas>>

    @Query("SELECT * FROM kelas WHERE id = :id LIMIT 1")
    suspend fun getKelasById(id: Int): Kelas?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKelas(kelas: Kelas)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKelasList(list: List<Kelas>)

    @Query("DELETE FROM kelas")
    suspend fun clearAllKelas()

    @Update
    suspend fun updateKelas(kelas: Kelas)

    @Delete
    suspend fun deleteKelas(kelas: Kelas)

    @Query("SELECT COUNT(*) FROM kelas")
    fun getKelasCountFlow(): Flow<Int>
}

@Dao
interface UstadzDao {
    @Query("SELECT * FROM ustadz ORDER BY nama ASC")
    fun getAllUstadzFlow(): Flow<List<Ustadz>>

    @Query("SELECT * FROM ustadz WHERE id = :id LIMIT 1")
    suspend fun getUstadzById(id: Int): Ustadz?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUstadz(ustadz: Ustadz)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUstadzList(list: List<Ustadz>)

    @Query("DELETE FROM ustadz")
    suspend fun clearAllUstadz()

    @Update
    suspend fun updateUstadz(ustadz: Ustadz)

    @Delete
    suspend fun deleteUstadz(ustadz: Ustadz)

    @Query("SELECT COUNT(*) FROM ustadz")
    fun getUstadzCountFlow(): Flow<Int>
}

@Dao
interface ConfigDao {
    @Query("SELECT * FROM app_config WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<AppConfig?>

    @Query("SELECT * FROM app_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): AppConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: AppConfig)
}

@Dao
interface AbsensiDao {
    @Query("SELECT * FROM absensi ORDER BY tanggal DESC, waktu DESC")
    fun getAllAbsensiFlow(): Flow<List<Absensi>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbsensi(absensi: Absensi)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbsensiList(list: List<Absensi>)

    @Query("DELETE FROM absensi")
    suspend fun clearAllAbsensi()
}

