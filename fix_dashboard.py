# Read file
with open("app/src/main/java/com/example/moneymanager/ui/screens/dashboard/DashboardScreen.kt", "r", encoding="utf-8") as f:
    lines = f.readlines()

new_lines = []
i = 0
while i < len(lines):
    line = lines[i]
    new_lines.append(line)
    
    # Add import after BudgetViewModel
    if "import com.example.moneymanager.ui.viewmodel.BudgetViewModel" in line:
        new_lines.append("import com.example.moneymanager.ui.screens.chat.ChatDialog\n")
    
    # Add state after transactionsState
    elif "val transactionsState by transactionViewModel.transactionsState.collectAsState()" in line:
        new_lines.append("    var showChatDialog by remember { mutableStateOf(false) }\n")
    
    # Add dialog before Scaffold - check if NEXT line is Scaffold and we just had LaunchedEffect close
    elif i + 1 < len(lines) and "Scaffold(" in lines[i + 1]:
        # Check if current line is closing brace  after LaunchedEffect
        if "}" in line and i > 2:
            # Look back to see if we're in the LaunchedEffect block
            for j in range(max(0, i - 10), i):
                if "LaunchedEffect(Unit)" in lines[j]:
                    new_lines.append("\n")
                    new_lines.append("    // Chat Dialog\n")
                    new_lines.append("    if (showChatDialog) {\n")
                    new_lines.append("        ChatDialog(onDismiss = { showChatDialog = false })\n")
                    new_lines.append("    }\n")
                    break
    
    # Modify Scaffold containerColor line
    elif "containerColor = BackgroundGray" in line and not "," in line:
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
    
    i += 1

with open("app/src/main/java/com/example/moneymanager/ui/screens/dashboard/DashboardScreen.kt", "w", encoding="utf-8") as f:
    f.writelines(new_lines)

print("Dashboard modified successfully!")
