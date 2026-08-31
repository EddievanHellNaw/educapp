package com.example.educapp.commons.teacher.groups

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import sh.calvin.reorderable.ReorderableItem
import com.example.educapp.commons.teacher.attendance.AttendanceGroup
import com.example.educapp.commons.teacher.attendance.AttendanceViewModel
import com.example.educapp.commons.teacher.attendance.NewGroupDialog
import com.example.educapp.commons.ui.GradientCard
import com.example.educapp.commons.ui.HapticFloatingActionButton
import com.example.educapp.commons.ui.hapticClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.IconButton

import com.example.educapp.commons.teacher.attendance.EditGroupDialog
import com.example.educapp.commons.teacher.attendance.DeleteConfirmationDialog


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsMainScreen(
    navController: NavHostController,
    viewModel: AttendanceViewModel,
    teacherId: String
) {
    var showNewGroupDialog by remember { mutableStateOf(false) }
    // Start listening to groups for this teacher
    LaunchedEffect(teacherId) {
        viewModel.startGroupsListener(teacherId)
        Log.d("GroupsMainScreen", "teacherId: $teacherId")
    }
    val groups = viewModel.groups
    Log.d("GroupsMainScreen", "groups: $groups")

    val lazyListState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    var lastTickIndex by remember { mutableStateOf(-1) }

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.moveGroup(from.index, to.index)
        if (to.index != lastTickIndex) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            lastTickIndex = to.index
        }
    }



    Surface(color = MaterialTheme.colorScheme.background) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Choose a group") },
                ) },
            floatingActionButton = {
                HapticFloatingActionButton(
                    onClick = { showNewGroupDialog = true },
                    modifier = Modifier
                        .size(75.dp) // Compact size
                        .padding(8.dp),
                ){
                    Icon(Icons.Filled.Add, contentDescription = "New Group")
                }
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                LazyColumn(state = lazyListState) {
                    items(groups, key = { it.id }) { group ->
                        ReorderableItem(reorderableState, key = group.id) { isDragging ->

                            // IMPORTANT: longPressDraggableHandle() MUST be called in this scope
                            val dragModifier = with(this) {
                                Modifier
                                    .animateItem() // optional but recommended
                                    .longPressDraggableHandle(
                                        onDragStarted =  {
                                            lastTickIndex = -1
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDragStopped = {
                                            viewModel.persistGroupsOrder(teacherId)
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    )
                            }

                            GroupCard(
                                group = group,
                                modifier = dragModifier
                                    .zIndex(if (isDragging) 1f else 0f),
                                isDragging = isDragging,

                                onClick = {
                                    navController.navigate(
                                        "teacher/group_dashboard/${group.id}"
                                    )
                                },

                                onEdit = { updatedGroup ->
                                    viewModel.updateGroup(
                                        updatedGroup,
                                        teacherId
                                    )
                                },

                                onDelete = { groupToDelete ->
                                    viewModel.deleteGroup(
                                        groupToDelete,
                                        teacherId
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
        if (showNewGroupDialog) {
            NewGroupDialog(viewModel, teacherId, onDismiss = { showNewGroupDialog = false })
        }
    }
}

@Composable
fun GroupCard(
    group: AttendanceGroup,
    isDragging: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onEdit: (AttendanceGroup) -> Unit,
    onDelete: (AttendanceGroup) -> Unit
) {
    var showEditDialog by remember {
        mutableStateOf(false)
    }

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    val selectedColor = remember(group.color) {
        mutableStateOf(group.getColor())
    }

    val shape = RoundedCornerShape(8.dp)

    // Wiggle while dragging
    val wiggle = rememberInfiniteTransition(
        label = "wiggle"
    )

    val rotation by wiggle.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 110,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    val lift by animateDpAsState(
        targetValue = if (isDragging) 10.dp else 0.dp,
        animationSpec = tween(120),
        label = "lift"
    )

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.02f else 1f,
        animationSpec = tween(120),
        label = "scale"
    )

    GradientCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .shadow(lift, shape)
            .border(
                width = if (isDragging) 2.dp else 0.dp,
                color = if (isDragging) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                },
                shape = shape
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ =
                    if (isDragging) rotation else 0f
            },

        shape = shape,

        gradientBrush = Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface,
                selectedColor.value
            )
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            /*
             * Main group information.
             * Tapping this opens the group dashboard.
             */
            Column(
                modifier = Modifier
                    .weight(1f)
                    .hapticClickable {
                        onClick()
                    }
            ) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = group.schedule,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            /*
             * Edit
             */
            IconButton(
                onClick = {
                    showEditDialog = true
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit ${group.name}",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            /*
             * Delete
             */
            IconButton(
                onClick = {
                    showDeleteDialog = true
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete ${group.name}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showEditDialog) {
        EditGroupDialog(
            group = group,

            onConfirm = { updatedGroup ->

                selectedColor.value =
                    Color(updatedGroup.color)

                onEdit(updatedGroup)

                showEditDialog = false
            },

            onDismiss = {
                showEditDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            groupName = group.name,

            onConfirm = {
                onDelete(group)
                showDeleteDialog = false
            },

            onDismiss = {
                showDeleteDialog = false
            }
        )
    }
}