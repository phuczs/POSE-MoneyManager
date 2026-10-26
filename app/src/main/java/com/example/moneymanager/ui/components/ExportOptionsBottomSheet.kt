package com.example.moneymanager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneymanager.R
import com.example.moneymanager.ui.theme.MediumGreen

enum class ExportFormat {
    CSV,
    PDF
}

enum class ExportRange {
    THIS_MONTH,
    LAST_MONTH,
    THIS_YEAR,
    ALL_TIME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportOptionsBottomSheet(
    onDismiss: () -> Unit,
    onExport: (format: ExportFormat, range: ExportRange) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
    var selectedRange by remember { mutableStateOf(ExportRange.THIS_MONTH) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.export_data),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(R.string.export_format),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedCard(
                    onClick = { selectedFormat = ExportFormat.PDF },
                    modifier = Modifier.weight(1f),
                    border = if (selectedFormat == ExportFormat.PDF) BorderStroke(1.5.dp, MediumGreen) else CardDefaults.outlinedCardBorder(),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (selectedFormat == ExportFormat.PDF) MediumGreen.copy(alpha = 0.1f) else Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = if (selectedFormat == ExportFormat.PDF) MediumGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PDF Report", fontWeight = FontWeight.Medium)
                    }
                }

                OutlinedCard(
                    onClick = { selectedFormat = ExportFormat.CSV },
                    modifier = Modifier.weight(1f),
                    border = if (selectedFormat == ExportFormat.CSV) BorderStroke(1.5.dp, MediumGreen) else CardDefaults.outlinedCardBorder(),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (selectedFormat == ExportFormat.CSV) MediumGreen.copy(alpha = 0.1f) else Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = if (selectedFormat == ExportFormat.CSV) MediumGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CSV Data", fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.time_range),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedRange == ExportRange.THIS_MONTH,
                    onClick = { selectedRange = ExportRange.THIS_MONTH },
                    label = { Text(stringResource(R.string.this_month), fontSize = 12.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedRange == ExportRange.LAST_MONTH,
                    onClick = { selectedRange = ExportRange.LAST_MONTH },
                    label = { Text(stringResource(R.string.last_month), fontSize = 12.sp) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedRange == ExportRange.THIS_YEAR,
                    onClick = { selectedRange = ExportRange.THIS_YEAR },
                    label = { Text(stringResource(R.string.this_year), fontSize = 12.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedRange == ExportRange.ALL_TIME,
                    onClick = { selectedRange = ExportRange.ALL_TIME },
                    label = { Text(stringResource(R.string.all_time), fontSize = 12.sp) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onExport(selectedFormat, selectedRange) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MediumGreen)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.export_and_share), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
