package com.meya.doumenculture.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meya.doumenculture.data.model.TimeSlot
import com.meya.doumenculture.data.model.Venue
import com.meya.doumenculture.data.repository.Injection
import com.meya.doumenculture.ui.state.UiState
import com.meya.doumenculture.ui.state.VenueDetailViewModel
import com.meya.doumenculture.utils.Background
import com.meya.doumenculture.utils.Border
import com.meya.doumenculture.utils.Card as CardColor
import com.meya.doumenculture.utils.DateUtils
import com.meya.doumenculture.utils.Muted
import com.meya.doumenculture.utils.Primary
import com.meya.doumenculture.utils.PrimaryDark
import com.meya.doumenculture.utils.TextPrimary
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenueDetailScreen(
    venueId: Int,
    teamId: Int?,
    onBack: () -> Unit,
    viewModel: VenueDetailViewModel = viewModel {
        VenueDetailViewModel(Injection.venueRepository, Injection.bookingRepository, venueId)
    }
) {
    val venue = viewModel.venue.value
    val uiState = viewModel.uiState.value
    val selectedDate = viewModel.selectedDate.value
    val slots = viewModel.slots.value
    val selectedSlotIds = viewModel.selectedSlotIds.value
    val isSubmitting = viewModel.isSubmitting.value
    val success = viewModel.success.value
    val errorMessage = viewModel.errorMessage.value

    var showNoTeamDialog by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            viewModel.clearError()
        }
    }

    if (showNoTeamDialog) {
        AlertDialog(
            onDismissRequest = { showNoTeamDialog = false },
            title = { Text("请先加入团队") },
            text = { Text("预约场地需要先加入一个团队。") },
            confirmButton = {
                TextButton(onClick = { showNoTeamDialog = false }) {
                    Text("确定")
                }
            }
        )
    }

    if (success) {
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissSuccess()
                onBack()
            },
            title = { Text("预约成功") },
            text = { Text("您的场地预约申请已提交，请等待审核结果。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissSuccess()
                        onBack()
                    }
                ) {
                    Text("确定")
                }
            }
        )
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("提示") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("确定")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("场馆详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        },
        bottomBar = {
            BookingBottomBar(
                selectedCount = selectedSlotIds.size,
                selectedDateText = DateUtils.formatDisplay(selectedDate),
                isSubmitting = isSubmitting,
                enabled = selectedSlotIds.isNotEmpty() && !isSubmitting,
                onSubmit = {
                    viewModel.submitBooking(teamId) { showNoTeamDialog = true }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        ) {
            venue?.let { VenueHeroSection(venue = it) }
            venue?.let { VenueInfoSection(venue = it) }

            DateSelectionSection(
                selectedDate = selectedDate,
                onDateSelected = { viewModel.selectDate(it) }
            )

            TimeSlotSection(
                uiState = uiState,
                morningSlots = viewModel.morningSlots(),
                afternoonSlots = viewModel.afternoonSlots(),
                selectedSlotIds = selectedSlotIds,
                onSlotClick = { viewModel.toggleSlot(it) },
                selectedCount = selectedSlotIds.size
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun VenueHeroSection(venue: Venue) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                Brush.linearGradient(listOf(Primary, PrimaryDark))
            )
    ) {
        Icon(
            imageVector = Icons.Default.Business,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.2f),
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.CenterEnd)
                .padding(end = 32.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f))
                    )
                )
                .padding(16.dp)
        ) {
            Text(
                text = venue.name,
                fontSize = 22.sp,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "${venue.capacity}人",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Text(
                    text = "${venue.area}m²",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Text(
                    text = venue.address,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun VenueInfoSection(venue: Venue) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardColor)
            .padding(16.dp)
    ) {
        Text(
            text = venue.description,
            fontSize = 14.sp,
            color = TextPrimary,
            lineHeight = 20.sp
        )
        if (venue.facilities.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                venue.facilities.forEach { facility ->
                    FacilityChip(facility = facility)
                }
            }
        }
    }
}

@Composable
private fun FacilityChip(facility: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Primary.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = facilityIcon(facility),
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = facility,
            fontSize = 12.sp,
            color = Primary,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

private fun facilityIcon(name: String): ImageVector {
    val lower = name.lowercase()
    return when {
        lower.contains("空调") || lower.contains("ac") -> Icons.Default.AcUnit
        lower.contains("投影") || lower.contains("projector") -> Icons.Default.Tv
        lower.contains("音响") || lower.contains("audio") -> Icons.Default.Speaker
        lower.contains("wifi") -> Icons.Default.Wifi
        lower.contains("停车") || lower.contains("parking") -> Icons.Default.LocalParking
        lower.contains("舞台") || lower.contains("stage") -> Icons.Default.TheaterComedy
        else -> Icons.Default.Label
    }
}

@Composable
private fun DateSelectionSection(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    Column(
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        Text(
            text = "选择日期",
            fontSize = 16.sp,
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            DateUtils.nextAvailableDates().forEach { date ->
                DateCell(
                    date = date,
                    isSelected = date == selectedDate,
                    onClick = { onDateSelected(date) }
                )
            }
        }
    }
}

@Composable
private fun DateCell(
    date: LocalDate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .size(width = 56.dp, height = 72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Primary else CardColor)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = DateUtils.formatWeekday(date),
            fontSize = 12.sp,
            color = if (isSelected) Color.White else Muted
        )
        Text(
            text = DateUtils.formatDay(date),
            fontSize = 18.sp,
            color = if (isSelected) Color.White else TextPrimary,
            style = if (isSelected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
        )
        Text(
            text = DateUtils.formatMonth(date),
            fontSize = 11.sp,
            color = if (isSelected) Color.White.copy(alpha = 0.8f) else Muted
        )
    }
}

@Composable
private fun TimeSlotSection(
    uiState: UiState<Unit>,
    morningSlots: List<TimeSlot>,
    afternoonSlots: List<TimeSlot>,
    selectedSlotIds: Set<Int>,
    onSlotClick: (TimeSlot) -> Unit,
    selectedCount: Int
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "选择时间段",
                fontSize = 16.sp,
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (selectedCount > 0) {
                Text(
                    text = "已选 $selectedCount/4 个",
                    fontSize = 13.sp,
                    color = Primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Primary.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        when (uiState) {
            is UiState.Loading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = Primary, modifier = Modifier.size(20.dp))
                    Text(
                        text = "加载时间段...",
                        fontSize = 14.sp,
                        color = Muted,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
            is UiState.Error -> {
                Text(
                    text = uiState.message,
                    fontSize = 14.sp,
                    color = Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                )
            }
            is UiState.Success -> {
                if (morningSlots.isEmpty() && afternoonSlots.isEmpty()) {
                    Text(
                        text = "该日期暂无可用时间段",
                        fontSize = 14.sp,
                        color = Muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                } else {
                    if (morningSlots.isNotEmpty()) {
                        PeriodSection(
                            title = "上午",
                            slots = morningSlots,
                            selectedSlotIds = selectedSlotIds,
                            onSlotClick = onSlotClick
                        )
                    }
                    if (afternoonSlots.isNotEmpty()) {
                        PeriodSection(
                            title = "下午",
                            slots = afternoonSlots,
                            selectedSlotIds = selectedSlotIds,
                            onSlotClick = onSlotClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodSection(
    title: String,
    slots: List<TimeSlot>,
    selectedSlotIds: Set<Int>,
    onSlotClick: (TimeSlot) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = Muted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            slots.chunked(2).forEach { rowSlots ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowSlots.forEach { slot ->
                        TimeSlotCell(
                            slot = slot,
                            isSelected = selectedSlotIds.contains(slot.id),
                            onClick = { onSlotClick(slot) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowSlots.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeSlotCell(
    slot: TimeSlot,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = slot.isOpen && !slot.isBooked
    val backgroundColor = when {
        isSelected -> Primary
        !enabled -> Border.copy(alpha = 0.3f)
        else -> CardColor
    }
    val textColor = when {
        isSelected -> Color.White
        !enabled -> Muted
        else -> TextPrimary
    }
    val borderColor = when {
        isSelected -> Primary
        !enabled -> Border
        else -> Border
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${slot.startTime} - ${slot.endTime}",
            fontSize = 14.sp,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun BookingBottomBar(
    selectedCount: Int,
    selectedDateText: String,
    isSubmitting: Boolean,
    enabled: Boolean,
    onSubmit: () -> Unit
) {
    Column {
        HorizontalDivider(color = Border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardColor)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "已选 $selectedCount 个时段",
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Text(
                    text = selectedDateText,
                    fontSize = 12.sp,
                    color = Muted
                )
            }
            Button(
                onClick = onSubmit,
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    disabledContainerColor = Muted.copy(alpha = 0.4f)
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text("确认预约", fontSize = 16.sp)
                }
            }
        }
    }
}
