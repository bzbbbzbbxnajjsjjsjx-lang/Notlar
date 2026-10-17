package com.vardiya.notlar.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.vardiya.notlar.MainActivity
import com.vardiya.notlar.R
import com.vardiya.notlar.domain.Note

/** Posts the notification of a reminder; tapping it opens the note. */
object ReminderNotifier {

    private const val CHANNEL = "reminders"
    private const val PREVIEW_ITEMS = 5

    fun show(context: Context, note: Note, private: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createChannel(context)
        val open = PendingIntent.getActivity(
            context,
            0,
            MainActivity.openNoteIntent(context, note.id).setData(Uri.parse("notlar://note/${note.id}")),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // What the lock screen and a locked app show: no title, no text.
        val hidden = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Notlar")
            .setContentText("Bir hatırlatıcın var")
            .build()
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(hidden)
        if (private) {
            builder.setContentTitle("Notlar").setContentText("Bir hatırlatıcın var")
        } else {
            val text = preview(note)
            builder.setContentTitle(note.title.trim().ifEmpty { "Hatırlatıcı" })
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        }
        NotificationManagerCompat.from(context).notify(note.id.toString(), 0, builder.build())
    }

    /** The note text, or its open to-do items, for the notification body. */
    private fun preview(note: Note): String = if (note.isChecklist) {
        note.items.filter { !it.checked && it.text.isNotBlank() }.take(PREVIEW_ITEMS).joinToString("\n") { "☐ " + it.text }
    } else {
        note.body.trim()
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, "Hatırlatıcılar", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Notlarına kurduğun hatırlatıcılar"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
