package com.example.moneymanager.ui.screens.transaction.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneymanager.R
import com.example.moneymanager.domain.model.AnomalySeverity
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.model.TransactionAnomaly
import com.example.moneymanager.ui.theme.MediumGreen
import com.example.moneymanager.ui.theme.TextGray
import com.example.moneymanager.ui.theme.TextPrimary
import com.example.moneymanager.util.toCurrencyString
import com.example.moneymanager.util.toDate
import java.text.SimpleDateFormat
import java.util.Locale

private data class AnomalyPalette(
    val accent: Color,
    val surface: Color,
    val border: Color
)

private fun paletteFor(severity: AnomalySeverity): AnomalyPalette = when (severity) {
    AnomalySeverity.HIGH -> AnomalyPalette(
        accent = Color(0xFFC62828),
        surface = Color(0xFFFFF1F1),
        border = Color(0xFFFFCDD2)
    )
    AnomalySeverity.MEDIUM -> AnomalyPalette(
        accent = Color(0xFFD97706),
        surface = Color(0xFFFFF8EB),
        border = Color(0xFFFDE0A8)
    )
    AnomalySeverity.LOW -> AnomalyPalette(
        accent = Color(0xFF3949AB),
        surface = Color(0xFFF1F3FC),
        border = Color(0xFFC5CAE9)
    )
}

/**
 * Compact protective strip at the top of TransactionsScreen when anomalies exist.
 */
@Composable
fun AnomalyAlertBanner(
    anomaliesCount: Int,
    highestSeverity: AnomalySeverity,
    onReviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = paletteFor(highestSeverity)

    Surface(
        onClick = onReviewClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(palette.accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GppMaybe,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.anomaly_banner_title),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = palette.accent
                )
                Text(
                    text = stringResource(R.string.anomaly_banner_count, anomaliesCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary.copy(alpha = 0.8f)
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = palette.accent
            ) {
                Text(
                    text = stringResource(R.string.anomaly_banner_review),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Inline warning pill placed on flagged transactions inside TransactionListItem.
 */
@Composable
fun AnomalyBadgePill(
    anomaly: TransactionAnomaly,
    languageCode: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = paletteFor(anomaly.severity)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = palette.surface,
        border = BorderStroke(0.8.dp, palette.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = anomaly.title(languageCode),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = palette.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Interactive detail dialog for a single flagged transaction.
 */
@Composable
fun AnomalyReviewDialog(
    transaction: Transaction,
    anomaly: TransactionAnomaly,
    languageCode: String,
    currencyCode: String,
    onDismiss: () -> Unit,
    onMarkLegit: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = paletteFor(anomaly.severity)
    val dateStr = SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault())
        .format(transaction.date.toDate())

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(palette.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GppMaybe,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                text = anomaly.title(languageCode),
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.surface,
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = transaction.category,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = transaction.amount.toCurrencyString(currencyCode),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = palette.accent
                            )
                        }
                        if (transaction.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = transaction.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray
                        )
                    }
                }

                Text(
                    text = anomaly.explanation(languageCode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onMarkLegit,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MediumGreen)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.anomaly_action_legit), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEdit) {
                    Text(stringResource(R.string.anomaly_action_edit), color = TextPrimary)
                }
                TextButton(onClick = onDelete) {
                    Text(
                        stringResource(R.string.anomaly_action_delete),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    )
}

/**
 * BottomSheet listing all currently flagged anomalous transactions for rapid triage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnomalyReviewBottomSheet(
    flaggedItems: List<Pair<Transaction, TransactionAnomaly>>,
    languageCode: String,
    currencyCode: String,
    onDismiss: () -> Unit,
    onMarkLegit: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = stringResource(R.string.anomaly_sheet_title, flaggedItems.size),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.anomaly_sheet_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = TextGray
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(flaggedItems, key = { it.first.id }) { (tx, anomaly) ->
                    val palette = paletteFor(anomaly.severity)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = palette.surface,
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = palette.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${tx.category} · ${anomaly.title(languageCode)}",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = palette.accent
                                    )
                                }
                                Text(
                                    text = tx.amount.toCurrencyString(currencyCode),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = TextPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = anomaly.explanation(languageCode),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary.copy(alpha = 0.85f)
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onMarkLegit(tx.id) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MediumGreen),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        stringResource(R.string.anomaly_action_legit),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onEdit(tx.id) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(15.dp))
                                }
                                OutlinedButton(
                                    onClick = { onDelete(tx.id) },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
