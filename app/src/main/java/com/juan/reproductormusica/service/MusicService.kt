package com.juan.reproductormusica.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.juan.reproductormusica.R
import com.juan.reproductormusica.data.Song

class MusicService : Service() {

    companion object {
        const val CHANNEL_ID = "music_channel"
        const val NOTIF_ID = 1
        var mediaPlayer: MediaPlayer? = null
        var currentSong: Song? = null
        var isPlaying: Boolean = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_PLAY" -> {
                mediaPlayer?.start()
                isPlaying = true
            }
            "ACTION_PAUSE" -> {
                mediaPlayer?.pause()
                isPlaying = false
            }
            "ACTION_NEXT" -> sendBroadcast(Intent("com.juan.ACTION_NEXT"))
            "ACTION_PREV" -> sendBroadcast(Intent("com.juan.ACTION_PREV"))
            "ACTION_CLOSE" -> stopSelf()
            else -> {
                val title = intent?.getStringExtra("title") ?: "Reproduciendo"
                val path = intent?.getStringExtra("path")
                if (path != null) {
                    stopAndReleasePlayer()
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(path)
                        prepare()
                        start()
                    }
                    currentSong = Song(0, title, "", path, mediaPlayer!!.duration.toLong())
                    isPlaying = true
                }
                createNotificationChannel()
                startForeground(NOTIF_ID, buildNotification())
            }
        }
        updateNotification()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reproductor",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                R.drawable.ic_pause,
                "Pausa",
                getPendingIntent("ACTION_PAUSE")
            )
        } else {
            NotificationCompat.Action(
                R.drawable.ic_play,
                "Play",
                getPendingIntent("ACTION_PLAY")
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎵 ${currentSong?.title}")
            .setSmallIcon(R.drawable.ic_music_note)
            .setStyle(MediaStyle())
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_previous,
                    "Anterior",
                    getPendingIntent("ACTION_PREV")
                )
            )
            .addAction(playPauseAction)
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_next,
                    "Siguiente",
                    getPendingIntent("ACTION_NEXT")
                )
            )
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_close,
                    "Cerrar",
                    getPendingIntent("ACTION_CLOSE")
                )
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply { this.action = action }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID, buildNotification())
    }

    private fun stopAndReleasePlayer() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
        isPlaying = false
    }

    override fun onDestroy() {
        stopAndReleasePlayer()
        stopForeground(true)
        super.onDestroy()
    }
}



