package com.android.posanof.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

// ---------- Warna (diambil dari Figma) ----------
val PosPrimary = Color(0xFF0F766E)
val PosBackground = Color(0xFFF1F5F3)
val PosSoft = Color(0xFFDCEEE9)
val PosInk = Color(0xFF14201D)
val PosMuted = Color(0xFF6B7773)
val PosBorder = Color(0xFFE1E8E5)
val PosWarnBg = Color(0xFFFFF3D6)
val PosWarnText = Color(0xFF8A5A00)
val PosDanger = Color(0xFFD92D20)

fun Long.toRupiah(): String =
    "Rp " + NumberFormat.getNumberInstance(Locale("in", "ID")).format(this)

// ---------- Komponen dasar ----------
@Composable
fun PosCard(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color,
        border = BorderStroke(1.dp, PosBorder),
        content = content
    )
}

@Composable
fun PosBadge(text: String, bg: Color = PosSoft, fg: Color = PosPrimary) {
    Box(
        Modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PosPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PosPrimary,
            contentColor = Color.White,
            disabledContainerColor = PosPrimary.copy(alpha = 0.4f),
            disabledContentColor = Color.White
        )
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun PosOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, PosPrimary),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = PosPrimary
        )
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

/** Field dengan label kecil di atas, seperti di Figma. */
@Composable
fun PosTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier) {
        Text(label, fontSize = 11.sp, color = PosPrimary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(placeholder, color = PosMuted, fontSize = 14.sp) },
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PosPrimary,
                unfocusedBorderColor = PosBorder,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = PosPrimary
            )
        )
    }
}

@Composable
fun PosFilterChips(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { opt ->
            val active = opt == selected
            Surface(
                shape = RoundedCornerShape(50),
                color = if (active) PosPrimary else Color.White,
                border = if (active) null else BorderStroke(1.dp, PosBorder),
                modifier = Modifier.clickable { onSelect(opt) }
            ) {
                Text(
                    opt,
                    Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    color = if (active) Color.White else PosInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/** Toggle dua pilihan, mis. Tunai | Kasbon. */
@Composable
fun PosSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(PosSoft.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        options.forEachIndexed { i, label ->
            val active = i == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .background(
                        if (active) Color.White else Color.Transparent,
                        RoundedCornerShape(9.dp)
                    )
                    .clickable { onSelect(i) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) PosInk else PosMuted
                )
            }
        }
    }
}

@Composable
fun PosWarningBox(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(PosWarnBg, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Default.Warning, null, tint = PosWarnText, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = PosWarnText, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
fun PosTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, dark: Boolean = false,
              trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack, "Kembali",
            tint = if (dark) Color.White else PosInk,
            modifier = Modifier
                .size(36.dp)
                .clip40(onBack)
                .padding(6.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
            color = if (dark) Color.White else PosInk, modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

private fun Modifier.clip40(onClick: () -> Unit) =
    this.then(Modifier.background(Color.Transparent, CircleShape).clickable(onClick = onClick))

// ---------- Bottom navigation ----------
data class PosNavItem(val route: String, val label: String, val icon: ImageVector)

/** Sesuaikan route dengan sealed class Screen kamu (pos, credit, report, product). */
val PosNavItems = listOf(
    PosNavItem("pos", "Kasir", Icons.Default.ShoppingCart),
    PosNavItem("credit", "Kasbon", Icons.Default.Receipt),
    PosNavItem("report", "Laporan", Icons.Default.BarChart),
    PosNavItem("product", "Admin", Icons.Default.Lock)
)

@Composable
fun PosBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    items: List<PosNavItem> = PosNavItems
) {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            items.forEach { item ->
                val active = item.route == currentRoute
                Column(
                    Modifier
                        .weight(1f)
                        .clickable { onNavigate(item.route) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(item.icon, item.label, tint = if (active) PosPrimary else PosMuted, modifier = Modifier.size(22.dp))
                    Text(
                        item.label, fontSize = 10.sp,
                        color = if (active) PosPrimary else PosMuted,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}