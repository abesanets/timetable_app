package com.example.schedule.features.staff.data

data class StaffMember(
    val fullName: String,
    val position: String,
    val qualification: String?,
    val education: String,
    val category: String,
    val photoUrl: String? = null
)

data class CachedStaffData(
    val timestamp: Long,
    val members: List<StaffMember>
)
