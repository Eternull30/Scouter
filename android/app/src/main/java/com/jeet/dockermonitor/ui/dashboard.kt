package com.jeet.dockermonitor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeet.dockermonitor.data.model.Container
import com.jeet.dockermonitor.ui.theme.JetBrainsMonoFontFamily
import com.jeet.dockermonitor.ui.theme.RunningGreen
import com.jeet.dockermonitor.ui.theme.StoppedRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    containers: List<Container>,
    isLoading: Boolean,
    error: String,
    onRefresh: () -> Unit,
    onContainerClick: (Container) -> Unit,
    onDisconnect : () -> Unit
) {

    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) Color(0xFF0D0D0D) else Color(0xFFF5F5F5)
    val textColor = if (isDark) Color(0xFFE8E8E8) else Color(0xFF1A1A1A)
    val subTextColor = if (isDark) Color(0xFF888888) else Color(0xFF666666)

    Scaffold(
        topBar = {
            TopAppBar(

                title = {
                    Text(
                        "Scouter",
                        style = MaterialTheme.typography.headlineMedium,
                        color = textColor
                    )
                },

                actions = {
                    IconButton(onClick = onDisconnect) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Change host",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
        },
    ) { padding ->

        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                error.isNotEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRefresh) {
                            Text("Retry")
                        }
                    }
                }
                containers.isEmpty() && !isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No containers running",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "${containers.size} container${if (containers.size != 1) "s" else ""} running",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(containers) { container ->
                            ContainerCard(
                                container = container,
                                onClick = { onContainerClick(container) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContainerCard(container: Container, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status dot
            Icon(
                imageVector = Icons.Default.Circle,
                contentDescription = null,
                tint = if (container.state == "running") RunningGreen else StoppedRed,
                modifier = Modifier.size(10.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = container.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = container.image,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontFamily = JetBrainsMonoFontFamily
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = container.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (container.state == "running")
                        RunningGreen
                    else
                        StoppedRed
                )
            }

            Text(
                text = "→",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}