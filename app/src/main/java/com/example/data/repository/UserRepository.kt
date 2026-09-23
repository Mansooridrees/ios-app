package com.example.data.repository

import com.example.data.local.UserDao
import com.example.data.local.UserEntity
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {

    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()

    suspend fun getUserByUsername(username: String): UserEntity? {
        return userDao.getUserByUsername(username)
    }

    suspend fun insertUser(user: UserEntity): Long {
        return userDao.insertUser(user)
    }

    suspend fun deleteUser(id: Long) {
        userDao.deleteUserById(id)
    }

    suspend fun ensureDefaultAdmin() {
        val count = userDao.getUserCount()
        if (count == 0) {
            userDao.insertUser(
                UserEntity(
                    username = "mansoor",
                    displayName = "Mansoor",
                    password = "admin123",
                    role = "Super Admin"
                )
            )
            userDao.insertUser(
                UserEntity(
                    username = "abdulqadir",
                    displayName = "Abdul Qadir",
                    password = "user123",
                    role = "Cashier"
                )
            )
        } else {
            // Ensure Abdul Qadir exists if Mansoor already exists
            val aq = userDao.getUserByUsername("abdulqadir")
            if (aq == null) {
                userDao.insertUser(
                    UserEntity(
                        username = "abdulqadir",
                        displayName = "Abdul Qadir",
                        password = "user123",
                        role = "Cashier"
                    )
                )
            }
        }
    }
}
