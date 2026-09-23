package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AppDatabase
import com.example.data.repository.InvoiceRepository
import com.example.data.repository.UserRepository
import com.example.ui.AppTab
import com.example.ui.InvoiceViewModel
import com.example.ui.screens.BackendConfigScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ImportScreen
import com.example.ui.screens.InvoicePreviewEditScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ManageUsersDialog
import com.example.ui.screens.PdfViewerScreen
import com.example.ui.theme.InvoiceTrackerTheme
import com.example.ui.theme.Navy900
import com.example.ui.theme.PrimaryBlue

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: InvoiceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = InvoiceRepository(database.invoiceDao())
        val userRepository = UserRepository(database.userDao())
        val factory = InvoiceViewModel.Factory(repository, userRepository)
        viewModel = ViewModelProvider(this, factory)[InvoiceViewModel::class.java]

        setContent {
            val darkModePref by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val effectiveDark = darkModePref ?: systemDark

            InvoiceTrackerTheme(darkTheme = effectiveDark) {
                MainAppScreen(viewModel = viewModel, isDarkTheme = effectiveDark)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: InvoiceViewModel,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val draft by viewModel.currentDraft.collectAsStateWithLifecycle()
    val activePdf by viewModel.activePdf.collectAsStateWithLifecycle()
    val darkModePref by viewModel.isDarkMode.collectAsStateWithLifecycle()
    var showUsersDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            modifier = Modifier.size(38.dp),
                            shadowElevation = 1.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_master_tech_logo),
                                    contentDescription = "Master Tech Logo",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Invoice Tracker",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Sales & Invoicing",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                actions = {
                    // Dark / Light Theme Toggle Button
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("app_bar_theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = when (darkModePref) {
                                true -> Icons.Default.DarkMode
                                false -> Icons.Default.LightMode
                                null -> Icons.Default.Brightness4
                            },
                            contentDescription = "Toggle Dark Mode",
                            tint = if (isDarkTheme) Color(0xFF38BDF8) else PrimaryBlue
                        )
                    }

                    IconButton(
                        onClick = { showUsersDialog = true },
                        modifier = Modifier.testTag("app_bar_manage_users_button")
                    ) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = "Manage Users",
                            tint = if (isDarkTheme) Color(0xFF38BDF8) else PrimaryBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                // 1. Dashboard (Ledger)
                NavigationBarItem(
                    selected = currentTab == AppTab.DASHBOARD,
                    onClick = { viewModel.setTab(AppTab.DASHBOARD) },
                    icon = {
                        Icon(
                            if (currentTab == AppTab.DASHBOARD) Icons.Filled.History else Icons.Outlined.History,
                            contentDescription = "Ledger"
                        )
                    },
                    label = { Text("Ledger") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryBlue,
                        selectedTextColor = PrimaryBlue,
                        indicatorColor = PrimaryBlue.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )

                // 2. New Bill (Create Sales Receipt)
                NavigationBarItem(
                    selected = currentTab == AppTab.IMPORT,
                    onClick = { viewModel.setTab(AppTab.IMPORT) },
                    icon = {
                        Icon(
                            if (currentTab == AppTab.IMPORT) Icons.Filled.AddCircle else Icons.Outlined.AddCircle,
                            contentDescription = "New Bill"
                        )
                    },
                    label = { Text("New Bill") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryBlue,
                        selectedTextColor = PrimaryBlue,
                        indicatorColor = PrimaryBlue.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.testTag("nav_tab_import")
                )

                // 3. Edit / Preview Draft
                NavigationBarItem(
                    selected = currentTab == AppTab.PREVIEW_EDIT,
                    onClick = { viewModel.setTab(AppTab.PREVIEW_EDIT) },
                    icon = {
                        if (draft != null) {
                            BadgedBox(badge = { Badge { Text(draft!!.items.size.toString()) } }) {
                                Icon(
                                    if (currentTab == AppTab.PREVIEW_EDIT) Icons.Filled.Description else Icons.Outlined.Description,
                                    contentDescription = "Edit Draft"
                                )
                            }
                        } else {
                            Icon(
                                if (currentTab == AppTab.PREVIEW_EDIT) Icons.Filled.Description else Icons.Outlined.Description,
                                contentDescription = "Edit Draft"
                            )
                        }
                    },
                    label = { Text("Draft") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryBlue,
                        selectedTextColor = PrimaryBlue,
                        indicatorColor = PrimaryBlue.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.testTag("nav_tab_preview_edit")
                )

                // 4. PDF View & Share
                NavigationBarItem(
                    selected = currentTab == AppTab.PDF_VIEWER,
                    onClick = { viewModel.setTab(AppTab.PDF_VIEWER) },
                    icon = {
                        Icon(
                            if (currentTab == AppTab.PDF_VIEWER) Icons.Filled.PictureAsPdf else Icons.Outlined.PictureAsPdf,
                            contentDescription = "PDF"
                        )
                    },
                    label = { Text("PDF") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryBlue,
                        selectedTextColor = PrimaryBlue,
                        indicatorColor = PrimaryBlue.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.testTag("nav_tab_pdf_viewer")
                )

                // 5. Backend Service
                NavigationBarItem(
                    selected = currentTab == AppTab.BACKEND_INFO,
                    onClick = { viewModel.setTab(AppTab.BACKEND_INFO) },
                    icon = {
                        Icon(
                            if (currentTab == AppTab.BACKEND_INFO) Icons.Filled.CloudSync else Icons.Outlined.CloudSync,
                            contentDescription = "Backend"
                        )
                    },
                    label = { Text("Backend") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryBlue,
                        selectedTextColor = PrimaryBlue,
                        indicatorColor = PrimaryBlue.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.testTag("nav_tab_backend")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentTab) {
                AppTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                AppTab.IMPORT -> ImportScreen(viewModel = viewModel)
                AppTab.PREVIEW_EDIT -> InvoicePreviewEditScreen(viewModel = viewModel)
                AppTab.PDF_VIEWER -> PdfViewerScreen(viewModel = viewModel)
                AppTab.BACKEND_INFO -> BackendConfigScreen(viewModel = viewModel)
            }
        }
    }

    if (showUsersDialog) {
        ManageUsersDialog(
            viewModel = viewModel,
            onDismiss = { showUsersDialog = false }
        )
    }
}
