package com.robocar.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.robocar.app.ble.BleState
import com.robocar.app.ui.*
import com.robocar.app.ui.blocks.BlockViewModel
import com.robocar.app.ui.blocks.BlocksScreen
import com.robocar.app.ui.joystick.JoystickScreen
import com.robocar.app.ui.joystick.FourMotorScreen
import com.robocar.app.scratch.ScratchScreen
import com.robocar.app.ui.settings.TuningDialog

// Кольори 1в1 з оригіналу
private val BgPage       = Color(0xFF0f172a) // body background
private val GlassBg      = Color(0x6A0e1628) // rgba(14,22,40,.42) -- glass
private val GlassStroke  = Color(0x240FFFFFF) // rgba(255,255,255,.14)
private val NavBtnBg     = Color(0x0FFFFFFF) // rgba(255,255,255,.06)
private val NavBtnBorder = Color(0x1AFFFFFF) // rgba(255,255,255,.10)
private val NavBtnActiveBg     = Color(0x293AA0FF) // rgba(58,160,255,.16)
private val NavBtnActiveBorder = Color(0x423AA0FF) // rgba(58,160,255,.26)
private val DividerColor = Color(0x1AFFFFFF) // rgba(255,255,255,.10)
private val BtBlue       = Color(0xFF2563EB) // bg-blue-600
private val DotRed       = Color(0xFFEF4444) // status-dot disconnected
private val DotGreen     = Color(0xFF22C55E) // status-dot connected

@Composable
fun AppScreen(viewModel: MainViewModel) {
    val blockViewModel: BlockViewModel = viewModel()
    val currentTab by viewModel.currentTab.collectAsState()
    val bleState by viewModel.bleState.collectAsState()
    val showLog by viewModel.showLog.collectAsState()
    val showTuning by viewModel.showTuning.collectAsState()
    val showPassword by viewModel.showPassword.collectAsState()
    val showScan by viewModel.showScanDialog.collectAsState()
    val isConnected = bleState is BleState.Connected
    val isScanning  = bleState is BleState.Scanning || bleState is BleState.Connecting

    // body: background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    // body: radial-gradient(1200px 700px at 20% 20%, rgba(17,27,58,.24), transparent 55%)
                    // +    radial-gradient(900px 600px at 70% 60%, rgba(11,42,46,.21), transparent 55%)
                    // + #060a16
                    colors = listOf(Color(0xFF060a16), Color(0xFF060a16))
                )
            )
    ) {
        // Фоновий градієнт glow (оригінал)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x3D11173A), Color(0x00000000)),
                        center = androidx.compose.ui.geometry.Offset(0.2f * Float.MAX_VALUE, 0.2f * Float.MAX_VALUE),
                        radius = 1200f
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {

            // Спейсер для топбару (він абсолютно позиціонований)
            Spacer(Modifier.height(54.dp + 14.dp + 8.dp)) // header height + margin top + gap

            // Контент
            Box(modifier = Modifier.weight(1f)) {
                if (currentTab == 0) JoystickScreen(viewModel = viewModel)
                if (currentTab == 1) ScratchScreen(mainViewModel = viewModel)
                if (currentTab == 2) FourMotorScreen(viewModel = viewModel)
            }
        }

        // =====================================================================
        // GLASS HEADER — 1в1 з оригіналу
        // .glass-header: position absolute, left:16px, right:16px, top:14px
        // border-radius:999px, height:54px, padding: 8px 10px
        // background: glass + blur, border: 1px solid rgba(255,255,255,.14)
        // =====================================================================
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp)
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x140FFFFFF), // rgba(255,255,255,.08)
                            Color(0x080FFFFFF), // rgba(255,255,255,.03)
                        )
                    )
                )
                .background(GlassBg)
                .border(1.dp, GlassStroke, RoundedCornerShape(999.dp))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            // === ЗЛІВА: status dot ===
            // .status-dot: width:8px height:8px border-radius:50%
            // .connected: background:#22c55e, box-shadow:0 0 8px #22c55e
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isConnected) DotGreen else DotRed)
            )

            Spacer(Modifier.weight(1f))

            // === ЦЕНТР: nav таблетка ===
            // #rcTopNav: gap:5px, transparent bg
            // .nav-btn: width:44px height:44px border-radius:16px
            //   bg: rgba(255,255,255,.06), border: 1px solid rgba(255,255,255,.10)
            // .nav-btn.active: bg rgba(58,160,255,.16), border rgba(58,160,255,.26)
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Джойстик (fa-gamepad)
                NavBtn(
                    active = currentTab == 0,
                    icon = { Icon(Icons.Default.SportsEsports, null, modifier = Modifier.size(20.dp)) },
                    onClick = { viewModel.setTab(0) }
                )
                // Scratch (fa-puzzle-piece)
                NavBtn(
                    active = currentTab == 1,
                    icon = { Icon(Icons.Default.Extension, null, modifier = Modifier.size(20.dp)) },
                    onClick = { viewModel.setTab(1) }
                )
                // 4 мотори (4WD)
                NavBtn(
                    active = currentTab == 2,
                    icon = { Icon(Icons.Default.GridView, null, modifier = Modifier.size(20.dp)) },
                    onClick = { viewModel.setTab(2) }
                )

                // Роздільник: w-px h-6 bg-slate-700 mx-1
                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .width(1.dp)
                        .height(24.dp)
                        .background(DividerColor)
                )

                // Пароль (fa-lock)
                NavBtn(
                    active = false,
                    icon = { Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp)) },
                    onClick = { viewModel.togglePassword() }
                )
                // Лог (fa-book)
                NavBtn(
                    active = false,
                    icon = { Icon(Icons.Default.Book, null, modifier = Modifier.size(18.dp)) },
                    onClick = { viewModel.toggleLog() }
                )
            }

            Spacer(Modifier.weight(1f))

            // === СПРАВА: Bluetooth кнопка ===
            // #btConnect: w-10 h-10 rounded-full bg-blue-600
            // При скануванні — індикатор завантаження
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isConnected -> Color(0xFFDC2626) // bg-red-600 коли підключено
                            isScanning  -> Color(0xFFF59E0B)
                            else        -> BtBlue             // bg-blue-600
                        }
                    )
                    .clickable { viewModel.onConnectClicked() },
                contentAlignment = Alignment.Center
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Модалки
        if (showScan)     ScanDialog(viewModel = viewModel, onDismiss = { viewModel.dismissScan() })
        if (showLog)      LogModal(viewModel = viewModel, onDismiss = { viewModel.toggleLog() })
        if (showTuning)   TuningDialog(viewModel = viewModel, onDismiss = { viewModel.toggleTuning() })
        if (showPassword) PasswordDialog(viewModel = viewModel, onDismiss = { viewModel.togglePassword() })
    }
}

// nav-btn — 44×44dp, border-radius:16dp
@Composable
private fun NavBtn(
    active: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) NavBtnActiveBg else NavBtnBg)
            .border(
                width = 1.dp,
                color = if (active) NavBtnActiveBorder else NavBtnBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (active) Color(0xFF3AA0FF) else Color(0xFF64748B)
        ) {
            icon()
        }
    }
}
