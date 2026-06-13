package com.elshadiqan.tpqaba.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [User::class, Santri::class, Kelas::class, Ustadz::class, AppConfig::class, Absensi::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun santriDao(): SantriDao
    abstract fun kelasDao(): KelasDao
    abstract fun ustadzDao(): UstadzDao
    abstract fun configDao(): ConfigDao
    abstract fun absensiDao(): AbsensiDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tpq_aba_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    try {
                        val configDao = database.configDao()
                        val currentConfig = configDao.getConfig()
                        if (currentConfig != null) {
                            if (currentConfig.firebaseDbUrl == "https://tpq-abu-bakar-amin-default-rtdb.asia-southeast1.firebasedatabase.app/" || 
                                currentConfig.firebaseDbUrl == "https://android-a94c8-default-rtdb.firebaseio.com/" || 
                                currentConfig.firebaseDbUrl.trim().isEmpty()) {
                                configDao.insertConfig(currentConfig.copy(firebaseDbUrl = "https://lpq-aba-default-rtdb.asia-southeast1.firebasedatabase.app/"))
                            }
                        } else {
                            configDao.insertConfig(AppConfig(firebaseDbUrl = "https://lpq-aba-default-rtdb.asia-southeast1.firebasedatabase.app/"))
                        }
                        if (database.userDao().getUserCount() == 0) {
                            populateDatabase(database)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        suspend fun populateDatabase(database: AppDatabase) {
            val userDao = database.userDao()
            val kelasDao = database.kelasDao()
            val ustadzDao = database.ustadzDao()
            val santriDao = database.santriDao()
            val configDao = database.configDao()

            // 0. Seed Default App Config
            configDao.insertConfig(AppConfig())

            // 1. Seed Defaults Users
            userDao.insertUser(User(username = "admin", passwordHash = "admin123", role = "Admin"))
            userDao.insertUser(User(username = "operator", passwordHash = "operator123", role = "Operator"))

            // 2. Seed Default Teachers (Ustadz/Ustadzah)
            val u1 = Ustadz(nama = "Ustadz H. Ahmad Amin", jenisKelamin = "Laki-laki", alamat = "Mangkubumen, Surakarta", hp = "081234567801", jabatan = "Kepala TPQ")
            val u2 = Ustadz(nama = "Ustadzah Fatimah Azzahra", jenisKelamin = "Perempuan", alamat = "Kerten, Surakarta", hp = "081234567802", jabatan = "Guru Utama")
            val u3 = Ustadz(nama = "Ustadzah Siti Munawwarah", jenisKelamin = "Perempuan", alamat = "Laweyan, Surakarta", hp = "081234567803", jabatan = "Guru Utama")
            ustadzDao.insertUstadz(u1)
            ustadzDao.insertUstadz(u2)
            ustadzDao.insertUstadz(u3)

            // 3. Seed Default Classes
            kelasDao.insertKelas(Kelas(namaKelas = "Kelas TKQ-A", tingkat = "TKQ", waliKelas = "Ustadzah Fatimah Azzahra"))
            kelasDao.insertKelas(Kelas(namaKelas = "Kelas TPQ-1", tingkat = "TPQ", waliKelas = "Ustadzah Siti Munawwarah"))
            kelasDao.insertKelas(Kelas(namaKelas = "Kelas TQA-Premium", tingkat = "TQA", waliKelas = "Ustadz H. Ahmad Amin"))

            // 4. Seed Default Santri (Students)
            santriDao.insertSantri(Santri(
                nis = "ABA26001",
                nama = "Ahmad Maulana Fauzi",
                jenisKelamin = "Laki-laki",
                tempatLahir = "Surakarta",
                tanggalLahir = "2015-05-12",
                namaAyah = "Handoko",
                namaIbu = "Siti Aminah",
                alamat = "Slamet Riyadi No. 45",
                rt = "02",
                rw = "05",
                desa = "Penumping",
                kecamatan = "Laweyan",
                kabupaten = "Surakarta",
                hpOrtu = "085712345678",
                kelasId = 2,
                status = "Aktif",
                qrCode = "https://tpqaba.id/santri/ABA26001"
            ))
            santriDao.insertSantri(Santri(
                nis = "ABA26002",
                nama = "Aisyah Nabila Putri",
                jenisKelamin = "Perempuan",
                tempatLahir = "Boyolali",
                tanggalLahir = "2017-08-20",
                namaAyah = "Supardi",
                namaIbu = "Rini Wulandari",
                alamat = "Adi Sucipto Gg. Dahlia 3",
                rt = "04",
                rw = "01",
                desa = "Kerten",
                kecamatan = "Laweyan",
                kabupaten = "Surakarta",
                hpOrtu = "081398765432",
                kelasId = 1,
                status = "Aktif",
                qrCode = "https://tpqaba.id/santri/ABA26002"
            ))
            santriDao.insertSantri(Santri(
                nis = "ABA26003",
                nama = "Muhammad Zidan Al-Fatih",
                jenisKelamin = "Laki-laki",
                tempatLahir = "Surakarta",
                tanggalLahir = "2014-11-03",
                namaAyah = "Rahmat Hidayat",
                namaIbu = "Mega Utami",
                alamat = "Kapten Mulyadi No. 120",
                rt = "01",
                rw = "12",
                desa = "Kedung Lumbu",
                kecamatan = "Pasar Kliwon",
                kabupaten = "Surakarta",
                hpOrtu = "081122334455",
                kelasId = 3,
                status = "Aktif",
                qrCode = "https://tpqaba.id/santri/ABA26003"
            ))
        }
    }
}
