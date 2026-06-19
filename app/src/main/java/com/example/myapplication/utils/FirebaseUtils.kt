package com.example.myapplication.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.TimeZone

object FirebaseUtils {
    suspend fun updateUserStreak() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)

        try {
            val snapshot = userRef.get().await()
            if (snapshot.exists()) {
                val lastStudyDate = snapshot.getLong("lastStudyDate") ?: 0L
                var currentStreak = snapshot.getLong("currentStreak")?.toInt() ?: 0
                var highestStreak = snapshot.getLong("highestStreak")?.toInt() ?: 0

                val currentTime = System.currentTimeMillis()
                
                // Chuẩn hóa thời gian về 0h00 của ngày
                val calendar = Calendar.getInstance(TimeZone.getDefault())
                
                calendar.timeInMillis = currentTime
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val todayMidnight = calendar.timeInMillis
                
                calendar.timeInMillis = lastStudyDate
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val lastStudyMidnight = calendar.timeInMillis

                val oneDayMillis = 24 * 60 * 60 * 1000L

                val diffDays = (todayMidnight - lastStudyMidnight) / oneDayMillis

                if (lastStudyDate == 0L || diffDays > 1) {
                    // Mới học lần đầu hoặc bỏ lỡ hơn 1 ngày -> Reset streak
                    currentStreak = 1
                } else if (diffDays == 1L) {
                    // Học ngày hôm qua -> Tăng streak
                    currentStreak += 1
                } else if (diffDays == 0L) {
                    // Đã học hôm nay rồi -> Giữ nguyên
                }

                if (currentStreak > highestStreak) {
                    highestStreak = currentStreak
                }

                userRef.update(
                    mapOf(
                        "lastStudyDate" to currentTime,
                        "currentStreak" to currentStreak,
                        "highestStreak" to highestStreak
                    )
                ).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
