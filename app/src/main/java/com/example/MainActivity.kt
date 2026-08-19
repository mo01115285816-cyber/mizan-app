package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.common.PermissionHelper
import com.example.core.designsystem.MizanTheme
import com.example.core.model.AppState
import com.example.data.remote.SupabaseAuthRepository
import com.example.feature.blocked.BlockedRoute
import com.example.feature.blocked.BlockedUiState
import com.example.feature.home.HomeScreen
import com.example.feature.setup.SetupScreen
import com.example.ui.viewmodel.MizanViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MizanViewModel by viewModels {
        MizanViewModel.Factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            MizanTheme(forceRtl = true) {
                val appState by viewModel.appState.collectAsStateWithLifecycle()
                val homeUiState by viewModel.homeUiState.collectAsStateWithLifecycle()
                val coroutineScope = rememberCoroutineScope()

                // 1. Notification Runtime Permission Launcher (Android 13+)
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { _ -> viewModel.checkPermissions() }
                )

                // 2. Wi-Fi / Location Runtime Permission Launcher
                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { _ -> viewModel.refreshUsage() }
                )

                // 3. VPN Consent Activity Result Launcher
                val vpnConsentLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult(),
                    onResult = { result ->
                        val granted = result.resultCode == Activity.RESULT_OK
                        viewModel.setVpnConsentGranted(granted)
                    }
                )

                // 4. Device Admin Activity Result Launcher
                val deviceAdminLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult(),
                    onResult = { result ->
                        val enabled = result.resultCode == Activity.RESULT_OK
                        viewModel.setDeviceAdminEnabled(enabled)
                    }
                )

                // Initial launch checks
                LaunchedEffect(Unit) {
                    if (!PermissionHelper.hasLocationPermission(this@MainActivity)) {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !PermissionHelper.hasNotificationPermission(this@MainActivity)
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                fun triggerVpnPreparation() {
                    val vpnIntent = VpnService.prepare(this@MainActivity)
                    if (vpnIntent != null) {
                        vpnConsentLauncher.launch(vpnIntent)
                    } else {
                        viewModel.setVpnConsentGranted(true)
                    }
                }

                fun triggerDeviceAdmin() {
                    try {
                        val adminIntent = PermissionHelper.createDeviceAdminIntent(this@MainActivity)
                        deviceAdminLauncher.launch(adminIntent)
                    } catch (_: Exception) {
                        PermissionHelper.openDeviceAdminSettingsDirectly(this@MainActivity)
                    }
                }

                fun triggerGoogleSignIn() {
                    coroutineScope.launch {
                        val credentialManager = CredentialManager.create(this@MainActivity)
                        val serverClientId = try {
                            val field = BuildConfig::class.java.getField("GOOGLE_WEB_CLIENT_ID")
                            val id = field.get(null) as? String ?: ""
                            if (id.isNotBlank() && !id.contains("placeholder")) id else DEFAULT_WEB_CLIENT_ID
                        } catch (_: Exception) {
                            DEFAULT_WEB_CLIENT_ID
                        }

                        // Generate cryptographic nonce pair (raw for Supabase, SHA256 hashed for Google)
                        val (rawNonce, hashedNonce) = SupabaseAuthRepository.generateCryptoNonce()

                        val googleIdOption = GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(serverClientId)
                            .setAutoSelectEnabled(false)
                            .setNonce(hashedNonce)
                            .build()

                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()

                        try {
                            val result = credentialManager.getCredential(
                                request = request,
                                context = this@MainActivity
                            )
                            val credential = result.credential
                            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                val idToken = googleIdTokenCredential.idToken
                                val displayName = googleIdTokenCredential.displayName
                                val photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                                viewModel.signInWithGoogle(
                                    idToken = idToken,
                                    rawNonce = rawNonce,
                                    displayName = displayName,
                                    photoUrl = photoUrl
                                )
                            } else {
                                viewModel.onSignInError("نوع بيانات الاعتماد المستلمة غير مدعوم")
                            }
                        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                            Log.i("MainActivity", "Google sign-in was cancelled by the user")
                            viewModel.onSignInCancelled()
                        } catch (e: GetCredentialException) {
                            Log.e("MainActivity", "Google sign in credential error: ${e.message}", e)
                            val errorMsg = when {
                                e.message?.contains("USER_CANCELED", ignoreCase = true) == true -> {
                                    viewModel.onSignInCancelled()
                                    return@launch
                                }
                                e.message?.contains("NETWORK_ERROR", ignoreCase = true) == true -> {
                                    "تعذر الاتصال بخدمات Google، يرجى التحقق من اتصال الإنترنت"
                                }
                                e.message?.contains("No credentials available", ignoreCase = true) == true ||
                                e.message?.contains("TYPE_NO_CREDENTIAL", ignoreCase = true) == true -> {
                                    "لم يتم العثور على حساب Google مسجل على هذا الجهاز، يرجى إضافة حساب Google في إعدادات الهاتف والمحاولة مجدداً"
                                }
                                else -> {
                                    "تعذر تسجيل الدخول بحساب Google (${e.message ?: "خطأ غير معروف"})"
                                }
                            }
                            viewModel.onSignInError(errorMsg)
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Google sign in error: ${e.message}", e)
                            viewModel.onSignInError("حدث خطأ أثناء الاتصال بخدمات تسجيل الدخول")
                        }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (val current = appState) {
                        is AppState.Ready -> {
                            HomeScreen(
                                state = homeUiState,
                                onTabSelected = viewModel::onTabSelected,
                                onProfileClick = { viewModel.onShowProfileSheet(true) },
                                onDismissProfileSheet = { viewModel.onShowProfileSheet(false) },
                                onSignOutClick = { viewModel.unlinkDevice() },
                                onWifiStatusClick = { viewModel.onShowNetworkDetails(true) },
                                onConnectionPillClick = { viewModel.onShowNetworkDetails(true) },
                                onDismissNetworkSheet = { viewModel.onShowNetworkDetails(false) },
                                onRefreshNetworkDetails = { viewModel.refreshUsage() },
                                onOpenPermissionsHub = { viewModel.onShowPermissionsHub(true) },
                                onDismissPermissionsHub = { viewModel.onShowPermissionsHub(false) },
                                onUpdateQuotaLimit = { viewModel.updateQuotaLimit(it) },
                                onToggleVpnConsent = { viewModel.setVpnConsentGranted(it) },
                                onToggleDeviceAdmin = { viewModel.setDeviceAdminEnabled(it) },
                                onRequestUsageAccess = { viewModel.openUsageAccessSettings(this@MainActivity) },
                                onRequestLocationOrWifi = {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                onRequestOverlay = { viewModel.openOverlaySettings(this@MainActivity) },
                                onRequestNotification = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                onRequestVpn = { triggerVpnPreparation() },
                                onRequestDeviceAdmin = { triggerDeviceAdmin() },
                                onRequestBatteryOptimization = { viewModel.openBatteryOptimizationSettings(this@MainActivity) },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        is AppState.QuotaExhausted -> {
                            BlockedRoute(
                                state = BlockedUiState(subtitle = current.reason),
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        else -> {
                            SetupScreen(
                                appState = current,
                                onGoogleSignInClick = { triggerGoogleSignIn() },
                                onManualInviteSubmit = { token -> viewModel.handleDeepLinkInvite(token) },
                                onRetryClick = { viewModel.restoreSessionAndProfile() },
                                onSignOutClick = { viewModel.unlinkDevice() },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null) {
            val token = when {
                data.scheme == "mizan" && data.host == "join" -> {
                    data.lastPathSegment ?: data.path?.trim('/')
                }
                data.scheme == "https" && data.host == "mizan.app" && data.path?.startsWith("/join") == true -> {
                    data.lastPathSegment
                }
                else -> null
            }
            if (!token.isNullOrBlank()) {
                viewModel.handleDeepLinkInvite(token)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshUsage()
        viewModel.checkPermissions()
    }

    companion object {
        const val DEFAULT_WEB_CLIENT_ID = "750758394013-79opo08avndvmasurc5mrljc171jsjta.apps.googleusercontent.com"
    }
}
