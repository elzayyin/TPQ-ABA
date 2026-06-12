package com.elshadiqan.tpqaba.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.elshadiqan.tpqaba.data.model.Santri
import com.elshadiqan.tpqaba.data.model.Ustadz
import com.elshadiqan.tpqaba.data.model.Absensi
import com.elshadiqan.tpqaba.utils.PDFExporter
import com.elshadiqan.tpqaba.ui.viewmodel.TPQViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    viewModel: TPQViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val santriList by viewModel.filteredSantri.collectAsState()
    val ustadzList by viewModel.allUstadz.collectAsState()
    val tempKelasList by viewModel.allKelas.collectAsState()
    val appConfig by viewModel.appConfig.collectAsState()
    val absensiList by viewModel.allAbsensi.collectAsState()
    
    val kelasMap = remember(tempKelasList) { tempKelasList.associate { it.id to it.namaKelas } }

    // Dropdown roles: "Santri" or "Ustadz/Guru"
    var selectedRole by remember { mutableStateOf("Santri") }
    var expandedRoleDropdown by remember { mutableStateOf(false) }

    // Selection list states
    var selectedSantriForSimul by remember { mutableStateOf<Santri?>(null) }
    var selectedUstadzForSimul by remember { mutableStateOf<Ustadz?>(null) }
    
    var expandedSelectDropdown by remember { mutableStateOf(false) }
    
    // Scan simulation states
    var simScanningState by remember { mutableStateOf(false) }
    var scannedSantriResult by remember { mutableStateOf<Santri?>(null) }
    var scannedUstadzResult by remember { mutableStateOf<Ustadz?>(null) }

    // Laser bar sliding animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser_anim")
    val laserYOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 180f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "laser_y"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAF6))
            .verticalScroll(rememberScrollState())
            .testTag("qrscanner_root"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
            Text(
                text = "Absensi & Validasi QR Code",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0E5C3A)
            )
            Text(
                text = "Scan QR Code Santri / Staff Pengajar untuk memvalidasi identitas dan mencatatkan kehadiran di ${appConfig.namaTpq}.",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        // Camera viewfinder Emulator with Laser scanner
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // Emulated background matrix lines
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = "Scan",
                    tint = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.size(100.dp)
                )
            }

            // Green Scanning Box outline
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .border(3.dp, Color(0xFF2E7D32), RoundedCornerShape(12.dp))
            ) {
                // Animated laser scanner bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .offset(y = laserYOffset.dp)
                        .background(Color(0xFF2E7D32))
                )
            }

            if (simScanningState) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Decoding QR Code...", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        // Simulations Selector Deck
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .testTag("simulator_card"),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pengaturan Masukan Simulasi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0E5C3A)
                )

                // 1. SELECT ROLE
                Text(
                    text = "Pilih Peran (Role):",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
                ExposedDropdownMenuBox(
                    expanded = expandedRoleDropdown,
                    onExpandedChange = { expandedRoleDropdown = !expandedRoleDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedRole,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRoleDropdown) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0E5C3A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedRoleDropdown,
                        onDismissRequest = { expandedRoleDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Santri") },
                            onClick = {
                                selectedRole = "Santri"
                                selectedSantriForSimul = null
                                selectedUstadzForSimul = null
                                expandedRoleDropdown = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ustadz/ah (Guru)") },
                            onClick = {
                                selectedRole = "Guru"
                                selectedSantriForSimul = null
                                selectedUstadzForSimul = null
                                expandedRoleDropdown = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 2. CHOOSE ROSTER ITEM
                Text(
                    text = "Pilih Data Personil:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
                ExposedDropdownMenuBox(
                    expanded = expandedSelectDropdown,
                    onExpandedChange = { expandedSelectDropdown = !expandedSelectDropdown }
                ) {
                    val activeLabel = if (selectedRole == "Santri") {
                        selectedSantriForSimul?.nama ?: "Pilih Roster Santri..."
                    } else {
                        selectedUstadzForSimul?.nama ?: "Pilih Roster Guru/Ustadz..."
                    }
                    
                    OutlinedTextField(
                        value = activeLabel,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSelectDropdown) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0E5C3A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expandedSelectDropdown,
                        onDismissRequest = { expandedSelectDropdown = false }
                    ) {
                        if (selectedRole == "Santri") {
                            if (santriList.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Data Santri Kosong") },
                                    onClick = { expandedSelectDropdown = false }
                                )
                            }
                            santriList.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text("${s.nis} - ${s.nama}") },
                                    onClick = {
                                        selectedSantriForSimul = s
                                        expandedSelectDropdown = false
                                    }
                                )
                            }
                        } else {
                            if (ustadzList.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Data Ustadz/ah Kosong") },
                                    onClick = { expandedSelectDropdown = false }
                                )
                            }
                            ustadzList.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text("${u.id} - ${u.nama} (${u.jabatan})") },
                                    onClick = {
                                        selectedUstadzForSimul = u
                                        expandedSelectDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 3. TRIGGER SCAN BUTTON
                Button(
                    onClick = {
                        if (selectedRole == "Santri" && selectedSantriForSimul == null) {
                            viewModel.showNotification("Harap pilih salah satu santri terlebih dahulu!")
                            return@Button
                        }
                        if (selectedRole == "Guru" && selectedUstadzForSimul == null) {
                            viewModel.showNotification("Harap pilih salah satu guru terlebih dahulu!")
                            return@Button
                        }

                        simScanningState = true
                        scannedSantriResult = null
                        scannedUstadzResult = null
                        
                        scope.launch {
                            delay(1000)
                            simScanningState = false
                            
                            val todayDateStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
                            val currentTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

                             if (selectedRole == "Santri") {
                                val sModel = selectedSantriForSimul!!
                                scannedSantriResult = sModel
                                
                                val newAbs = Absensi(
                                    role = "Santri",
                                    itemId = sModel.id,
                                    nama = sModel.nama,
                                    detail = kelasMap[sModel.kelasId] ?: "Murid",
                                    tanggal = todayDateStr,
                                    waktu = currentTimeStr,
                                    status = "Hadir"
                                )
                                viewModel.recordAbsensi(newAbs)
                                viewModel.showNotification("Presensi Hadir Santri ${sModel.nama} Berhasil!")
                            } else {
                                val uModel = selectedUstadzForSimul!!
                                scannedUstadzResult = uModel
                                
                                val newAbs = Absensi(
                                    role = "Ustadz/ah",
                                    itemId = uModel.id,
                                    nama = uModel.nama,
                                    detail = uModel.jabatan,
                                    tanggal = todayDateStr,
                                    waktu = currentTimeStr,
                                    status = "Hadir"
                                )
                                viewModel.recordAbsensi(newAbs)
                                viewModel.showNotification("Presensi Hadir Ustadz ${uModel.nama} Berhasil!")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E5C3A)),
                    enabled = !simScanningState && (selectedSantriForSimul != null || selectedUstadzForSimul != null)
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = "Simulate")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SIMULASIKAN SCAN PRESENSI", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // SCANNED VALIDATION DETAILS DISPLAY (SANTRI)
        AnimatedVisibility(
            visible = scannedSantriResult != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            scannedSantriResult?.let { s ->
                val sKelas = kelasMap[s.kelasId] ?: "Unassigned"
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(bottom = 10.dp)
                        .testTag("res_validation_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF8F2)), // Warm Green highlight
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF2E7D32)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Valid",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "SANTRI TERVALIDASI - HADIR",
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Divider(color = Color(0xFFB4D8C2))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, Color(0xFF2E7D32)),
                                color = Color.White
                            ) {
                                if (s.foto != null && File(s.foto).exists()) {
                                    AsyncImage(
                                        model = s.foto,
                                        contentDescription = s.nama,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Text(
                                            s.nama.take(1).uppercase(),
                                            color = Color(0xFF0E5C3A),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                    }
                                }
                            }

                            Column {
                                Text(s.nama, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1F2937))
                                Text("NIS: ${s.nis}", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("Kelas: $sKelas", fontSize = 12.sp, color = Color.DarkGray)
                            }
                        }

                        Divider(color = Color(0xFFB4D8C2))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("STATUS KEAKTIFAN", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(
                                    s.status,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (s.status == "Aktif") Color(0xFF2E7D32) else Color.Red
                                )
                            }

                            Button(
                                onClick = { scannedSantriResult = null },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E5C3A)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Selesai", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // SCANNED VALIDATION DETAILS DISPLAY (USTADZ / GURU)
        AnimatedVisibility(
            visible = scannedUstadzResult != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            scannedUstadzResult?.let { u ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(bottom = 10.dp)
                        .testTag("res_validation_teacher_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF8F2)), // Warm Green highlight
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF2E7D32)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Valid",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "USTADZ/AH TERVALIDASI - HADIR",
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Divider(color = Color(0xFFB4D8C2))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, Color(0xFF2E7D32)),
                                color = Color.White
                            ) {
                                if (u.foto != null && File(u.foto).exists()) {
                                    AsyncImage(
                                        model = u.foto,
                                        contentDescription = u.nama,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Text(
                                            u.nama.take(1).uppercase(),
                                            color = Color(0xFF0E5C3A),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                    }
                                }
                            }

                            Column {
                                Text(u.nama, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1F2937))
                                Text("ID STAFF: ABA-UT-${u.id.toString().padStart(3, '0')}", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("Jabatan: ${u.jabatan}", fontSize = 12.sp, color = Color.DarkGray)
                            }
                        }

                        Divider(color = Color(0xFFB4D8C2))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("KONTAK TELEPON", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(
                                    u.hp,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                            }

                            Button(
                                onClick = { scannedUstadzResult = null },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E5C3A)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Selesai", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- REAL-TIME ATTENDANCE HISTORY LOGS ---
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(bottom = 30.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Riwayat Absensi Harian",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1F2937)
                    )
                    
                    Text(
                        text = "${absensiList.size} Riwayat",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider()

                if (absensiList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada riwayat absensi masuk hari ini.",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        absensiList.reversed().take(15).forEach { record ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF9FAF6), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "[${record.role}]",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = if (record.role == "Santri") Color(0xFF0F52BA) else Color(0xFF0E5C3A)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = record.nama,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF1F2937)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${record.detail} • ${record.tanggal} ${record.waktu}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                
                                Text(
                                    text = record.status,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0E5C3A),
                                    modifier = Modifier
                                        .background(Color(0xFFEFF8F2), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cetak Rekap PDF Button
                    Button(
                        onClick = {
                            if (absensiList.isEmpty()) {
                                viewModel.showNotification("Tidak ada riwayat absensi untuk dicetak!")
                                return@Button
                            }
                            val pdfFile = PDFExporter.exportAbsensiToPDF(
                                context = context,
                                title = "REKAP ABSENSI HARIAN SANTRI & USTADZ/AH",
                                absensiList = absensiList,
                                appConfig = appConfig
                            )
                            if (pdfFile != null && pdfFile.exists()) {
                                viewModel.showNotification("PDF Rekap Absensi Berhasil dicetak!")
                                // Trigger share/print intent
                                try {
                                    val authority = "${context.packageName}.fileprovider"
                                    val contentUri = androidx.core.content.FileProvider.getUriForFile(context, authority, pdfFile)
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Rekap_Absensi.pdf")
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Bagikan Rekap Absensi"))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            } else {
                                viewModel.showNotification("Gagal mencetak PDF Rekap.")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        enabled = absensiList.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Print PDF")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CETAK REKAP", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                    }

                    // Reset Logs Button
                    OutlinedButton(
                        onClick = {
                            viewModel.clearAllAbsensi()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                        enabled = absensiList.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear logs")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RESET LOGS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
