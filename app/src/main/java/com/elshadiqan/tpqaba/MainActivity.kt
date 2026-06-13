package com.elshadiqan.tpqaba

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import coil.compose.AsyncImage
import com.elshadiqan.tpqaba.data.model.User
import com.elshadiqan.tpqaba.data.model.AppConfig
import java.io.File
import com.elshadiqan.tpqaba.data.database.AppDatabase
import com.elshadiqan.tpqaba.data.repository.TPQRepository
import com.elshadiqan.tpqaba.ui.screens.*
import com.elshadiqan.tpqaba.ui.theme.*
import com.elshadiqan.tpqaba.ui.viewmodel.TPQViewModel
import com.elshadiqan.tpqaba.ui.viewmodel.TPQViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize Room Database & Seeding Lifecycles
        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = TPQRepository(
            database.userDao(),
            database.santriDao(),
            database.kelasDao(),
            database.ustadzDao(),
            database.configDao(),
            database.absensiDao()
        )
        val factory = TPQViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[TPQViewModel::class.java]

        setContent {
            MyApplicationTheme {
                val currentUser by viewModel.currentUser.collectAsState()
                val notification by viewModel.notification.collectAsState()
                val appConfig by viewModel.appConfig.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                var isPublicPortalActive by remember { mutableStateOf(false) }
                var isConfigScreenActive by remember { mutableStateOf(false) }

                // Register reactive snackbars for any database modifications
                LaunchedEffect(notification) {
                    notification?.let { msg ->
                        scope.launch {
                            snackbarHostState.showSnackbar(msg)
                            viewModel.clearNotification()
                        }
                    }
                }

                if (isPublicPortalActive) {
                    PublicPortalScreen(
                        viewModel = viewModel,
                        onBackToAdmin = { isPublicPortalActive = false }
                    )
                } else if (isConfigScreenActive) {
                    ConfigScreen(
                        viewModel = viewModel,
                        onBackClick = { isConfigScreenActive = false }
                    )
                } else if (currentUser == null) {
                    // Show Auth portal
                    LoginScreen(
                        viewModel = viewModel,
                        onLoginSuccess = { /* ViewModel keeps session state */ },
                        onEnterPublicPortal = { isPublicPortalActive = true }
                    )
                } else {
                    // Show Main application with responsive sidebar layout
                    var selectedTab by remember { mutableStateOf(0) } // 0: Dash, 1: Santri, 2: Kelas, 3: Ustadz, 4: QR, 5: Card
                    var selectedNisForIDCard by remember { mutableStateOf<String?>(null) }
                    var selectedUstadzIdForIDCard by remember { mutableStateOf<Int?>(null) }

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isWideScreen = maxWidth >= 600.dp

                        Row(modifier = Modifier.fillMaxSize()) {
                            if (isWideScreen) {
                                SidebarNavigation(
                                    selectedTab = if (selectedNisForIDCard != null || selectedUstadzIdForIDCard != null) 5 else selectedTab,
                                    onTabSelected = { tab ->
                                        selectedNisForIDCard = null
                                        selectedUstadzIdForIDCard = null
                                        selectedTab = tab
                                    },
                                    appConfig = appConfig,
                                    currentUser = currentUser,
                                    onConfigClick = { isConfigScreenActive = true },
                                    onLogoutClick = { viewModel.logout() }
                                )
                            }

                            Scaffold(
                                modifier = Modifier.weight(1f),
                                topBar = {
                                    if (!isWideScreen) {
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
                                                        text = appConfig.subHeader,
                                                        fontSize = 11.sp,
                                                        color = Color.White.copy(alpha = 0.8f)
                                                    )
                                                }
                                            },
                                            actions = {
                                                // Settings shortcut for Admin
                                                if (currentUser?.role == "Admin") {
                                                    IconButton(
                                                        onClick = { isConfigScreenActive = true },
                                                        modifier = Modifier.testTag("admin_settings_button")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Settings,
                                                            contentDescription = "Pengaturan",
                                                            tint = Color.White
                                                        )
                                                    }
                                                }
                                                // Session info + Logout
                                                Text(
                                                    text = "[ ${currentUser?.role} ]",
                                                    color = HighDensityAccentPurple, // Purple light accent instead of gold
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                                IconButton(onClick = { viewModel.logout() }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Logout,
                                                        contentDescription = "Keluar Log",
                                                        tint = Color.White
                                                    )
                                                }
                                            },
                                            colors = TopAppBarDefaults.topAppBarColors(
                                                containerColor = HighDensityPrimary // High Density Purple Top App Bar
                                             )
                                        )
                                    }
                                },
                                bottomBar = {
                                    if (!isWideScreen) {
                                        NavigationBar(
                                            containerColor = HighDensitySecondaryContainer, // High Density Bottom Bar Background
                                            modifier = Modifier.testTag("main_navigation_bar")
                                        ) {
                                            NavigationBarItem(
                                                selected = selectedTab == 0 && selectedNisForIDCard == null && selectedUstadzIdForIDCard == null,
                                                onClick = {
                                                    selectedTab = 0
                                                    selectedNisForIDCard = null
                                                    selectedUstadzIdForIDCard = null
                                                },
                                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                                label = { Text("Dash", fontSize = 11.sp) }
                                            )
                                            NavigationBarItem(
                                                selected = (selectedTab == 1 || selectedNisForIDCard != null) && selectedTab != 4 && selectedTab != 2 && selectedTab != 3 && selectedTab != 5 && selectedUstadzIdForIDCard == null,
                                                onClick = {
                                                    selectedTab = 1
                                                    selectedNisForIDCard = null
                                                    selectedUstadzIdForIDCard = null
                                                },
                                                icon = { Icon(Icons.Default.People, contentDescription = "Santri") },
                                                label = { Text("Santri", fontSize = 11.sp) }
                                            )
                                            NavigationBarItem(
                                                selected = selectedTab == 2 && selectedNisForIDCard == null && selectedUstadzIdForIDCard == null,
                                                onClick = {
                                                    selectedTab = 2
                                                    selectedNisForIDCard = null
                                                    selectedUstadzIdForIDCard = null
                                                },
                                                icon = { Icon(Icons.Default.School, contentDescription = "Kelas") },
                                                label = { Text("Kelas", fontSize = 11.sp) }
                                            )
                                            NavigationBarItem(
                                                selected = (selectedTab == 3 || selectedUstadzIdForIDCard != null) && selectedTab != 4 && selectedTab != 2 && selectedTab != 1 && selectedTab != 5 && selectedNisForIDCard == null,
                                                onClick = {
                                                    selectedTab = 3
                                                    selectedNisForIDCard = null
                                                    selectedUstadzIdForIDCard = null
                                                },
                                                icon = { Icon(Icons.Default.SupervisorAccount, contentDescription = "Ustadz/ah") },
                                                label = { Text("Guru", fontSize = 11.sp) }
                                            )
                                            NavigationBarItem(
                                                selected = (selectedTab == 5 || selectedNisForIDCard != null || selectedUstadzIdForIDCard != null) && selectedTab != 0 && selectedTab != 1 && selectedTab != 2 && selectedTab != 3 && selectedTab != 4,
                                                onClick = {
                                                    selectedTab = 5
                                                    selectedNisForIDCard = null
                                                    selectedUstadzIdForIDCard = null
                                                },
                                                icon = { Icon(Icons.Default.ContactPage, contentDescription = "Cetak") },
                                                label = { Text("Kartu", fontSize = 11.sp) }
                                            )
                                            NavigationBarItem(
                                                selected = selectedTab == 4 && selectedNisForIDCard == null && selectedUstadzIdForIDCard == null,
                                                onClick = {
                                                    selectedTab = 4
                                                    selectedNisForIDCard = null
                                                    selectedUstadzIdForIDCard = null
                                                },
                                                icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "QR Absensi") },
                                                label = { Text("Scan", fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                },
                                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    if (selectedNisForIDCard != null || selectedUstadzIdForIDCard != null) {
                                        // Sub navigation view representing printable IDCard Mode
                                        IDCardScreen(
                                            viewModel = viewModel,
                                            initialNis = selectedNisForIDCard,
                                            initialUstadzId = selectedUstadzIdForIDCard,
                                            onBackClick = {
                                                selectedNisForIDCard = null
                                                selectedUstadzIdForIDCard = null
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        when (selectedTab) {
                                            0 -> DashboardScreen(
                                                viewModel = viewModel,
                                                onNavigateToSantri = { selectedTab = 1 },
                                                onNavigateToKelas = { selectedTab = 2 },
                                                onNavigateToUstadz = { selectedTab = 3 },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            1 -> SantriScreen(
                                                viewModel = viewModel,
                                                onNavigateToIDCard = { nis ->
                                                    selectedNisForIDCard = nis
                                                },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            2 -> KelasScreen(
                                                viewModel = viewModel,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            3 -> UstadzScreen(
                                                viewModel = viewModel,
                                                onNavigateToIDCard = { ustadzId ->
                                                    selectedUstadzIdForIDCard = ustadzId
                                                },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            4 -> QRScannerScreen(
                                                viewModel = viewModel,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            5 -> IDCardScreen(
                                                viewModel = viewModel,
                                                initialNis = null,
                                                initialUstadzId = null,
                                                onBackClick = { selectedTab = 0 },
                                                modifier = Modifier.fillMaxSize()
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

@Composable
fun SidebarNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    appConfig: AppConfig,
    currentUser: User?,
    onConfigClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    // High premium, responsive sidebar layout
    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(HighDensitySecondaryContainer)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawLine(
                    color = HighDensityBorder,
                    start = androidx.compose.ui.geometry.Offset(size.width - strokeWidth / 2, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width - strokeWidth / 2, size.height),
                    strokeWidth = strokeWidth
                )
            }
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            // TPQ Main Branding header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                if (appConfig.logoTpq != null && File(appConfig.logoTpq).exists()) {
                    AsyncImage(
                        model = appConfig.logoTpq,
                        contentDescription = "Logo TPQ",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.elshadiqan.tpqaba.R.drawable.logo_tpq),
                        contentDescription = "School Emblem",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appConfig.namaTpq,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextDark,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = appConfig.subHeader,
                        fontSize = 10.sp,
                        color = HighDensityTextMedium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Navigation Items
            val menuItems = listOf(
                SidebarItem(0, "Beranda & Statistik", Icons.Default.Dashboard, "PENGATUR"),
                SidebarItem(1, "Data Santri", Icons.Default.People, "DATA UTAMA"),
                SidebarItem(2, "Data Kelas", Icons.Default.School, "DATA UTAMA"),
                SidebarItem(3, "Data Pengajar", Icons.Default.SupervisorAccount, "DATA UTAMA"),
                SidebarItem(5, "Cetak Kartu", Icons.Default.ContactPage, "LAYANAN DIGITAL"),
                SidebarItem(4, "Scan QR Absensi", Icons.Default.QrCodeScanner, "LAYANAN DIGITAL")
            )

            var currentHeader = ""
            menuItems.forEach { item ->
                if (item.category != currentHeader) {
                    currentHeader = item.category
                    Text(
                        text = currentHeader,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextMedium.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 10.dp, top = 12.dp, bottom = 4.dp)
                    )
                }

                val isSelected = selectedTab == item.id
                Surface(
                    onClick = { onTabSelected(item.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("sidebar_item_${item.id}"),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) HighDensityPrimary else Color.Transparent,
                    contentColor = if (isSelected) Color.White else HighDensityTextDark
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.size(20.dp),
                            tint = if (isSelected) Color.White else HighDensityPrimary
                        )
                        Text(
                            text = item.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Bottom Column: Profile details and administrative settings shortcuts
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 12.dp)
        ) {
            HorizontalDivider(color = HighDensityBorder.copy(alpha = 0.5f))

            // Inline user profile card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(HighDensityPrimaryContainer, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (currentUser?.username?.firstOrNull() ?: 'U').uppercaseChar().toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityOnPrimaryContainer
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentUser?.username?.replaceFirstChar { it.uppercase() } ?: "User",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextDark,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Akses: ${currentUser?.role ?: "Staff"}",
                        fontSize = 10.sp,
                        color = HighDensityPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (currentUser?.role == "Admin") {
                    OutlinedButton(
                        onClick = onConfigClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .testTag("admin_settings_button_wide"),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, HighDensityPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HighDensityPrimary),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Pengaturan",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Config", fontSize = 10.sp)
                    }
                }

                Button(
                    onClick = onLogoutClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("logout_button_wide"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Logout",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Keluar", fontSize = 10.sp, color = Color.White)
                }
            }
        }
    }
}

data class SidebarItem(
    val id: Int,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val category: String
)
