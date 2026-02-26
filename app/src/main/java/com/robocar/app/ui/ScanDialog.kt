package com.robocar.app.ui

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.robocar.app.MainViewModel
import com.robocar.app.ui.theme.*

@Composable
fun ScanDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val devices by viewModel.scanResults.collectAsState()

    Dialog(onDismissRequest = {
        viewModel.dismissScan()
        onDismiss()
    }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0F1B2E))
                .padding(20.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔵 Пошук пристроїв", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                IconButton(onClick = { viewModel.dismissScan(); onDismiss() }) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (devices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Сканування...", color = TextMuted, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(devices) { device ->
                        DeviceItem(device = device, onClick = { viewModel.onDeviceSelected(device) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { viewModel.dismissScan(); onDismiss() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
            ) {
                Text("Скасувати")
            }
        }
    }
}

@Composable
private fun DeviceItem(device: BluetoothDevice, onClick: () -> Unit) {
    val name = try { device.name ?: "Unknown" } catch (e: SecurityException) { "Unknown" }
    val addr = device.address

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Bluetooth,
            contentDescription = null,
            tint = AccentBlue,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(addr, color = TextMuted, fontSize = 11.sp)
        }
    }
}
