package com.example.myapplication.util

import android.content.Context
import android.net.Uri
import com.example.myapplication.data.model.QuizQuestion
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.zip.ZipInputStream

object WordParserUtils {

    fun parseWordDocx(context: Context, fileUri: Uri): List<QuizQuestion> {
        val wordImportedList = mutableListOf<QuizQuestion>()
        var inputStream: InputStream? = null
        var zipInputStream: ZipInputStream? = null

        try {
            inputStream = context.contentResolver.openInputStream(fileUri) ?: return emptyList()
            zipInputStream = ZipInputStream(inputStream)
            var zipEntry = zipInputStream.nextEntry

            // Duyệt qua các file bên trong cấu trúc nén của file Word (.docx)
            while (zipEntry != null) {
                if (zipEntry.name == "word/document.xml") {
                    // Trích xuất toàn bộ các đoạn văn bản từ file XML nội bộ
                    val paragraphs = parseXmlText(zipInputStream)

                    var currentQText = ""
                    var currentOpA = ""
                    var currentOpB = ""
                    var currentOpC = ""
                    var currentOpD = ""
                    var currentCorrect = "A"
                    var currentExp = ""

                    for (line in paragraphs) {
                        val trimmedLine = line.trim()
                        if (trimmedLine.isEmpty()) continue

                        when {
                            trimmedLine.startsWith("Câu", ignoreCase = true) -> {
                                if (currentQText.isNotBlank()) {
                                    wordImportedList.add(
                                        QuizQuestion(currentQText, currentOpA, currentOpB, currentOpC, currentOpD, currentCorrect, currentExp)
                                    )
                                    currentOpA = ""; currentOpB = ""; currentOpC = ""; currentOpD = ""; currentCorrect = "A"; currentExp = ""
                                }
                                currentQText = trimmedLine.substringAfter(":").trim()
                            }
                            trimmedLine.startsWith("A.", ignoreCase = true) -> currentOpA = trimmedLine.substringAfter("A.").trim()
                            trimmedLine.startsWith("B.", ignoreCase = true) -> currentOpB = trimmedLine.substringAfter("B.").trim()
                            trimmedLine.startsWith("C.", ignoreCase = true) -> currentOpC = trimmedLine.substringAfter("C.").trim()
                            trimmedLine.startsWith("D.", ignoreCase = true) -> currentOpD = trimmedLine.substringAfter("D.").trim()
                            trimmedLine.startsWith("Đáp án:", ignoreCase = true) -> {
                                currentCorrect = trimmedLine.substringAfter("Đáp án:").trim().uppercase()
                            }
                            trimmedLine.startsWith("Giải thích:", ignoreCase = true) -> currentExp = trimmedLine.substringAfter("Giải thích:").trim()
                        }
                    }

                    // Đóng nốt câu hỏi cuối cùng
                    if (currentQText.isNotBlank()) {
                        wordImportedList.add(
                            QuizQuestion(currentQText, currentOpA, currentOpB, currentOpC, currentOpD, currentCorrect, currentExp)
                        )
                    }
                    break
                }
                zipEntry = zipInputStream.nextEntry
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                zipInputStream?.close()
                inputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return wordImportedList
    }

    // Hàm phân tích cú pháp XML để lọc chuỗi trong thẻ văn bản <w:t> của Microsoft Word
    private fun parseXmlText(inputStream: InputStream): List<String> {
        val paragraphs = mutableListOf<String>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()

        // Ngăn chặn parser tự động đóng luồng dữ liệu của ZipInputStream khi đọc xong
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(inputStream, "UTF-8")

        var eventType = parser.eventType
        val currentParagraph = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (name == "p") {
                        currentParagraph.setLength(0) // Reset bộ đệm khi gặp đoạn mới <w:p>
                    }
                }
                XmlPullParser.TEXT -> {
                    currentParagraph.append(parser.text) // Gom dữ liệu chữ nằm trong thẻ <w:t>
                }
                XmlPullParser.END_TAG -> {
                    if (name == "p") {
                        val text = currentParagraph.toString().trim()
                        if (text.isNotEmpty()) {
                            paragraphs.add(text)
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return paragraphs
    }
}