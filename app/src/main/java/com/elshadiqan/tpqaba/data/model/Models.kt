package com.elshadiqan.tpqaba.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val passwordHash: String, // MD5 or simple hashed/plain for verification
    val role: String, // "Admin" or "Operator"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "santri")
data class Santri(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nis: String,
    val nama: String,
    val jenisKelamin: String, // "Laki-laki" or "Perempuan"
    val tempatLahir: String,
    val tanggalLahir: String, // "YYYY-MM-DD"
    val namaAyah: String,
    val namaIbu: String,
    val alamat: String,
    val rt: String,
    val rw: String,
    val desa: String,
    val kecamatan: String,
    val kabupaten: String,
    val hpOrtu: String,
    val kelasId: Int = 0, // 0 means unassigned
    val foto: String? = null, // local path inside context.filesDir/images/santri/
    val qrCode: String? = null,
    val status: String = "Aktif", // "Aktif", "Lulus", "Keluar"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "kelas")
data class Kelas(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val namaKelas: String,
    val tingkat: String, // e.g. "TKQ", "TPQ Level 1", "TPQ Level 2", "TQA"
    val waliKelas: String
)

@Entity(tableName = "ustadz")
data class Ustadz(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String,
    val jenisKelamin: String, // "Laki-laki" or "Perempuan"
    val alamat: String,
    val hp: String,
    val jabatan: String, // "Kepala TPQ", "Wakil", "Guru Utama", "Staf Admin"
    val foto: String? = null // local path inside context.filesDir/images/ustadz/
)

@Entity(tableName = "absensi")
data class Absensi(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String, // "Santri" or "Guru"
    val itemId: Int, // Santri.id or Ustadz.id
    val nama: String, // Cached name
    val detail: String, // NIS for Santri, Jabatan for Guru
    val tanggal: String, // YYYY-MM-DD
    val waktu: String, // HH:mm:ss
    val status: String = "Hadir" // "Hadir", "Sakit", "Izin", "Alpa"
)

@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey val id: Int = 1,
    val namaTpq: String = "TPQ ABU BAKAR AMIN",
    val subHeader: String = "Sistem Manajemen LPQ Digital",
    val alamat: String = "Mangkubumen, Surakarta",
    val telepon: String = "081234567801",
    val kepalaTpq: String = "Ustadz H. Ahmad Amin",
    val tahunAjaran: String = "2026/2027",
    val izinkanPencarianPublik: Boolean = true,
    val firebaseDbUrl: String = "https://lpq-aba-default-rtdb.asia-southeast1.firebasedatabase.app/",
    val firebaseSecret: String = "",
    val logoTpq: String? = null
)

