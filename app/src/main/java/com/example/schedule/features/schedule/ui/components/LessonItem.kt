package com.example.schedule.features.schedule.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.schedule.data.models.Lesson

@Composable
fun LessonItem(lesson: Lesson, isHighlighted: Boolean = false) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (isHighlighted)
            MaterialTheme.colorScheme.surfaceContainerHigh
        else
            MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = lesson.lessonNumber,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (lesson.subgroups.size == 1) {
                    val subgroup = lesson.subgroups[0]
                    val isNoLesson = subgroup.subject == "-" || subgroup.subject == "—" || subgroup.subject.isBlank()
                    
                    Text(
                        text = if (isNoLesson) "—" else subgroup.subject,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isNoLesson) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (!isNoLesson && subgroup.room.isNotBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = subgroup.room,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    lesson.subgroups.forEachIndexed { index, subgroup ->
                        val isNoLesson = subgroup.subject == "-" || subgroup.subject == "—" || subgroup.subject.isBlank()
                        Column(
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isNoLesson) "${subgroup.number}. —" else "${subgroup.number}. ${subgroup.subject}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isNoLesson) FontWeight.Normal else FontWeight.Medium,
                                    color = if (isNoLesson) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                if (!isNoLesson && subgroup.room.isNotBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = subgroup.room,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (index < lesson.subgroups.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 2.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}