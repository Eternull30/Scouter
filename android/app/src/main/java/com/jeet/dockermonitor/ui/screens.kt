package com.jeet.dockermonitor.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jeet.dockermonitor.data.DockerRepository
import com.jeet.dockermonitor.data.createApiService
import com.jeet.dockermonitor.data.model.Container
import com.jeet.dockermonitor.data.model.LiveStat
import com.jeet.dockermonitor.data.model.UiState
import com.jeet.dockermonitor.di.dataStore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.sample

val KEY_HOST = stringPreferencesKey("docker_host")
val KEY_PORT = stringPreferencesKey("docker_port")

@Composable
fun AppNavigation(){
    val context = LocalContext.current
    val scope =  rememberCoroutineScope()
//    App Stats
    var host by remember {mutableStateOf<String?> (null)}
    var port by remember {mutableStateOf(8000)}
    var repository by remember {mutableStateOf<DockerRepository?>(null)}

//    Current Screen
    var currentScreen by remember { mutableStateOf("setup") }
    var selectedContainer by remember {mutableStateOf<Container?>(null)}

//    Dashboard Screen
    var containers by remember { mutableStateOf<List<Container>>(emptyList()) }
    var isLoadingContainers by remember { mutableStateOf(false) }
    var containersError by remember { mutableStateOf("") }

    // Detail state
    var liveStat by remember { mutableStateOf<LiveStat?>(null) }
    var isWsConnected by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        context.dataStore.data.collectLatest { prefs ->
            val savedHost = prefs[KEY_HOST]
            val savedPort = prefs[KEY_PORT]?.toIntOrNull() ?: 8000
            if (savedHost != null){
                host = savedHost
                port = savedPort
                val baseUrl = if (savedPort == 443) "https://$savedHost/" else "http://$savedHost:$savedPort/"
                val wsUrl = if (savedPort == 443) "wss://$savedHost" else "ws://$savedHost:$savedPort"
                repository = DockerRepository(createApiService(baseUrl), wsUrl)
                currentScreen = "dashboard"
            }
        }
    }
    // Load containers when on dashboard
    LaunchedEffect(currentScreen, repository) {
        if (currentScreen == "dashboard") {
            val repo = repository ?: return@LaunchedEffect
            isLoadingContainers = true
            containersError = ""
            when (val result = repo.getContainers()) {
                is UiState.Success -> {
                    containers = result.data
                    isLoadingContainers = false
                }
                is UiState.Error -> {
                    containersError = result.message
                    isLoadingContainers = false
                }
                is UiState.Loading -> {}
            }
        }
    }

    // Connect WebSocket when on detail screen
    LaunchedEffect(currentScreen, selectedContainer) {
        if (currentScreen == "detail") {
            val repo = repository ?: return@LaunchedEffect
            val container = selectedContainer ?: return@LaunchedEffect
            isWsConnected = true
            liveStat = null
            try {
                repo.observeLiveStats(container.name)
                    .sample(2000) // update UI every 2 seconds instead of every second
                    .collectLatest { stat ->
                        liveStat = stat
                    }
            } catch (e: Exception) {
                isWsConnected = false
            }
        } else {
            isWsConnected = false
            liveStat = null
        }
    }

    // Render current screen
    when (currentScreen) {
        "setup" -> SetupScreen(
            onConnect = { address, portNum ->
                scope.launch {
                    // Save to DataStore
                    context.dataStore.edit { prefs ->
                        prefs[KEY_HOST] = address
                        prefs[KEY_PORT] = portNum.toString()
                    }
                    host = address
                    port = portNum
                    val baseUrl = if (portNum == 443) "https://$address/" else "http://$address:$portNum/"
                    val wsUrl = if (portNum == 443) "wss://$address" else "ws://$address:$portNum"
                    repository = DockerRepository(createApiService(baseUrl), wsUrl)
                    currentScreen = "dashboard"
                }
            }
        )

        "dashboard" -> DashboardScreen(
            containers = containers,
            isLoading = isLoadingContainers,
            error = containersError,
            onRefresh = {
                scope.launch {
                    val repo = repository ?: return@launch
                    isLoadingContainers = true
                    containersError = ""
                    when (val result = repo.getContainers()) {
                        is UiState.Success -> {
                            containers = result.data
                            isLoadingContainers = false
                        }
                        is UiState.Error -> {
                            containersError = result.message
                            isLoadingContainers = false
                        }
                        is UiState.Loading -> {}
                    }
                }
            },
            onContainerClick = { container ->
                selectedContainer = container
                currentScreen = "detail"
            }
        )

        "detail" -> DetailScreen(
            containerName = selectedContainer?.name ?: "",
            liveStat = liveStat,
            isConnected = isWsConnected,
            onBack = {
                currentScreen = "dashboard"
            },
            onStart = {
                scope.launch {
                    repository?.startContainer(selectedContainer?.name ?: "")
                }
            },
            onStop = {
                scope.launch {
                    repository?.stopContainer(selectedContainer?.name ?: "")
                }
            },
            onRestart = {
                scope.launch {
                    repository?.restartContainer(selectedContainer?.name ?: "")
                }
            }
        )
    }
}