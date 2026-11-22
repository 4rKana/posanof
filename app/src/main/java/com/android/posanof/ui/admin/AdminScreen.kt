
package com.android.posanof.ui.admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.posanof.data.repository.BackupRepository
import com.android.posanof.security.AuthManager
import com.android.posanof.ui.components.AdminPinScreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AdminBackground = Color(0xFFF3F5F3)
private val AdminPrimary = Color(0xFF087F73)
private val AdminText = Color(0xFF1B2926)
private val AdminMuted = Color(0xFF71807B)
private val AdminBorder = Color(0xFFE3E9E5)

private enum class PinChangeStage {
    VERIFY,
    CREATE
}

@Composable
fun AdminScreen(
    authManager: AuthManager,
    backupRepository: BackupRepository,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val preferences = remember {
        context.getSharedPreferences(
            "posanof_admin_settings",
            android.content.Context.MODE_PRIVATE
        )
    }

    var storeName by remember {
        mutableStateOf(
            preferences.getString("store_name", "") ?: ""
        )
    }

    var taxEnabled by remember {
        mutableStateOf(
            preferences.getBoolean("tax_enabled", false)
        )
    }

    var defaultDiscount by remember {
        mutableStateOf(
            preferences.getString("default_discount", "0") ?: "0"
        )
    }

    var minimumStock by remember {
        mutableStateOf(
            preferences.getString("minimum_stock", "10") ?: "10"
        )
    }

    var cashierName by remember {
        mutableStateOf(
            preferences.getString("cashier_name", "") ?: ""
        )
    }

    var pinChangeStage by remember {
        mutableStateOf<PinChangeStage?>(null)
    }

    var isProcessing by remember {
        mutableStateOf(false)
    }

    var confirmRestoreUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var showLogoutConfirmation by remember {
        mutableStateOf(false)
    }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/octet-stream"
        )
    ) { uri ->
        uri?.let { target ->
            scope.launch {
                isProcessing = true

                val result = backupRepository.exportBackup(target)

                isProcessing = false

                result.fold(
                    onSuccess = { summary ->
                        Toast.makeText(
                            context,
                            "Backup berhasil: ${summary.productCount} produk, " +
                                    "${summary.transactionCount} transaksi",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    onFailure = { error ->
                        Toast.makeText(
                            context,
                            "Gagal backup: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        confirmRestoreUri = uri
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AdminBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pengaturan",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AdminText
                )

                Surface(
                    color = Color(0xFFDDF3EF),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "Admin",
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        ),
                        color = AdminPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // TOKO
            AdminSectionTitle("Toko")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(
                        text = "Nama toko",
                        color = AdminMuted,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(Modifier.height(4.dp))

                    OutlinedTextField(
                        value = storeName,
                        onValueChange = {
                            storeName = it
                            preferences.edit()
                                .putString("store_name", it)
                                .apply()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("[Nama Toko]")
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // TRANSAKSI
            AdminSectionTitle("Transaksi")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Pajak (PPN)",
                            color = AdminText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Switch(
                            checked = taxEnabled,
                            onCheckedChange = {
                                taxEnabled = it
                                preferences.edit()
                                    .putBoolean("tax_enabled", it)
                                    .apply()
                            }
                        )
                    }

                    AdminDivider()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            "Diskon default (%)",
                            color = AdminText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.height(6.dp))

                        OutlinedTextField(
                            value = defaultDiscount,
                            onValueChange = { value ->
                                if (
                                    value.isEmpty() ||
                                    value.all { it.isDigit() }
                                ) {
                                    defaultDiscount = value
                                    preferences.edit()
                                        .putString("default_discount", value)
                                        .apply()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("0") },
                            shape = RoundedCornerShape(12.dp),
                            suffix = { Text("%") }
                        )
                    }

                    AdminDivider()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            "Batas stok minimum",
                            color = AdminText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.height(6.dp))

                        OutlinedTextField(
                            value = minimumStock,
                            onValueChange = { value ->
                                if (
                                    value.isEmpty() ||
                                    value.all { it.isDigit() }
                                ) {
                                    minimumStock = value
                                    preferences.edit()
                                        .putString("minimum_stock", value)
                                        .apply()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("10") },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    AdminDivider()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            "Nama kasir",
                            color = AdminText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.height(6.dp))

                        OutlinedTextField(
                            value = cashierName,
                            onValueChange = {
                                cashierName = it
                                preferences.edit()
                                    .putString("cashier_name", it)
                                    .apply()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("[Nama Kasir]")
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // KEAMANAN
            AdminSectionTitle("Keamanan")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    AdminActionRow(
                        title = "Ubah PIN Admin",
                        onClick = {
                            pinChangeStage = PinChangeStage.VERIFY
                        }
                    )

                    AdminDivider()

                    AdminActionRow(
                        title = "Backup dan Restore",
                        onClick = {
                            val stamp = SimpleDateFormat(
                                "yyyyMMdd_HHmm",
                                Locale.getDefault()
                            ).format(Date())

                            backupLauncher.launch(
                                "POSANOF_BACKUP_$stamp.backup"
                            )
                        }
                    )

                    OutlinedButton(
                        onClick = {
                            restoreLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        enabled = !isProcessing
                    ) {
                        Text("Pulihkan dari File Backup")
                    }

                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 12.dp),
                            color = AdminPrimary
                        )
                    }
                }
            }

            // SESI ADMIN
            AdminSectionTitle("Sesi Admin")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    AdminActionRow(
                        title = "Keluar dari Admin",
                        onClick = {
                            showLogoutConfirmation = true
                        }
                    )
                }
            }
        }

        // LAYAR VERIFIKASI DAN PEMBUATAN PIN BARU
        when (pinChangeStage) {
            PinChangeStage.VERIFY -> {
                AdminPinScreen(
                    authManager = authManager,
                    onSuccess = {
                        // PIN lama tidak dihapus sebelum PIN baru berhasil disimpan.
                        pinChangeStage = PinChangeStage.CREATE
                    },
                    onDismiss = {
                        pinChangeStage = null
                    }
                )
            }

            PinChangeStage.CREATE -> {
                AdminPinScreen(
                    authManager = authManager,
                    startInCreateMode = true,
                    onSuccess = {
                        pinChangeStage = null

                        Toast.makeText(
                            context,
                            "PIN Admin berhasil diperbarui.",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    onDismiss = {
                        pinChangeStage = null
                    }
                )
            }

            null -> Unit
        }
    }

    // KONFIRMASI LOGOUT
    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showLogoutConfirmation = false
            },
            title = {
                Text("Keluar dari Admin?")
            },
            text = {
                Text(
                    "Apakah Anda yakin ingin mengakhiri sesi Admin " +
                            "dan kembali ke halaman Kasir?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmation = false
                        onLogout()
                    }
                ) {
                    Text("Ya, Keluar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirmation = false
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // KONFIRMASI RESTORE
    confirmRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = {
                confirmRestoreUri = null
            },
            title = {
                Text("Konfirmasi Restore Database")
            },
            text = {
                Text(
                    "Restore akan menimpa seluruh data lokal produk, " +
                            "transaksi, dan piutang dengan data dari file backup. " +
                            "Lanjutkan?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isProcessing = true

                            val result = backupRepository.importRestore(uri)

                            isProcessing = false
                            confirmRestoreUri = null

                            result.fold(
                                onSuccess = { summary ->
                                    Toast.makeText(
                                        context,
                                        "Restore berhasil: ${summary.productCount} produk, " +
                                                "${summary.transactionCount} transaksi",
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                onFailure = { error ->
                                    Toast.makeText(
                                        context,
                                        "Gagal restore: ${error.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        }
                    },
                    enabled = !isProcessing
                ) {
                    Text("Ya, Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmRestoreUri = null
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun AdminSectionTitle(title: String) {
    Text(
        text = title,
        color = AdminMuted,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun AdminDivider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AdminBorder)
    )
}

@Composable
private fun AdminActionRow(
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Color.Transparent)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = AdminText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        TextButton(onClick = onClick) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = title,
                tint = AdminMuted
            )
        }
    }
}
