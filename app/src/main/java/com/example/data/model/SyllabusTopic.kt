package com.example.data.model

data class VivaQuestion(
    val question: String,
    val answer: String
)

data class MCQQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class SyllabusTopic(
    val id: String,
    val unitNumber: Int,
    val unitTitle: String,
    val topicTitle: String,
    val simpleExplanation: String,
    val technicalDetails: String,
    val realWorldExample: String,
    val vivaQuestions: List<VivaQuestion>,
    val mcqQuestions: List<MCQQuestion>
)
