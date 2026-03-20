package com.jeet.dockermonitor.ui

import android.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.jeet.dockermonitor.data.model.LiveStat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    containerName: String,
    liveStat: LiveStat?,
    isConnected: Boolean,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
) {
    // Rolling data lists — last 30 points
    val cpuData = remember { mutableStateListOf<Entry>() }
    val memData = remember { mutableStateListOf<Entry>() }
    val netRxData = remember { mutableStateListOf<Entry>() }
    val netTxData = remember { mutableStateListOf<Entry>() }
    var index by remember { mutableStateOf(0f) }

    // Chart references
    val cpuChart = remember { mutableStateOf<LineChart?>(null) }
    val memChart = remember { mutableStateOf<LineChart?>(null) }
    val netChart = remember { mutableStateOf<LineChart?>(null) }

    // Update charts when new stat arrives
    LaunchedEffect(liveStat) {
        liveStat?.let { stat ->
            // Add new entries
            cpuData.add(Entry(index, stat.cpu_percent.toFloat()))
            memData.add(Entry(index, stat.memory_usage_mb.toFloat()))
            netRxData.add(Entry(index, stat.network_rx_kb.toFloat()))
            netTxData.add(Entry(index, stat.network_tx_kb.toFloat()))

            // Keep only last 30 points
            if (cpuData.size > 30) cpuData.removeAt(0)
            if (memData.size > 30) memData.removeAt(0)
            if (netRxData.size > 30) netRxData.removeAt(0)
            if (netTxData.size > 30) netTxData.removeAt(0)

            index++

            // Update CPU chart
            cpuChart.value?.apply {
                val dataSet = LineDataSet(cpuData.toList(), "CPU %").apply {
                    color = Color.parseColor("#7C4DFF")
                    setDrawCircles(false)
                    lineWidth = 2f
                    setDrawFilled(true)
                    fillColor = Color.parseColor("#7C4DFF")
                    fillAlpha = 50
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                }
                data = LineData(dataSet)
                notifyDataSetChanged()
                invalidate()
            }

            // Update Memory chart
            memChart.value?.apply {
                val dataSet = LineDataSet(memData.toList(), "Memory MB").apply {
                    color = Color.parseColor("#00BCD4")
                    setDrawCircles(false)
                    lineWidth = 2f
                    setDrawFilled(true)
                    fillColor = Color.parseColor("#00BCD4")
                    fillAlpha = 50
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                }
                data = LineData(dataSet)
                notifyDataSetChanged()
                invalidate()
            }

            // Update Network chart
            netChart.value?.apply {
                val rxSet = LineDataSet(netRxData.toList(), "RX KB").apply {
                    color = Color.parseColor("#4CAF50")
                    setDrawCircles(false)
                    lineWidth = 2f
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                }
                val txSet = LineDataSet(netTxData.toList(), "TX KB").apply {
                    color = Color.parseColor("#FF5722")
                    setDrawCircles(false)
                    lineWidth = 2f
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                }
                data = LineData(rxSet, txSet)
                notifyDataSetChanged()
                invalidate()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(containerName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Connection status
            Text(
                text = if (isConnected) "● Live" else "○ Disconnected",
                color = if (isConnected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error,
                fontSize = 13.sp
            )

            // Stat cards
            liveStat?.let { stat ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "CPU",
                        value = "${"%.4f".format(stat.cpu_percent)}%",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Memory",
                        value = "${"%.1f".format(stat.memory_usage_mb)} MB",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Net RX",
                        value = "${"%.1f".format(stat.network_rx_kb)} KB",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Net TX",
                        value = "${"%.1f".format(stat.network_tx_kb)} KB",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Disk Read",
                        value = "${"%.1f".format(stat.disk_read_kb)} KB",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Disk Write",
                        value = "${"%.1f".format(stat.disk_write_kb)} KB",
                        modifier = Modifier.weight(1f)
                    )
                }
            } ?: CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))

            // CPU Chart
            ChartCard(title = "CPU Usage (%)") {
                AndroidView(
                    factory = { context ->
                        LineChart(context).apply {
                            cpuChart.value = this
                            setupChart(this)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }

            // Memory Chart
            ChartCard(title = "Memory Usage (MB)") {
                AndroidView(
                    factory = { context ->
                        LineChart(context).apply {
                            memChart.value = this
                            setupChart(this)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }

            // Network Chart
            ChartCard(title = "Network RX / TX (KB)") {
                AndroidView(
                    factory = { context ->
                        LineChart(context).apply {
                            netChart.value = this
                            setupChart(this)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }

            // Controls
            Text(
                text = "Controls",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStart,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Start")
                }
                Button(
                    onClick = onStop,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Stop")
                }
                Button(
                    onClick = onRestart,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Restart")
                }
            }
        }
    }
}

// Chart setup helper
private fun setupChart(chart: LineChart) {
    chart.apply {
        description.isEnabled = false
        setTouchEnabled(false)
        isDragEnabled = false
        setScaleEnabled(false)
        setDrawGridBackground(false)
        legend.isEnabled = true
        legend.textColor = Color.GRAY
        axisRight.isEnabled = false
        axisLeft.apply {
            textColor = Color.GRAY
            gridColor = Color.parseColor("#333333")
        }
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            textColor = Color.GRAY
            gridColor = Color.parseColor("#333333")
            setDrawLabels(false)
        }
        setBackgroundColor(Color.TRANSPARENT)
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}