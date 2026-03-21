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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.isSystemInDarkTheme
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.DataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.jeet.dockermonitor.data.model.LiveStat
import com.jeet.dockermonitor.ui.theme.CpuChartColor
import com.jeet.dockermonitor.ui.theme.MemoryChartColor
import com.jeet.dockermonitor.ui.theme.NetworkRxColor
import com.jeet.dockermonitor.ui.theme.NetworkTxColor
import com.jeet.dockermonitor.ui.theme.JetBrainsMonoFontFamily

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
    val isDark = isSystemInDarkTheme()
    val colorScheme = MaterialTheme.colorScheme

    // Chart background and text colors based on theme
    val chartBgColor = if (isDark) Color.parseColor("#1A1A1A") else Color.parseColor("#FFFFFF")
    val chartTextColor = if (isDark) Color.parseColor("#AAAAAA") else Color.parseColor("#666666")
    val chartGridColor = if (isDark) Color.parseColor("#333333") else Color.parseColor("#E0E0E0")

    val cpuData = remember { mutableStateListOf<Entry>() }
    val memData = remember { mutableStateListOf<Entry>() }
    val netRxData = remember { mutableStateListOf<Entry>() }
    val netTxData = remember { mutableStateListOf<Entry>() }
    var index by remember { mutableStateOf(0f) }

    val cpuChart = remember { mutableStateOf<LineChart?>(null) }
    val memChart = remember { mutableStateOf<LineChart?>(null) }
    val netChart = remember { mutableStateOf<LineChart?>(null) }

    LaunchedEffect(liveStat) {
        liveStat?.let { stat ->
            cpuData.add(Entry(index, stat.cpu_percent.toFloat()))
            memData.add(Entry(index, stat.memory_usage_mb.toFloat()))
            netRxData.add(Entry(index, stat.network_rx_kb.toFloat()))
            netTxData.add(Entry(index, stat.network_tx_kb.toFloat()))

            if (cpuData.size > 30) cpuData.removeAt(0)
            if (memData.size > 30) memData.removeAt(0)
            if (netRxData.size > 30) netRxData.removeAt(0)
            if (netTxData.size > 30) netTxData.removeAt(0)

            index++

            // CPU chart
            // CPU chart
            cpuChart.value?.apply {
                if (data == null) {
                    val dataSet = LineDataSet(cpuData.toList(), "CPU %").apply {
                        color = CpuChartColor.toArgb()
                        setDrawCircles(false)
                        lineWidth = 2.5f
                        setDrawFilled(true)
                        fillColor = CpuChartColor.toArgb()
                        fillAlpha = 40
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                        setDrawValues(false)
                        isHighlightEnabled = false
                    }
                    data = LineData(dataSet)
                } else {
                    val dataSet = data.getDataSetByIndex(0) as LineDataSet
                    dataSet.values = cpuData.toList()
                    dataSet.notifyDataSetChanged()
                }
                setBackgroundColor(chartBgColor)
                data.notifyDataChanged()
                notifyDataSetChanged()
                setVisibleXRangeMaximum(30f)
                moveViewToX(data.entryCount.toFloat())
            }

            // Memory chart
            memChart.value?.apply {
                if (data == null) {
                    val dataSet = LineDataSet(memData.toList(), "Memory MB").apply {
                        color = MemoryChartColor.toArgb()
                        setDrawCircles(false)
                        lineWidth = 2.5f
                        setDrawFilled(true)
                        fillColor = MemoryChartColor.toArgb()
                        fillAlpha = 40
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                        setDrawValues(false)
                        isHighlightEnabled = false
                    }
                    data = LineData(dataSet)
                }else {
                    val dataSet = data.getDataSetByIndex(0) as LineDataSet
                    dataSet.values = memData.toList()
                    dataSet.notifyDataSetChanged()
                }
                setBackgroundColor(chartBgColor)
                data.notifyDataChanged()
                notifyDataSetChanged()
                setVisibleXRangeMaximum(30f)
                moveViewToX(data.entryCount.toFloat())
            }

            // Network chart
            netChart.value?.apply {
                if (data == null) {
                    val rxSet = LineDataSet(netRxData.toList(), "RX KB").apply {
                        color = NetworkRxColor.toArgb()
                        setDrawCircles(false)
                        lineWidth = 2.5f
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                        setDrawValues(false)
                        isHighlightEnabled = false
                    }
                    val txSet = LineDataSet(netTxData.toList(), "TX KB").apply {
                        color = NetworkTxColor.toArgb()
                        setDrawCircles(false)
                        lineWidth = 2.5f
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                        setDrawValues(false)
                        isHighlightEnabled = false
                    }
                    data = LineData(rxSet, txSet)
                } else {
                    (data.getDataSetByIndex(0) as LineDataSet).values = netRxData.toList()
                    (data.getDataSetByIndex(1) as LineDataSet).values = netTxData.toList()
                    data.notifyDataChanged()
                }
                setBackgroundColor(chartBgColor)
                data.notifyDataChanged()
                notifyDataSetChanged()
                setVisibleXRangeMaximum(30f)
                moveViewToX(data.entryCount.toFloat())
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
                    colorScheme.primary
                else
                    colorScheme.error,
                fontSize = 13.sp,
                fontFamily = JetBrainsMonoFontFamily
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
                        color = colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Memory",
                        value = "${"%.1f".format(stat.memory_usage_mb)} MB",
                        color = colorScheme.secondary,
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
                        color = colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Net TX",
                        value = "${"%.1f".format(stat.network_tx_kb)} KB",
                        color = colorScheme.error,
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
                        color = colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Disk Write",
                        value = "${"%.1f".format(stat.disk_write_kb)} KB",
                        color = colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            } ?: CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // CPU Chart
            ChartCard(title = "CPU Usage (%)") {
                AndroidView(
                    factory = { context ->
                        LineChart(context).apply {
                            cpuChart.value = this
                            setupChart(this, chartTextColor, chartGridColor)
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
                            setupChart(this, chartTextColor, chartGridColor)
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
                            setupChart(this, chartTextColor, chartGridColor)
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
                style = MaterialTheme.typography.titleLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStart,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("Start", fontSize = 12.sp, maxLines = 1)
                }
                Button(
                    onClick = onStop,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("Stop", fontSize = 12.sp, maxLines = 1)
                }
                Button(
                    onClick = onRestart,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("Restart", fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}

private fun setupChart(chart: LineChart, labelColor: Int, gridLineColor: Int) {
    chart.apply {
        description.isEnabled = false
        setTouchEnabled(false)
        setHardwareAccelerationEnabled(true)
        isDragEnabled = false
        setScaleEnabled(false)
        setDrawGridBackground(false)
        isAutoScaleMinMaxEnabled = true
        setVisibleXRangeMaximum(30f)
        legend.apply {
            isEnabled = true
            textColor = labelColor
        }
        axisRight.isEnabled = false
        axisLeft.apply {
            textColor = labelColor
            gridColor = gridLineColor
            gridLineWidth = 0.5f
        }
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            textColor = labelColor
            gridColor = gridLineColor
            gridLineWidth = 0.5f
            setDrawLabels(false)
        }
        setBackgroundColor(Color.TRANSPARENT)
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
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
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontFamily = JetBrainsMonoFontFamily,
                color = color
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
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}