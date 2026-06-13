package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.screens.CameraDashboardScreen
import com.example.ui.screens.MediaLibraryScreen
import com.example.viewmodel.CameraViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : ComponentActivity() {
    private val viewModel: CameraViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()

                // High fidelity Accompanist Multi-permission request for camera and audio
                val permissionState = rememberMultiplePermissionsState(
                    permissions = listOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                    )
                )

                // Trigger request when app launches
                LaunchedEffect(Unit) {
                    permissionState.launchMultiplePermissionRequest()
                }

                LaunchedEffect(permissionState.allPermissionsGranted) {
                    viewModel.setSimulationMode(!permissionState.allPermissionsGranted)
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "camera"
                    ) {
                        composable("camera") {
                            Box(modifier = Modifier.fillMaxSize()) {
                                CameraDashboardScreen(
                                    viewModel = viewModel,
                                    hasPermissions = permissionState.allPermissionsGranted,
                                    onRequestPermissions = {
                                        permissionState.launchMultiplePermissionRequest()
                                    },
                                    onNavigateToLibrary = {
                                        navController.navigate("library")
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Simple subtle banner at the top if permissions are missing
                                if (!permissionState.allPermissionsGranted) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 140.dp)
                                            .padding(horizontal = 16.dp)
                                    ) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1111)),
                                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.4f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    permissionState.launchMultiplePermissionRequest()
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = "Permission warning",
                                                    tint = Color.Red
                                                )
                                                Text(
                                                    text = "Camera & Audio permissions disabled. Click here to grant permissions and use physical camera hardware.",
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    fontSize = 8.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    lineHeight = 11.sp,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        composable("library") {
                            MediaLibraryScreen(
                                viewModel = viewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
