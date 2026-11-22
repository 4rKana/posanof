
package com.android.posanof.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.posanof.security.AuthManager

private val PinBackground = Color(0xFFF3F5F3)
private val PinPrimary = Color(0xFF087F73)
private val PinText = Color(0xFF1B2926)
private val PinMuted = Color(0xFF71807B)
private val PinBorder = Color(0xFFE3E9E5)

private enum class PinMode {
    CREATE,
    CONFIRM,
    VERIFY
}

@Composable
fun AdminPinScreen(
    authManager: AuthManager,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
    startInCreateMode: Boolean = false
) {
    val isPinAlreadySet = remember {
        authManager.isPinSet()
    }

    var pinMode by remember {
        mutableStateOf(
            when {
                startInCreateMode -> PinMode.CREATE
                isPinAlreadySet -> PinMode.VERIFY
                else -> PinMode.CREATE
            }
        )
    }

    var pinInput by remember { mutableStateOf("") }
    var createdPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun handleNumber(number: String) {
        if (pinInput.length >= 6) return

        errorMessage = null
        pinInput += number

        if (pinInput.length == 6) {
            when (pinMode) {
                PinMode.CREATE -> {
                    createdPin = pinInput
                    pinInput = ""
                    pinMode = PinMode.CONFIRM
                }

                PinMode.CONFIRM -> {
                    if (pinInput == createdPin) {
                        if (authManager.savePin(pinInput)) {
                            authManager.loginAdmin(pinInput)
                            onSuccess()
                        } else {
                            pinInput = ""
                            errorMessage =
                                "Gagal menyimpan PIN. Silakan coba lagi."
                            pinMode = PinMode.CREATE
                            createdPin = ""
                        }
                    } else {
                        pinInput = ""
                        errorMessage = "PIN tidak cocok. Silakan ulangi."
                    }
                }

                PinMode.VERIFY -> {
                    if (authManager.verifyPin(pinInput)) {
                        onSuccess()
                    } else {
                        pinInput = ""
                        errorMessage = "PIN salah. Silakan coba lagi."
                    }
                }
            }
        }
    }

    fun deleteNumber() {
        if (pinInput.isNotEmpty()) {
            pinInput = pinInput.dropLast(1)
            errorMessage = null
        }
    }

    val title = when (pinMode) {
        PinMode.CREATE -> "Buat PIN Admin"
        PinMode.CONFIRM -> "Konfirmasi PIN Admin"
        PinMode.VERIFY -> "Masukkan PIN Admin"
    }

    val subtitle = when (pinMode) {
        PinMode.CREATE -> {
            "Buat PIN 6 digit untuk mengamankan fitur Admin"
        }
        PinMode.CONFIRM -> "Masukkan kembali PIN Admin Anda"
        PinMode.VERIFY -> "Untuk membuka fitur Admin"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PinBackground)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = PinText
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = PinMuted
        )

        Spacer(modifier = Modifier.height(26.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(6) { index ->
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .background(
                            color = if (index < pinInput.length) {
                                PinPrimary
                            } else {
                                Color.Transparent
                            },
                            shape = CircleShape
                        )
                        .border(
                            width = 1.4.dp,
                            color = PinPrimary,
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        val keypadRows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "DELETE")
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            keypadRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        10.dp,
                        Alignment.CenterHorizontally
                    )
                ) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            when (key) {
                                "" -> Unit

                                "DELETE" -> {
                                    PinKeyButton(
                                        text = "⌫",
                                        onClick = { deleteNumber() }
                                    )
                                }

                                else -> {
                                    PinKeyButton(
                                        text = key,
                                        onClick = {
                                            handleNumber(key)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        } else {
            Spacer(modifier = Modifier.height(14.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Batal",
            color = PinPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable {
                    onDismiss()
                }
                .padding(12.dp)
        )
    }
}

@Composable
private fun PinKeyButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White, CircleShape)
            .border(
                width = 1.dp,
                color = PinBorder,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = if (text == "⌫") 22.sp else 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = PinText
        )
    }
}
