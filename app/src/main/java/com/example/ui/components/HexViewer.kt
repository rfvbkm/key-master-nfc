package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SectorData
import com.example.model.toHex
import com.example.nfc.MifareKeys
import com.example.ui.theme.AmberKeyB
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SectorCard(
    sector: SectorData,
    onBlockClick: (sectorIndex: Int, blockIndex: Int, blockBytes: ByteArray) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Sector Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Сектор ${sector.sectorIndex}",
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (!sector.isReadSuccess) {
                        Text(
                            text = "Заблокирован / Ошибка ключа",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            text = "${sector.blocks.size} блоков",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Sector Keys info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    sector.keyA?.let {
                        Text(
                            text = "A: $it",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (sector.keyA != null && sector.keyB != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    sector.keyB?.let {
                        Text(
                            text = "B: $it",
                            color = AmberKeyB,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Blocks table
            sector.blocks.forEachIndexed { bIndex, blockBytes ->
                val isTrailer = bIndex == sector.blocks.size - 1
                val isManufacturer = sector.sectorIndex == 0 && bIndex == 0

                BlockRow(
                    blockIndex = bIndex,
                    blockBytes = blockBytes,
                    isTrailer = isTrailer,
                    isManufacturer = isManufacturer,
                    onClick = {
                        onBlockClick(sector.sectorIndex, bIndex, blockBytes)
                    }
                )
                if (bIndex < sector.blocks.size - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // Access condition breakdown if available
            if (sector.isReadSuccess && sector.blocks.isNotEmpty() && sector.blocks.last().size >= 9) {
                val accessBytes = sector.blocks.last().sliceArray(6..8)
                val cond = MifareKeys.decodeAccessBits(accessBytes)

                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurfaceVariant)
                        .padding(8.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (cond.isDefaultOrInsecure) AmberKeyB else CyberCyan,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = "Access Bits: ${accessBytes.toHex(" ")} ${if (cond.isDefaultOrInsecure) "(Заводские условия FF 07 80)" else ""}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (cond.isDefaultOrInsecure) AmberKeyB else CyberCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "Трейлер: ${cond.trailerPermission}",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BlockRow(
    blockIndex: Int,
    blockBytes: ByteArray,
    isTrailer: Boolean,
    isManufacturer: Boolean,
    onClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val bgColor = when {
        isManufacturer -> CyberPurple.copy(alpha = 0.08f)
        isTrailer -> DarkSurfaceVariant
        else -> DarkSurface
    }
    val borderColor = when {
        isManufacturer -> CyberPurple.copy(alpha = 0.4f)
        isTrailer -> CyberCyan.copy(alpha = 0.3f)
        else -> DarkSurfaceBorder
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Block Index label
            Text(
                text = "B$blockIndex",
                color = when {
                    isManufacturer -> CyberPurple
                    isTrailer -> CyberCyan
                    else -> TextMuted
                },
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(26.dp)
            )

            // Hex representation
            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 4.dp)
            ) {
                if (isTrailer && blockBytes.size >= 16) {
                    // Split trailer: Key A (0..5), Access bits (6..9), Key B (10..15)
                    val keyA = blockBytes.sliceArray(0..5).toHex(" ")
                    val ab = blockBytes.sliceArray(6..9).toHex(" ")
                    val keyB = blockBytes.sliceArray(10..15).toHex(" ")

                    Text(text = keyA, color = NeonGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = ab, color = CyberCyan, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = keyB, color = AmberKeyB, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                } else {
                    Text(
                        text = blockBytes.toHex(" "),
                        color = if (isManufacturer) CyberPurple else TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // ASCII preview
        val ascii = String(blockBytes, Charsets.US_ASCII).map {
            if (it in ' '..'~') it else '.'
        }.joinToString("")

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "| $ascii",
                color = TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Block",
                tint = TextMuted,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}
