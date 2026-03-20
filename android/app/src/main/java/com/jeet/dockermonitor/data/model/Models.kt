package com.jeet.dockermonitor.data.model

data class Container(
    val id: String,
    val full_id: String,
    val name: String,
    val image: String,
    val status: String,
    val state: String,
)

data class LiveStat(
    val container_name: String,
    val cpu_percent: Double,
    val memory_usage_mb: Double,
    val memory_limit_mb: Double,
    val memory_percent: Double,
    val network_rx_kb: Double,
    val network_tx_kb: Double,
    val disk_read_kb: Double,
    val disk_write_kb: Double,
    val timestamp: String,
)

data class StatHistory(
    val id: Int,
    val container_id: Int,
    val cpu_percent: Double,
    val memory_usage: Long,
    val memory_limit: Long,
    val network_rx: Long,
    val network_tx: Long,
    val disk_read: Long,
    val disk_write: Long,
    val recorded_at: String,
)

data class DockerHost(
    val address: String,
    val port: Int = 2375,
){
    fun baseUrl() = "http://$address:8000/"
    fun wsUrl() = "ws://$address:8000"
}

sealed class UiState<out T>{
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
