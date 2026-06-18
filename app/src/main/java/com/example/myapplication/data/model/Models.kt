package com.example.myapplication.data.model

// ĐỐI TƯỢNG NGƯỜI DÙNG
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val role: String = "user"
)

// ĐỐI TƯỢNG LỚP HỌC
data class ClassModel(
    val classId: String = "",
    val className: String = "",
    val code: String? = null,
    val adminId: String = "",
    val memberIds: List<String> = emptyList(),
    val private: Boolean = false,
    val avatarUrl: String = "",
    val maxMembers: Int = -1 // -1 đại diện cho "Không giới hạn" số người tham gia
)

// ĐỐI TƯỢNG THÀNH VIÊN TRONG LỚP
data class ClassMember(
    val userId: String = "",
    val name: String = "",
    val joinedAt: Long = 0L
)

// ĐỐI TƯỢNG BỘ TỪ VỰNG (FLASHCARD SET)
data class FlashcardSet(
    val setId: String = "",
    val title: String = "",
    val words: List<WordItem> = emptyList(),
    val adminId: String = "",
    val assignedClassIds: List<String> = emptyList() // Lưu danh sách ID các lớp được giao bộ từ vựng này
)

data class WordItem(
    val word: String = "",
    val meaning: String = "",
<<<<<<< HEAD
    val imageUrl: String = ""
=======
    val imageUrl: String = "" // Thêm trường link ảnh minh họa cho từ vựng sang trọng hơn
>>>>>>> d415b274fde0a734c2fda544671c34c1b8ef3ec4
)

// ĐỐI TƯỢNG BÀI KIỂM TRA TRONG LỚP
data class ClassQuiz(
    val quizId: String = "",
    val classId: String = "",
    val title: String = "",
    val duration: Int = 15,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val questions: List<QuizQuestion> = emptyList()
)


data class QuizQuestion(
    val questionText: String = "",
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val correctOption: String = "",
    val explanation: String = "",
<<<<<<< HEAD
    val imageUrl: String = ""
)

// Đối tượng lưu lịch sử và kết quả làm bài Quiz của Học sinh
data class QuizResult(
    val resultId: String = "",       // Mã kết quả (UUID)
    val userId: String = "",         // Mã học sinh làm bài
    val classId: String = "",        // Mã lớp học chứa bài quiz này
    val quizId: String = "",         // Mã bài quiz gốc
    val quizTitle: String = "",      // Lưu lại tiêu đề để hiển thị nhanh ở Main Screen mà không cần join bảng
    val score: Int = 0,              // Số câu đúng (Ví dụ: 7)
    val totalQuestions: Int = 0,     // Tổng số câu (Ví dụ: 10)
    val completedAt: Long = 0L,      // Thời gian nộp bài (System.currentTimeMillis())
    val wrongAnswers: List<WrongQuestionSnapshot> = emptyList() // Danh sách các câu làm sai
)

// Bản chụp (Snapshot) của câu hỏi bị làm sai để học sinh luyện lại
data class WrongQuestionSnapshot(
    val questionText: String = "",
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val correctOption: String = "",  // Đáp án đúng (A, B, C hoặc D)
    val userSelected: String = "",   // Đáp án mà học sinh đã chọn sai
    val explanation: String = ""     // Giải thích đáp án
=======
    val imageUrl: String = "" // Thêm dòng này để hỗ trợ đính kèm hình ảnh
>>>>>>> d415b274fde0a734c2fda544671c34c1b8ef3ec4
)

// ĐỐI TƯỢNG THÔNG BÁO
data class NotificationModel(
    val notiId: String = "",
    val title: String = "",
    val content: String = "",
    val targetClassId: String = "",
    val createdAt: Long = 0L
)