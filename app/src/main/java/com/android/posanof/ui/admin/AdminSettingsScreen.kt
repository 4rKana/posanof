
package com.android.posanof.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AdminBackground = Color(0xFFF3F5F3)
private val AdminPrimary = Color(0xFF087F73)
private val AdminText = Color(0xFF1B2926)
private val AdminMuted = Color(0xFF71807B)
private val AdminBorder = Color(0xFFE3E9E5)

@Composable
fun AdminSettingsScreen(
    onChangePin: () -> Unit,
    onBackupRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val preferences = remember {
        context.getSharedPreferences("posanof_settings", android.content.Context.MODE_PRIVATE)
    }

    var storeName by remember {
        mutableStateOf(preferences.getString("store_name", "Nama Toko") ?: "Nama Toko")
    }

    var taxEnabled by remember {
        mutableStateOf(preferences.getBoolean("tax_enabled", false))
    }

    var defaultDiscount by remember {
        mutableStateOf(preferences.getString("default_discount", "0") ?: "0")
    }

    var minimumStock by remember {
        mutableStateOf(preferences.getString("minimum_stock", "10") ?: "10")
    }

    var cashierName by remember {
        mutableStateOf(preferences.getString("cashier_name", "Kasir 1") ?: "Kasir 1")
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf("") }
    var editValue by remember { mutableStateOf("") }
    var editKey by remember { mutableStateOf("") }

    fun openEditDialog(title: String, key: String, value: String) {
        editTitle = title
        editKey = key
        editValue = value
        showEditDialog = true
    }

    fun saveEdit() {
        when (editKey) {
            "store_name" -> {
                storeName = editValue
            }
            "default_discount" -> {
                val value = editValue.toDoubleOrNull()
                if (value != null && value in 0.0..100.0) {
                    defaultDiscount = value.toString()
                }
            }
            "minimum_stock" -> {
                val value = editValue.toIntOrNull()
                if (value != null && value >= 0) {
                    minimumStock = value.toString()
                }
            }
            "cashier_name" -> {
                cashierName = editValue
            }
        }

        preferences.edit()
            .putString("store_name", storeName)
            .putString("default_discount", defaultDiscount)
            .putString("minimum_stock", minimumStock)
            .putString("cashier_name", cashierName)
            .apply()

        showEditDialog = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AdminBackground)
            .padding(horizontal = 18.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pengaturan",
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = AdminText
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Admin",
                color = AdminPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(
                        color = Color(0xFFDDF1ED),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 13.dp, vertical = 7.dp)
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        SectionTitle("Toko")

        SettingsCard {
            SettingRow(
                title = "Nama toko",
                value = storeName,
                onClick = {
                    openEditDialog("Nama Toko", "store_name", storeName)
                }
            )
        }

        Spacer(modifier = Modifier.height(15.dp))

        SectionTitle("Transaksi")

        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pajak (PPN)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AdminText
                )

                Spacer(modifier = Modifier.weight(1f))

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

            SettingDivider()

            SettingRow(
                title = "Diskon default",
                value = "${defaultDiscount}%",
                onClick = {
                    openEditDialog(
                        "Diskon Default (%)",
                        "default_discount",
                        defaultDiscount
                    )
                }
            )

            SettingDivider()

            SettingRow(
                title = "Batas stok minimum",
                value = minimumStock,
                onClick = {
                    openEditDialog(
                        "Batas Stok Minimum",
                        "minimum_stock",
                        minimumStock
                    )
                }
            )

            SettingDivider()

            SettingRow(
                title = "Nama kasir",
                value = cashierName,
                onClick = {
                    openEditDialog("Nama Kasir", "cashier_name", cashierName)
                }
            )
        }

        Spacer(modifier = Modifier.height(15.dp))

        SectionTitle("Keamanan")

        SettingsCard {
            ActionSettingRow(
                title = "Ubah PIN Admin",
                onClick = onChangePin
            )

            SettingDivider()

            ActionSettingRow(
                title = "Backup dan Restore",
                onClick = onBackupRestore
            )
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = {
                showEditDialog = false
            },
            title = {
                Text(
                    text = editTitle,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = {
                        editValue = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        saveEdit()
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditDialog = false
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = AdminMuted,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 15.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AdminText
            )

            if (title == "Nama toko") {
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AdminText
                )
            }
        }

        if (title != "Nama toko") {
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AdminMuted
            )
        }
    }
}

@Composable
private fun ActionSettingRow(
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = AdminText
        )

        androidx.compose.material3.Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(17.dp),
            tint = AdminMuted
        )
    }
}

@Composable
private fun SettingDivider() {
    HorizontalDivider(
        color = AdminBorder,
        thickness = 1.dp
    )
}
