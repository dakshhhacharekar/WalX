package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.CurrencyCode
import com.example.domain.UserProfile
import com.example.ui.components.WalXBrandLockup
import com.example.ui.components.WalXCard
import com.example.ui.theme.LocalWalXColors

@Composable
fun SettingsScreen(
    user: UserProfile,
    onSaveProfile: (fullName: String, currency: CurrencyCode, monthlyBudget: Double, darkMode: Boolean) -> Unit,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onPrintStatement: () -> Unit,
    onClearAllData: () -> Unit,
    onSignOut: () -> Unit
) {
    val extended = LocalWalXColors.current
    var fullName by remember(user) { mutableStateOf(user.fullName) }
    var currency by remember(user) { mutableStateOf(user.currency) }
    var monthlyBudgetText by remember(user) { mutableStateOf(user.monthlyBudget.toString()) }
    var darkMode by remember(user) { mutableStateOf(user.darkMode) }
    var confirmClearDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Settings & Data",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Profile, Currency & Theme Card
            item {
                WalXCard(contentPadding = 16.dp) {
                    Text("Profile & Preferences", style = MaterialTheme.typography.titleMedium)
                    Text(user.email, style = MaterialTheme.typography.bodySmall, color = extended.textMuted)

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = monthlyBudgetText,
                        onValueChange = { monthlyBudgetText = it },
                        label = { Text("Monthly Overall Budget (${currency.symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Currency Formatting", style = MaterialTheme.typography.labelLarge, color = extended.textMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CurrencyCode.entries.forEach { cur ->
                            FilterChip(
                                selected = currency == cur,
                                onClick = { currency = cur },
                                label = { Text(cur.label) },
                                modifier = Modifier.testTag("currency_${cur.code.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Deep Obsidian Dark Mode", style = MaterialTheme.typography.bodyLarge)
                            Text("Persisted per user account", style = MaterialTheme.typography.bodySmall, color = extended.textMuted)
                        }
                        Switch(
                            checked = darkMode,
                            onCheckedChange = { darkMode = it },
                            modifier = Modifier.testTag("settings_dark_mode_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val budget = monthlyBudgetText.trim().toDoubleOrNull() ?: user.monthlyBudget
                            onSaveProfile(fullName, currency, budget, darkMode)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = extended.accent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_save_button")
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Preferences", color = Color.White)
                    }
                }
            }

            // Data Portability & Print Statement
            item {
                WalXCard(contentPadding = 16.dp) {
                    Text("Export, Backup & Print", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Portable CSV/JSON exports and printable statements for ${user.fullName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textMuted
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExportCsv,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_csv_button")
                        ) {
                            Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV")
                        }
                        OutlinedButton(
                            onClick = onExportJson,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_json_button")
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("JSON Backup")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onImportJson,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_json_button")
                        ) {
                            Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import JSON")
                        }
                        Button(
                            onClick = onPrintStatement,
                            colors = ButtonDefaults.buttonColors(containerColor = extended.accent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("print_statement_button")
                        ) {
                            Icon(Icons.Rounded.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Print / PDF", color = Color.White)
                        }
                    }
                }
            }

            // Account & Data Lifecycle
            item {
                WalXCard(contentPadding = 16.dp) {
                    Text("Account & Workspace Actions", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { confirmClearDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_all_data_button")
                    ) {
                        Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = extended.expense, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Financial Data", color = extended.expense)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = extended.border)
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onSignOut,
                        colors = ButtonDefaults.buttonColors(containerColor = extended.expenseContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_signout_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = extended.expense, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Switch Account / Sign Out", color = extended.expense)
                    }
                }
            }

            // About WalX by Nor Gaxmer Builds
            item {
                WalXCard(contentPadding = 16.dp) {
                    WalXBrandLockup(compact = false)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "WalX v1.0 • Crafted with precision by Nor Gaxmer Builds. Features atomic multi-wallet ledger accounting, safe arithmetic parsing, isolated per-user vaults, and Gemini-powered financial intelligence.",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textMuted
                    )
                }
            }
        }
    }

    if (confirmClearDialog) {
        AlertDialog(
            onDismissRequest = { confirmClearDialog = false },
            title = { Text("Clear all data for ${user.fullName}?") },
            text = {
                Text("This permanently removes all transactions, wallets, vaults, and recurring bills for your account and restores a clean starter wallet and default categories. Other accounts on this device are unaffected.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmClearDialog = false
                        onClearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = extended.expense)
                ) { Text("Clear Data", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}
