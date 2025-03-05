package com.example.educapp.commons.assistant

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapp.teacher.assistant.AssistantViewModel
import com.example.myapp.teacher.assistant.ChatMessage
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText
import androidx.compose.material3.Surface
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import com.example.educapp.commons.RegistrationViewModel
import com.example.educapp.commons.UserRole
import com.example.educapp.commons.ui.hapticClickable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AssistantScreen(
    navController: NavController,
    registrationViewModel: RegistrationViewModel // or however you retrieve it
) {
    // 1) Local states for the user role and loading
    var userRole by remember { mutableStateOf<UserRole?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // 2) Call `getUserRole` once, store the result in Compose state
    LaunchedEffect(Unit) {
        registrationViewModel.getUserRole { fetchedRole ->
            userRole = fetchedRole
            isLoading = false
        }
    }

    // 3) Show a spinner if still loading
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        // If no role was found, fallback to STUDENT or TEACHER
        val finalRole = userRole ?: UserRole.STUDENT

        // 4) Now we can create the AssistantViewModel with the role
        val assistantViewModel: AssistantViewModel = koinViewModel(parameters = {
            // If your Koin factory expects a String, pass finalRole.name:
            parametersOf(finalRole.name)

            // Or if your Koin factory has factory { (userRole: UserRole) -> ... }:
            // parametersOf(finalRole)
        })

        // 5) Render your main assistant UI
        MainAssistantScreen(navController, assistantViewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAssistantScreen(
    navController: NavController,
    viewModel: AssistantViewModel
) {
    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    var userInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Suppose we have a boolean in the ViewModel indicating if the AI is responding
    val isResponding by viewModel.isResponding.collectAsState() // e.g. a StateFlow<Boolean>

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1) Messages list
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState
        ) {
            items(messages) { message ->
                MessageBubble(message,isResponding)
            }
        }

        // Scroll to bottom when the messages list changes
        LaunchedEffect(Unit) {
            snapshotFlow { messages.lastOrNull() }
                .collect {
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.lastIndex)
                    }
                }
        }


        // 2) Our new ChatInputBar at the bottom
        ChatInputBar(
            userInput = userInput,
            onUserInputChange = { userInput = it },
            onAttachClick = {
                // e.g., handle file picker or attachments
            },
            onSendClick = {
                focusManager.clearFocus()
                viewModel.sendMessageStream("teacher", userInput)
                userInput = ""
            },
            isResponding = isResponding
        )
    }
}

@Composable
fun ChatInputBar(
    userInput: String,
    onUserInputChange: (String) -> Unit,
    onAttachClick: () -> Unit,
    onSendClick: () -> Unit,
    isResponding: Boolean
) {
    // A Surface or Box with a rounded shape
    // to replicate a “bubble” style input area.
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left icon (plus / attach)
            IconButton(
                onClick = onAttachClick,
                enabled = !isResponding
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Attach"
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            // Wrap BasicTextField in a Box to add placeholder functionality.
            Box(
                modifier = Modifier
                    .weight(1f),
                contentAlignment = Alignment.CenterStart // Ensures content is aligned to the start
            ) {
                if (userInput.isEmpty()) {
                    Text(
                        text = "What can I help you with today?",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                }

                BasicTextField(
                    value = userInput,
                    onValueChange = onUserInputChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Start // Force text to start from the left
                    ),
                    modifier = Modifier
                        .fillMaxWidth()            // Use full width so text doesn't appear in the middle
                )
            }


            Spacer(modifier = Modifier.width(8.dp))

            // Right icon (send)
            IconButton(
                onClick = onSendClick,
                // Disable if AI is responding or no text
                enabled = userInput.isNotBlank() && !isResponding
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send"
                )
            }
        }
    }
}


@Composable
fun MessageBubble(
    message: ChatMessage,
    isResponding: Boolean
) {
    val isUser = (message.sender == "user")
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleBackground = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    var showReasoning by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = alignment
    ) {
        val bubbleModifier = if (isUser) {
            Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bubbleBackground)
                .padding(12.dp)
        } else {
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(bubbleBackground)
                .padding(12.dp)
        }

        Column(modifier = bubbleModifier) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {

                // Only show the toggle row if this is the assistant
                if (!isUser) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .hapticClickable {
                                // If desired, you can disable toggling while responding
                                showReasoning = !showReasoning
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val arrowIcon = if (showReasoning) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown
                        Icon(
                            imageVector = arrowIcon,
                            contentDescription = if (showReasoning) "Hide Reasoning" else "Show Reasoning",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))

                        val label = when {
                            isResponding -> "Thinking..."
                            showReasoning -> "Hide Reasoning"
                            else -> "Show Reasoning"
                        }

                        AnimatedGradientText(text = label)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // If the user toggles reasoning (and it's available), show it **above** the final text
                    if (!message.reasoning.isNullOrEmpty() && showReasoning) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        ) {
                            RichText {
                                Markdown(content = message.reasoning!!)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // Now show the final text below the reasoning
                RichText {
                    Markdown(content = message.text)
                }
            }
        }
    }
}



/**
 * A simple infinite gradient text animation.
 * Make sure your primary and secondary theme colors are somewhat distinct,
 * or replace with your own color stops for a more obvious animation.
 */
@Composable
fun AnimatedGradientText(text: String) {
    // Animate a gradient across the text using an infinite transition
    val infiniteTransition = rememberInfiniteTransition()
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Create a linear gradient brush that moves (this is just one approach)
    val brush = Brush.radialGradient(
        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onPrimary),
        center = Offset(0f + gradientOffset * 300, 40f),
        radius = 200f
    )

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, brush = brush)
    )
}
