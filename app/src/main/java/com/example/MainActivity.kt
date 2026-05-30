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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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

import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result if needed
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
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        val app = application as ZeroApplication
        setContent {
            MyApplicationTheme {
                val viewModel: StreakViewModel =
                    viewModel(factory = StreakViewModel.Factory(app.repository, app.preferencesManager))
                StreakApp(viewModel, quotes)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakApp(viewModel: StreakViewModel, quotes: List<String>) {
    val pressStartFont = FontFamily.Monospace
    val vt323Font = FontFamily.Monospace
    
    val recordState by viewModel.streakRecord.collectAsStateWithLifecycle()
    val elapsedTime by viewModel.elapsedTime.collectAsStateWithLifecycle()
    val displayedRecord by viewModel.displayedRecord.collectAsStateWithLifecycle()
    val todayUrgeCount by viewModel.todayUrgeCount.collectAsStateWithLifecycle()
    val weeklyUrgeCount by viewModel.weeklyUrgeCount.collectAsStateWithLifecycle()
    val last7DaysUrges by viewModel.last7DaysUrges.collectAsStateWithLifecycle()
    val lastCelebratedMilestone by viewModel.lastCelebratedMilestone.collectAsStateWithLifecycle()
    
    val currentDays = elapsedTime.days
    val currentHours = elapsedTime.hours
    val currentMinutes = elapsedTime.minutes
    val currentSeconds = elapsedTime.seconds
    
    var showResetDialog by remember { mutableStateOf(false) }

    val textColor = MaterialTheme.colorScheme.onBackground
    val mutedColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val history by viewModel.history.collectAsStateWithLifecycle()

    val milestones = listOf(7, 14, 30, 90, 180, 365)
    var milestoneToCelebrate by remember { mutableStateOf<Int?>(null) }
    
    LaunchedEffect(currentDays, lastCelebratedMilestone) {
        val reachedMilestones = milestones.filter { currentDays >= it }
        val maxReached = reachedMilestones.maxOrNull() ?: 0
        if (maxReached > lastCelebratedMilestone) {
            milestoneToCelebrate = maxReached
        }
    }

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
            val currentRecord = recordState
            if (currentRecord == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = textColor, strokeWidth = 1.dp)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        StatsSection(
                            currentDays = currentDays,
                            currentHours = currentHours,
                            currentMinutes = currentMinutes,
                            currentSeconds = currentSeconds,
                            textColor = textColor,
                            mutedColor = mutedColor,
                            pressStartFont = pressStartFont,
                            vt323Font = vt323Font
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        QuoteSection(
                            quotes = quotes,
                            textColor = textColor,
                            vt323Font = vt323Font
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        MilestoneSection(
                            currentDays = currentDays,
                            textColor = textColor,
                            mutedColor = mutedColor,
                            vt323Font = vt323Font
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    RecordEpochSection(
                        displayedRecord = displayedRecord,
                        streakStartDateMillis = currentRecord.streakStartDateMillis,
                        textColor = textColor,
                        mutedColor = mutedColor,
                        vt323Font = vt323Font
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    UrgeSection(
                        todayUrgeCount = todayUrgeCount,
                        weeklyUrgeCount = weeklyUrgeCount,
                        last7DaysUrges = last7DaysUrges,
                        onAddUrge = { viewModel.addUrge() },
                        onResetStreak = { showResetDialog = true },
                        textColor = textColor,
                        mutedColor = mutedColor,
                        vt323Font = vt323Font
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
        
        if (showResetDialog) {
            ResetDialog(
                onConfirm = { 
                    viewModel.resetStreak()
                    showResetDialog = false
                },
                onDismiss = { showResetDialog = false },
                textColor = textColor,
                mutedColor = mutedColor,
                vt323Font = vt323Font
            )
        }

        if (showBottomSheet) {
            HistoryBottomSheet(
                history = history,
                sheetState = sheetState,
                onDismiss = { showBottomSheet = false },
                textColor = textColor,
                mutedColor = mutedColor,
                vt323Font = vt323Font
            )
        }

        milestoneToCelebrate?.let { milestone ->
            MilestoneCelebrationOverlay(
                milestone = milestone,
                onDismiss = {
                    viewModel.setLastCelebratedMilestone(milestone)
                    milestoneToCelebrate = null
                },
                textColor = textColor,
                vt323Font = vt323Font
            )
        }
    }
}

@Composable
fun StatsSection(
    currentDays: Int,
    currentHours: Int,
    currentMinutes: Int,
    currentSeconds: Int,
    textColor: androidx.compose.ui.graphics.Color,
    mutedColor: androidx.compose.ui.graphics.Color,
    pressStartFont: FontFamily,
    vt323Font: FontFamily
) {
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
}

@Composable
fun QuoteSection(
    quotes: List<String>,
    textColor: androidx.compose.ui.graphics.Color,
    vt323Font: FontFamily
) {
    val todayHash = (System.currentTimeMillis() / (1000 * 60 * 60 * 24)).toInt()
    val quoteIndex = (todayHash and Int.MAX_VALUE) % quotes.size
    val quoteOfDay = if (quotes.isNotEmpty()) quotes[quoteIndex] else "STAY STRONG."

    Text(
        text = "\"$quoteOfDay\"",
        fontFamily = vt323Font,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 1.sp,
        color = textColor,
        textAlign = TextAlign.Start
    )
}

@Composable
fun MilestoneSection(
    currentDays: Int,
    textColor: androidx.compose.ui.graphics.Color,
    mutedColor: androidx.compose.ui.graphics.Color,
    vt323Font: FontFamily
) {
    val milestones = listOf(7, 14, 30, 90, 180, 365)
    val nextMilestone = milestones.firstOrNull { it > currentDays } ?: (currentDays + 30) // fallback if > 365
    val prevMilestone = milestones.lastOrNull { it <= currentDays } ?: 0
    
    val progress = if (nextMilestone == prevMilestone) 1f else {
        ((currentDays - prevMilestone).toFloat() / (nextMilestone - prevMilestone).toFloat()).coerceIn(0f, 1f)
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
}

@Composable
fun RecordEpochSection(
    displayedRecord: Int,
    streakStartDateMillis: Long,
    textColor: androidx.compose.ui.graphics.Color,
    mutedColor: androidx.compose.ui.graphics.Color,
    vt323Font: FontFamily
) {
    val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(streakStartDateMillis))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("RECORD", fontFamily = vt323Font, fontWeight = FontWeight.Bold, color = mutedColor, fontSize = 12.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(4.dp))
            Text("$displayedRecord", fontFamily = vt323Font, fontWeight = FontWeight.Bold, color = textColor, fontSize = 20.sp, letterSpacing = 2.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("EPOCH", fontFamily = vt323Font, fontWeight = FontWeight.Bold, color = mutedColor, fontSize = 12.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(4.dp))
            Text(formattedDate, fontFamily = vt323Font, fontWeight = FontWeight.Bold, color = textColor, fontSize = 20.sp, letterSpacing = 2.sp)
        }
    }
}

@Composable
fun UrgeSection(
    todayUrgeCount: Int,
    weeklyUrgeCount: Int,
    last7DaysUrges: List<Int>,
    onAddUrge: () -> Unit,
    onResetStreak: () -> Unit,
    textColor: androidx.compose.ui.graphics.Color,
    mutedColor: androidx.compose.ui.graphics.Color,
    vt323Font: FontFamily
) {
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
                    modifier = Modifier.minimumInteractiveComponentSize().clickable(onClick = onAddUrge).padding(horizontal = 12.dp, vertical = 12.dp)
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
            
            Column(modifier = Modifier.width(120.dp)) {
                Canvas(modifier = Modifier.fillMaxWidth().height(24.dp)) {
                    val maxUrges = (last7DaysUrges.maxOrNull() ?: 0).coerceAtLeast(1)
                    val barWidth = size.width / 13f // 7 bars + 6 spaces
                    val spacing = barWidth
                    
                    last7DaysUrges.forEachIndexed { index, count ->
                        val barHeight = (count.toFloat() / maxUrges.toFloat()) * size.height
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
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                    dayLabels.forEach { label ->
                        Text(label, fontSize = 8.sp, color = mutedColor, fontFamily = vt323Font)
                    }
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
            modifier = Modifier.minimumInteractiveComponentSize().clickable(onClick = onResetStreak).padding(horizontal = 12.dp, vertical = 12.dp)
        )
    }
}

@Composable
fun ResetDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    textColor: androidx.compose.ui.graphics.Color,
    mutedColor: androidx.compose.ui.graphics.Color,
    vt323Font: FontFamily
) {
    var countdownMs by remember { mutableIntStateOf(3000) }
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        while (countdownMs > 0) {
            delay(16)
            countdownMs = maxOf(0, 3000 - (System.currentTimeMillis() - startTime).toInt())
        }
    }
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
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
                
                Spacer(modifier = Modifier.height(24.dp))
                
                LinearProgressIndicator(
                    progress = { 1f - (countdownMs / 3000f) },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = androidx.compose.ui.graphics.Color(0xFFD12626),
                    trackColor = mutedColor.copy(alpha = 0.2f),
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                val displaySeconds = (countdownMs / 1000) + if (countdownMs % 1000 > 0) 1 else 0
                Text(
                    text = if (countdownMs > 0) "[ WAIT ($displaySeconds) ]" else "[ CONFIRM ]",
                    fontFamily = vt323Font,
                    fontWeight = FontWeight.Bold,
                    color = if (countdownMs > 0) mutedColor else androidx.compose.ui.graphics.Color(0xFFD12626),
                    modifier = Modifier.minimumInteractiveComponentSize().clickable(enabled = countdownMs == 0, onClick = onConfirm).padding(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "[ CANCEL ]",
                    fontFamily = vt323Font,
                    fontWeight = FontWeight.Bold,
                    color = mutedColor,
                    modifier = Modifier.minimumInteractiveComponentSize().clickable(onClick = onDismiss).padding(12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryBottomSheet(
    history: List<com.example.data.AttemptHistory>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    textColor: androidx.compose.ui.graphics.Color,
    mutedColor: androidx.compose.ui.graphics.Color,
    vt323Font: FontFamily
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
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

@Composable
fun MilestoneCelebrationOverlay(
    milestone: Int,
    onDismiss: () -> Unit,
    textColor: androidx.compose.ui.graphics.Color,
    vt323Font: FontFamily
) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "celebration_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.85f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "MILESTONE REACHED",
                fontFamily = vt323Font,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                color = textColor
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "$milestone",
                fontFamily = vt323Font,
                fontSize = 140.sp,
                fontWeight = FontWeight.Black,
                color = textColor,
                modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "DAYS",
                fontFamily = vt323Font,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 16.sp,
                color = textColor
            )
            Spacer(modifier = Modifier.height(64.dp))
            Text(
                text = "[ TAP TO CONTINUE ]",
                fontFamily = vt323Font,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                color = textColor.copy(alpha = 0.4f)
            )
        }
    }
}
