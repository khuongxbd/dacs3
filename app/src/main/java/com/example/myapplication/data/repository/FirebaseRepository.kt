package com.example.myapplication.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.myapplication.data.model.ClassQuiz
import com.example.myapplication.data.model.QuizQuestion

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Lấy ID của user hiện tại đang đăng nhập
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * Lấy danh sách bài kiểm tra thuộc về một lớp học cụ thể (Dành cho Học sinh)
     * Vì Quiz bây giờ nằm trong sub-collection của từng lớp: classes -> {classId} -> class_quizzes
     */
    fun getQuizzesForClass(
        classId: String,
        onSuccess: (List<ClassQuiz>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("classes")
            .document(classId)
            .collection("class_quizzes")
            .get()
            .addOnSuccessListener { snapshot ->
                val quizList = snapshot.toObjects(ClassQuiz::class.java)
                onSuccess(quizList)
            }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Lấy danh sách câu hỏi của một bài kiểm tra cụ thể dựa vào cấu trúc mảng mới
     * Trích xuất trực tiếp trường mảng "questions" có sẵn trong Document của bài Quiz đó
     */
    fun getQuestionsForQuiz(
        classId: String,
        quizId: String,
        onSuccess: (List<QuizQuestion>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("classes")
            .document(classId)
            .collection("class_quizzes")
            .document(quizId)
            .get()
            .addOnSuccessListener { document ->
                val quiz = document.toObject(ClassQuiz::class.java)
                if (quiz != null) {
                    onSuccess(quiz.questions)
                } else {
                    onSuccess(emptyList())
                }
            }
            .addOnFailureListener { onFailure(it) }
    }

    // Đăng xuất tài khoản
    fun logout() {
        auth.signOut()
    }
}