package com.jeet.dockermonitor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SetupScreen(onConnect:(address: String, port: Int) -> Unit){
    var address by remember() { mutableStateOf("") }
    var port by remember() {mutableStateOf("8000")}
    var isLoading by remember() {mutableStateOf(false)}
    var error by remember() {mutableStateOf("")}

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Dns,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Docker-Monitor",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = address,
            onValueChange = {address = it},
            label = { Text("Host IP Address")},
            placeholder = { Text("e.g. 192.168.1.100")},
            keyboardOptions = KeyboardOptions(keyboardType= KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = port,
            onValueChange = {port = it},
            label = {Text("Backend Port")},
            placeholder = {Text("8000")},
            keyboardOptions = KeyboardOptions(keyboardType= KeyboardType.Number),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

//        error message
        if (error.isNotEmpty()){
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp
            )}

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = {
                if (address.isBlank()) {
                    error = "Please Enter a Host Address"
                    return@Button
                }
                val portInt = port.toIntOrNull()
                if (portInt == null){
                    error = "Invalid port number"
                    return@Button
                }
                error = ""
                isLoading = true
                onConnect(address.trim(),portInt)
            },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isLoading
            ){
                if(isLoading){
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else{
                    Text("Connect", fontSize = 16.sp)
                }
            }

    }
 }