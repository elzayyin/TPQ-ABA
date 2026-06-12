package com.elshadiqan.tpqaba.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elshadiqan.tpqaba.ui.viewmodel.TPQViewModel
import com.elshadiqan.tpqaba.ui.theme.*
import com.elshadiqan.tpqaba.data.model.AppConfig
import coil.compose.AsyncImage
import java.io.File

@Composable
fun DashboardScreen(
    viewModel: TPQViewModel,
    onNavigateToSantri: () -> Unit,
    onNavigateToKelas: () -> Unit,
    onNavigateToUstadz: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val totalSantri by viewModel.santriCount.collectAsState()
    val activeSantri by viewModel.activeSantriCount.collectAsState()
    val graduatedSantri by viewModel.graduatedSantriCount.collectAsState()
    val totalKelas by viewModel.kelasCount.collectAsState()
    val totalUstadz by viewModel.ustadzCount.collectAsState()

    val santriList by viewModel.filteredSantri.collectAsState()
    val kelasList by viewModel.allKelas.collectAsState()
    
    val isSyncing by viewModel.isSyncing.collectAsState()
    val appConfig by viewModel.appConfig.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(HighDensityBackground) // M3 Lavender Gray Background
            .testTag("dashboard_root"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Header Area (High Density Welcome Card)
        item {
            WelcomeBanner(
                username = currentUser?.username ?: "Guest",
                role = currentUser?.role ?: "Operator",
                appConfig = appConfig,
                onBackupClick = { viewModel.simulateBackup() }
            )
        }

        // Numeric Statistics Cards row/column
        item {
            Text(
                text = "Ringkasan Statistik",
                color = HighDensityTextDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            // Grid of items: 2 columns, exactly matching HTML's outlined stats cards
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Total Santri",
                        value = totalSantri.toString(),
                        subtitle = "+4 dari bulan lalu",
                        subtitleColor = Color(0xFF16A34A), // green
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Santri Aktif",
                        value = activeSantri.toString(),
                        subtitle = "85% tingkat kehadiran",
                        useProgress = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Jumlah Kelas",
                        value = totalKelas.toString().padStart(2, '0'),
                        subtitle = "Terbagi Jenjang TPQ",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Asatidzah",
                        value = totalUstadz.toString().padStart(2, '0'),
                        subtitle = "Status: Standby",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Dynamic Graphical Charts (Distribusi Jenjang as in theme)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, HighDensityBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Distribusi Jenjang",
                            color = HighDensityTextDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Detail",
                            color = HighDensityPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Grouping logic for jenjang
                    val levelsCounts = remember(santriList, kelasList) {
                        val counts = mutableMapOf("TKQ" to 0, "TPQ" to 0, "TQA" to 0)
                        val idToTingkat = kelasList.associate { it.id to it.tingkat }
                        santriList.forEach { s ->
                            val tingkat = idToTingkat[s.kelasId] ?: "TPQ" // fallback
                            val key = when {
                                tingkat.contains("TKQ", ignoreCase = true) -> "TKQ"
                                tingkat.contains("TQA", ignoreCase = true) -> "TQA"
                                else -> "TPQ"
                            }
                            counts[key] = (counts[key] ?: 0) + 1
                        }
                        counts
                    }

                    JenjangChartBar("TKQ (Iqro' 1-6)", levelsCounts["TKQ"] ?: 0, HighDensityAccentPurple)
                    Spacer(modifier = Modifier.height(14.dp))
                    JenjangChartBar("TPQ (Al-Qur'an 1-3)", levelsCounts["TPQ"] ?: 0, HighDensityPrimary)
                    Spacer(modifier = Modifier.height(14.dp))
                    JenjangChartBar("TQA (Tahfidz Juzz 30)", levelsCounts["TQA"] ?: 0, Color(0xFFE8DEF8))
                }
            }
        }

        // Firebase Realtime Database Synchronization Control Deck
        if (currentUser?.role == "Admin" || currentUser?.role == "Operator") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("firebase_sync_card"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HighDensityBorder),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sinkronisasi Firebase Cloud",
                                    color = HighDensityTextDark,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Sinkronkan database lokal LPQ ke data awan",
                                    color = HighDensityTextMedium,
                                    fontSize = 11.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Firebase Sync Status",
                                tint = HighDensityPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(HighDensityBorder))

                        if (isSyncing) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = HighDensityPrimary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Menghubungkan ke Firebase Database...",
                                    fontSize = 12.sp,
                                    color = HighDensityPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            val displayUrl = appConfig.firebaseDbUrl.trim()
                            val hostName = if (displayUrl.startsWith("http")) {
                                displayUrl.substringAfter("://").substringBefore("/")
                            } else {
                                displayUrl.substringBefore("/")
                            }
                            
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Database URL: ${hostName.ifEmpty { "Belum dikonfigurasi" }}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hostName.isEmpty()) Color.Red else HighDensityTextDark
                                )
                                if (hostName.isEmpty()) {
                                    Text(
                                        text = "*Atur URL Firebase Anda terlebih dahulu di menu Konfigurasi",
                                        fontSize = 10.sp,
                                        color = Color.Red
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.syncPush() },
                                    enabled = displayUrl.isNotEmpty() && !isSyncing,
                                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("sync_push_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Upload,
                                        contentDescription = "Upload",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Upload ke Firebase", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { viewModel.syncPull() },
                                    enabled = displayUrl.isNotEmpty() && !isSyncing,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = HighDensitySecondaryContainer,
                                        contentColor = HighDensityPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("sync_pull_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Download",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Download dari Firebase", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Actions Shortcut Menu (styled to match material bottom card)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToSantri),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, HighDensityBorder),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(HighDensitySecondaryContainer)
                            .border(1.dp, HighDensityBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Input Santri",
                            tint = HighDensityPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Input Santri Baru",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = HighDensityTextDark
                        )
                        Text(
                            text = "Tambah data & generate QR otomatis",
                            fontSize = 12.sp,
                            color = HighDensityTextMedium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(HighDensitySecondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Buka",
                            tint = HighDensityPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeBanner(
    username: String,
    role: String,
    appConfig: AppConfig,
    onBackupClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HighDensityPrimaryContainer), // Light Lavender card styling
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (appConfig.logoTpq != null && File(appConfig.logoTpq).exists()) {
                AsyncImage(
                    model = appConfig.logoTpq,
                    contentDescription = "Logo TPQ",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, HighDensityBorder, RoundedCornerShape(12.dp))
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                    Text(
                        text = "SELAMAT DATANG",
                        color = HighDensityOnPrimaryContainer,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = username.replaceFirstChar { it.uppercase() },
                        color = HighDensityOnPrimaryContainer,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Kelola data santri dengan mudah hari ini • Role: $role",
                        color = HighDensityOnPrimaryContainer.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }

                // Backup button if admin
                if (role == "Admin") {
                    Button(
                        onClick = onBackupClick,
                        colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .testTag("backup_button")
                    ) {
                        Icon(
                            Icons.Default.Backup,
                            contentDescription = "Cloud Backup",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Backup", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    subtitleColor: Color = HighDensityTextMedium,
    useProgress: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, HighDensityBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(100.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title.uppercase(),
                color = HighDensityTextMedium,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = value,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = HighDensityPrimary
            )
            if (useProgress) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(HighDensitySecondaryContainer)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.85f)
                            .background(HighDensityPrimary)
                    )
                }
            } else {
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = subtitleColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun JenjangChartBar(
    label: String,
    count: Int,
    barColor: Color
) {
    val displayWidthFrac = if (count > 0) (count / 15f).coerceIn(0.1f, 1f) else 0.05f
    val animateVal by animateFloatAsState(
        targetValue = displayWidthFrac,
        animationSpec = tween(600)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = HighDensityTextDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$count Santri",
                color = HighDensityTextDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(HighDensitySecondaryContainer)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animateVal)
                    .background(barColor)
            )
        }
    }
}
