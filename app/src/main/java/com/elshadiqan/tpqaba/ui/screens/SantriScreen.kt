package com.elshadiqan.tpqaba.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.elshadiqan.tpqaba.data.model.Kelas
import com.elshadiqan.tpqaba.data.model.Santri
import com.elshadiqan.tpqaba.data.model.AppConfig
import com.elshadiqan.tpqaba.ui.viewmodel.TPQViewModel
import com.elshadiqan.tpqaba.utils.PDFExporter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SantriScreen(
    viewModel: TPQViewModel,
    onNavigateToIDCard: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val role = viewModel.currentUser.collectAsState().value?.role ?: "Operator"
    val appConfig by viewModel.appConfig.collectAsState()

    val santriList by viewModel.filteredSantri.collectAsState()
    val kelasList by viewModel.allKelas.collectAsState()
    val searchVal by viewModel.searchText.collectAsState()
    val selectedGender by viewModel.filterGender.collectAsState()
    val selectedStatus by viewModel.filterStatus.collectAsState()
    val selectedKelasId by viewModel.filterKelasId.collectAsState()

    val kelasMap = remember(kelasList) { kelasList.associate { it.id to it.namaKelas } }

    var expandedFilter by remember { mutableStateOf(false) }
    var selectedSantriForDetail by remember { mutableStateOf<Santri?>(null) }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var santriToEdit by remember { mutableStateOf<Santri?>(null) }

    var showDeleteConfirm by remember { mutableStateOf<Santri?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val text = inputStream?.bufferedReader()?.use { r -> r.readText() } ?: ""
                val imported = parseCsvSantri(text)
                if (imported.isNotEmpty()) {
                    imported.forEach { s -> viewModel.saveSantri(s) }
                    viewModel.showNotification("Berhasil mengimpor ${imported.size} data santri!")
                    showImportDialog = false
                } else {
                    viewModel.showNotification("Tidak ada data santri yang valid!")
                }
            } catch (e: Exception) {
                viewModel.showNotification("Gagal membaca file: ${e.message}")
            }
        }
    }

    Scaffold(
        modifier = modifier.testTag("santri_root"),
        floatingActionButton = {
            // FAB to Add Santri
            FloatingActionButton(
                onClick = {
                    santriToEdit = null
                    showAddEditDialog = true
                },
                containerColor = Color(0xFF6750A4),
                contentColor = Color.White,
                modifier = Modifier.testTag("add_santri_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Santri")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF9FAF6))
        ) {
            // Search Bar & Filter Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchVal,
                        onValueChange = { viewModel.updateSearchText(it) },
                        placeholder = { Text("Cari Nama, NIS, Ortu...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("santri_search_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4)
                        )
                    )

                    // Filter Expand Toolbar
                    IconButton(
                        onClick = { expandedFilter = !expandedFilter },
                        modifier = Modifier
                            .background(
                                color = if (expandedFilter) Color(0xFFE9F5EE) else Color(0xFFF1F1F1),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .size(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (expandedFilter) Color(0xFF6750A4) else Color.DarkGray
                        )
                    }

                    // Export PDF Report of filtered items
                    IconButton(
                        onClick = {
                            val reportFile = PDFExporter.exportLaporanSantriToPDF(
                                context,
                                "Laporan Data Santri Terfilter",
                                santriList,
                                kelasMap,
                                appConfig
                            )
                            if (reportFile != null && reportFile.exists()) {
                                viewModel.showNotification("Laporan PDF berhasil dibuat!")
                                // Trigger android share intent
                                triggerShareIntent(context, reportFile, "Laporan_Santri.pdf")
                            } else {
                                viewModel.showNotification("Gagal membuat laporan!")
                            }
                        },
                        modifier = Modifier
                            .background(Color(0xFFFFF7E6), shape = RoundedCornerShape(12.dp))
                            .size(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF",
                            tint = Color(0xFFE65100)
                        )
                    }

                    // Import CSV/Excel button
                    IconButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier
                            .background(Color(0xFFE2F0D9), shape = RoundedCornerShape(12.dp))
                            .size(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Import Excel/CSV",
                            tint = Color(0xFF385723)
                        )
                    }
                }

                // Filter Panels
                AnimatedVisibility(
                    visible = expandedFilter,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Divider(color = Color(0xFFEBEBEB))

                        // Gender filter chips
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            Text("JK: ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            listOf("Semu", "Laki-laki", "Perempuan").forEach { option ->
                                FilterChip(
                                    selected = selectedGender == option,
                                    onClick = { viewModel.updateFilterGender(option) },
                                    label = { Text(option, fontSize = 11.sp) }
                                )
                            }
                        }

                        // Status filter chips
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            Text("Status: ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            listOf("Semu", "Aktif", "Lulus", "Keluar").forEach { option ->
                                FilterChip(
                                    selected = selectedStatus == option,
                                    onClick = { viewModel.updateFilterStatus(option) },
                                    label = { Text(option, fontSize = 11.sp) }
                                )
                            }
                        }

                        // Kelas Dropdown filter
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            Text("Kelas: ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            FilterChip(
                                selected = selectedKelasId == 0,
                                onClick = { viewModel.updateFilterKelasId(0) },
                                label = { Text("Semua Kelas", fontSize = 11.sp) }
                            )
                            kelasList.forEach { kelas ->
                                FilterChip(
                                    selected = selectedKelasId == kelas.id,
                                    onClick = { viewModel.updateFilterKelasId(kelas.id) },
                                    label = { Text(kelas.namaKelas, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // Student Cards List
            if (santriList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Inbox,
                            contentDescription = "Empty",
                            tint = Color.LightGray,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            "Tidak ada data santri ditemukan",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(santriList, key = { it.id }) { santri ->
                        SantriItemCard(
                            santri = santri,
                            kelasName = kelasMap[santri.kelasId] ?: "Unassigned",
                            onDetailClick = { selectedSantriForDetail = santri },
                            onEditClick = {
                                santriToEdit = santri
                                showAddEditDialog = true
                            },
                            onDeleteClick = {
                                if (role == "Admin") {
                                    showDeleteConfirm = santri
                                } else {
                                    viewModel.showNotification("Akses Terbatas: Hanya Admin yang dapat menghapus!")
                                }
                            },
                            onNavigateToIDCard = { onNavigateToIDCard(santri.nis) }
                        )
                    }
                }
            }
        }
    }

    // -- DETAIL DIALOG --
    if (selectedSantriForDetail != null) {
        val s = selectedSantriForDetail!!
        AlertDialog(
            onDismissRequest = { selectedSantriForDetail = null },
            title = {
                Text(
                    text = "Detail Informasi Santri",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6750A4)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Profile Frame
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = Color(0xFFE9F5EE)
                        ) {
                            if (s.foto != null && File(s.foto).exists()) {
                                AsyncImage(
                                    model = s.foto,
                                    contentDescription = s.nama,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = s.nama.take(1).uppercase(),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0D5C3A)
                                    )
                                }
                            }
                        }

                        Column {
                            Text(s.nama, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1F2937))
                            Text("NIS: ${s.nis}", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                            Text(
                                text = s.status,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (s.status == "Aktif") Color(0xFF2E7D32) else Color.Red,
                                modifier = Modifier
                                    .background(
                                        if (s.status == "Aktif") Color(0xFFEFF8F2) else Color(0xFFFFEBEE),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Divider()

                    // Visual Tables Info
                    DetailRowItem(label = "Jenis Kelamin", value = s.jenisKelamin)
                    DetailRowItem(label = "Tempat, Tgl Lahir", value = "${s.tempatLahir}, ${s.tanggalLahir}")
                    DetailRowItem(label = "Kelas TPQ", value = kelasMap[s.kelasId] ?: "Belum Ditentukan")
                    DetailRowItem(label = "Nama Ayah", value = s.namaAyah)
                    DetailRowItem(label = "Nama Ibu", value = s.namaIbu)
                    DetailRowItem(label = "Nomor HP Ortu", value = s.hpOrtu)
                    DetailRowItem(
                        label = "Alamat Rumah",
                        value = "${s.alamat}, RT ${s.rt}/RW ${s.rw}, Ds. ${s.desa}, Kec. ${s.kecamatan}, Kab. ${s.kabupaten}"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedSantriForDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D5C3A))
                ) {
                    Text("Tutup")
                }
            }
        )
    }

    // -- DELETE CONFIRM DIALOG --
    if (showDeleteConfirm != null) {
        val s = showDeleteConfirm!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Konfirmasi Hapus") },
            text = { Text("Apakah Anda yakin ingin menghapus data santri '${s.nama}' dari pendaftaran? Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSantri(s)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Ya, Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // -- ADD / EDIT FORM DIALOG --
    if (showAddEditDialog) {
        AddEditSantriDialog(
            viewModel = viewModel,
            santriToEdit = santriToEdit,
            kelasList = kelasList,
            onDismiss = { showAddEditDialog = false }
        )
    }

    // Excel Paste & CSV Import Dialog for Santri
    if (showImportDialog) {
        var rawTextToPaste by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Text(
                    text = "Import Data Santri (Excel / CSV)",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6750A4),
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Format Kolom Excel / CSV:\nNama | NIS | Jenis_Kelamin | Tempat_Lahir | Tgl_Lahir (YYYY-MM-DD) | Alamat | Ayah | Ibu | HP\n\nContoh:\nRahmawati, ABA01, Perempuan, Surakarta, 2016-03-12, Gg Mawar, Ahmad, Aminah, 085732\n\nCatatan: Santri yang diimpor akan terdaftar sebagai murid aktif jilid pemula secara otomatis.",
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        lineHeight = 15.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                val templateText = "Nama,Nis,Jenis_Kelamin,Tempat_Lahir,Tanggal_Lahir,Alamat,Nama_Ayah,Nama_Ibu,Hp\nRahmawati,ABA01,Perempuan,Surakarta,2016-03-12,Gg Mawar,Ahmad,Aminah,085732\n"
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Template Santri", templateText)
                                clipboard.setPrimaryClip(clip)
                                viewModel.showNotification("Template CSV Santri disalin ke clipboard!")
                            },
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Salin Template", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val templateText = "Nama,Nis,Jenis_Kelamin,Tempat_Lahir,Tanggal_Lahir,Alamat,Nama_Ayah,Nama_Ibu,Hp\nRahmawati,ABA01,Perempuan,Surakarta,2016-03-12,Gg Mawar,Ahmad,Aminah,085732\n"
                                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                                    val file = File(downloadsDir, "template_santri.csv")
                                    file.writeText(templateText)
                                    viewModel.showNotification("Berhasil mengunduh ke: /Downloads/template_santri.csv")
                                } catch (e: Exception) {
                                    viewModel.showNotification("Gagal unduh file, disarankan Menyalin saja.")
                                }
                            },
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Unduh CSV", fontSize = 10.sp)
                        }
                    }

                    // 1. File Upload Button
                    Button(
                        onClick = { filePickerLauncher.launch("text/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Upload CSV")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pilih/Upload file .CSV")
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Atau salin berkas dari Excel dan tempelkan di bawah ini:", fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    // 2. Paste Area text box
                    OutlinedTextField(
                        value = rawTextToPaste,
                        onValueChange = { rawTextToPaste = it },
                        placeholder = { Text("Rahma\tABA01\tPerempuan\tSolo\t2016-01-01\tJl Jambu\tBudi\tSiti\t0812", fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rawTextToPaste.isNotBlank()) {
                            val imported = parseCsvSantri(rawTextToPaste)
                            if (imported.isNotEmpty()) {
                                imported.forEach { s -> viewModel.saveSantri(s) }
                                viewModel.showNotification("Berhasil mengimpor ${imported.size} data santri!")
                                showImportDialog = false
                            } else {
                                viewModel.showNotification("Tidak ada data valid yang ditemukan!")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5C3A))
                ) {
                    Text("Proses Tempel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

// Inline CSV/TSV helper parser for Santri
private fun parseCsvSantri(text: String): List<Santri> {
    val list = mutableListOf<Santri>()
    val lines = text.split("\n")
    for (line in lines) {
        if (line.isBlank() || line.startsWith("Nama", true) || line.startsWith("Nis", true)) continue
        val cols = line.split(Regex("[\t,;]")).map { it.trim() }
        if (cols.size >= 2 && cols[0].isNotBlank()) {
            val nama = cols[0]
            val nis = cols[1]
            val jkRaw = cols.getOrNull(2) ?: "Laki-laki"
            val jk = if (jkRaw.contains("p", true) || jkRaw.contains("wan", true) || jkRaw.contains("per", true)) "Perempuan" else "Laki-laki"
            val tempatLahir = cols.getOrNull(3) ?: "Surakarta"
            val tglLahir = cols.getOrNull(4) ?: "2015-01-01"
            val alamat = cols.getOrNull(5) ?: "Surakarta"
            val namaAyah = cols.getOrNull(6) ?: "-"
            val namaIbu = cols.getOrNull(7) ?: "-"
            val hp = cols.getOrNull(8) ?: "-"
            val status = cols.getOrNull(9) ?: "Aktif"
            list.add(
                Santri(
                    nama = nama,
                    nis = nis,
                    jenisKelamin = jk,
                    tempatLahir = tempatLahir,
                    tanggalLahir = tglLahir,
                    alamat = alamat,
                    rt = "01",
                    rw = "01",
                    desa = "Mangkubumen",
                    kecamatan = "Banjarsari",
                    kabupaten = "Surakarta",
                    namaAyah = namaAyah,
                    namaIbu = namaIbu,
                    hpOrtu = hp,
                    status = status,
                    kelasId = 1, // Default first level / class
                    foto = null,
                    qrCode = "https://tpqaba.id/santri/$nis"
                )
            )
        }
    }
    return list
}

@Composable
fun DetailRowItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = 14.sp, color = Color(0xFF1F2937), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SantriItemCard(
    santri: Santri,
    kelasName: String,
    onDetailClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onNavigateToIDCard: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDetailClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF6750A4).copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Foto sebelah kiri Identitas Santri
            Surface(
                modifier = Modifier
                    .size(width = 72.dp, height = 90.dp)
                    .border(1.dp, Color(0xFF6750A4), RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF3EDF7)
            ) {
                if (santri.foto != null && File(santri.foto).exists()) {
                    AsyncImage(
                        model = santri.foto,
                        contentDescription = santri.nama,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "No photo",
                                tint = Color(0xFF6750A4).copy(alpha = 0.6f),
                                modifier = Modifier.size(28.dp)
                            )
                            Text("No Foto", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Center details: Identitas Santri
            Column(modifier = Modifier.weight(1f)) {
                // 1. Nama Lengkap
                Text(
                    text = santri.nama,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF6750A4),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // 2. TTL
                Text(
                    text = "TTL: ${santri.tempatLahir}, ${santri.tanggalLahir}",
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // 3. Alamat
                Text(
                    text = "Alamat: ${santri.alamat}",
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(2.dp))

                // 4. Jilid/Kelas
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Kelas: $kelasName",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(Color(0xFF6750A4), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Text(
                        text = santri.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (santri.status == "Aktif") Color(0xFF2E7D32) else Color.Red,
                        modifier = Modifier
                            .background(
                                color = if (santri.status == "Aktif") Color(0xFFE9F5EE) else Color(0xFFFFEBEE),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Right Actions Area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onNavigateToIDCard,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = "ID Card",
                        tint = Color(0xFF6750A4),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color(0xFF339AF0),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = Color(0xFFFA5252),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// Dialog helper for Add / Edit
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSantriDialog(
    viewModel: TPQViewModel,
    santriToEdit: Santri?,
    kelasList: List<Kelas>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var nis by remember { mutableStateOf(santriToEdit?.nis ?: "ABA26${(100..999).random()}") }
    var nama by remember { mutableStateOf(santriToEdit?.nama ?: "") }
    var jenisKelamin by remember { mutableStateOf(santriToEdit?.jenisKelamin ?: "Laki-laki") }
    var tempatLahir by remember { mutableStateOf(santriToEdit?.tempatLahir ?: "Surakarta") }
    var tanggalLahir by remember { mutableStateOf(santriToEdit?.tanggalLahir ?: "YYYY-MM-DD") }
    var namaAyah by remember { mutableStateOf(santriToEdit?.namaAyah ?: "") }
    var namaIbu by remember { mutableStateOf(santriToEdit?.namaIbu ?: "") }
    var alamat by remember { mutableStateOf(santriToEdit?.alamat ?: "") }
    var rt by remember { mutableStateOf(santriToEdit?.rt ?: "") }
    var rw by remember { mutableStateOf(santriToEdit?.rw ?: "") }
    var desa by remember { mutableStateOf(santriToEdit?.desa ?: "") }
    var kecamatan by remember { mutableStateOf(santriToEdit?.kecamatan ?: "") }
    var kabupaten by remember { mutableStateOf(santriToEdit?.kabupaten ?: "Surakarta") }
    var hpOrtu by remember { mutableStateOf(santriToEdit?.hpOrtu ?: "") }
    var kelasId by remember { mutableStateOf(santriToEdit?.kelasId ?: if (kelasList.isNotEmpty()) kelasList[0].id else 0) }
    var status by remember { mutableStateOf(santriToEdit?.status ?: "Aktif") }
    var localFotoPath by remember { mutableStateOf(santriToEdit?.foto) }

    var expandedKelasDropdown by remember { mutableStateOf(false) }

    // Launchers for Gallery selection & Camera picture capturing
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val f = saveAndCompressSelectedImage(context, it, nis)
            if (f != null) {
                localFotoPath = f.absolutePath
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            val f = saveAndCompressCameraImage(context, it, nis)
            if (f != null) {
                localFotoPath = f.absolutePath
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (santriToEdit == null) "Tambah Data Santri" else "Edit Data Santri",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0D5C3A)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // UI section photo editing
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            border = BorderStroke(2.dp, Color(0xFF0D5C3A)),
                            color = Color(0xFFEFF8F2)
                        ) {
                            if (localFotoPath != null && File(localFotoPath!!).exists()) {
                                AsyncImage(
                                    model = localFotoPath,
                                    contentDescription = "Foto",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color(0xFF0D5C3A))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Camera trigger
                            Button(
                                onClick = { cameraLauncher.launch() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D5C3A)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = "Kamera", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ambil Kamera", fontSize = 10.sp)
                            }

                            // Gallery trigger
                            Button(
                                onClick = { galleryLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = "Galeri", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pilih Galeri", fontSize = 10.sp)
                            }
                        }
                    }
                }

                Divider()

                OutlinedTextField(
                    value = nis,
                    onValueChange = { nis = it },
                    label = { Text("NIS TPQ (Nomor Induk)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Nama Lengkap") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Select Gender
                Column {
                    Text("Jenis Kelamin", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = jenisKelamin == "Laki-laki",
                                onClick = { jenisKelamin = "Laki-laki" }
                            )
                            Text("Laki-laki", fontSize = 14.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = jenisKelamin == "Perempuan",
                                onClick = { jenisKelamin = "Perempuan" }
                            )
                            Text("Perempuan", fontSize = 14.sp)
                        }
                    }
                }

                // Tempat, Tanggal lahir
                OutlinedTextField(
                    value = tempatLahir,
                    onValueChange = { tempatLahir = it },
                    label = { Text("Tempat Lahir") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Custom simple DatePicker trigger
                OutlinedTextField(
                    value = tanggalLahir,
                    onValueChange = {},
                    label = { Text("Tanggal Lahir") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            val c = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val formatted = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                    tanggalLahir = formatted
                                },
                                c.get(Calendar.YEAR) - 8,
                                c.get(Calendar.MONTH),
                                c.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pilih Tanggal")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Dropdown Kelas Select
                ExposedDropdownMenuBox(
                    expanded = expandedKelasDropdown,
                    onExpandedChange = { expandedKelasDropdown = !expandedKelasDropdown }
                ) {
                    val activeKelasName = kelasList.firstOrNull { it.id == kelasId }?.namaKelas ?: "Pilih Kelas"
                    OutlinedTextField(
                        value = activeKelasName,
                        onValueChange = {},
                        label = { Text("Sematkan Kelas") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedKelasDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedKelasDropdown,
                        onDismissRequest = { expandedKelasDropdown = false }
                    ) {
                        kelasList.forEach { kelas ->
                            DropdownMenuItem(
                                text = { Text(kelas.namaKelas) },
                                onClick = {
                                    kelasId = kelas.id
                                    expandedKelasDropdown = false
                                }
                            )
                        }
                    }
                }

                // Alamat
                OutlinedTextField(
                    value = alamat,
                    onValueChange = { alamat = it },
                    label = { Text("Alamat Jalan/Gang") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = rt,
                        onValueChange = { rt = it },
                        label = { Text("RT") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rw,
                        onValueChange = { rw = it },
                        label = { Text("RW") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = desa,
                    onValueChange = { desa = it },
                    label = { Text("Desa / Kelurahan") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = kecamatan,
                    onValueChange = { kecamatan = it },
                    label = { Text("Kecamatan") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = kabupaten,
                    onValueChange = { kabupaten = it },
                    label = { Text("Kabupaten") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Orang tua
                OutlinedTextField(
                    value = namaAyah,
                    onValueChange = { namaAyah = it },
                    label = { Text("Nama Ayah Kandung") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = namaIbu,
                    onValueChange = { namaIbu = it },
                    label = { Text("Nama Ibu Kandung") },
                    modifier = Modifier.fillMaxWidth()
                )

                // HP Wali
                OutlinedTextField(
                    value = hpOrtu,
                    onValueChange = { hpOrtu = it },
                    label = { Text("Nomor HP Orang Tua / WhatsApp") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Status
                Column {
                    Text("Status Keaktifan", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        listOf("Aktif", "Lulus", "Keluar").forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = status == item,
                                    onClick = { status = item }
                                )
                                Text(item, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nama.isBlank() || nis.isBlank()) {
                        viewModel.showNotification("Harap lengkapi NIS dan Nama!")
                        return@Button
                    }

                    val updated = Santri(
                        id = santriToEdit?.id ?: 0,
                        nis = nis,
                        nama = nama,
                        jenisKelamin = jenisKelamin,
                        tempatLahir = tempatLahir,
                        tanggalLahir = tanggalLahir,
                        namaAyah = namaAyah,
                        namaIbu = namaIbu,
                        alamat = alamat,
                        rt = rt,
                        rw = rw,
                        desa = desa,
                        kecamatan = kecamatan,
                        kabupaten = kabupaten,
                        hpOrtu = hpOrtu,
                        kelasId = kelasId,
                        status = status,
                        foto = localFotoPath,
                        qrCode = "https://tpqaba.id/santri/$nis"
                    )
                    viewModel.saveSantri(updated)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D5C3A))
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

// Image copy & compress helpers
private fun saveAndCompressSelectedImage(context: Context, uri: Uri, nis: String): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val original = BitmapFactory.decodeStream(inputStream)
        val imagesDir = File(context.filesDir, "images/santri")
        if (!imagesDir.exists()) imagesDir.mkdirs()

        val destFile = File(imagesDir, "${nis}.jpg")
        val outputStream = FileOutputStream(destFile)
        original.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        outputStream.flush()
        outputStream.close()
        destFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun saveAndCompressCameraImage(context: Context, bitmap: Bitmap, nis: String): File? {
    return try {
        val imagesDir = File(context.filesDir, "images/santri")
        if (!imagesDir.exists()) imagesDir.mkdirs()

        val destFile = File(imagesDir, "${nis}.jpg")
        val outputStream = FileOutputStream(destFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        outputStream.flush()
        outputStream.close()
        destFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Intent sharing helper
private fun triggerShareIntent(context: Context, file: File, fileName: String) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, fileName)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Bagikan Laporan PDF"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
