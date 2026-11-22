package com.android.posanof.ui.settings

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.posanof.data.local.QrisMode
import com.android.posanof.data.local.SettingsManager
import com.android.posanof.security.AuthManager
import com.android.posanof.ui.components.*
import com.android.posanof.util.QrisGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
    val requirePinOnLaunch by settingsManager.requirePinOnLaunch.collectAsState()
    val qrisMode by settingsManager.qrisMode.collectAsState()
    val qrisRawPayload by settingsManager.qrisRawPayload.collectAsState()
    val qrisStaticImagePath by settingsManager.qrisStaticImagePath.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showStoreNameDialog by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var showMinStockDialog by remember { mutableStateOf(false) }
    var showCashierNameDialog by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }

    fun saveStaticImageUri(uri: Uri) {
        coroutineScope.launch {
            try {
                val file = File(context.filesDir, "qris_static.png")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    withContext(Dispatchers.IO) {
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } ?: throw IllegalStateException("Tidak dapat membaca gambar.")
                
                settingsManager.updateQrisStaticImagePath(file.absolutePath)
                
                val bitmap = withContext(Dispatchers.IO) {
                    BitmapFactory.decodeFile(file.absolutePath)
                }
                
                if (bitmap != null) {
                    QrisGenerator.decodeQrisFromBitmap(bitmap) { decodedPayload ->
                        if (!decodedPayload.isNullOrBlank() && QrisGenerator.isValidQrisPayload(decodedPayload)) {
                            settingsManager.updateQrisRawPayload(decodedPayload)
                            Toast.makeText(context, "Gambar QRIS tersimpan & QRIS Dinamis aktif otomatis", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Gambar tersimpan untuk Mode Statis. Kode QRIS tidak terbaca untuk Mode Dinamis, coba gambar lebih jelas atau gunakan Scan Kamera.", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Gambar tersimpan untuk Mode Statis. Kode QRIS tidak terbaca untuk Mode Dinamis, coba gambar lebih jelas atau gunakan Scan Kamera.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal menyimpan gambar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val pickVisualMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { saveStaticImageUri(it) }
    }

    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { saveStaticImageUri(it) }
    }

    if (showScanner) {
        BarcodeScannerOverlay(
            title = "Scan QRIS Toko",
            onBarcodeScanned = { barcode ->
                if (QrisGenerator.isValidQrisPayload(barcode)) {
                    settingsManager.updateQrisRawPayload(barcode)
                    Toast.makeText(context, "QRIS Toko Berhasil Disimpan", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Format QRIS tidak valid / bukan QRIS EMVCo", Toast.LENGTH_LONG).show()
                }
                showScanner = false
            },
            onBack = { showScanner = false }
        )
        return
    }

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

        // Section Pembayaran QRIS
        SectionTitle("Pembayaran QRIS")
        Column(Modifier.padding(horizontal = 16.dp)) {
            PosSegmented(
                options = listOf("Dinamis", "Statis"),
                selectedIndex = if (qrisMode == QrisMode.DYNAMIC) 0 else 1,
                onSelect = { settingsManager.updateQrisMode(if (it == 0) QrisMode.DYNAMIC else QrisMode.STATIC) }
            )
            Spacer(Modifier.height(12.dp))

            if (qrisMode == QrisMode.DYNAMIC) {
                PosCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("QRIS Dinamis (Nominal Otomatis)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PosInk)
                        Spacer(Modifier.height(4.dp))
                        Text("Membentuk QRIS unik otomatis saat checkout berdasarkan nominal keranjang.", fontSize = 12.sp, color = PosMuted)
                        Spacer(Modifier.height(12.dp))

                        val merchantName = if (qrisRawPayload.isNotBlank()) QrisGenerator.extractMerchantName(qrisRawPayload) ?: storeName else null
                        if (merchantName != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth().background(PosSoft.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("Status: Tersimpan", fontSize = 11.sp, color = PosPrimary, fontWeight = FontWeight.Bold)
                                    Text(merchantName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PosInk)
                                }
                                TextButton(onClick = { settingsManager.clearQrisRawPayload() }) {
                                    Text("Hapus", color = PosDanger, fontSize = 12.sp)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        } else {
                            Text("Status: Belum Diatur", fontSize = 12.sp, color = PosDanger)
                            Spacer(Modifier.height(12.dp))
                        }

                        PosOutlineButton(
                            text = if (qrisRawPayload.isBlank()) "Scan QRIS Toko" else "Ganti QRIS Toko",
                            onClick = { showScanner = true }
                        )
                    }
                }
            } else {
                PosCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("QRIS Statis (Gambar)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PosInk)
                        Spacer(Modifier.height(4.dp))
                        Text("Menampilkan gambar QRIS toko fisik dari galeri.", fontSize = 12.sp, color = PosMuted)
                        Spacer(Modifier.height(12.dp))

                        val imageFile = if (qrisStaticImagePath.isNotBlank()) File(qrisStaticImagePath) else null
                        if (imageFile != null && imageFile.exists()) {
                            val bitmap = remember(qrisStaticImagePath) { BitmapFactory.decodeFile(qrisStaticImagePath) }
                            if (bitmap != null) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(160.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Gambar QRIS",
                                        modifier = Modifier.fillMaxHeight().clip(RoundedCornerShape(8.dp))
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PosOutlineButton(
                                text = if (qrisStaticImagePath.isBlank()) "Pilih Gambar QRIS" else "Ganti Gambar",
                                onClick = {
                                    try {
                                        pickVisualMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    } catch (_: android.content.ActivityNotFoundException) {
                                        try {
                                            getContentLauncher.launch("image/*")
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Tidak ada aplikasi galeri/berkas yang mendukung", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (qrisStaticImagePath.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { settingsManager.clearQrisStaticImage(context) },
                                    modifier = Modifier.height(48.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PosDanger),
                                    border = BorderStroke(1.dp, PosDanger),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Hapus", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Keamanan
        SectionTitle("Keamanan")
        SettingSwitchItem(
            title = "Kunci saat buka aplikasi",
            checked = requirePinOnLaunch,
            onCheckedChange = { settingsManager.updateRequirePinOnLaunch(it) }
        )
        HorizontalDivider(color = PosBorder)
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