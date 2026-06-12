package com.elshadiqan.tpqaba.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elshadiqan.tpqaba.data.model.Ustadz
import com.elshadiqan.tpqaba.ui.viewmodel.TPQViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontFamily
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Context
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UstadzScreen(
    viewModel: TPQViewModel,
    onNavigateToIDCard: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ustadzList by viewModel.allUstadz.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var ustadzToEdit by remember { mutableStateOf<Ustadz?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Ustadz?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }

    // CSV File Picker Launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val text = inputStream?.bufferedReader()?.use { r -> r.readText() } ?: ""
                val imported = parseCsvUstadz(text)
                if (imported.isNotEmpty()) {
                    imported.forEach { u -> viewModel.saveUstadz(u) }
                    viewModel.showNotification("Berhasil mengimpor ${imported.size} data pengajar!")
                    showImportDialog = false
                } else {
                    viewModel.showNotification("Tidak ada data pengajar valid yang ditemukan!")
                }
            } catch (e: Exception) {
                viewModel.showNotification("Gagal membaca file: ${e.message}")
            }
        }
    }

    Scaffold(
        modifier = modifier.testTag("ustadz_root"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    ustadzToEdit = null
                    showAddEditDialog = true
                },
                containerColor = Color(0xFF6750A4),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Guru")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF9FAF6))
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Asatidzah (Staff Pengajar)",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6750A4)
                    )
                    Text(
                        text = "Kelola data ustadz / ustadzah pengajar LPQ Abu Bakar Amin.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                // Import Excel/CSV Button
                Button(
                    onClick = { showImportDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5C3A)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = "Import", modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("IMPORT EXCEL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (ustadzList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Group, contentDescription = "Empty", tint = Color.LightGray, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Belum ada data asatidzah", color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = { showImportDialog = true }) {
                            Text("Impor Berkas atau Tempel Data")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(ustadzList) { ustadz ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
                                // Left: Initial / Gender Monogram Avatar Box
                                Surface(
                                    modifier = Modifier
                                        .size(width = 72.dp, height = 90.dp)
                                        .border(1.dp, Color(0xFF6750A4), RoundedCornerShape(8.dp)),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (ustadz.jenisKelamin == "Laki-laki") Color(0xFFE3F2FD) else Color(0xFFFCE4EC)
                                ) {
                                    if (ustadz.foto != null && File(ustadz.foto).exists()) {
                                        AsyncImage(
                                            model = ustadz.foto,
                                            contentDescription = ustadz.nama,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = if (ustadz.jenisKelamin == "Laki-laki") Icons.Default.Male else Icons.Default.Female,
                                                    contentDescription = ustadz.jenisKelamin,
                                                    tint = if (ustadz.jenisKelamin == "Laki-laki") Color(0xFF1976D2) else Color(0xFFC2185B),
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                val initial = ustadz.nama.take(2).uppercase()
                                                Text(
                                                    text = initial,
                                                    fontSize = 11.sp,
                                                    color = Color.Black.copy(alpha = 0.7f),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Center details: Identitas Ustadz/Wali
                                Column(modifier = Modifier.weight(1f)) {
                                    // 1. Nama Lengkap
                                    Text(
                                        text = ustadz.nama,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF6750A4),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    // 2. Alamat
                                    Text(
                                        text = "Alamat: ${ustadz.alamat}",
                                        fontSize = 11.sp,
                                        color = Color.DarkGray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    // 3. No HP / WhatsApp
                                    Text(
                                        text = "HP: ${ustadz.hp}",
                                        fontSize = 11.sp,
                                        color = Color.DarkGray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    // 4. Badges (Jabatan & Gender)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = ustadz.jabatan,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier
                                                .background(Color(0xFF6750A4), shape = RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                        Text(
                                            text = ustadz.jenisKelamin,
                                            fontSize = 10.sp,
                                            color = if (ustadz.jenisKelamin == "Laki-laki") Color(0xFF1976D2) else Color(0xFFC2185B),
                                            modifier = Modifier
                                                .background(if (ustadz.jenisKelamin == "Laki-laki") Color(0xFFE3F2FD) else Color(0xFFFCE4EC), shape = RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Right Actions Area (Columed)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // WhatsApp Action
                                    IconButton(
                                        onClick = {
                                            val number = ustadz.hp.replace("-", "").replace(" ", "")
                                            val formatted = if (number.startsWith("0")) "62" + number.substring(1) else number
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                data = Uri.parse("https://api.whatsapp.com/send?phone=$formatted&text=Assalamualaikum%20Ustadz/ah%20${ustadz.nama}")
                                            }
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "WhatsApp",
                                            tint = Color(0xFF25D366),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Cetak ID Card Action
                                    IconButton(
                                        onClick = { onNavigateToIDCard(ustadz.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Badge,
                                            contentDescription = "Cetak Kartu",
                                            tint = Color(0xFF6750A4),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Edit Action
                                    IconButton(
                                        onClick = {
                                            ustadzToEdit = ustadz
                                            showAddEditDialog = true
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = Color(0xFF339AF0),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Delete Action
                                    IconButton(
                                        onClick = { showDeleteConfirm = ustadz },
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
                }
            }
        }
    }

    // Delete confirmation
    if (showDeleteConfirm != null) {
        val u = showDeleteConfirm!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Hapus Data Pengajar?") },
            text = { Text("Apakah Anda yakin ingin menghapus asatidzah '${u.nama}' dari data pengajar?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUstadz(u)
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

    // Add Edit Dialog Form
    if (showAddEditDialog) {
        var nama by remember { mutableStateOf(ustadzToEdit?.nama ?: "") }
        var jenisKelamin by remember { mutableStateOf(ustadzToEdit?.jenisKelamin ?: "Laki-laki") }
        var alamat by remember { mutableStateOf(ustadzToEdit?.alamat ?: "") }
        var hp by remember { mutableStateOf(ustadzToEdit?.hp ?: "") }
        var jabatan by remember { mutableStateOf(ustadzToEdit?.jabatan ?: "Guru Utama") }
        var localFotoPath by remember { mutableStateOf(ustadzToEdit?.foto) }

        val imageId = remember { ustadzToEdit?.id?.toString() ?: System.currentTimeMillis().toString() }

        val galleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                val f = saveAndCompressSelectedUstadzImage(context, it, imageId)
                if (f != null) {
                    localFotoPath = f.absolutePath
                }
            }
        }

        val cameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicturePreview()
        ) { bitmap: Bitmap? ->
            bitmap?.let {
                val f = saveAndCompressCameraUstadzImage(context, it, imageId)
                if (f != null) {
                    localFotoPath = f.absolutePath
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = {
                Text(
                    text = if (ustadzToEdit == null) "Tambah Data Staff" else "Edit Data Staff",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6750A4)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                ) {
                    // Photo selector widget
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                modifier = Modifier.size(80.dp),
                                shape = CircleShape,
                                border = BorderStroke(2.dp, Color(0xFF6750A4)),
                                color = Color(0xFFF3EDF7)
                            ) {
                                if (localFotoPath != null && File(localFotoPath!!).exists()) {
                                    AsyncImage(
                                        model = localFotoPath,
                                        contentDescription = "Foto Guru",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color(0xFF6750A4))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { cameraLauncher.launch(null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = "Kamera", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ambil Kamera", fontSize = 10.sp)
                                }

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
                        value = nama,
                        onValueChange = { nama = it },
                        label = { Text("Nama Lengkap") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Gender Selection
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

                    OutlinedTextField(
                        value = alamat,
                        onValueChange = { alamat = it },
                        label = { Text("Alamat Tempat Tinggal") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = hp,
                        onValueChange = { hp = it },
                        label = { Text("Nomor HP / WA") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = jabatan,
                        onValueChange = { jabatan = it },
                        label = { Text("Staff Jabatan (misal: Guru Utama)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nama.isNotBlank()) {
                            val newU = Ustadz(
                                id = ustadzToEdit?.id ?: 0,
                                nama = nama,
                                jenisKelamin = jenisKelamin,
                                alamat = alamat,
                                hp = hp,
                                jabatan = jabatan,
                                foto = localFotoPath
                            )
                            viewModel.saveUstadz(newU)
                            showAddEditDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEditDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Excel Paste & CSV Import Dialog
    if (showImportDialog) {
        var rawTextToPaste by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Text(
                    text = "Import Data Staff (Excel / CSV)",
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
                        text = "Format Kolom Excel / CSV:\nNama | Jenis_Kelamin | Alamat | No_HP | Jabatan\n\nContoh:\nUstadz Ahmad, Laki-laki, Mangkubumen, 081234, Guru Utama",
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        lineHeight = 15.sp
                    )

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
                        placeholder = { Text("Nama_Guru\tLaki-laki\tSurakarta\t081234\tGuru Utama", fontSize = 11.sp) },
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
                            val imported = parseCsvUstadz(rawTextToPaste)
                            if (imported.isNotEmpty()) {
                                imported.forEach { u -> viewModel.saveUstadz(u) }
                                viewModel.showNotification("Berhasil mengimpor ${imported.size} data pengajar!")
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

// Inline CSV/TSV helper parser
private fun parseCsvUstadz(text: String): List<Ustadz> {
    val list = mutableListOf<Ustadz>()
    val lines = text.split("\n")
    for (line in lines) {
        if (line.isBlank() || line.startsWith("Nama", true)) continue
        val cols = line.split(Regex("[\t,;]")).map { it.trim() }
        if (cols.isNotEmpty() && cols[0].isNotBlank()) {
            val nama = cols[0]
            val jkRaw = cols.getOrNull(1) ?: "Laki-laki"
            val jk = if (jkRaw.contains("p", true) || jkRaw.contains("wan", true) || jkRaw.contains("per", true)) "Perempuan" else "Laki-laki"
            val alamat = cols.getOrNull(2) ?: "Surakarta"
            val hp = cols.getOrNull(3) ?: "-"
            val jabatan = cols.getOrNull(4) ?: "Guru"
            list.add(
                Ustadz(
                    nama = nama,
                    jenisKelamin = jk,
                    alamat = alamat,
                    hp = hp,
                    jabatan = jabatan
                )
            )
        }
    }
    return list
}

private fun saveAndCompressSelectedUstadzImage(context: Context, uri: Uri, id: String): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val original = BitmapFactory.decodeStream(inputStream)
        val imagesDir = File(context.filesDir, "images/ustadz")
        if (!imagesDir.exists()) imagesDir.mkdirs()

        val destFile = File(imagesDir, "${id}.jpg")
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

private fun saveAndCompressCameraUstadzImage(context: Context, bitmap: Bitmap, id: String): File? {
    return try {
        val imagesDir = File(context.filesDir, "images/ustadz")
        if (!imagesDir.exists()) imagesDir.mkdirs()

        val destFile = File(imagesDir, "${id}.jpg")
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
