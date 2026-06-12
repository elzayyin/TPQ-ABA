package com.elshadiqan.tpqaba.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elshadiqan.tpqaba.data.model.Kelas
import com.elshadiqan.tpqaba.ui.viewmodel.TPQViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KelasScreen(
    viewModel: TPQViewModel,
    modifier: Modifier = Modifier
) {
    val kelasList by viewModel.allKelas.collectAsState()
    val santriList by viewModel.filteredSantri.collectAsState() // reactive list

    var showAddEditDialog by remember { mutableStateOf(false) }
    var kelasToEdit by remember { mutableStateOf<Kelas?>(null) }
    var expandedGroupId by remember { mutableStateOf<Int?>(null) } // To view Daftar Santri in this class

    var showDeleteConfirm by remember { mutableStateOf<Kelas?>(null) }

    Scaffold(
        modifier = modifier.testTag("kelas_root"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    kelasToEdit = null
                    showAddEditDialog = true
                },
                containerColor = Color(0xFF6750A4),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Kelas")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF9FAF6))
        ) {
            // Screen Title
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Daftar Kelas TPQ",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6750A4)
                )
                Text(
                    text = "Klik kelas untuk menampilkan daftar santri terdaftar.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            if (kelasList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Empty", tint = Color.LightGray, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Belum ada data kelas", color = Color.Gray)
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
                    items(kelasList) { kelas ->
                        val isExpanded = expandedGroupId == kelas.id
                        val studentsInClass = remember(santriList, kelas.id) {
                            santriList.filter { it.kelasId == kelas.id }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedGroupId = if (isExpanded) null else kelas.id
                                },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(46.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFE9F5EE)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Class,
                                                contentDescription = "Kelas",
                                                tint = Color(0xFF0D5C3A)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = kelas.namaKelas,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF1F2937)
                                        )
                                        Text(
                                            text = "Tingkat: ${kelas.tingkat} • Wali: ${kelas.waliKelas}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = {
                                            kelasToEdit = kelas
                                            showAddEditDialog = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF339AF0))
                                        }
                                        IconButton(onClick = {
                                            showDeleteConfirm = kelas
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFFA5252))
                                        }
                                    }
                                }

                                // Expandable Daftar Santri roster
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 14.dp, start = 8.dp)
                                    ) {
                                        Divider(color = Color(0xFFECECEC))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Roster Santri Terdaftar (${studentsInClass.size})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF0D5C3A)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        if (studentsInClass.isEmpty()) {
                                            Text(
                                                "Tidak ada santri yang ditugaskan ke kelas ini.",
                                                fontSize = 12.sp,
                                                color = Color.LightGray,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        } else {
                                            studentsInClass.forEachIndexed { sIdx, s ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "${sIdx + 1}. ",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = Color.Gray
                                                        )
                                                        Text(
                                                            text = s.nama,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color.DarkGray,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Text(
                                                        text = s.nis,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.Gray
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
            }
        }
    }

    // Delete confirm Dialog
    if (showDeleteConfirm != null) {
        val k = showDeleteConfirm!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Hapus Kelas?") },
            text = { Text("Apakah Anda yakin ingin menghapus kelas '${k.namaKelas}'? Santri di kelas ini akan kehilangan penugasan kelas.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteKelas(k)
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

    // Add/Edit Dialog Form
    if (showAddEditDialog) {
        var namaKelas by remember { mutableStateOf(kelasToEdit?.namaKelas ?: "") }
        var tingkat by remember { mutableStateOf(kelasToEdit?.tingkat ?: "TPQ") }
        var waliKelas by remember { mutableStateOf(kelasToEdit?.waliKelas ?: "") }

        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = {
                Text(
                    text = if (kelasToEdit == null) "Tambah Data Kelas" else "Edit Data Kelas",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D5C3A)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = namaKelas,
                        onValueChange = { namaKelas = it },
                        label = { Text("Nama Kelas") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Select Tingkat
                    Column {
                        Text("Tingkatan Jenjang", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf("TKQ", "TPQ", "TQA").forEach { item ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = tingkat == item,
                                        onClick = { tingkat = item }
                                    )
                                    Text(item, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = waliKelas,
                        onValueChange = { waliKelas = it },
                        label = { Text("Wali Kelas / Guru Pengajar") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (namaKelas.isNotBlank()) {
                            val newKelas = Kelas(
                                id = kelasToEdit?.id ?: 0,
                                namaKelas = namaKelas,
                                tingkat = tingkat,
                                waliKelas = waliKelas
                            )
                            viewModel.saveKelas(newKelas)
                            showAddEditDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D5C3A))
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
}
