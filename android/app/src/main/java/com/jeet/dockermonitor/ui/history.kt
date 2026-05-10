package com.jeet.dockermonitor.ui

import android.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.jeet.dockermonitor.data.DockerRepository
import com.jeet.dockermonitor.data.model.StatHistory
import com.jeet.dockermonitor.data.model.UiState
import com.jeet.dockermonitor.ui.theme.*
import kotlinx.coroutines.launch
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    containerName : String,
    repository: DockerRepository?,
    onBack : () -> Unit,
){
    val scope = rememberCoroutineScope()
    var historyState by remember { mutableStateOf<UiState<List<StatHistory>>>(UiState.Loading) }
    val isDark = isSystemInDarkTheme()
    val chartBgColor = if (isDark) android.graphics.Color.parseColor("#1A1A1A") else android.graphics.Color.parseColor("#FFFFFF")
    val chartTextColor = if (isDark) android.graphics.Color.parseColor("#AAAAAA") else android.graphics.Color.parseColor("#666666")
    val chartGridColor = if (isDark) android.graphics.Color.parseColor("#333333") else android.graphics.Color.parseColor("#E0E0E0")

    LaunchedEffect(containerName) {
        repository?.let { repo ->
            historyState = repo.getHistory(containerName)
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
            Spacer(Modifier.height(8.dp))
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
                            repository?.let { historyState = it.getHistory(containerName)}
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
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) { Text(
                        "Last ${history.size} readings ${history.size * 10/60} minutes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.6f)
                    ) }

                    val latest = history.first()
                    Text("Latest State", style = MaterialTheme.typography.titleLarge)
                    Row(Modifier.fillMaxWidth()
                        .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("CPU", "${latest.cpu_percent}%", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        StatCard("Memory", "${latest.memory_usage} MB", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("Net RX", "${latest.network_rx} KB", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                        StatCard("Net TX","${latest.network_tx} KB", MaterialTheme.colorScheme.error, Modifier.weight(1f))
                    // Chart will go here
                        ChartCard("CPU Usage(%)") {
                            AndroidView(
                                factory = {
                                    ctx ->
                                    LineChart(ctx).apply {
                                        setupHistoryChart(this, chartTextColor, chartGridColor)
                                        val entries = history.mapIndexed { i, s-> Entry(i.toFloat(), s.cpu_percent.toFloat()) }
                                        val dataset = LineDataSet(entries, "CPU %").apply{
                                            color = CpuChartColor.toArgb()
                                            setDrawCircles(false)
                                            lineWidth = 2f
                                            setDrawFilled(true)
                                            fillColor = CpuChartColor.toArgb()
                                            fillAlpha = 40
                                            setDrawValues(false)
                                        }
                                        data = LineData(dataset)
                                        setBackgroundColor(chartBgColor)
                                        invalidate()
                                    }
                                },
                                modifier = Modifier. fillMaxWidth().height(200.dp)
                                    .padding()


                            )
                        }
                    }

                }
            }
        }

    }

}

private fun setupHistoryChart(chart: LineChart, labelColor: Int, gridColors: Int){
    chart.apply {
        description.isEnabled = false
        setTouchEnabled(true)
        isDragEnabled = true
        setScaleEnabled(true)
        setPinchZoom(true)
        setDrawGridBackground(false)
        legend.textColor = labelColor
        axisRight.isEnabled = false
        axisLeft.apply {
            textColor = labelColor
            gridColor = gridColors
            gridLineWidth = 0.5f
        }
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            textColor = labelColor
            gridColor = gridColors
            gridLineWidth = 0.5f
        }
    }
}