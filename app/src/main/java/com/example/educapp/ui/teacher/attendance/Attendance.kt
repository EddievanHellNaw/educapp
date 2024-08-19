package com.example.educapp.ui.teacher.attendance

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(viewModel: AttendanceViewModel, navController: NavHostController) {
    var showNewGroupDialog by remember { mutableStateOf(false) }
    val groups = viewModel.groups



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance") },
                navigationIcon = {
                    IconButton(
                        onClick = { showNewGroupDialog = true },
                        modifier = Modifier.size(48.dp) // Adjust the size as needed
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "New Group")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            LazyColumn {
                items(groups) {group ->
                    GroupBox(
                        group = group,
                        onEdit = { groupToEdit ->
                            viewModel.updateGroup(groupToEdit)
                        },
                        onDelete = {groupToDelete ->
                            viewModel.deleteGroup(groupToDelete)
                        }
                    )
                }
            }
            // Display group boxes using LazyColumn
            if (showNewGroupDialog) {
                NewGroupDialog(viewModel, onDismiss = { showNewGroupDialog = false })
            }
        }
    }
}

@Composable
fun GroupBox(
    group: AttendanceGroup,
    onEdit: (AttendanceGroup) -> Unit,
    onDelete: (AttendanceGroup) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var menuAnchor by remember { mutableStateOf<IntOffset?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = group.name, style = MaterialTheme.typography.titleMedium)
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        menuAnchor = coordinates.positionInRoot().round()
                    }
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }
            }
            Text(text = group.schedule, style = MaterialTheme.typography.bodyMedium)
        }

        if (menuAnchor != null) {
            var density by remember { mutableStateOf<Density?>(null) }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    density = LocalDensity.current
                },
                offset = density?.let {
                    DpOffset(
                        it.run { menuAnchor!!.x.toDp() },
                        it.run { menuAnchor!!.y.toDp() }
                    )
                } ?: DpOffset(0.dp, 0.dp)
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        onEdit(group)
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        onDelete(group)
                        showMenu = false
                    }
                )
            }
        }
    }
}