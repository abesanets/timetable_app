package com.example.schedule.features.schedule.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schedule.data.models.Lesson
import com.example.schedule.data.models.Subgroup
import com.example.schedule.features.staff.data.StaffMember
import com.example.schedule.features.staff.utils.StaffUtils

@Composable
fun LessonDetailsSheet(
    lesson: Lesson,
    onTeacherClick: (StaffMember) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Информация о занятии",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        lesson.subgroups.forEachIndexed { index, subgroup ->
            val isNoLesson = subgroup.subject == "-" || subgroup.subject == "—" || subgroup.subject.isBlank()
            
            if (!isNoLesson) {
                SubgroupDetailItem(
                    subgroup = subgroup,
                    showDivider = index < lesson.subgroups.size - 1,
                    onTeacherClick = onTeacherClick
                )
            }
        }
    }
}

@Composable
fun SubgroupDetailItem(
    subgroup: Subgroup,
    showDivider: Boolean,
    onTeacherClick: (StaffMember) -> Unit
) {
    val teacherRegex = Regex("""([А-ЯЁ][а-яё]+)\s+([А-ЯЁ]\.\s*[А-ЯЁ]\.)""")
    val teacherMatch = teacherRegex.find(subgroup.subject)
    val shortTeacherName = teacherMatch?.value
    
    val cleanSubject = if (shortTeacherName != null) {
        subgroup.subject.replace(shortTeacherName, "").replace("()", "").trim()
    } else {
        subgroup.subject
    }

    val staffMember = if (shortTeacherName != null) {
        StaffUtils.findStaffByShortName(shortTeacherName)
    } else null

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (subgroup.number != null && subgroup.number != 0) {
            Text(
                text = "Подгруппа ${subgroup.number}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        DetailRow(
            icon = Icons.Default.Info,
            label = "Предмет",
            text = cleanSubject.ifBlank { subgroup.subject }
        )

        if (subgroup.room.isNotBlank() && subgroup.room != "-" && subgroup.room != "—") {
            val roomDescription = StaffUtils.getRoomDescription(subgroup.room)
            DetailRow(
                icon = Icons.Default.LocationOn,
                label = "Аудитория: ${subgroup.room}",
                text = roomDescription
            )
        }

        if (shortTeacherName != null) {
             val teacherLabel = if (staffMember != null) {
                 staffMember.fullName
             } else {
                 shortTeacherName
             }
             
             DetailRow(
                icon = Icons.Default.Person,
                label = "Преподаватель",
                text = teacherLabel,
                onClick = if (staffMember != null) { { onTeacherClick(staffMember) } } else null,
                isClickable = staffMember != null
            )
        }
    }
    
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(top = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun DetailRow(
    icon: ImageVector,
    label: String,
    text: String,
    onClick: (() -> Unit)? = null,
    isClickable: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = if (isClickable) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isClickable) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )
            
            if (isClickable) {
                Text(
                    text = "Нажмите, чтобы узнать больше",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }
        }
    }
}