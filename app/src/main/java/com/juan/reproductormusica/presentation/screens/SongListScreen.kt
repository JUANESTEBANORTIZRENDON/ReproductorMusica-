package com.juan.reproductormusica.presentation.screens

import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juan.reproductormusica.R
import com.juan.reproductormusica.data.Song
import com.juan.reproductormusica.service.MusicService
import kotlinx.coroutines.delay
import java.io.File

private val MusicFont = FontFamily(Font(R.font.montserrat_medium))

@Composable
fun SongListScreen(canciones: List<Song>) {
    val total = canciones.size
    val agrupadas = canciones.groupBy {
        File(it.data).parentFile?.name ?: "Desconocida"
    }.toSortedMap()

    var currentSong by remember { mutableStateOf<Song?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentPosition by remember { mutableStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(mediaPlayer, isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let {
                currentPosition = it.currentPosition
            }
            delay(500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Total: $total canciones",
            style = TextStyle(
                fontFamily = MusicFont,
                color = Color.White,
                fontSize = 18.sp
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            agrupadas.forEach { (carpeta, lista) ->
                item {
                    Text(
                        text = carpeta.uppercase(),
                        style = TextStyle(
                            fontFamily = MusicFont,
                            color = Color(0xFF1E1E1E),
                            fontSize = 14.sp
                        ),
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
                items(lista) { song ->
                    SongItem(song, onClick = {
                        try {
                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                        } catch (_: Exception) {}

                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(song.data)
                            prepare()
                            start()
                        }
                        currentSong = song
                        isPlaying = true

                        val intent = Intent(context, MusicService::class.java).apply {
                            putExtra("title", song.title)
                            putExtra("path", song.data)
                            action = "ACTION_START"
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    })
                }
            }
        }

        currentSong?.let { song ->
            MusicPlayer(
                song = song,
                isPlaying = isPlaying,
                position = currentPosition,
                duration = mediaPlayer?.duration ?: song.duration.toInt(),
                onToggle = {
                    if (isPlaying) mediaPlayer?.pause() else mediaPlayer?.start()
                    isPlaying = !isPlaying

                    val action = if (isPlaying) "ACTION_PLAY" else "ACTION_PAUSE"
                    val intent = Intent(context, MusicService::class.java).apply { this.action = action }
                    context.startService(intent)
                },
                onNext = {
                    val allSongs = canciones
                    val currentIndex = allSongs.indexOf(song)
                    if (currentIndex < allSongs.lastIndex) {
                        val nextSong = allSongs[currentIndex + 1]
                        mediaPlayer?.stop()
                        mediaPlayer?.release()
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(nextSong.data)
                            prepare()
                            start()
                        }
                        currentSong = nextSong
                        isPlaying = true

                        val intent = Intent(context, MusicService::class.java).apply {
                            putExtra("title", nextSong.title)
                            putExtra("path", nextSong.data)
                            action = "ACTION_START"
                        }
                        context.startService(intent)
                    }
                },
                onPrevious = {
                    val allSongs = canciones
                    val currentIndex = allSongs.indexOf(song)
                    if (currentIndex > 0) {
                        val prevSong = allSongs[currentIndex - 1]
                        mediaPlayer?.stop()
                        mediaPlayer?.release()
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(prevSong.data)
                            prepare()
                            start()
                        }
                        currentSong = prevSong
                        isPlaying = true

                        val intent = Intent(context, MusicService::class.java).apply {
                            putExtra("title", prevSong.title)
                            putExtra("path", prevSong.data)
                            action = "ACTION_START"
                        }
                        context.startService(intent)
                    }
                },
                onSeekTo = { pos ->
                    mediaPlayer?.seekTo(pos)
                    currentPosition = pos
                }
            )
        }
    }
}

@Composable
fun SongItem(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color(0xFF3B0909))
            .padding(1.dp)
            .background(Color(0xFF5F100D))
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        Text(
            text = "🎵 ${song.title}",
            style = TextStyle(
                fontFamily = MusicFont,
                color = Color.White,
                fontSize = 16.sp
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "${File(song.data).parentFile?.name ?: "-"}",
                color = Color(0xFFCCCCCC),
                style = TextStyle(fontFamily = MusicFont, fontSize = 12.sp)
            )
            Text(
                text = formatDuration(song.duration),
                color = Color(0xFFCCCCCC),
                style = TextStyle(fontFamily = MusicFont, fontSize = 12.sp)
            )
        }
    }
}

@Composable
fun MusicPlayer(
    song: Song,
    isPlaying: Boolean,
    position: Int,
    duration: Int,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF3B0909))
            .padding(12.dp)
    ) {
        Text(
            text = "📀 ${song.title}",
            style = TextStyle(
                fontFamily = MusicFont,
                color = Color.White,
                fontSize = 16.sp
            )
        )

        Slider(
            value = position.toFloat(),
            onValueChange = { newValue -> onSeekTo(newValue.toInt()) },
            valueRange = 0f..duration.toFloat(),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = formatDuration(position.toLong()),
                style = TextStyle(fontFamily = MusicFont, color = Color.LightGray, fontSize = 12.sp)
            )
            IconButton(onClick = onPrevious) {
                Icon(painter = painterResource(R.drawable.ic_previous), contentDescription = "Anterior", tint = Color.White)
            }
            IconButton(onClick = onToggle) {
                Icon(
                    painter = painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                    contentDescription = "Play/Pause",
                    tint = Color.White
                )
            }
            IconButton(onClick = onNext) {
                Icon(painter = painterResource(R.drawable.ic_next), contentDescription = "Siguiente", tint = Color.White)
            }
            Text(
                text = formatDuration(duration.toLong()),
                style = TextStyle(fontFamily = MusicFont, color = Color.LightGray, fontSize = 12.sp)
            )
        }
    }
}

fun formatDuration(durationMs: Long): String {
    return DateUtils.formatElapsedTime(durationMs / 1000)
}






