package com.example.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity

/**
 * Floating Overlay Service (Draw Over Other Apps).
 * Pops up a non-blocking, informative HUD alert when monthly quota is exhausted on the home WiFi router.
 * Once dismissed, the user can freely use their phone or switch to mobile data.
 */
class QuotaOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    companion object {
        const val ACTION_SHOW_OVERLAY = "com.example.mizan.action.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.example.mizan.action.HIDE_OVERLAY"

        fun show(context: Context) {
            if (Settings.canDrawOverlays(context)) {
                val intent = Intent(context, QuotaOverlayService::class.java).apply {
                    action = ACTION_SHOW_OVERLAY
                }
                try {
                    context.startService(intent)
                } catch (_: Exception) {}
            }
        }

        fun hide(context: Context) {
            val intent = Intent(context, QuotaOverlayService::class.java).apply {
                action = ACTION_HIDE_OVERLAY
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HIDE_OVERLAY -> {
                removeOverlay()
                stopSelf()
            }
            else -> {
                showOverlayWindow()
            }
        }
        return START_NOT_STICKY
    }

    @SuppressLint("InflateParams")
    private fun showOverlayWindow() {
        if (!Settings.canDrawOverlays(this) || overlayView != null) return

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@QuotaOverlayService)
            setViewTreeSavedStateRegistryOwner(this@QuotaOverlayService)
            setContent {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color(0xFF151515))
                                .border(1.5.dp, Color(0xFFC0EE2B), RoundedCornerShape(28.dp))
                                .padding(24.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Top Icon Badge
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF242720)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.WifiOff,
                                        contentDescription = null,
                                        tint = Color(0xFFC0EE2B),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Title
                                Text(
                                    text = "اكتملت حصتك من شبكة المنزل",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Description
                                Text(
                                    text = "تم حظر الإنترنت على راوتر البيت تلقائياً وفقاً لسياسة ميزان.\nيمكنك استخدام التطبيقات والألعاب غير المتصلة أو التبديل لباقة الشريحة (Mobile Data).",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 13.sp,
                                        color = Color(0xFFB0B4A8),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 19.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(22.dp))

                                // Dismiss Button
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color(0xFFC0EE2B))
                                        .clickable {
                                            removeOverlay()
                                            stopSelf()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "فهمت • متابعة استخدام الهاتف",
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF151515)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Open App Button
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(Color(0xFF262626))
                                        .clickable {
                                            val appIntent = Intent(this@QuotaOverlayService, MainActivity::class.java).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                            }
                                            startActivity(appIntent)
                                            removeOverlay()
                                            stopSelf()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "فتح تفاصيل ميزان",
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = Color(0xFFE2E4DC)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        try {
            windowManager?.addView(composeView, params)
            overlayView = composeView
        } catch (_: Exception) {}
    }

    private fun removeOverlay() {
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (_: Exception) {}
            overlayView = null
        }
    }

    override fun onDestroy() {
        removeOverlay()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
