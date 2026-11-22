package com.android.posanof.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.posanof.data.local.SettingsManager
import com.android.posanof.security.AuthManager
import com.android.posanof.ui.components.*

@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    authManager: AuthManager,
    onNavigateToChangePin: () -> Unit,
    onNavigateToBackupRestore: () -> Unit,
    onLogoutAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val storeName by settingsManager.storeName.collectAsState()
    val taxEnabled by settingsManager.taxEnabled.collectAsState()
    val defaultDiscountPercent by settingsManager.defaultDiscountPercent.collectAsState()
    val defaultMinStock by settingsManager.defaultMinStock.collectAsState()
    val cashierName by settingsManager.cashierName.collectAsState()

    var showStoreNameDialog by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var showMinStockDialog by remember { mutableStateOf(false) }
    var showCashierNameDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PosBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pengaturan",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PosInk,
                modifier = Modifier.weight(1f)
            )
            PosBadge(text = "Admin", bg = Color(0xFFE0F2F1), fg = Color(0xFF00796B))
        }

        // Section Toko
        SectionTitle("Toko")
        SettingItem(
            title = "Nama toko",
            value = storeName,
            onClick = { showStoreNameDialog = true }
        )

        // Section Transaksi
        SectionTitle("Transaksi")
        SettingSwitchItem(
            title = "Pajak (PPN)",
            checked = taxEnabled,
            onCheckedChange = { settingsManager.updateTaxEnabled(it) }
        )
        HorizontalDivider(color = PosBorder)
        SettingItem(
            title = "Diskon default",
            value = "$defaultDiscountPercent%",
            onClick = { showDiscountDialog = true }
        )
        HorizontalDivider(color = PosBorder)
        SettingItem(
            title = "Batas stok minimum",
            value = defaultMinStock.toString(),
            onClick = { showMinStockDialog = true }
        )
        HorizontalDivider(color = PosBorder)
        SettingItem(
            title = "Nama kasir",
            value = cashierName,
            onClick = { showCashierNameDialog = true }
        )

        // Section Keamanan
        SectionTitle("Keamanan")
        SettingNavigationItem(
            title = "Ubah PIN Admin",
            onClick = onNavigateToChangePin
        )
        HorizontalDivider(color = PosBorder)
        SettingNavigationItem(
            title = "Backup dan Restore",
            onClick = onNavigateToBackupRestore
        )
        
        Spacer(Modifier.height(32.dp))
        
        TextButton(
            onClick = {
                authManager.logoutAdmin()
                onLogoutAdmin()
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Keluar Mode Admin", color = PosDanger)
        }
        
        Spacer(Modifier.height(32.dp))
    }

    if (showStoreNameDialog) {
        InputDialog(
            title = "Ubah Nama Toko",
            initialValue = storeName,
            onDismiss = { showStoreNameDialog = false },
            onConfirm = { 
                settingsManager.updateStoreName(it)
                showStoreNameDialog = false
            }
        )
    }
    
    if (showDiscountDialog) {
        InputDialog(
            title = "Diskon Default (%)",
            initialValue = defaultDiscountPercent.toString(),
            keyboardType = KeyboardType.Number,
            onDismiss = { showDiscountDialog = false },
            onConfirm = { 
                it.toIntOrNull()?.let { value -> settingsManager.updateDefaultDiscountPercent(value) }
                showDiscountDialog = false
            }
        )
    }

    if (showMinStockDialog) {
        InputDialog(
            title = "Batas Stok Minimum",
            initialValue = defaultMinStock.toString(),
            keyboardType = KeyboardType.Number,
            onDismiss = { showMinStockDialog = false },
            onConfirm = { 
                it.toIntOrNull()?.let { value -> settingsManager.updateDefaultMinStock(value) }
                showMinStockDialog = false
            }
        )
    }

    if (showCashierNameDialog) {
        InputDialog(
            title = "Nama Kasir",
            initialValue = cashierName,
            onDismiss = { showCashierNameDialog = false },
            onConfirm = { 
                settingsManager.updateCashierName(it)
                showCashierNameDialog = false
            }
        )
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = PosMuted,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingItem(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 16.sp, color = PosInk)
        Text(text = value, fontSize = 16.sp, color = PosMuted, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SettingSwitchItem(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 16.sp, color = PosInk)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PosPrimary
            )
        )
    }
}

@Composable
fun SettingNavigationItem(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 16.sp, color = PosInk)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Detail",
            tint = PosMuted
        )
    }
}

@Composable
fun InputDialog(
    title: String,
    initialValue: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            PosTextField(
                label = "",
                value = value,
                onValueChange = { value = it },
                keyboardType = keyboardType,
                placeholder = "Masukkan nilai"
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text("Simpan", color = PosPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = PosMuted)
            }
        }
    )
}