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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

class MainActivity : ComponentActivity() {
    private val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "streak-database"
        )
        .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
        .build()
    }

    private val repository by lazy {
        StreakRepository(database.streakDao(), database.attemptDao(), database.urgeDao())
    }

    private val quotes: List<String> by lazy {
        try {
            val json = applicationContext.assets.open("quotes.json").bufferedReader().use { it.readText() }
            org.json.JSONArray(json).let { jsonArray ->
                List(jsonArray.length()) { jsonArray.getString(it) }
            }
        } catch (e: Exception) {
            listOf("STAY STRONG.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: StreakViewModel =
                    viewModel(factory = StreakViewModel.Factory(repository))
                StreakApp(viewModel, quotes)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakApp(viewModel: StreakViewModel, quotes: List<String>) {
    val pressStartFont = FontFamily(Font(R.font.press_start_2p))
    val vt323Font = FontFamily(Font(R.font.vt323))

    val record by viewModel.streakRecord.collectAsStateWithLifecycle()
    val todayUrgeCount by viewModel.todayUrgeCount.collectAsStateWithLifecycle()
    val weeklyUrgeCount by viewModel.weeklyUrgeCount.collectAsStateWithLifecycle()
    val last7DaysUrges by viewModel.last7DaysUrges.collectAsStateWithLifecycle()
    var currentDays by remember { mutableIntStateOf(0) }
    var currentHours by remember { mutableIntStateOf(0) }
    var currentMinutes by remember { mutableIntStateOf(0) }
    var currentSeconds by remember { mutableIntStateOf(0) }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(record) {
        while (true) {
            val r = record
            if (r != null) {
                val diff = System.currentTimeMillis() - r.streakStartDateMillis
                currentDays = TimeUnit.MILLISECONDS.toDays(diff).toInt()
                currentHours = (TimeUnit.MILLISECONDS.toHours(diff) % 24).toInt()
                currentMinutes = (TimeUnit.MILLISECONDS.toMinutes(diff) % 60).toInt()
                currentSeconds = (TimeUnit.MILLISECONDS.toSeconds(diff) % 60).toInt()
            }
            delay(1000)
        }
    }

    val textColor = MaterialTheme.colorScheme.onBackground
    val mutedColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val history by viewModel.history.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { showBottomSheet = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "[ VIEW HISTORY ]",
                    fontFamily = vt323Font,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = mutedColor,
                    letterSpacing = 2.sp
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
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

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.Bottom
                        ) {
                        Text(
                            text = "$currentDays",
                            fontFamily = pressStartFont,
                            fontSize = 110.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-4).sp,
                            lineHeight = 110.sp,
                            color = textColor
                        )
                        Box(
                            modifier = Modifier
                                .padding(bottom = 26.dp, start = 8.dp)
                                .width(36.dp)
                                .height(20.dp)
                                .background(androidx.compose.ui.graphics.Color(0xFFD12626).copy(alpha = cursorAlpha))
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "DAYS",
                        fontFamily = vt323Font,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 16.sp,
                        color = textColor
                    )
                     
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${currentHours}H ${currentMinutes}M ${currentSeconds}S",
                        fontFamily = vt323Font,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 4.sp,
                        color = mutedColor
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    val todayHash = (System.currentTimeMillis() / (1000 * 60 * 60 * 24)).toInt()
                    val quoteOfDay = if (quotes.isNotEmpty()) quotes[Math.abs(todayHash) % quotes.size] else "STAY STRONG."

                    Text(
                        text = "\"$quoteOfDay\"",
                        fontFamily = vt323Font,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.sp,
                        color = textColor,
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    val milestones = listOf(7, 14, 30, 90, 180, 365)
                    val nextMilestone = milestones.firstOrNull { it > currentDays } ?: (currentDays + 30) // fallback if > 365
                    val prevMilestone = milestones.lastOrNull { it <= currentDays } ?: 0
                    
                    val progress = if (currentDays >= 365 && nextMilestone == currentDays) 1f else {
                        val totalRange = nextMilestone - prevMilestone
                        val currentProgress = currentDays - prevMilestone
                        currentProgress.toFloat() / totalRange.toFloat()
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = textColor,
                        trackColor = mutedColor.copy(alpha = 0.2f),
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "NEXT: $nextMilestone",
                        fontFamily = vt323Font,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp,
                        color = mutedColor
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    } // End of top-centered Column

                    val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                    val formattedDate = dateFormat.format(Date(record!!.streakStartDateMillis))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("RECORD", fontFamily = vt323Font, fontWeight = FontWeight.Bold, color = mutedColor, fontSize = 12.sp, letterSpacing = 2.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("${record!!.longestStreakDays}", fontFamily = vt323Font, fontWeight = FontWeight.Bold, color = textColor, fontSize = 20.sp, letterSpacing = 2.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("EPOCH", fontFamily = vt323Font, fontWeight = FontWeight.Bold, color = mutedColor, fontSize = 12.sp, letterSpacing = 2.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(formattedDate, fontFamily = vt323Font, fontWeight = FontWeight.Bold, color = textColor, fontSize = 20.sp, letterSpacing = 2.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "[ + URGE ]",
                                fontFamily = vt323Font,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = textColor,
                                modifier = Modifier.clickable { viewModel.addUrge() }.padding(end = 8.dp, top = 8.dp, bottom = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "TODAY: $todayUrgeCount  |  WEEK: $weeklyUrgeCount",
                                fontFamily = vt323Font,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = mutedColor,
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Canvas(modifier = Modifier.width(120.dp).height(24.dp)) {
                                val maxUrges = (last7DaysUrges.maxOrNull() ?: 0).coerceAtLeast(1)
                                val barWidth = size.width / 13f // 7 bars + 6 spaces
                                val spacing = barWidth
                                
                                last7DaysUrges.forEachIndexed { index, count ->
                                    val barHeight = (count.toFloat() / maxUrges.toFloat()) * size.height
                                    // ensure minimum height for visibility
                                    val finalHeight = if (count > 0) barHeight.coerceAtLeast(2.dp.toPx()) else 2.dp.toPx()
                                    val color = if (index == 6) textColor else mutedColor
                                    val startX = index * (barWidth + spacing)
                                    val startY = size.height - finalHeight
                                    
                                    drawRect(
                                        color = color,
                                        topLeft = Offset(startX, startY),
                                        size = Size(barWidth, finalHeight)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "[ RESET ]",
                            fontFamily = vt323Font,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = textColor,
                            modifier = Modifier.clickable { showResetDialog = true }.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
        
        if (showResetDialog) {
            var countdown by remember { mutableIntStateOf(3) }
            LaunchedEffect(showResetDialog) {
                while (countdown > 0) {
                    delay(1000)
                    countdown--
                }
            }
            
            androidx.compose.ui.window.Dialog(onDismissRequest = { showResetDialog = false }) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .border(2.dp, MaterialTheme.colorScheme.onBackground)
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "TERMINATE STREAK?",
                            fontFamily = vt323Font,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "THIS ACTION CANNOT BE UNDONE.",
                            fontFamily = vt323Font,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = mutedColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text(
                            text = if (countdown > 0) "[ CONFIRM ($countdown) ]" else "[ CONFIRM ]",
                            fontFamily = vt323Font,
                            fontWeight = FontWeight.Bold,
                            color = if (countdown > 0) mutedColor else androidx.compose.ui.graphics.Color(0xFFD12626),
                            modifier = Modifier.clickable(enabled = countdown == 0) {
                                viewModel.resetStreak()
                                showResetDialog = false
                            }.padding(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "[ CANCEL ]",
                            fontFamily = vt323Font,
                            fontWeight = FontWeight.Bold,
                            color = mutedColor,
                            modifier = Modifier.clickable { showResetDialog = false }.padding(12.dp)
                        )
                    }
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.background,
                dragHandle = { BottomSheetDefaults.DragHandle(color = mutedColor) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "ATTEMPT HISTORY",
                        fontFamily = vt323Font,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    if (history.isEmpty()) {
                        Text(
                            text = "NO PRIOR ATTEMPTS. STAY STRONG.",
                            fontFamily = vt323Font,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = mutedColor
                        )
                    } else {
                        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                        history.forEach { attempt ->
                            val startStr = dateFormat.format(Date(attempt.startMillis))
                            val endStr = dateFormat.format(Date(attempt.endMillis))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "$startStr -> $endStr",
                                        fontFamily = vt323Font,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = mutedColor
                                    )
                                }
                                Text(
                                    text = "${attempt.daysAchieved} DAYS",
                                    fontFamily = vt323Font,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                            HorizontalDivider(color = mutedColor.copy(alpha = 0.2f))
                        }
                    }
                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
    }
}
