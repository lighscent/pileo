package com.pileo.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pileo.data.SettingsPrefs
import com.pileo.notification.NotificationHelper
import com.pileo.ui.theme.PileoTheme
import kotlin.math.roundToInt

/** Full-screen alarm UI shown over the lock screen when a reminder fires. */
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val medId = intent.getLongExtra(AlarmService.EXTRA_MED_ID, -1L)
        val name = intent.getStringExtra(AlarmService.EXTRA_NAME) ?: "Médicament"
        val dosage = intent.getStringExtra(AlarmService.EXTRA_DOSAGE) ?: ""
        val timeLabel = intent.getStringExtra(AlarmService.EXTRA_TIME_LABEL) ?: ""
        val nid = intent.getIntExtra(AlarmService.EXTRA_NOTIFICATION_ID, 0)
        val delay = SettingsPrefs.snoozeMinutes(this)

        setContent {
            PileoTheme {
                AlarmScreen(
                    name = name,
                    dosage = dosage,
                    timeLabel = timeLabel,
                    delayMinutes = delay,
                    onTaken = {
                        sendAction(NotificationHelper.ACTION_MARK_TAKEN, medId, nid, name, dosage, timeLabel)
                        finish()
                    },
                    onSnooze = {
                        sendAction(NotificationHelper.ACTION_SNOOZE, medId, nid, name, dosage, timeLabel)
                        finish()
                    }
                )
            }
        }
    }

    private fun sendAction(
        action: String, medId: Long, nid: Int,
        name: String, dosage: String, timeLabel: String
    ) {
        val i = Intent(this, AlarmActionReceiver::class.java).apply {
            this.action = action
            putExtra(AlarmScheduler.EXTRA_MED_ID, medId)
            putExtra(AlarmScheduler.EXTRA_NOTIFICATION_ID, nid)
            putExtra(AlarmScheduler.EXTRA_NAME, name)
            putExtra(AlarmScheduler.EXTRA_DOSAGE, dosage)
            putExtra(AlarmScheduler.EXTRA_TIME_LABEL, timeLabel)
        }
        sendBroadcast(i)
    }
}

@Composable
private fun AlarmScreen(
    name: String,
    dosage: String,
    timeLabel: String,
    delayMinutes: Int,
    onTaken: () -> Unit,
    onSnooze: () -> Unit
) {
    // Glisser toute la fenêtre vers le haut pour rappeler plus tard.
    var offsetY by remember { mutableFloatStateOf(0f) }
    val dismissPx = -280f

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, offsetY.roundToInt()) }
                .pointerInput(delayMinutes) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (offsetY <= dismissPx) onSnooze()
                            offsetY = 0f
                        },
                        onDragCancel = { offsetY = 0f }
                    ) { _, dragAmount ->
                        offsetY = (offsetY + dragAmount).coerceIn(dismissPx * 1.5f, 0f)
                    }
                }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "C'est l'heure de votre médicament",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (dosage.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(dosage, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                }
                if (timeLabel.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        timeLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(40.dp))

                Button(
                    onClick = onTaken,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E9E6B),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.size(10.dp))
                    Text("J'ai pris mes médicaments", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(Modifier.height(32.dp))

                SnoozeHint(delayMinutes = delayMinutes, onSnooze = onSnooze)
            }
        }
    }
}

@Composable
private fun SnoozeHint(delayMinutes: Int, onSnooze: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onSnooze).padding(12.dp)
    ) {
        Icon(
            Icons.Default.KeyboardArrowUp,
            contentDescription = null,
            tint = Color(0xFF00ACC1),
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Glissez vers le haut pour rappeler dans $delayMinutes min",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF00838F),
            textAlign = TextAlign.Center
        )
    }
}
