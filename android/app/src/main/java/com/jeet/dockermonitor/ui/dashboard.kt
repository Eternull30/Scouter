package com.jeet.dockermonitor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeet.dockermonitor.data.model.Container
import retrofit2.http.OPTIONS
import com.jeet.dockermonitor.ui.theme.RunningGreen
import com.jeet.dockermonitor.ui.theme.StoppedRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    containers: List<Container>,
    isLoading: Boolean,
    error: String,
    onRefresh: () -> Unit,
    onContainerClick: (Container) -> Unit
){
    Scaffold(
        topBar = {
            TopAppBar(
                title = {Text("Docker Monitor")},
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh")
                    }
                }
            )
        }
    ) {
        padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ){
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error.isNotEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRefresh){
                            Text("Retry")
                        }
                    }
                }
                containers.isEmpty() -> {
                    Text(
                        text = "No Container Running",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }else ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(containers){ container ->
                            ContainerCard(
                                container = container,
                                onClick = {onContainerClick(container)}
                            )
                        }
                    }
            }
        }
    }
}

@Composable
fun ContainerCard(container: Container, onClick: ()-> Unit){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable{onClick()},
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
            ) {
            Icon(
                imageVector = Icons.Default.Circle ,
                contentDescription = null,
                tint = if (container.state == "running") RunningGreen else StoppedRed,
                modifier  = Modifier.height(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = container.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = container.image,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = container.state,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Text(
                text = "→",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}