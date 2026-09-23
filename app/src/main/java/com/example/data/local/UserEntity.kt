package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val displayName: String,
    val password: String,
    val role: String = "Staff", // "Super Admin", "Cashier", "Sales Agent", "Staff"
    val createdAt: Long = System.currentTimeMillis()
)
