package com.juan.reproductormusica.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@Composable
fun SleepTimerDialog(
    isActive: Boolean,
    remainingTime: Long,
    onDismiss: () -> Unit,
    onSetTimer: (Int, Int) -> Unit,
    onSetTimerInMinutes: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    formatRemainingTime: (Long) -> String
) {
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MINUTE)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color(0xFFB71C1C),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Temporizador de Suspensión",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                // Estado actual del temporizador
                if (isActive) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2A0A0A)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Temporizador Activo",
                                color = Color(0xFFB71C1C),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = formatRemainingTime(remainingTime),
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    onCancelTimer()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFB71C1C)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cancelar Temporizador")
                            }
                        }
                    }
                } else {
                    // Opciones de temporizador
                    Text(
                        text = "Selecciona cuándo quieres que se detenga la reproducción:",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(300.dp)
                    ) {
                        // Opciones rápidas por minutos
                        val quickOptions = listOf(15, 30, 45, 60, 90, 120)
                        items(quickOptions) { minutes ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSetTimerInMinutes(minutes)
                                        onDismiss()
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF1A0000)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "En $minutes minutos",
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }

                        // Opción para seleccionar hora específica
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showTimePicker = true },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF2A0A0A)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = Color(0xFFB71C1C),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Seleccionar hora específica",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = Color.White)
            }
        },
        containerColor = Color(0xFF0D0D0D)
    )

    // Diálogo para seleccionar hora específica
    if (showTimePicker) {
        TimePickerDialog(
            hour = selectedHour,
            minute = selectedMinute,
            onTimeSelected = { hour, minute ->
                selectedHour = hour
                selectedMinute = minute
                onSetTimer(hour, minute)
                showTimePicker = false
                onDismiss()
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
private fun TimePickerDialog(
    hour: Int,
    minute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableStateOf(hour) }
    var selectedMinute by remember { mutableStateOf(minute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Seleccionar Hora",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            // Selector de hora y minuto compacto
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Selector de hora
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Hora", color = Color.Gray, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { 
                                selectedHour = if (selectedHour > 0) selectedHour - 1 else 23 
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("-", color = Color.White, fontSize = 16.sp)
                        }
                        Text(
                            text = String.format("%02d", selectedHour),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(
                                    Color(0xFF2A0A0A),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        IconButton(
                            onClick = { 
                                selectedHour = if (selectedHour < 23) selectedHour + 1 else 0 
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("+", color = Color.White, fontSize = 16.sp)
                        }
                    }
                }

                Text(":", color = Color.White, fontSize = 18.sp)

                // Selector de minuto
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Minuto", color = Color.Gray, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { 
                                selectedMinute = if (selectedMinute > 0) selectedMinute - 1 else 59 
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("-", color = Color.White, fontSize = 16.sp)
                        }
                        Text(
                            text = String.format("%02d", selectedMinute),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(
                                    Color(0xFF2A0A0A),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        IconButton(
                            onClick = { 
                                selectedMinute = if (selectedMinute < 59) selectedMinute + 1 else 0 
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("+", color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onTimeSelected(selectedHour, selectedMinute) }
            ) {
                Text("Confirmar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.White)
            }
        },
        containerColor = Color(0xFF0D0D0D)
    )
}
