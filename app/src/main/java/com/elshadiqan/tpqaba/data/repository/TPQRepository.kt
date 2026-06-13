package com.elshadiqan.tpqaba.data.repository

import com.elshadiqan.tpqaba.data.dao.KelasDao
import com.elshadiqan.tpqaba.data.dao.SantriDao
import com.elshadiqan.tpqaba.data.dao.UserDao
import com.elshadiqan.tpqaba.data.dao.UstadzDao
import com.elshadiqan.tpqaba.data.dao.ConfigDao
import com.elshadiqan.tpqaba.data.dao.AbsensiDao
import com.elshadiqan.tpqaba.data.model.Kelas
import com.elshadiqan.tpqaba.data.model.Santri
import com.elshadiqan.tpqaba.data.model.User
import com.elshadiqan.tpqaba.data.model.Ustadz
import com.elshadiqan.tpqaba.data.model.AppConfig
import com.elshadiqan.tpqaba.data.model.Absensi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TPQRepository(
    private val userDao: UserDao,
    private val santriDao: SantriDao,
    private val kelasDao: KelasDao,
    private val ustadzDao: UstadzDao,
    private val configDao: ConfigDao,
    private val absensiDao: AbsensiDao
) : TPQPublicInterface {

    // --- 0.5 Absensi Operations ---
    val allAbsensi: Flow<List<Absensi>> = absensiDao.getAllAbsensiFlow()

    suspend fun insertAbsensi(absensi: Absensi) {
        absensiDao.insertAbsensi(absensi)
    }

    suspend fun clearAllAbsensi() {
        absensiDao.clearAllAbsensi()
    }
    // 1. User Auth Operations
    suspend fun getUserByUsername(username: String): User? {
        return userDao.getUserByUsername(username)
    }

    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
    }

    // 2. Santri CRUD
    val allSantri: Flow<List<Santri>> = santriDao.getAllSantriFlow()

    suspend fun getSantriById(id: Int): Santri? {
        return santriDao.getSantriById(id)
    }

    suspend fun getSantriByNis(nis: String): Santri? {
        return santriDao.getSantriByNis(nis)
    }

    fun getSantriByKelas(kelasId: Int): Flow<List<Santri>> {
        return santriDao.getSantriByKelasFlow(kelasId)
    }

    suspend fun insertSantri(santri: Santri) {
        santriDao.insertSantri(santri)
    }

    suspend fun updateSantri(santri: Santri) {
        santriDao.updateSantri(santri)
    }

    suspend fun deleteSantri(santri: Santri) {
        santriDao.deleteSantri(santri)
    }

    val santriCount: Flow<Int> = santriDao.getSantriCountFlow()
    val activeSantriCount: Flow<Int> = santriDao.getActiveSantriCountFlow()
    val graduatedSantriCount: Flow<Int> = santriDao.getGraduatedSantriCountFlow()

    // 3. Kelas CRUD
    val allKelas: Flow<List<Kelas>> = kelasDao.getAllKelasFlow()

    suspend fun getKelasById(id: Int): Kelas? {
        return kelasDao.getKelasById(id)
    }

    suspend fun insertKelas(kelas: Kelas) {
        kelasDao.insertKelas(kelas)
    }

    suspend fun updateKelas(kelas: Kelas) {
        kelasDao.updateKelas(kelas)
    }

    suspend fun deleteKelas(kelas: Kelas) {
        kelasDao.deleteKelas(kelas)
    }

    val kelasCount: Flow<Int> = kelasDao.getKelasCountFlow()

    // 4. Ustadz CRUD
    val allUstadz: Flow<List<Ustadz>> = ustadzDao.getAllUstadzFlow()

    suspend fun getUstadzById(id: Int): Ustadz? {
        return ustadzDao.getUstadzById(id)
    }

    suspend fun insertUstadz(ustadz: Ustadz) {
        ustadzDao.insertUstadz(ustadz)
    }

    suspend fun updateUstadz(ustadz: Ustadz) {
        ustadzDao.updateUstadz(ustadz)
    }

    suspend fun deleteUstadz(ustadz: Ustadz) {
        ustadzDao.deleteUstadz(ustadz)
    }

    val ustadzCount: Flow<Int> = ustadzDao.getUstadzCountFlow()

    // --- 5. AppConfig Operations ---
    val appConfig: Flow<AppConfig?> = configDao.getConfigFlow()

    suspend fun getAppConfig(): AppConfig? {
        return configDao.getConfig()
    }

    suspend fun updateAppConfig(config: AppConfig) {
        configDao.insertConfig(config)
    }

    suspend fun syncPushToFirebase(): Boolean {
        return try {
            val config = getAppConfig() ?: AppConfig()
            val santriList = allSantri.first()
            val kelasList = allKelas.first()
            val ustadzList = allUstadz.first()
            val absensiList = allAbsensi.first()

            val syncData = com.elshadiqan.tpqaba.utils.FirebaseSyncData(
                appConfig = config,
                kelasList = kelasList,
                santriList = santriList,
                ustadzList = ustadzList,
                absensiList = absensiList
            )

            com.elshadiqan.tpqaba.utils.FirebaseSyncManager.pushToFirebase(
                baseUrl = config.firebaseDbUrl,
                authSecret = config.firebaseSecret,
                syncData = syncData
            )
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun syncPullFromFirebase(): Boolean {
        return try {
            val config = getAppConfig() ?: AppConfig()
            val syncData = com.elshadiqan.tpqaba.utils.FirebaseSyncManager.pullFromFirebase(
                baseUrl = config.firebaseDbUrl,
                authSecret = config.firebaseSecret
            )

            if (syncData != null) {
                // Restore Config
                if (syncData.appConfig != null) {
                    configDao.insertConfig(syncData.appConfig)
                }
                
                // Overwrite Kelas
                kelasDao.clearAllKelas()
                val kList = syncData.kelasList
                if (kList != null && kList.isNotEmpty()) {
                    kelasDao.insertKelasList(kList)
                }

                // Overwrite Santri
                santriDao.clearAllSantri()
                val sList = syncData.santriList
                if (sList != null && sList.isNotEmpty()) {
                    santriDao.insertSantriList(sList)
                }

                // Overwrite Ustadz
                ustadzDao.clearAllUstadz()
                val uList = syncData.ustadzList
                if (uList != null && uList.isNotEmpty()) {
                    ustadzDao.insertUstadzList(uList)
                }

                // Overwrite Absensi
                absensiDao.clearAllAbsensi()
                val aList = syncData.absensiList
                if (aList != null && aList.isNotEmpty()) {
                    absensiDao.insertAbsensiList(aList)
                }

                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- 6. TPQPublicInterface Implementation ---
    override fun getPublicConfigFlow(): Flow<AppConfig?> = configDao.getConfigFlow()

    override suspend fun checkSantriByNis(nis: String): Santri? {
        return santriDao.getSantriByNis(nis)
    }

    override suspend fun searchSantriByName(name: String): List<Santri> {
        return try {
            val list = allSantri.first()
            list.filter { it.nama.contains(name, ignoreCase = true) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun getPublicSantriCountFlow(): Flow<Int> = santriDao.getSantriCountFlow()
    override fun getPublicUstadzCountFlow(): Flow<Int> = ustadzDao.getUstadzCountFlow()
}
