package com.example.schedule.features.staff.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.schedule.core.utils.TextUtils
import com.example.schedule.features.staff.data.StaffMember

@Composable
fun StaffDetailContent(
    member: StaffMember,
    onViewScheduleClick: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StaffAvatar(
                fullName = member.fullName,
                photoUrl = member.photoUrl,
                modifier = Modifier
                    .width(76.dp)
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(16.dp))
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = member.fullName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (member.position.isNotBlank()) {
                    Text(
                        text = member.position,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            member.qualification?.takeIf { it.isNotBlank() }?.let {
                DetailItem(label = "Квалификация", value = it)
            }

            if (member.education.isNotBlank()) {
                DetailItem(label = "Образование", value = member.education)
            }
        }

        Button(
            onClick = {
                val shortName = TextUtils.toShortName(member.fullName)
                onViewScheduleClick(shortName)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            contentPadding = PaddingValues(16.dp)
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Посмотреть расписание")
        }
    }
}

@Composable
fun StaffAvatar(
    fullName: String,
    photoUrl: String?,
    modifier: Modifier = Modifier
) {
    val initials = fullName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifBlank { "?" }

    val fallback: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }

    if (photoUrl.isNullOrBlank()) {
        Box(modifier = modifier) {
            fallback()
        }
    } else {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photoUrl)
                .crossfade(true)
                .build(),
            contentDescription = fullName,
            contentScale = ContentScale.Crop,
            modifier = modifier,
            loading = { fallback() },
            error = { fallback() }
        )
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            shape = CircleShape,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )
    }
}
