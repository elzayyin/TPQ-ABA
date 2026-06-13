package com.elshadiqan.tpqaba.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Launch
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.elshadiqan.tpqaba.data.model.AppConfig
import com.elshadiqan.tpqaba.ui.theme.*
import com.elshadiqan.tpqaba.ui.viewmodel.TPQViewModel
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    viewModel: TPQViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appConfig by viewModel.appConfig.collectAsState()
    val context = LocalContext.current

    var namaTpq by remember(appConfig) { mutableStateOf(appConfig.namaTpq) }
    var subHeader by remember(appConfig) { mutableStateOf(appConfig.subHeader) }
    var alamat by remember(appConfig) { mutableStateOf(appConfig.alamat) }
    var telepon by remember(appConfig) { mutableStateOf(appConfig.telepon) }
    var kepalaTpq by remember(appConfig) { mutableStateOf(appConfig.kepalaTpq) }
    var tahunAjaran by remember(appConfig) { mutableStateOf(appConfig.tahunAjaran) }
    var izinkanPencarianPublik by remember(appConfig) { mutableStateOf(appConfig.izinkanPencarianPublik) }
    var firebaseDbUrl by remember(appConfig) { mutableStateOf(appConfig.firebaseDbUrl) }
    var firebaseSecret by remember(appConfig) { mutableStateOf(appConfig.firebaseSecret) }
    var localLogoPath by remember(appConfig) { mutableStateOf(appConfig.logoTpq) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = saveAndCompressLogo(context, it)
            if (file != null) {
                localLogoPath = file.absolutePath
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Konfigurasi Aplikasi", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HighDensityPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val newConfig = AppConfig(
                        id = 1,
                        namaTpq = namaTpq.trim(),
                        subHeader = subHeader.trim(),
                        alamat = alamat.trim(),
                        telepon = telepon.trim(),
                        kepalaTpq = kepalaTpq.trim(),
                        tahunAjaran = tahunAjaran.trim(),
                        izinkanPencarianPublik = izinkanPencarianPublik,
                        firebaseDbUrl = firebaseDbUrl.trim(),
                        firebaseSecret = firebaseSecret.trim(),
                        logoTpq = localLogoPath
                    )
                    viewModel.updateAppConfig(newConfig)
                    onBackClick()
                },
                containerColor = HighDensityPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Save, contentDescription = "Simpan") },
                text = { Text("Simpan Konfigurasi", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("save_config_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(HighDensityBackground)
                .testTag("config_screen_root"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HighDensityBorder),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Identitas Lembaga / Sekolah",
                            color = HighDensityPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Custom Logo Pick & Upload component
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(HighDensitySecondaryContainer)
                                    .border(1.dp, HighDensityBorder, RoundedCornerShape(16.dp))
                                    .clickable { logoPickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (localLogoPath != null && File(localLogoPath!!).exists()) {
                                    AsyncImage(
                                        model = localLogoPath,
                                        contentDescription = "Logo TPQ",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = com.elshadiqan.tpqaba.R.drawable.logo_tpq),
                                        contentDescription = "Logo Placeholder",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Logo / Icon TPQ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = HighDensityTextDark
                                )
                                Text(
                                    text = "Unggah file gambar logo untuk instansi Anda",
                                    fontSize = 10.sp,
                                    color = HighDensityTextMedium
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Button(
                                        onClick = { logoPickerLauncher.launch("image/*") },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp).testTag("upload_logo_btn")
                                    ) {
                                        Text("Pilih Gambar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    if (localLogoPath != null) {
                                        OutlinedButton(
                                            onClick = { localLogoPath = null },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            border = BorderStroke(1.dp, Color.Red),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(32.dp).testTag("delete_logo_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus",
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Hapus", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        if (localLogoPath != null && File(localLogoPath!!).exists()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = HighDensitySecondaryContainer.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, HighDensityBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("pin_shortcut_card")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Launch,
                                        contentDescription = "Pintasan Launcher",
                                        tint = HighDensityPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Ikon Launcher Home Screen",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HighDensityTextDark
                                        )
                                        Text(
                                            "Pasang/buat ikon pintasan baru di Home Screen HP dengan Logo & nama lembaga ini.",
                                            fontSize = 9.sp,
                                            color = HighDensityTextMedium
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            try {
                                                pinAppShortcut(context, namaTpq, localLogoPath)
                                                android.widget.Toast.makeText(context, "Minta penambahan pintasan Home Screen...", android.widget.Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                android.widget.Toast.makeText(context, "Gagal: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(28.dp).testTag("pin_shortcut_btn")
                                    ) {
                                        Text("Pasang", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = namaTpq,
                            onValueChange = { namaTpq = it },
                            label = { Text("Nama Lembaga (TPQ)") },
                            modifier = Modifier.fillMaxWidth().testTag("config_name_tpq"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = subHeader,
                            onValueChange = { subHeader = it },
                            label = { Text("Deskripsi / Sub-Header Sistem") },
                            modifier = Modifier.fillMaxWidth().testTag("config_subheader"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = kepalaTpq,
                            onValueChange = { kepalaTpq = it },
                            label = { Text("Kepala Lembaga / Kepala TPQ") },
                            modifier = Modifier.fillMaxWidth().testTag("config_kepala_tpq"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = tahunAjaran,
                            onValueChange = { tahunAjaran = it },
                            label = { Text("Tahun Ajaran Aktif") },
                            modifier = Modifier.fillMaxWidth().testTag("config_tahun_ajaran"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HighDensityBorder),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Kontak & Alamat",
                            color = HighDensityPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = alamat,
                            onValueChange = { alamat = it },
                            label = { Text("Alamat Kantor TPQ") },
                            modifier = Modifier.fillMaxWidth().testTag("config_alamat"),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2
                        )

                        OutlinedTextField(
                            value = telepon,
                            onValueChange = { telepon = it },
                            label = { Text("Nomor Telepon / WhatsApp") },
                            modifier = Modifier.fillMaxWidth().testTag("config_telepon"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HighDensityBorder),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Konfigurasi Firebase Sync (Awan)",
                            color = HighDensityPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Gunakan Firebase Realtime Database untuk mensinkronkan data santri, ustadz, kelas, dan absensi lintas perangkat.",
                            color = HighDensityTextMedium,
                            fontSize = 11.sp
                        )

                        OutlinedTextField(
                            value = firebaseDbUrl,
                            onValueChange = { firebaseDbUrl = it },
                            label = { Text("Firebase Database URL") },
                            placeholder = { Text("https://lpq-aba-default-rtdb.asia-southeast1.firebasedatabase.app/") },
                            modifier = Modifier.fillMaxWidth().testTag("config_firebase_url"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = firebaseSecret,
                            onValueChange = { firebaseSecret = it },
                            label = { Text("Firebase Database Secret (Opsional)") },
                            placeholder = { Text("Masukkan DB secret token jika dikunci") },
                            modifier = Modifier.fillMaxWidth().testTag("config_firebase_secret"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HighDensityBorder),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Izinkan Pencarian Publik",
                                color = HighDensityTextDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Mengizinkan tamu / wali santri mencari nama dan NIS tanpa kata sandi",
                                color = HighDensityTextMedium,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = izinkanPencarianPublik,
                            onCheckedChange = { izinkanPencarianPublik = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = HighDensityPrimary,
                                checkedTrackColor = HighDensitySecondaryContainer
                            ),
                            modifier = Modifier.testTag("config_toggle_pencarian")
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HighDensityBorder),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Backup & Impor / Ekspor Konfigurasi",
                            color = HighDensityPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Gunakan template JSON untuk membackup atau memulihkan konfigurasi sistem, nama TPQ, alamat, dan URL Firebase sinkronisasi.",
                            color = HighDensityTextMedium,
                            fontSize = 11.sp
                        )

                        var jsonInputText by remember { mutableStateOf("") }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val templateText = """{
  "namaTpq": "$namaTpq",
  "subHeader": "$subHeader",
  "alamat": "$alamat",
  "telepon": "$telepon",
  "kepalaTpq": "$kepalaTpq",
  "tahunAjaran": "$tahunAjaran",
  "izinkanPencarianPublik": ${izinkanPencarianPublik},
  "firebaseDbUrl": "$firebaseDbUrl",
  "firebaseSecret": "$firebaseSecret"
}"""
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Template Config", templateText)
                                    clipboard.setPrimaryClip(clip)
                                    viewModel.showNotification("Format JSON Konfigurasi disalin ke clipboard!")
                                },
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Salin Config", fontSize = 10.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        val templateText = """{
  "namaTpq": "$namaTpq",
  "subHeader": "$subHeader",
  "alamat": "$alamat",
  "telepon": "$telepon",
  "kepalaTpq": "$kepalaTpq",
  "tahunAjaran": "$tahunAjaran",
  "izinkanPencarianPublik": ${izinkanPencarianPublik},
  "firebaseDbUrl": "$firebaseDbUrl",
  "firebaseSecret": "$firebaseSecret"
}"""
                                        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                                        val file = File(downloadsDir, "template_config.json")
                                        file.writeText(templateText)
                                        viewModel.showNotification("Berhasil ekspor ke: /Downloads/template_config.json")
                                    } catch (e: Exception) {
                                        viewModel.showNotification("Gagal ekspor ke file, disarankan Menyalin saja.")
                                    }
                                },
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Unduh JSON", fontSize = 10.sp)
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Tempel JSON konfigurasi untuk impor:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextDark
                            )
                            OutlinedTextField(
                                value = jsonInputText,
                                onValueChange = { jsonInputText = it },
                                placeholder = { Text("""{ "namaTpq": "LPQ Abu Bakar Amin", ... }""", fontSize = 11.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                            )
                            Button(
                                onClick = {
                                    if (jsonInputText.isNotBlank()) {
                                        val imported = parseConfigJson(jsonInputText)
                                        if (imported != null) {
                                            namaTpq = imported.namaTpq
                                            subHeader = imported.subHeader
                                            alamat = imported.alamat
                                            telepon = imported.telepon
                                            kepalaTpq = imported.kepalaTpq
                                            tahunAjaran = imported.tahunAjaran
                                            izinkanPencarianPublik = imported.izinkanPencarianPublik
                                            firebaseDbUrl = imported.firebaseDbUrl
                                            firebaseSecret = imported.firebaseSecret
                                            viewModel.showNotification("Berhasil impor JSON! Tekan tombol Save mengambang di pojok kanan bawah untuk menyimpan.")
                                            jsonInputText = ""
                                        } else {
                                            viewModel.showNotification("Format JSON tidak valid atau namaTpq kosong!")
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.align(Alignment.End).height(32.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)
                            ) {
                                Text("Proses Impor", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseConfigJson(jsonText: String): AppConfig? {
    return try {
        fun extractKey(key: String): String {
            val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
            return pattern.find(jsonText)?.groupValues?.get(1)?.trim() ?: ""
        }
        fun extractBool(key: String): Boolean {
            val pattern = "\"$key\"\\s*:\\s*(true|false)".toRegex()
            return pattern.find(jsonText)?.groupValues?.get(1)?.toBoolean() ?: true
        }

        val nama = extractKey("namaTpq")
        if (nama.isBlank()) return null
        
        AppConfig(
            id = 1,
            namaTpq = nama,
            subHeader = extractKey("subHeader").ifBlank { "Sistem Informasi & Kartu Pengenal Digital" },
            alamat = extractKey("alamat"),
            telepon = extractKey("telepon"),
            kepalaTpq = extractKey("kepalaTpq"),
            tahunAjaran = extractKey("tahunAjaran").ifBlank { "2026/2027" },
            izinkanPencarianPublik = extractBool("izinkanPencarianPublik"),
            firebaseDbUrl = extractKey("firebaseDbUrl").ifBlank { "https://lpq-aba-default-rtdb.asia-southeast1.firebasedatabase.app/" },
            firebaseSecret = extractKey("firebaseSecret"),
            logoTpq = null
        )
    } catch (e: Exception) {
        null
    }
}

private fun saveAndCompressLogo(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val original = BitmapFactory.decodeStream(inputStream)
        val imagesDir = File(context.filesDir, "images/logo")
        if (!imagesDir.exists()) imagesDir.mkdirs()

        val destFile = File(imagesDir, "logo_tpq.jpg")
        val outputStream = FileOutputStream(destFile)
        original.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        outputStream.flush()
        outputStream.close()
        destFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun pinAppShortcut(context: Context, label: String, logoPath: String?) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val shortcutManager = context.getSystemService(android.content.pm.ShortcutManager::class.java)
        if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
            val intent = android.content.Intent(context, com.elshadiqan.tpqaba.MainActivity::class.java).apply {
                action = android.content.Intent.ACTION_MAIN
            }
            
            val icon = if (logoPath != null && File(logoPath).exists()) {
                val bitmap = BitmapFactory.decodeFile(logoPath)
                if (bitmap != null) {
                    androidx.core.graphics.drawable.IconCompat.createWithBitmap(bitmap)
                } else {
                    androidx.core.graphics.drawable.IconCompat.createWithResource(context, com.elshadiqan.tpqaba.R.drawable.logo_tpq)
                }
            } else {
                androidx.core.graphics.drawable.IconCompat.createWithResource(context, com.elshadiqan.tpqaba.R.drawable.logo_tpq)
            }
            
            val pinShortcutInfo = androidx.core.content.pm.ShortcutInfoCompat.Builder(context, "tpq_dynamic_shortcut")
                .setShortLabel(label)
                .setIcon(icon)
                .setIntent(intent)
                .build()
                
            androidx.core.content.pm.ShortcutManagerCompat.requestPinShortcut(context, pinShortcutInfo, null)
        }
    }
}
