package com.elshadiqan.tpqaba.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.elshadiqan.tpqaba.data.model.Santri
import com.elshadiqan.tpqaba.data.model.Ustadz
import com.elshadiqan.tpqaba.data.model.AppConfig
import com.elshadiqan.tpqaba.ui.viewmodel.TPQViewModel
import com.elshadiqan.tpqaba.utils.PDFExporter
import com.elshadiqan.tpqaba.utils.QRCodeGenerator
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IDCardScreen(
    viewModel: TPQViewModel,
    initialNis: String? = null,
    initialUstadzId: Int? = null,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val santriList by viewModel.filteredSantri.collectAsState()
    val ustadzList by viewModel.allUstadz.collectAsState()
    val kelasList by viewModel.allKelas.collectAsState()
    val appConfig by viewModel.appConfig.collectAsState()
    val kelasMap = remember(kelasList) { kelasList.associate { it.id to it.namaKelas } }

    var currentTabMode by remember { mutableStateOf(if (initialUstadzId != null) "ustadz" else "santri") }
    var massModeEnabled by remember { mutableStateOf(false) }

    // Multi select buffers
    val selectedSantriNisList = remember { mutableStateListOf<String>() }
    val selectedUstadzIdList = remember { mutableStateListOf<Int>() }

    // Individual selected item
    var selectedNis by remember { mutableStateOf(initialNis ?: (if (santriList.isNotEmpty()) santriList[0].nis else "")) }
    val selectedSantri = remember(selectedNis, santriList) {
        santriList.firstOrNull { it.nis == selectedNis } ?: santriList.firstOrNull()
    }

    var selectedUstadzId by remember { mutableStateOf(initialUstadzId ?: (if (ustadzList.isNotEmpty()) ustadzList[0].id else 0)) }
    val selectedUstadz = remember(selectedUstadzId, ustadzList) {
        ustadzList.firstOrNull { it.id == selectedUstadzId } ?: ustadzList.firstOrNull()
    }

    var showFrontSide by remember { mutableStateOf(true) }

    // Initial triggers
    LaunchedEffect(santriList, ustadzList) {
        if (selectedNis.isBlank() && santriList.isNotEmpty()) {
            selectedNis = santriList[0].nis
        }
        if (selectedUstadzId == 0 && ustadzList.isNotEmpty()) {
            selectedUstadzId = ustadzList[0].id
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAF6))
            .testTag("idcard_root"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App top interactive title bar with local back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .background(Color(0xFFF1F3F9), shape = RoundedCornerShape(12.dp))
                    .size(40.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.DarkGray)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Cetak Kartu Pengenal",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6750A4)
                )
                Text(
                    text = "Ekspor kartu digital satuan atau massal cetak instan ke PDF.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        // Dual Segmented Tabs [KARTU SANTRI] and [KARTU USTADZ/AH]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { currentTabMode = "santri" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentTabMode == "santri") Color(0xFF6750A4) else Color(0xFFF1F3F9),
                    contentColor = if (currentTabMode == "santri") Color.White else Color.DarkGray
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.School, contentDescription = "Santri", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("KARTU SANTRI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { currentTabMode = "ustadz" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentTabMode == "ustadz") Color(0xFF6750A4) else Color(0xFFF1F3F9),
                    contentColor = if (currentTabMode == "ustadz") Color.White else Color.DarkGray
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.SupervisorAccount, contentDescription = "Ustadz", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("KARTU STAFF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Checklist selector or preview area based on MASS toggle
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mass mode option toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CopyAll, contentDescription = "Mass print", tint = Color(0xFF6750A4))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Aktifkan Cetak Massal", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Cetak banyak kartu sekaligus", fontSize = 10.sp, color = Color.Gray)
                    }
                }
                Switch(
                    checked = massModeEnabled,
                    onCheckedChange = { massModeEnabled = it }
                )
            }

            if (!massModeEnabled) {
                // SINGLE CARD PREVIEW MODE
                if (currentTabMode == "santri") {
                    // SANTRI SINGLE SELECTOR
                    if (santriList.isEmpty()) {
                        Text("Belum ada data santri.", modifier = Modifier.padding(20.dp), color = Color.Gray)
                    } else {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                            Text("PILIH SANTRI:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                santriList.forEach { s ->
                                    FilterChip(
                                        selected = selectedNis == s.nis,
                                        onClick = { selectedNis = s.nis },
                                        label = { Text(s.nama, maxLines = 1) }
                                    )
                                }
                            }
                        }

                        if (selectedSantri != null) {
                            CardStateAndPreviewSection(
                                showFrontSide = showFrontSide,
                                onSideToggle = { showFrontSide = it },
                                frontLayout = {
                                    CardDepanLayout(
                                        santri = selectedSantri,
                                        kelasName = kelasMap[selectedSantri.kelasId] ?: "Belum Ditentukan",
                                        appConfig = appConfig
                                    )
                                },
                                backLayout = {
                                    CardBelakangLayout(santri = selectedSantri, appConfig = appConfig)
                                }
                            )

                            // Single Print Trigger
                            Button(
                                onClick = {
                                    val file = PDFExporter.exportIDCardsToPDF(
                                        context,
                                        listOf(selectedSantri),
                                        kelasMap,
                                        appConfig
                                    )
                                    if (file != null && file.exists()) {
                                        viewModel.showNotification("Kartu Santri PDF Berhasil dibuat!")
                                        triggerShareIDCardFile(context, file, "Kartu_Santri_${selectedSantri.nis}.pdf")
                                    } else {
                                        viewModel.showNotification("Gagal mencetak PDF Kartu!")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E5C3A))
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CETAK KARTU PDF", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // STAFF SINGLE SELECTOR
                    if (ustadzList.isEmpty()) {
                        Text("Belum ada data asatidzah.", modifier = Modifier.padding(20.dp), color = Color.Gray)
                    } else {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                            Text("PILIH PENGASUT / STAFF:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ustadzList.forEach { u ->
                                    FilterChip(
                                        selected = selectedUstadzId == u.id,
                                        onClick = { selectedUstadzId = u.id },
                                        label = { Text(u.nama, maxLines = 1) }
                                    )
                                }
                            }
                        }

                        if (selectedUstadz != null) {
                            CardStateAndPreviewSection(
                                showFrontSide = showFrontSide,
                                onSideToggle = { showFrontSide = it },
                                frontLayout = {
                                    CardUstadzFrontLayout(ustadz = selectedUstadz, appConfig = appConfig)
                                },
                                backLayout = {
                                    CardUstadzBackLayout(ustadz = selectedUstadz, appConfig = appConfig)
                                }
                            )

                            // Single Teacher Print Trigger
                            Button(
                                onClick = {
                                    val file = PDFExporter.exportTeacherCardsToPDF(
                                        context,
                                        listOf(selectedUstadz),
                                        appConfig
                                    )
                                    if (file != null && file.exists()) {
                                        viewModel.showNotification("Kartu Ustadz/ah PDF Berhasil dibuat!")
                                        triggerShareIDCardFile(context, file, "Kartu_Ustadz_${selectedUstadz.id}.pdf")
                                    } else {
                                        viewModel.showNotification("Gagal mencetak PDF Kartu!")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E5C3A))
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CETAK KARTU PDF", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // MASS PRINT SELECTION CHECKBOXES MODE
                if (currentTabMode == "santri") {
                    if (santriList.isEmpty()) {
                        Text("Belum ada data santri.", modifier = Modifier.padding(20.dp), color = Color.Gray)
                    } else {
                        MassPrintCheckboxSelector(
                            itemsCount = santriList.size,
                            selectedCount = selectedSantriNisList.size,
                            onSelectAll = {
                                selectedSantriNisList.clear()
                                selectedSantriNisList.addAll(santriList.map { it.nis })
                            },
                            onClearAll = { selectedSantriNisList.clear() },
                            listContent = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    santriList.forEach { s ->
                                        val isChecked = selectedSantriNisList.contains(s.nis)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.White, RoundedCornerShape(10.dp))
                                                .clickable {
                                                    if (isChecked) {
                                                        selectedSantriNisList.remove(s.nis)
                                                    } else {
                                                        selectedSantriNisList.add(s.nis)
                                                    }
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = {
                                                    if (isChecked) {
                                                        selectedSantriNisList.remove(s.nis)
                                                    } else {
                                                        selectedSantriNisList.add(s.nis)
                                                    }
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(s.nama, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("NIS: ${s.nis} | ${kelasMap[s.kelasId] ?: "Murid"}", fontSize = 11.sp, color = Color.Gray)
                                            }
                                        }
                                    }
                                }
                            },
                            onPrintClick = {
                                if (selectedSantriNisList.isEmpty()) {
                                    viewModel.showNotification("Harap pilih minimal 1 santri!")
                                } else {
                                    val subset = santriList.filter { selectedSantriNisList.contains(it.nis) }
                                    val file = PDFExporter.exportIDCardsToPDF(context, subset, kelasMap, appConfig)
                                    if (file != null && file.exists()) {
                                        viewModel.showNotification("Berhasil mengekspor ${subset.size} Kartu Santri Massal!")
                                        triggerShareIDCardFile(context, file, "Cetak_Massal_Santri.pdf")
                                    } else {
                                        viewModel.showNotification("Gagal melahirkan cetakan PDF massal.")
                                    }
                                }
                            }
                        )
                    }
                } else {
                    if (ustadzList.isEmpty()) {
                        Text("Belum ada data asatidzah.", modifier = Modifier.padding(20.dp), color = Color.Gray)
                    } else {
                        MassPrintCheckboxSelector(
                            itemsCount = ustadzList.size,
                            selectedCount = selectedUstadzIdList.size,
                            onSelectAll = {
                                selectedUstadzIdList.clear()
                                selectedUstadzIdList.addAll(ustadzList.map { it.id })
                            },
                            onClearAll = { selectedUstadzIdList.clear() },
                            listContent = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ustadzList.forEach { u ->
                                        val isChecked = selectedUstadzIdList.contains(u.id)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.White, RoundedCornerShape(10.dp))
                                                .clickable {
                                                    if (isChecked) {
                                                        selectedUstadzIdList.remove(u.id)
                                                    } else {
                                                        selectedUstadzIdList.add(u.id)
                                                    }
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = {
                                                    if (isChecked) {
                                                        selectedUstadzIdList.remove(u.id)
                                                    } else {
                                                        selectedUstadzIdList.add(u.id)
                                                    }
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(u.nama, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(u.jabatan, fontSize = 11.sp, color = Color.Gray)
                                            }
                                        }
                                    }
                                }
                            },
                            onPrintClick = {
                                if (selectedUstadzIdList.isEmpty()) {
                                    viewModel.showNotification("Harap pilih minimal 1 asatidzah!")
                                } else {
                                    val subset = ustadzList.filter { selectedUstadzIdList.contains(it.id) }
                                    val file = PDFExporter.exportTeacherCardsToPDF(context, subset, appConfig)
                                    if (file != null && file.exists()) {
                                        viewModel.showNotification("Berhasil mengekspor ${subset.size} Kartu Ustadz/ah Massal!")
                                        triggerShareIDCardFile(context, file, "Cetak_Massal_Ustadz.pdf")
                                    } else {
                                        viewModel.showNotification("Gagal melahirkan cetakan PDF massal.")
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// Mass print list frame structure
@Composable
fun MassPrintCheckboxSelector(
    itemsCount: Int,
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    listContent: @Composable () -> Unit,
    onPrintClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Pilih Anggota ($selectedCount / $itemsCount):",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.Gray
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onSelectAll, contentPadding = PaddingValues(0.dp)) {
                    Text("Pilih Semua", fontSize = 11.sp)
                }
                TextButton(onClick = onClearAll, contentPadding = PaddingValues(0.dp)) {
                    Text("Bersihkan", fontSize = 11.sp, color = Color.Red)
                }
            }
        }

        listContent()

        Spacer(modifier = Modifier.height(10.dp))

        // Large print selected button
        Button(
            onClick = onPrintClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5C3A))
        ) {
            Icon(Icons.Default.Print, contentDescription = "Printer")
            Spacer(modifier = Modifier.width(8.dp))
            Text("CETAK BELAKANG & DEPAN MASSAL ($selectedCount)", fontWeight = FontWeight.Bold)
        }
    }
}

// Toggle section preview helper
@Composable
fun CardStateAndPreviewSection(
    showFrontSide: Boolean,
    onSideToggle: (Boolean) -> Unit,
    frontLayout: @Composable () -> Unit,
    backLayout: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .background(Color(0xFFE9F5EE), shape = RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { onSideToggle(true) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (showFrontSide) Color(0xFF6750A4) else Color.Transparent,
                contentColor = if (showFrontSide) Color.White else Color(0xFF6750A4)
            ),
            shape = RoundedCornerShape(10.dp),
            elevation = null,
            modifier = Modifier.width(110.dp)
        ) {
            Text("Sisi Depan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = { onSideToggle(false) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (!showFrontSide) Color(0xFF6750A4) else Color.Transparent,
                contentColor = if (!showFrontSide) Color.White else Color(0xFF6750A4)
            ),
            shape = RoundedCornerShape(10.dp),
            elevation = null,
            modifier = Modifier.width(110.dp)
        ) {
            Text("Sisi Belakang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }

    AnimatedContent(
        targetState = showFrontSide,
        label = "card_render"
    ) { isFront ->
        if (isFront) {
            frontLayout()
        } else {
            backLayout()
        }
    }
}

// Subordinate Compose layout for front visual ID card
@Composable
fun CardDepanLayout(
    santri: Santri,
    kelasName: String,
    appConfig: AppConfig
) {
    val greenGradient = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(
            Color(0xFF0B8043),
            Color(0xFF04542A)
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .aspectRatio(1.58f)
            .testTag("idcard_depan_card"),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(greenGradient)
        ) {
            AsyncImage(
                model = if (appConfig.logoTpq != null && File(appConfig.logoTpq).exists()) appConfig.logoTpq else com.elshadiqan.tpqaba.R.drawable.logo_tpq,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(0.65f)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(24.dp)),
                alpha = 0.18f,
                contentScale = ContentScale.Fit
            )

            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Band
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (appConfig.logoTpq != null && File(appConfig.logoTpq).exists()) {
                        AsyncImage(
                            model = appConfig.logoTpq,
                            contentDescription = "Logo School",
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    } else {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.elshadiqan.tpqaba.R.drawable.logo_tpq),
                            contentDescription = "Logo School",
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = appConfig.namaTpq,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = appConfig.alamat,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "e-Pesantren",
                            tint = Color(0xFFA3E635),
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "ePesantren",
                            color = Color.White,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Main area: Content Row (KARTU SANTRI title + circular portrait)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "KARTU SANTRI",
                            color = Color(0xFFA3E635),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val spacedNis = santri.nis.map { "$it" }.joinToString(" ")
                        Text(
                            text = spacedNis,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .size(74.dp)
                            .border(2.5.dp, Color.White, CircleShape),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f)
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
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "No photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }

                // Bottom area: QR on Left, Student Details (Right aligned as in sample) on Right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // QR Code
                    val qrData = "https://tpqaba.id/santri/${santri.nis}"
                    val qrBitmap = remember(qrData) { QRCodeGenerator.generateQRCode(qrData, 120) }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.White, RoundedCornerShape(6.dp))
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "QR Validation",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = santri.nama.uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "NIS ${santri.nis}",
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp
                        )
                        Text(
                            text = "${santri.alamat}, Ds. ${santri.desa}, ${santri.kabupaten}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Normal,
                            fontSize = 7.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Right
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color(0xFFA3E635))
                    .align(Alignment.BottomStart)
            )
        }
    }
}

// Subordinate Compose layout for back visual ID card
@Composable
fun CardBelakangLayout(
    santri: Santri,
    appConfig: AppConfig
) {
    val greenGradient = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(
            Color(0xFF0B8043),
            Color(0xFF04542A)
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .aspectRatio(1.58f)
            .testTag("idcard_belakang_card"),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(greenGradient)
        ) {
            AsyncImage(
                model = if (appConfig.logoTpq != null && File(appConfig.logoTpq).exists()) appConfig.logoTpq else com.elshadiqan.tpqaba.R.drawable.logo_tpq,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(0.65f)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(24.dp)),
                alpha = 0.18f,
                contentScale = ContentScale.Fit
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (appConfig.logoTpq != null && File(appConfig.logoTpq).exists()) {
                        AsyncImage(
                            model = appConfig.logoTpq,
                            contentDescription = "Logo School",
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    } else {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.elshadiqan.tpqaba.R.drawable.logo_tpq),
                            contentDescription = "Logo School",
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TATA TERTIB & KETENTUAN KARTU SANTRI",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "1. Harap selalu membawa Kartu Santri saat KBM.",
                            "2. Kartu ini digunakan untuk scan validasi & presensi.",
                            "3. Dilarang meminjamkan / merusak fisik kartu.",
                            "4. Jika terjadi kehilangan, segera lapor Tata Usaha TPQ."
                        ).forEach { rule ->
                            Text(
                                text = rule,
                                fontSize = 8.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Normal,
                                lineHeight = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Divider(color = Color.White.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(2.dp))

                        Column {
                            Text(
                                text = "Orang Tua / Wali Santri:",
                                fontSize = 7.5.sp,
                                color = Color(0xFFA3E635),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${santri.namaAyah} / ${santri.hpOrtu}",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier
                            .weight(0.7f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val qrData = "https://tpqaba.id/santri/${santri.nis}"
                        val qrBitmap = remember(qrData) { QRCodeGenerator.generateQRCode(qrData, 120) }

                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .background(Color.White, RoundedCornerShape(6.dp))
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "QR Validation",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "SCAN VALIDASI",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA3E635),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}


// --- TEACHER PORTRAIT ID CARD PREVIEW COMPOSABLES ---

@Composable
fun CardUstadzFrontLayout(ustadz: Ustadz, appConfig: AppConfig) {
    val emeraldGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = listOf(
            Color(0xFF05351E),
            Color(0xFF0E5C3A)
        )
    )

    Card(
        modifier = Modifier
            .width(280.dp)
            .height(440.dp)
            .testTag("idcard_ustadz_front"),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.5.dp, Color(0xFFD4AF37)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(emeraldGradient)
        ) {
            AsyncImage(
                model = if (appConfig.logoTpq != null && File(appConfig.logoTpq).exists()) appConfig.logoTpq else com.elshadiqan.tpqaba.R.drawable.logo_tpq,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(0.65f)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(24.dp)),
                alpha = 0.18f,
                contentScale = ContentScale.Fit
            )

            // Upper wave header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(Color(0xFF0E5C3A))
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(14.dp))
                
                // Head Title
                Text(
                    text = appConfig.namaTpq,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "KARTU ANGGOTA STAFF / GURU",
                    color = Color(0xFFD4AF37),
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Profile Image / Monogram Spot with Gold trim
                Surface(
                    modifier = Modifier
                        .size(100.dp)
                        .border(3.dp, Color(0xFFD4AF37), CircleShape),
                    shape = CircleShape,
                    color = Color(0xFFF3EDF7)
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
                            Text(
                                text = ustadz.nama.take(1).uppercase(),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF05351E)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Teacher details name & class
                Text(
                    text = ustadz.nama.uppercase(),
                    color = Color(0xFFD4AF37),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = ustadz.jabatan.uppercase(),
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0xFFD4AF37), thickness = 1.dp, modifier = Modifier.fillMaxWidth(0.7f))
                Spacer(modifier = Modifier.height(12.dp))

                // Details Grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row {
                        Text("ID STAFF :  ", color = Color(0xFFD4AF37), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("ABA-UT-${ustadz.id.toString().padStart(3, '0')}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row {
                        Text("JABATAN  :  ", color = Color(0xFFD4AF37), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(ustadz.jabatan, color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row {
                        Text("KONTAK   :  ", color = Color(0xFFD4AF37), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(ustadz.hp, color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row {
                        Text("ALAMAT   :  ", color = Color(0xFFD4AF37), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(
                            text = if (ustadz.alamat.length > 18) ustadz.alamat.take(16) + "..." else ustadz.alamat,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Footer banner with solid accent shape
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(Color(0xFF0E5C3A))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // QR Spot
                        val qrData = "https://tpqaba.id/ustadz/${ustadz.id}"
                        val qrBitmap = remember(qrData) { QRCodeGenerator.generateQRCode(qrData, 120) }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White, RoundedCornerShape(4.dp))
                                .padding(2.dp)
                        ) {
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "QR validation ustadz",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                "Mencetak Generasi Qur'ani",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                            Text(
                                "DAN BERAKHLAK MULIA",
                                color = Color(0xFFD4AF37),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 8.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    // Gold divider bar on top of footer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Color(0xFFD4AF37))
                            .align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

@Composable
fun CardUstadzBackLayout(ustadz: Ustadz, appConfig: AppConfig) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(440.dp)
            .testTag("idcard_ustadz_back"),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.5.dp, Color(0xFFD4AF37)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF05351E))
        ) {
            AsyncImage(
                model = if (appConfig.logoTpq != null && File(appConfig.logoTpq).exists()) appConfig.logoTpq else com.elshadiqan.tpqaba.R.drawable.logo_tpq,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(0.65f)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(24.dp)),
                alpha = 0.18f,
                contentScale = ContentScale.Fit
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header bands
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0E5C3A))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "TATA TERTIB & KETENTUAN",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color(0xFFD4AF37))
                )

                // Guidelines body text
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "1. Kartu ini milik asatidzah pengajar resmi " + appConfig.namaTpq + ".",
                        "2. Harap selalu mengenakan kartu identitas resmi selama jam pendaftaran & KBM berlangsung.",
                        "3. Scan QR Code di sisi depan kartu untuk memvalidasi presensi mengajar harian asatidzah.",
                        "4. Penyalahgunaan kartu identitas resmi ini merupakan pelanggaran berat tata tertib TPQ.",
                        "5. Jika terjadi kehilangan, silahkan lapor ke bagian Tata Usaha / Pimpinan TPQ."
                    ).forEach { r ->
                        Text(
                            text = r,
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 8.5.sp,
                            lineHeight = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Sign-off
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 24.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("Surakarta, Juni 2026", color = Color(0xFFD4AF37), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text("Kepala " + appConfig.namaTpq + ",", color = Color(0xFFD4AF37), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(appConfig.kepalaTpq, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


// Share file trigger helper
private fun triggerShareIDCardFile(context: Context, file: File, fileName: String) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, fileName)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Bagikan Kartu PDF"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
