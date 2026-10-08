package com.android.posanof.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.android.posanof.security.AuthManager

/**
 * Dialog Verifikasi atau Pembuatan PIN Admin untuk melindungi menu Master Data & Laporan.
 */
@Composable
fun AdminPinDialog(
    authManager: AuthManager,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isPinAlreadySet = remember { authManager.isPinSet() }

    var pinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isPinAlreadySet) "Verifikasi PIN Admin" else "Buat PIN Admin Baru",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isPinAlreadySet)
                        "Masukkan 4-6 digit PIN Admin untuk membuka menu Master Data & Laporan."
                    else
                        "Buat 4-6 digit PIN Admin untuk mengamankan akses menu Master Data & Laporan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            pinInput = it
                            errorMessage = null
                        }
                    },
                    label = { Text("PIN Admin (4-6 digit)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isPinAlreadySet) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                confirmPinInput = it
                                errorMessage = null
                            }
                        },
                        label = { Text("Konfirmasi PIN Admin") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isPinAlreadySet) {
                        // Verifikasi PIN
                        if (authManager.verifyPin(pinInput)) {
                            Toast.makeText(context, "Akses Admin Diberikan", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        } else {
                            errorMessage = "PIN Admin salah, silakan coba lagi."
                        }
                    } else {
                        // Buat PIN Baru
                        if (pinInput.length !in 4..6) {
                            errorMessage = "PIN harus terdiri dari 4 sampai 6 digit angka."
                        } else if (pinInput != confirmPinInput) {
                            errorMessage = "Konfirmasi PIN tidak cocok."
                        } else {
                            val saved = authManager.savePin(pinInput)
                            if (saved) {
                                authManager.loginAdmin(pinInput)
                                Toast.makeText(context, "PIN Admin Berhasil Dibuat", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            } else {
                                errorMessage = "Gagal menyimpan PIN."
                            }
                        }
                    }
                }
            ) {
                Text(if (isPinAlreadySet) "Masuk" else "Simpan PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
