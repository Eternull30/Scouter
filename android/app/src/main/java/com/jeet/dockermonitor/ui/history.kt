package com.jeet.dockermonitor.ui

import android.graphics.Paint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.unit.dp
import com.jeet.dockermonitor.data.DockerRepository
import com.jeet.dockermonitor.data.model.StatHistory
import com.jeet.dockermonitor.data.model.UiState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    containerName : String,
    repository: DockerRepository?,
    onBack : () -> Unit,
){
    val scope = rememberCoroutineScope()
    val historyState by remember { mutableStateOf<UiState<List<StatHistory>>>(UiState.Loading) }
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(containerName) {
        repository?.let { repo ->
            historyState = repo.getHistory(containerName, limit = 60)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {Text("$containerName - History")},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = historyState) {
            is UiState.Loading->{
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center){
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ){
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        scope.launch {
                            historyState = UiState.Loading
                            repository?.let { historyState = it.getHistory(containerName, 60)}
                        }
                    }){
                        Text("Retry")
                    }
                }
            }
            is UiState.Success -> {
                val history = state.data
                if (history.isEmpty()){
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center){
                        Text("No Historical data available for $containerName", color = MaterialTheme.colorScheme.onBackground.copy(0.5f))
                    }
                }else{
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) { Text(
                        "Last ${history.size} readings ${history.size * 10/60} minutes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.6f)
                    ) }
                }
            }
        }

    }

}