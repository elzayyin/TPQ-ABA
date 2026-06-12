package com.elshadiqan.tpqaba.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.elshadiqan.tpqaba.data.model.Kelas
import com.elshadiqan.tpqaba.data.model.Santri
import com.elshadiqan.tpqaba.data.model.User
import com.elshadiqan.tpqaba.data.model.Ustadz
import com.elshadiqan.tpqaba.data.model.AppConfig
import com.elshadiqan.tpqaba.data.model.Absensi
import com.elshadiqan.tpqaba.data.repository.TPQRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TPQViewModel(private val repository: TPQRepository) : ViewModel() {

    // --- 0. AppConfig State ---
    val appConfig: StateFlow<AppConfig> = repository.appConfig
        .map { it ?: AppConfig() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppConfig())

    // --- 0.5 Absensi State & Flows ---
    val allAbsensi: StateFlow<List<Absensi>> = repository.allAbsensi
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun recordAbsensi(absensi: Absensi) {
        viewModelScope.launch {
            repository.insertAbsensi(absensi)
        }
    }

    fun clearAllAbsensi() {
        viewModelScope.launch {
            repository.clearAllAbsensi()
            showNotification("Semua rekap absensi berhasil direset!")
        }
    }

    fun updateAppConfig(config: AppConfig) {
        viewModelScope.launch {
            repository.updateAppConfig(config)
            showNotification("Konfigurasi TPQ berhasil diperbarui!")
        }
    }

    suspend fun checkSantriByNis(nis: String): Santri? {
        return repository.checkSantriByNis(nis)
    }

    suspend fun searchSantriByName(name: String): List<Santri> {
        return repository.searchSantriByName(name)
    }

    // --- 1. Authenticaton State ---
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    suspend fun login(username: String, passwordHash: String): Boolean {
        _loginError.value = null
        val user = repository.getUserByUsername(username)
        return if (user != null && user.passwordHash == passwordHash) {
            _currentUser.value = user
            showNotification("Login Berhasil sebagai ${user.role}!")
            true
        } else {
            _loginError.value = "Username atau password salah!"
            false
        }
    }

    fun logout() {
        _currentUser.value = null
        showNotification("Logout Berhasil")
    }

    fun resetPassword(username: String, newPass: String) {
        viewModelScope.launch {
            val user = repository.getUserByUsername(username)
            if (user != null) {
                repository.insertUser(user.copy(passwordHash = newPass))
                showNotification("Password untuk user '$username' berhasil direset!")
            } else {
                showNotification("User '$username' tidak ditemukan!")
            }
        }
    }

    // --- 2. Live Notifications State ---
    private val _notification = MutableStateFlow<String?>(null)
    val notification: StateFlow<String?> = _notification.asStateFlow()

    fun showNotification(message: String) {
        _notification.value = message
    }

    fun clearNotification() {
        _notification.value = null
    }

    // --- 3. Live Data Flows from Repository ---
    val allKelas: StateFlow<List<Kelas>> = repository.allKelas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUstadz: StateFlow<List<Ustadz>> = repository.allUstadz
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Santri query configurations
    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _filterGender = MutableStateFlow("Semu") // "Semu", "Laki-laki", "Perempuan"
    val filterGender = _filterGender.asStateFlow()

    private val _filterStatus = MutableStateFlow("Semu") // "Semu", "Aktif", "Lulus", "Keluar"
    val filterStatus = _filterStatus.asStateFlow()

    private val _filterKelasId = MutableStateFlow(0) // 0 means Semu / All
    val filterKelasId = _filterKelasId.asStateFlow()

    val filteredSantri: StateFlow<List<Santri>> = combine(
        repository.allSantri,
        _searchText,
        _filterGender,
        _filterStatus,
        _filterKelasId
    ) { list, search, gender, status, kId ->
        list.filter { s ->
            val matchSearch = s.nama.contains(search, ignoreCase = true) ||
                    s.nis.contains(search, ignoreCase = true) ||
                    s.namaAyah.contains(search, ignoreCase = true) ||
                    s.namaIbu.contains(search, ignoreCase = true) ||
                    s.alamat.contains(search, ignoreCase = true)

            val matchGender = gender == "Semu" || s.jenisKelamin == gender
            val matchStatus = status == "Semu" || s.status == status
            val matchKelas = kId == 0 || s.kelasId == kId

            matchSearch && matchGender && matchStatus && matchKelas
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Dashboard Statistics ---
    val santriCount = repository.santriCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeSantriCount = repository.activeSantriCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val graduatedSantriCount = repository.graduatedSantriCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val kelasCount = repository.kelasCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val ustadzCount = repository.ustadzCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- 4. CRUD Actions ---

    // -- Santri CRUD --
    fun saveSantri(santri: Santri) {
        viewModelScope.launch {
            if (santri.id == 0) {
                repository.insertSantri(santri)
                showNotification("Data Santri '${santri.nama}' berhasil disimpan!")
            } else {
                repository.updateSantri(santri)
                showNotification("Data Santri '${santri.nama}' berhasil diperbarui!")
            }
        }
    }

    fun deleteSantri(santri: Santri) {
        viewModelScope.launch {
            repository.deleteSantri(santri)
            showNotification("Data Santri '${santri.nama}' berhasil dihapus!")
        }
    }

    suspend fun getSantriByNis(nis: String): Santri? {
        return repository.getSantriByNis(nis)
    }

    // -- Kelas CRUD --
    fun saveKelas(kelas: Kelas) {
        viewModelScope.launch {
            if (kelas.id == 0) {
                repository.insertKelas(kelas)
                showNotification("Data Kelas '${kelas.namaKelas}' berhasil disimpan!")
            } else {
                repository.updateKelas(kelas)
                showNotification("Data Kelas '${kelas.namaKelas}' berhasil diperbarui!")
            }
        }
    }

    fun deleteKelas(kelas: Kelas) {
        viewModelScope.launch {
            repository.deleteKelas(kelas)
            showNotification("Data Kelas '${kelas.namaKelas}' berhasil dihapus!")
        }
    }

    // -- Ustadz CRUD --
    fun saveUstadz(ustadz: Ustadz) {
        viewModelScope.launch {
            if (ustadz.id == 0) {
                repository.insertUstadz(ustadz)
                showNotification("Data Ustadz/ah '${ustadz.nama}' berhasil disimpan!")
            } else {
                repository.updateUstadz(ustadz)
                showNotification("Data Ustadz/ah '${ustadz.nama}' berhasil diperbarui!")
            }
        }
    }

    fun deleteUstadz(ustadz: Ustadz) {
        viewModelScope.launch {
            repository.deleteUstadz(ustadz)
            showNotification("Data Ustadz/ah '${ustadz.nama}' berhasil dihapus!")
        }
    }

    // --- Search & Filter Setters ---
    fun updateSearchText(text: String) {
        _searchText.value = text
    }

    fun updateFilterGender(gender: String) {
        _filterGender.value = gender
    }

    fun updateFilterStatus(status: String) {
        _filterStatus.value = status
    }

    fun updateFilterKelasId(kelasId: Int) {
        _filterKelasId.value = kelasId
    }

    // --- Backup Simulation ---
    fun simulateBackup() {
        viewModelScope.launch {
            // Emulates backup
            kotlinx.coroutines.delay(1200)
            showNotification("Backup Database Berhasil disimpan di /backups/tpq_aba_db.bak")
        }
    }

    // --- Firebase Sync Operations ---
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun syncPush() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            showNotification("Memulai sinkronisasi ke Firebase...")
            val result = repository.syncPushToFirebase()
            _isSyncing.value = false
            if (result) {
                showNotification("Sinkronisasi BERHASIL! Data berhasil diunggah ke Firebase.")
            } else {
                showNotification("Sinkronisasi GAGAL! Periksa koneksi internet atau setelan URL database Anda.")
            }
        }
    }

    fun syncPull() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            showNotification("Memulai sinkronisasi dari Firebase...")
            val result = repository.syncPullFromFirebase()
            _isSyncing.value = false
            if (result) {
                showNotification("Sinkronisasi BERHASIL! Database diperbarui dengan data awan.")
            } else {
                showNotification("Sinkronisasi GAGAL! Periksa koneksi internet, atau pastikan data Firebase tidak kosong.")
            }
        }
    }
}

class TPQViewModelFactory(private val repository: TPQRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TPQViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TPQViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
