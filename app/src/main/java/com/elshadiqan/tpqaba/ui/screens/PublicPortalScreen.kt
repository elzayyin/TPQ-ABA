package com.elshadiqan.tpqaba.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elshadiqan.tpqaba.data.model.Santri
import com.elshadiqan.tpqaba.ui.theme.*
import com.elshadiqan.tpqaba.ui.viewmodel.TPQViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicPortalScreen(
    viewModel: TPQViewModel,
    onBackToAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appConfig by viewModel.appConfig.collectAsState()
    val totalSantri by viewModel.santriCount.collectAsState()
    val totalUstadz by viewModel.ustadzCount.collectAsState()
    val kelasList by viewModel.allKelas.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Santri>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Build the class name map
    val classMap = remember(kelasList) {
        kelasList.associate { it.id to it.namaKelas }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = appConfig.namaTpq,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Portal Wali Santri & Publik",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = onBackToAdmin,
                        colors = ButtonDefaults.textButtonColors(contentColor = HighDensityAccentPurple)
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Portal")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Admin Portal", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HighDensityPrimary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(HighDensityBackground)
                .testTag("public_portal_root"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Header
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = HighDensityPrimaryContainer),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "PORTAL INFORMASI & LAYANAN PUBLIK",
                            color = HighDensityOnPrimaryContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = appConfig.namaTpq,
                            color = HighDensityOnPrimaryContainer,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = appConfig.subHeader,
                            color = HighDensityOnPrimaryContainer.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = HighDensityOnPrimaryContainer.copy(alpha = 0.15f)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Alamat",
                                tint = HighDensityPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = appConfig.alamat,
                                color = HighDensityOnPrimaryContainer.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = "Sms WhatsApp",
                                tint = HighDensityPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Hubungi Kami: ${appConfig.telepon}",
                                color = HighDensityOnPrimaryContainer.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Public Quick Stats Counter Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, HighDensityBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("TOTAL SANTRI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HighDensityTextMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(totalSantri.toString(), fontSize = 28.sp, fontWeight = FontWeight.Black, color = HighDensityPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Terdaftar di Database", fontSize = 10.sp, color = HighDensityTextMedium)
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, HighDensityBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("STAF PENGAJAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HighDensityTextMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(totalUstadz.toString(), fontSize = 28.sp, fontWeight = FontWeight.Black, color = HighDensityPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Asatidzah Aktif", fontSize = 10.sp, color = HighDensityTextMedium)
                        }
                    }
                }
            }

            // Head and Semester info
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HighDensityBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Kepala TPQ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HighDensityTextMedium)
                            Text(appConfig.kepalaTpq, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HighDensityTextDark)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Tahun Ajaran", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HighDensityTextMedium)
                            Text(appConfig.tahunAjaran, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HighDensityPrimary)
                        }
                    }
                }
            }

            // Student Search Panel (Wali Santri lookup)
            item {
                Text(
                    text = "Pencarian Informasi Santri",
                    color = HighDensityTextDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )

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
                        if (!appConfig.izinkanPencarianPublik) {
                            // Show search is disabled notification
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Terkunci",
                                        tint = Color(0xFFDC2626)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Fitur Pencarian Dinonaktifkan",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF991B1B)
                                    )
                                    Text(
                                        text = "Untuk melindugi privasi santri, pencarian publik ditutup sementara oleh Administrator.",
                                        textAlign = TextAlign.Center,
                                        fontSize = 12.sp,
                                        color = Color(0xFF7F1D1D)
                                    )
                                }
                            }
                        } else {
                            // Search enabled
                            Text(
                                text = "Masukkan Nama Lengkap atau Nomor Induk Santri (NIS) untuk mengecek status & wali kelas:",
                                fontSize = 13.sp,
                                color = HighDensityTextMedium
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    label = { Text("Nama atau NIS Santri") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("public_search_input"),
                                    shape = RoundedCornerShape(12.dp),
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") }
                                )

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        if (searchQuery.isNotBlank()) {
                                            isSearching = true
                                            scope.launch {
                                                // Call repository search
                                                searchResults = viewModel.searchSantriByName(searchQuery)
                                                // If empty, try NIS match exact
                                                if (searchResults.isEmpty()) {
                                                    val matchNis = viewModel.checkSantriByNis(searchQuery.trim())
                                                    searchResults = if (matchNis != null) listOf(matchNis) else emptyList()
                                                }
                                                hasSearched = true
                                                isSearching = false
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .height(54.dp)
                                        .testTag("public_search_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                                    enabled = searchQuery.isNotBlank() && !isSearching
                                ) {
                                    if (isSearching) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                    } else {
                                        Text("Cari", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Handle Render search result
            if (appConfig.izinkanPencarianPublik && hasSearched) {
                if (searchResults.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, HighDensityBorder),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Tidak ditemukan",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Santri Tidak Ditemukan",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = HighDensityTextDark
                                )
                                Text(
                                    text = "Tidak ada records yang cocok dengan kueri '$searchQuery'. Periksa penulisan huruf atau hubungi Admin.",
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp,
                                    color = HighDensityTextMedium
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            text = "Hasil Pencarian (${searchResults.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityPrimary
                        )
                    }

                    items(searchResults) { santri ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, HighDensityBorder),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().testTag("search_result_item")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(HighDensitySecondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Foto Santri",
                                        tint = HighDensityPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = santri.nama,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = HighDensityTextDark
                                    )
                                    Text(
                                        text = "NIS: ${santri.nis} • ${santri.jenisKelamin}",
                                        fontSize = 12.sp,
                                        color = HighDensityTextMedium
                                    )

                                    val className = classMap[santri.kelasId] ?: "Belum Ditentukan"
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Wali Kelas: $className",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = HighDensityPrimary
                                    )
                                }

                                Badge(
                                    containerColor = when (santri.status) {
                                        "Aktif" -> Color(0xFFDCFCE7)
                                        "Lulus" -> Color(0xFFDBEAFE)
                                        else -> Color(0xFFF3F4F6)
                                    },
                                    contentColor = when (santri.status) {
                                        "Aktif" -> Color(0xFF15803D)
                                        "Lulus" -> Color(0xFF1D4ED8)
                                        else -> Color(0xFF4B5563)
                                    }
                                ) {
                                    Text(
                                        text = santri.status,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
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
