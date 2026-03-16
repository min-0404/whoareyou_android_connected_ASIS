package com.example.whoareyou.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whoareyou.model.Employee
import com.example.whoareyou.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDetailScreen(
    employee: Employee,
    onBack: () -> Unit,
    onToggleFavorite: (Employee) -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFFFE8E8), Background)
                        )
                    )
            ) {
                // Back button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = TextPrimary
                    )
                }

                // Favorite button
                IconButton(
                    onClick = { onToggleFavorite(employee) },
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = if (employee.isFavorite) Icons.Default.Star else Icons.Default.Star,
                        contentDescription = "즐겨찾기",
                        tint = if (employee.isFavorite) AccentOrange else TextSecondary
                    )
                }

                // Profile avatar
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ProfileAvatar(size = 80, photoUrl = employee.photoUrl)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = employee.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = employee.nickname,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }

            // Info card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-16).dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Team & position
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoBadge(text = employee.team, color = Primary.copy(alpha = 0.1f), textColor = Primary)
                        InfoBadge(text = employee.position, color = AccentBlue.copy(alpha = 0.1f), textColor = AccentBlue)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Job title badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(BadgeBackground)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = Color(0xFF263DA0),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = employee.jobTitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BadgeText
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Divider(color = Background)
                    Spacer(modifier = Modifier.height(20.dp))

                    // Call buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CallButton(
                            label = "내선",
                            number = employee.internalPhone,
                            icon = Icons.Default.Phone,
                            color = CallGreen,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${employee.internalPhone}"))
                                context.startActivity(intent)
                            }
                        )
                        CallButton(
                            label = "휴대폰",
                            number = employee.mobilePhone,
                            icon = Icons.Default.Phone,
                            color = AccentBlue,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${employee.mobilePhone}"))
                                context.startActivity(intent)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Contact info rows
                    ContactInfoRow(icon = Icons.Default.Phone, label = "내선번호", value = employee.internalPhone)
                    ContactInfoRow(icon = Icons.Default.Phone, label = "휴대폰", value = employee.mobilePhone)
                    ContactInfoRow(icon = Icons.Default.Call, label = "팩스", value = employee.fax)
                    ContactInfoRow(icon = Icons.Default.Email, label = "이메일", value = employee.email)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun InfoBadge(text: String, color: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textColor)
    }
}

@Composable
fun CallButton(
    label: String,
    number: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = Color.White)
            Text(text = number, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun ContactInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = label, fontSize = 12.sp, color = TextSecondary)
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
    }
    Divider(color = Background)
}
