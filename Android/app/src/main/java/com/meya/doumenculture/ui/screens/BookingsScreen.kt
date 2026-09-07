package com.meya.doumenculture.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meya.doumenculture.data.model.Booking
import com.meya.doumenculture.data.repository.Injection
import com.meya.doumenculture.ui.state.BookingsViewModel
import com.meya.doumenculture.ui.state.UiState
import com.meya.doumenculture.utils.Background
import com.meya.doumenculture.utils.Border
import com.meya.doumenculture.utils.Card as CardColor
import com.meya.doumenculture.utils.Muted
import com.meya.doumenculture.utils.Primary
import com.meya.doumenculture.utils.TextPrimary
import com.meya.doumenculture.utils.formatTimeRange
import com.meya.doumenculture.utils.hexToColor

@Composable
fun BookingsScreen(
    userId: Int?,
    bookingsViewModel: BookingsViewModel = viewModel {
        BookingsViewModel(Injection.bookingRepository, Injection.teamRepository, userId)
    }
) {
    val uiState = bookingsViewModel.uiState.value
    val bookings = bookingsViewModel.bookings.value
    val errorMessage = bookingsViewModel.errorMessage.value

    var bookingToCancel by remember { mutableStateOf<Booking?>(null) }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            bookingsViewModel.clearError()
        }
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { bookingsViewModel.clearError() },
            title = { Text("提示") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { bookingsViewModel.clearError() }) {
                    Text("确定")
                }
            }
        )
    }

    bookingToCancel?.let { booking ->
        AlertDialog(
            onDismissRequest = { bookingToCancel = null },
            title = { Text("取消预约") },
            text = { Text("确定要取消这次预约吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        bookingsViewModel.cancelBooking(booking)
                        bookingToCancel = null
                    }
                ) {
                    Text("确定取消", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookingToCancel = null }) {
                    Text("再想想")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        when (uiState) {
            is UiState.Loading -> {
                LoadingView()
            }
            is UiState.Error -> {
                ErrorView(message = uiState.message) {
                    bookingsViewModel.loadBookings()
                }
            }
            is UiState.Success -> {
                if (bookings.isEmpty()) {
                    EmptyView()
                } else {
                    BookingsList(
                        bookings = bookings,
                        bookerName = "",
                        canCancel = { bookingsViewModel.canCancel(it) },
                        onCancel = { bookingToCancel = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(100.dp))
        CircularProgressIndicator(color = Primary)
        Text(
            text = "加载预约记录...",
            fontSize = 14.sp,
            color = Muted,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun EmptyView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(100.dp))
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            tint = Muted.copy(alpha = 0.3f),
            modifier = Modifier.size(56.dp)
        )
        Text(
            text = "暂无预约记录",
            fontSize = 18.sp,
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "去首页选择场馆开始预约吧",
            fontSize = 14.sp,
            color = Muted,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(100.dp))
        Text(message, color = Muted)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry) {
            Text("重试")
        }
    }
}

@Composable
private fun BookingsList(
    bookings: List<Booking>,
    bookerName: String,
    canCancel: (Booking) -> Boolean,
    onCancel: (Booking) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
    ) {
        bookings.forEach { booking ->
            BookingCard(
                booking = booking,
                bookerName = bookerName,
                canCancel = canCancel(booking),
                onCancel = { onCancel(booking) }
            )
        }
    }
}

@Composable
private fun BookingCard(
    booking: Booking,
    bookerName: String,
    canCancel: Boolean,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.venueName,
                        fontSize = 16.sp,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = booking.teamName,
                        fontSize = 13.sp,
                        color = Muted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                StatusBadge(status = booking.status.displayName, colorHex = booking.status.colorHex)
            }

            HorizontalDivider(color = Border, modifier = Modifier.padding(horizontal = 14.dp))

            Column(modifier = Modifier.padding(14.dp)) {
                InfoRow(label = "预约日期", value = booking.timeSlot?.date ?: "--")
                InfoRow(
                    label = "预约时间",
                    value = formatTimeRange(booking.timeSlot?.start, booking.timeSlot?.end)
                )
                InfoRow(label = "预约人", value = bookerName.ifBlank { "--" })
            }

            if (canCancel) {
                HorizontalDivider(color = Border, modifier = Modifier.padding(horizontal = 14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Text("取消预约", color = Color.Red, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Muted,
            modifier = Modifier.width(56.dp)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = TextPrimary,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun StatusBadge(status: String, colorHex: String) {
    val color = colorHex.hexToColor()
    Text(
        text = status,
        fontSize = 12.sp,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
