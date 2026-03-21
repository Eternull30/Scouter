package com.jeet.dockermonitor.data


import com.google.gson.Gson
import com.jeet.dockermonitor.data.model.Container
import com.jeet.dockermonitor.data.model.LiveStat
import com.jeet.dockermonitor.data.model.StatHistory
import com.jeet.dockermonitor.data.model.UiState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class DockerRepository(private val api: ApiService, private val wsBaseUrl:String){

    suspend fun getContainers(all: Boolean =false): UiState<List<Container>>{
        return try {
            UiState.Success(api.getContainers(all = all))
        }catch (e: Exception){
            UiState.Error(e.message ?: "Unknown Error")
        }
    }

    suspend fun getStats(containerId: String): UiState<LiveStat>{
        return try{
            UiState.Success(api.getStats(containerId))
        } catch (e: Exception){
            UiState.Error(e.message ?: "Unknown Error")
        }
    }

    suspend fun getHistory(containerId: String): UiState<List<StatHistory>>{
        return try {
            UiState.Success(api.getHistory(containerId))
        } catch (e: Exception){
            UiState.Error(e.message ?: "Unknown Error")
        }
    }
    suspend fun startContainer(containerId: String): UiState<String>{
        return try{
            val res = api.startContainer(containerId)
            UiState.Success(res["message"]?: "Started")
        } catch (e: Exception){
            UiState.Error(e.message?: "Unknown Error")
        }
    }

    suspend fun stopContainer(containerId: String): UiState<String>{
        return try{
            val res = api.stopContainer(containerId)
            UiState.Success(res["message"] ?: "Stopped")
        } catch (e: Exception){
            UiState.Error(e.message ?: "Unknown Error")
        }
    }

    suspend fun restartContainer(containerId: String): UiState<String>{
        return try{
            val res = api.restartContainer(containerId)
            UiState.Success(res["message"] ?: "Restarted")
        } catch (e: Exception){
            UiState.Error(e.message ?: "Unknown Error")
        }
    }

    fun observeLiveStats(containerId: String): Flow<LiveStat> = callbackFlow {
        val gson = Gson()
        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

        val request = Request.Builder()
            .url("$wsBaseUrl/ws/containers/$containerId/stats")
            .build()

        val ws = client.newWebSocket(request,object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val stat = gson.fromJson(text, LiveStat::class.java)
                    trySend(stat)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            override fun onFailure(webSocket: WebSocket,t: Throwable, response: Response ?){
                close(t)
            }
            override fun onClosed(websocket: WebSocket, code:Int, reason: String){
                close()
            }
        })

        awaitClose {
            ws.close(1000, "Screen Closed")
        }
    }
}