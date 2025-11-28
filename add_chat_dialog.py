file_path = "app/src/main/java/com/example/moneymanager/ui/screens/dashboard/DashboardScreen.kt"

with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    new_lines.append(line)
    
    # Add ChatDialog import after BudgetViewModel
    if "import com.example.moneymanager.ui.viewmodel.BudgetViewModel" in line:
        new_lines.append("import com.example.moneymanager.ui.screens.chat.ChatDialog\n")
    
    # Add state variable after transactionsState
    if "val transactionsState by transactionViewModel.transactionsState.collectAsState()" in line:
        new_lines.append("    var showChatDialog by remember { mutableStateOf(false) }\n")
    
    # Add ChatDialog display after LaunchedEffect block (before Scaffold)
    if i > 0 and "budgetViewModel.loadBudgets()" in lines[i-1] and line.strip() == "}":
        new_lines.append("\n")
        new_lines.append("    // Chat Dialog\n")
        new_lines.append("    if (showChatDialog) {\n")
        new_lines.append("        ChatDialog(onDismiss = { showChatDialog = false })\n")
        new_lines.append("    }\n")
    
    # Add FAB to Scaffold
    if "containerColor = BackgroundGray" in line and line.strip().endswith("BackgroundGray"):
        new_lines[-1] = "        containerColor = BackgroundGray,\n"
        new_lines.append("        floatingActionButton = {\n")
        new_lines.append("            FloatingActionButton(\n")
        new_lines.append("                onClick = { showChatDialog = true },\n")
        new_lines.append("                containerColor = MediumGreen,\n")
        new_lines.append("                modifier = Modifier.padding(bottom = 16.dp)\n")
        new_lines.append("            ) {\n")
        new_lines.append("                Icon(\n")
        new_lines.append("                    imageVector = Icons.Default.SmartToy,\n")
        new_lines.append("                    contentDescription = \"AI Chat\",\n")
        new_lines.append("                    tint = Color.White\n")
        new_lines.append("                )\n")
        new_lines.append("            }\n")
        new_lines.append("        }\n")

with open(file_path, 'w', encoding='utf-8') as f:
    f.writelines(new_lines)

print("✅ Dashboard updated successfully!")
print("Added:")
print("- ChatDialog import")
print("- showChatDialog state variable")
print("- ChatDialog display when state is true")
print("- Floating Action Button to trigger chat")
