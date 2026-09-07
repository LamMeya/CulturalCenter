package com.meya.doumenculture.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.meya.doumenculture.data.model.Venue
import com.meya.doumenculture.data.repository.Injection
import com.meya.doumenculture.ui.state.HomeViewModel
import com.meya.doumenculture.ui.state.UiState
import com.meya.doumenculture.utils.Accent
import com.meya.doumenculture.utils.Background
import com.meya.doumenculture.utils.Border
import com.meya.doumenculture.utils.Card as CardColor
import com.meya.doumenculture.utils.Muted
import com.meya.doumenculture.utils.Primary
import com.meya.doumenculture.utils.TextPrimary

@Composable
fun HomeScreen(
    onVenueClick: (Int) -> Unit,
    homeViewModel: HomeViewModel = viewModel {
        HomeViewModel(Injection.venueRepository, Injection.notificationRepository)
    }
) {
    val uiState = homeViewModel.uiState.value
    val venues = homeViewModel.venues.value
    val notification = homeViewModel.notification.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
    ) {
        HomeHeader()

        notification?.content?.let { content ->
            NotificationBanner(content = content)
        }

        Text(
            text = "场馆列表",
            fontSize = 18.sp,
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
        )

        when (uiState) {
            is UiState.Loading -> VenueGridSkeleton()
            is UiState.Error -> ErrorState(message = uiState.message) {
                homeViewModel.loadData()
            }
            is UiState.Success -> {
                if (venues.isEmpty()) {
                    EmptyVenueView()
                } else {
                    VenueGrid(venues = venues, onVenueClick = onVenueClick)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun HomeHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "珠海文化中心",
                    fontSize = 24.sp,
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "场地预约平台",
                    fontSize = 14.sp,
                    color = Muted
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        HorizontalDivider(color = Border)
    }
}

@Composable
private fun NotificationBanner(content: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Accent.copy(alpha = 0.1f))
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(Accent)
        )
        Text(
            text = content,
            fontSize = 13.sp,
            color = TextPrimary,
            maxLines = 2
        )
    }
}

@Composable
private fun VenueGrid(venues: List<Venue>, onVenueClick: (Int) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        venues.chunked(2).forEach { rowVenues ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowVenues.forEach { venue ->
                    VenueCard(
                        venue = venue,
                        onClick = { onVenueClick(venue.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowVenues.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun VenueCard(venue: Venue, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            ) {
                if (!venue.imageURL.isNullOrBlank()) {
                    AsyncImage(
                        model = venue.imageURL,
                        contentDescription = venue.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    GradientPlaceholder()
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = venue.name,
                    fontSize = 16.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Muted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${venue.capacity}人",
                        fontSize = 13.sp,
                        color = Muted,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = Muted,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(14.dp)
                    )
                    Text(
                        text = "${venue.area}m²",
                        fontSize = 13.sp,
                        color = Muted,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GradientPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Primary.copy(alpha = 0.7f), Primary.copy(alpha = 0.3f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Business,
            contentDescription = null,
            tint = CardColor.copy(alpha = 0.8f),
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
private fun VenueGridSkeleton() {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(16f / 10f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Border)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyVenueView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Business,
            contentDescription = null,
            tint = Muted.copy(alpha = 0.4f),
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "暂无可用场馆",
            fontSize = 16.sp,
            color = Muted,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = Muted.copy(alpha = 0.4f),
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = message,
            fontSize = 14.sp,
            color = Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp)
        )
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = CardColor),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("重试", color = Primary)
        }
    }
}
