package com.example.moneymanager.ui.screens.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moneymanager.data.model.ChatMessage
import com.example.moneymanager.ui.theme.MediumGreen
import com.example.moneymanager.ui.theme.TextGray
import com.example.moneymanager.ui.viewmodel.ChatUiState
import com.example.moneymanager.ui.viewmodel.ChatViewModel
// ============================
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onClose: () -> Unit
) {
    // Sử dụng collectAsStateWithLifecycle để fix lỗi "Cannot infer type"
    val chatState by viewModel.chatState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = MediumGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Financial Advisor", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        ChatBubble(message = message)
                    }
                }

                MessageInput(
                    value = inputText,
                    onValueChange = { inputText = it },
                    onSendClick = {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                        keyboardController?.hide()
                    },
                    // Kiểm tra chatState có phải là Success không
                    isEnabled = chatState is ChatUiState.Success
                )
            }

            // Xử lý các trạng thái UI
            when (val state = chatState) {
                is ChatUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MediumGreen)
                    }
                }
                is ChatUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                is ChatUiState.Success -> {
                    // Không làm gì
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    // Nếu ChatMessage chưa được định nghĩa đúng ở Bước 1, các thuộc tính này sẽ báo lỗi
    val isUser = message.isFromUser
    val backgroundColor = if (isUser) MediumGreen else Color(0xFFE0E0E0)
    val contentColor = if (isUser) Color.White else Color.Black
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(shape)
                    .background(backgroundColor)
                    .padding(12.dp)
            ) {
                if (message.isLoading) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dotSize = 8.dp
                        val delayUnit = 300
                        repeat(3) { index ->
                            val infiniteTransition = rememberInfiniteTransition(label = "dot")
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(delayUnit * 3, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse,
                                    initialStartOffset = StartOffset(index * delayUnit)
                                ), label = "alpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(dotSize)
                                    .clip(CircleShape)
                                    .background(contentColor.copy(alpha = alpha))
                            )
                        }
                    }
                } else {
                    Text(
                        text = message.content,
                        color = if (message.isError) MaterialTheme.colorScheme.error else contentColor,
                        fontSize = 16.sp
                    )
                }
            }
            if (!message.isLoading) {
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timestamp),
                    fontSize = 10.sp,
                    color = TextGray,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Hỏi tôi về tài chính của bạn...") },
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MediumGreen,
                unfocusedBorderColor = Color.LightGray,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = { if (isEnabled && value.isNotBlank()) onSendClick() }
            ),
            enabled = isEnabled,
            maxLines = 3
        )

        IconButton(
            onClick = onSendClick,
            enabled = isEnabled && value.isNotBlank(),
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isEnabled && value.isNotBlank()) MediumGreen else Color.LightGray)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Gửi",
                tint = Color.White
            )
        }
    }
}