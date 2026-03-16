package com.example.whoareyou.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.whoareyou.model.CallRecord
import com.example.whoareyou.model.ContactStorage
import com.example.whoareyou.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var records by remember { mutableStateOf(ContactStorage.getCallRecords(context)) }
    var showClearDialog by remember { mutableStateOf(false) }

    // 화면 복귀 시마다 최신 통화내역 재로드
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                records = ContactStorage.getCallRecords(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "뒤로가기", tint = TextPrimary)
            }
            Text(
                text = "통화내역",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (records.isNotEmpty()) {
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "전체 삭제", tint = Color.Red.copy(alpha = 0.6f))
                }
            }
        }

        if (records.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(PrimaryLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Primary, modifier = Modifier.size(36.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("통화내역이 없습니다", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("후아유에 등록된 번호와 통화하면\n자동으로 기록됩니다", fontSize = 14.sp, color = TextSecondary)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                items(records, key = { it.id }) { record ->
                    CallRecordRow(record = record)
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("통화내역 삭제") },
            text = { Text("모든 통화내역을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    ContactStorage.clearCallRecords(context)
                    records = emptyList()
                    showClearDialog = false
                }) { Text("삭제", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("취소", color = TextSecondary) }
            }
        )
    }
}

// 통화 유형별 스타일 정보
private data class CallTypeStyle(
    val icon: ImageVector,
    val iconRotation: Float,   // 발신은 아이콘을 180도 회전해 방향 구분
    val color: Color,
    val label: String
)

private fun callTypeStyle(callType: String): CallTypeStyle = when (callType) {
    "outgoing" -> CallTypeStyle(Icons.Default.Phone, 180f, Color(0xFF4CAF50), "발신")
    "missed"   -> CallTypeStyle(Icons.Default.Phone,   0f, Color(0xFFE53935), "부재중")
    else       -> CallTypeStyle(Icons.Default.Phone,   0f, Color(0xFF1565C0), "수신")
}

@Composable
fun CallRecordRow(record: CallRecord) {
    val style = callTypeStyle(record.callType)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 통화 유형 아이콘 (색상 + 회전으로 수신/발신/부재중 구분)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(style.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = style.label,
                    tint = style.color,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(style.iconRotation)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.callerName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = style.label,
                        fontSize = 11.sp,
                        color = style.color,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (record.callerTeam.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = record.callerTeam, fontSize = 12.sp, color = TextSecondary)
                }
                if (record.callerJob.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(BadgeBackground)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(text = record.callerJob, fontSize = 11.sp, color = BadgeText, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatTimestamp(record.timestampMs),
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                if (record.durationSeconds > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatDuration(record.durationSeconds),
                        fontSize = 12.sp,
                        color = AccentGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(ms: Long): String {
    val sdf = SimpleDateFormat("MM.dd HH:mm", Locale.KOREA)
    return sdf.format(Date(ms))
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}분 ${s}초" else "${s}초"
}
