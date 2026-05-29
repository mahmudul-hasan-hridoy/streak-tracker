package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.StreakRepository
import com.example.ui.StreakViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "streak-database"
        ).build()
    }

    private val repository by lazy {
        StreakRepository(database.streakDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: StreakViewModel =
                    viewModel(factory = StreakViewModel.Factory(repository))
                StreakApp(viewModel)
            }
        }
    }
}

@Composable
fun StreakApp(viewModel: StreakViewModel) {
    val record by viewModel.streakRecord.collectAsStateWithLifecycle()
    var currentDays by remember { mutableIntStateOf(0) }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(record) {
        while (true) {
            val r = record
            if (r != null) {
                val diff = System.currentTimeMillis() - r.streakStartDateMillis
                currentDays = TimeUnit.MILLISECONDS.toDays(diff).toInt()
            }
            delay(1000 * 60) // Check every minute
        }
    }

    val textColor = MaterialTheme.colorScheme.onBackground
    val mutedColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (record != null) {
                val infiniteTransition = rememberInfiniteTransition(label = "blink")
                val cursorAlpha by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = keyframes {
                            durationMillis = 1000
                            0f at 0
                            0f at 499
                            1f at 500
                            1f at 1000
                        },
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "cursorAlpha"
                )

                // CENTER
                Row(
                    modifier = Modifier.align(Alignment.Center).offset(y = (-40).dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$currentDays",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 130.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-6).sp,
                        lineHeight = 130.sp,
                        color = textColor
                    )
                    Box(
                        modifier = Modifier
                            .padding(bottom = 26.dp, start = 8.dp)
                            .width(36.dp)
                            .height(20.dp)
                            .background(textColor.copy(alpha = cursorAlpha))
                    )
                }

                // BOTTOM CONTENT
                Column(
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                     Text(
                        text = "DAYS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 16.sp,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                    val formattedDate = dateFormat.format(Date(record!!.streakStartDateMillis))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("RECORD", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = mutedColor, fontSize = 12.sp, letterSpacing = 2.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("${record!!.longestStreakDays}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = textColor, fontSize = 20.sp, letterSpacing = 2.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("EPOCH", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = mutedColor, fontSize = 12.sp, letterSpacing = 2.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(formattedDate, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = textColor, fontSize = 20.sp, letterSpacing = 2.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(64.dp))

                    Text(
                        text = "[ RESET ]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        color = textColor,
                        modifier = Modifier
                            .clickable { showResetDialog = true }
                            .padding(8.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
        
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = {
                    Text(
                        text = "TERMINATE STREAK?",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                },
                text = {
                    Text(
                        text = "THIS ACTION CANNOT BE UNDONE.",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                },
                confirmButton = {
                    Text(
                        text = "[ CONFIRM ]",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.clickable {
                            viewModel.resetStreak()
                            showResetDialog = false
                        }.padding(16.dp)
                    )
                },
                dismissButton = {
                    Text(
                        text = "[ CANCEL ]",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.clickable { showResetDialog = false }.padding(16.dp)
                    )
                },
                shape = RectangleShape,
                containerColor = MaterialTheme.colorScheme.background,
                textContentColor = MaterialTheme.colorScheme.onBackground,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.border(2.dp, MaterialTheme.colorScheme.onBackground)
            )
        }
    }
}
