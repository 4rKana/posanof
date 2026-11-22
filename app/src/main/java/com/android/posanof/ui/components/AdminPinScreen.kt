package com.android.posanof.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.posanof.security.AuthManager

enum class PinMode {
    VERIFY, CREATE, CHANGE
}

@Composable
fun AdminPinScreen(
    authManager: AuthManager,
    mode: PinMode = PinMode.VERIFY,
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Internal state
    var currentPin by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(if (mode == PinMode.CHANGE) 0 else 1) } // 0=Verify Old, 1=Enter New, 2=Confirm New
    var firstNewPin by remember { mutableStateOf("") }

    val pinLength = 6 // Target 6 digit PIN (or we can just auto-submit at 6)
    
    val title = when (mode) {
        PinMode.VERIFY -> "Masukkan PIN Admin"
        PinMode.CREATE -> if (step == 1) "Buat PIN Baru" else "Konfirmasi PIN Baru"
        PinMode.CHANGE -> {
            when (step) {
                0 -> "Masukkan PIN Lama"
                1 -> "Buat PIN Baru"
                else -> "Konfirmasi PIN Baru"
            }
        }
    }
    
    val subtitle = "Untuk membuka fitur Admin"

    LaunchedEffect(currentPin) {
        if (currentPin.length == pinLength) {
            when (mode) {
                PinMode.VERIFY -> {
                    if (authManager.loginAdmin(currentPin)) {
                        onSuccess()
                    } else {
                        Toast.makeText(context, "PIN salah", Toast.LENGTH_SHORT).show()
                        currentPin = ""
                    }
                }
                PinMode.CREATE -> {
                    if (step == 1) {
                        firstNewPin = currentPin
                        currentPin = ""
                        step = 2
                    } else if (step == 2) {
                        if (currentPin == firstNewPin) {
                            if (authManager.savePin(currentPin)) {
                                authManager.loginAdmin(currentPin) // Login immediately
                                Toast.makeText(context, "PIN berhasil dibuat", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            } else {
                                Toast.makeText(context, "Format PIN tidak valid", Toast.LENGTH_SHORT).show()
                                currentPin = ""
                                step = 1
                            }
                        } else {
                            Toast.makeText(context, "PIN tidak cocok, ulangi dari awal", Toast.LENGTH_SHORT).show()
                            currentPin = ""
                            step = 1
                        }
                    }
                }
                PinMode.CHANGE -> {
                    if (step == 0) {
                        if (authManager.verifyPin(currentPin)) {
                            currentPin = ""
                            step = 1
                        } else {
                            Toast.makeText(context, "PIN lama salah", Toast.LENGTH_SHORT).show()
                            currentPin = ""
                        }
                    } else if (step == 1) {
                        firstNewPin = currentPin
                        currentPin = ""
                        step = 2
                    } else if (step == 2) {
                        if (currentPin == firstNewPin) {
                            if (authManager.savePin(currentPin)) {
                                Toast.makeText(context, "PIN berhasil diubah", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            } else {
                                Toast.makeText(context, "Format PIN tidak valid", Toast.LENGTH_SHORT).show()
                                currentPin = ""
                                step = 1
                            }
                        } else {
                            Toast.makeText(context, "PIN tidak cocok, ulangi pembuatan", Toast.LENGTH_SHORT).show()
                            currentPin = ""
                            step = 1
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PosBackground),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(1f))

        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = PosInk
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = PosMuted
        )

        Spacer(Modifier.height(40.dp))

        // PIN Indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until pinLength) {
                val isFilled = i < currentPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (isFilled) PosPrimary else Color.Transparent)
                        .border(
                            width = 2.dp,
                            color = if (isFilled) PosPrimary else PosMuted.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(Modifier.height(60.dp))

        // Keypad
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "DEL")
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            for (row in keys) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    for (key in row) {
                        if (key == "") {
                            Spacer(modifier = Modifier.size(64.dp))
                        } else if (key == "DEL") {
                            Surface(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (currentPin.isNotEmpty()) {
                                            currentPin = currentPin.dropLast(1)
                                        }
                                    },
                                color = Color.Transparent,
                                shape = CircleShape
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Hapus",
                                        tint = PosInk
                                    )
                                }
                            }
                        } else {
                            Surface(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (currentPin.length < pinLength) {
                                            currentPin += key
                                        }
                                    },
                                color = Color.White,
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.dp, PosBorder)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = key,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PosInk
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        
        TextButton(
            onClick = onCancel,
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Text("Batal", color = PosPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
