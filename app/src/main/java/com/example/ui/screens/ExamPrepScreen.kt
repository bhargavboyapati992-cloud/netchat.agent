package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MCQQuestion
import com.example.data.model.SyllabusTopic
import com.example.data.remote.GeminiApiClient
import kotlinx.coroutines.launch

@Composable
fun ExamPrepScreen(
    topics: List<SyllabusTopic>,
    onRecordQuizResult: (String, Int, Int) -> Unit,
    onAskAiAboutExam: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPrepTab by remember { mutableIntStateOf(0) } // 0 = MCQs, 1 = Viva Question Bank
    var selectedTopicId by remember { mutableStateOf(topics.firstOrNull()?.id ?: "") }

    val currentTopic = remember(selectedTopicId, topics) {
        topics.find { it.id == selectedTopicId } ?: topics.firstOrNull()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TabRow(
            selectedTabIndex = selectedPrepTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = (selectedPrepTab == 0),
                onClick = { selectedPrepTab = 0 },
                text = { Text("MCQs", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("prep_tab_mcq")
            )
            Tab(
                selected = (selectedPrepTab == 1),
                onClick = { selectedPrepTab = 1 },
                text = { Text("Viva Bank", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Help, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("prep_tab_viva")
            )
            Tab(
                selected = (selectedPrepTab == 2),
                onClick = { selectedPrepTab = 2 },
                text = { Text("GATE & Logic", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("prep_tab_gate")
            )
            Tab(
                selected = (selectedPrepTab == 3),
                onClick = { selectedPrepTab = 3 },
                text = { Text("Formulas", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("prep_tab_formulas")
            )
        }

        // Topic Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            topics.forEach { t ->
                FilterChip(
                    selected = (currentTopic?.id == t.id),
                    onClick = { selectedTopicId = t.id },
                    label = { Text("Unit ${t.unitNumber}: ${t.topicTitle}", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        if (currentTopic != null) {
            when (selectedPrepTab) {
                0 -> McqQuizView(currentTopic, onRecordQuizResult, onAskAiAboutExam)
                1 -> VivaBankView(currentTopic, onAskAiAboutExam)
                2 -> GateLogicView(currentTopic, onAskAiAboutExam)
                3 -> FormulasSheetView(currentTopic, onAskAiAboutExam)
            }
        }
    }
}

@Composable
private fun McqQuizView(
    topic: SyllabusTopic,
    onRecordQuizResult: (String, Int, Int) -> Unit,
    onAskAiAboutExam: (String) -> Unit
) {
    val userAnswers = remember(topic.id) { mutableStateMapOf<Int, Int>() }
    var quizSubmitted by remember(topic.id) { mutableStateOf(false) }

    val totalQuestions = topic.mcqQuestions.size
    val correctCount = remember(quizSubmitted, userAnswers) {
        if (!quizSubmitted) 0 else {
            topic.mcqQuestions.count { mcq ->
                userAnswers[mcq.id] == mcq.correctIndex
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = topic.topicTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${topic.unitTitle} • $totalQuestions Multiple Choice Questions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (quizSubmitted) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (correctCount == totalQuestions) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Quiz Result: $correctCount / $totalQuestions Correct",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )

                                Button(
                                    onClick = {
                                        onAskAiAboutExam("I just completed the quiz on ${topic.topicTitle} scoring $correctCount/$totalQuestions. Can you explain key concepts for any questions I might have missed?")
                                    },
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ask AI Feedback", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        items(topic.mcqQuestions) { mcq ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Q${mcq.id}. ${mcq.question}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    mcq.options.forEachIndexed { optIdx, optionText ->
                        val isSelected = (userAnswers[mcq.id] == optIdx)
                        val isCorrect = (optIdx == mcq.correctIndex)

                        val optionBg = when {
                            !quizSubmitted && isSelected -> MaterialTheme.colorScheme.primaryContainer
                            quizSubmitted && isCorrect -> MaterialTheme.colorScheme.secondaryContainer
                            quizSubmitted && isSelected && !isCorrect -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surface
                        }

                        Card(
                            onClick = {
                                if (!quizSubmitted) {
                                    userAnswers[mcq.id] = optIdx
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = optionBg)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${('A' + optIdx)}. $optionText",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )

                                if (quizSubmitted && isCorrect) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                } else if (quizSubmitted && isSelected && !isCorrect) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    if (quizSubmitted) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Explanation: ${mcq.explanation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }

        item {
            if (!quizSubmitted) {
                Button(
                    onClick = {
                        quizSubmitted = true
                        onRecordQuizResult(topic.topicTitle, correctCount, totalQuestions)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("submit_quiz_button"),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Submit Quiz Answers", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun VivaBankView(
    topic: SyllabusTopic,
    onAskAiAboutExam: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var activeRecordingVivaId by remember { mutableIntStateOf(-1) }
    val userSpokenAnswers = remember { mutableStateMapOf<Int, String>() }
    val isEvaluatingMap = remember { mutableStateMapOf<Int, Boolean>() }
    val aiEvaluationMap = remember { mutableStateMapOf<Int, String>() }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank() && activeRecordingVivaId != -1) {
                userSpokenAnswers[activeRecordingVivaId] = spokenText
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🎙️ ${topic.topicTitle} — Oral Viva Practice",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Orally answer viva questions using Speech-to-Text. Gemini AI evaluates technical accuracy, assigns a score, and provides instant feedback.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        itemsIndexed(topic.vivaQuestions) { vivaIdx, viva ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Help,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = viva.question,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "MODEL VIVA ANSWER:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = viva.answer,
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Speech-To-Text Oral Practice Section
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Oral Answer Practice (Speech-to-Text)",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Button(
                                    onClick = {
                                        activeRecordingVivaId = vivaIdx
                                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your answer for: ${viva.question}")
                                        }
                                        try {
                                            speechRecognizerLauncher.launch(intent)
                                        } catch (_: Exception) {}
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.testTag("record_oral_viva_$vivaIdx")
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Record Voice", fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val currentSpoken = userSpokenAnswers[vivaIdx] ?: ""

                            OutlinedTextField(
                                value = currentSpoken,
                                onValueChange = { userSpokenAnswers[vivaIdx] = it },
                                placeholder = { Text("Tap 'Record Voice' or type your oral answer here...", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            if (currentSpoken.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            isEvaluatingMap[vivaIdx] = true
                                            coroutineScope.launch {
                                                try {
                                                    val prompt = """
                                                    Evaluate the student's oral viva response for technical accuracy.
                                                    Question: "${viva.question}"
                                                    Expected Model Answer: "${viva.answer}"
                                                    Student's Spoken Answer: "$currentSpoken"
                                                    
                                                    Please structure your response strictly as follows:
                                                    1. 🎯 **Score**: X/10 (Numerical score based on technical accuracy)
                                                    2. 📊 **Technical Accuracy**: (Excellent / Satisfactory / Needs Improvement)
                                                    3. ✅ **Correct Concepts Mentioned**: Key technical terms correctly used.
                                                    4. ❌ **Missing Technical Terms/Misconceptions**: Important RFC rules, protocol steps, or formulas omitted.
                                                    5. 💡 **Examiner Feedback & Viva Tip**: Constructive advice on how to answer in an actual university oral viva exam.
                                                    """.trimIndent()

                                                    val evaluation = GeminiApiClient.generateAnswer(prompt, topicContext = topic.topicTitle)
                                                    aiEvaluationMap[vivaIdx] = evaluation
                                                } catch (e: Exception) {
                                                    aiEvaluationMap[vivaIdx] = "Evaluation Error: ${e.message}"
                                                } finally {
                                                    isEvaluatingMap[vivaIdx] = false
                                                }
                                            }
                                        },
                                        enabled = isEvaluatingMap[vivaIdx] != true,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                        modifier = Modifier.testTag("evaluate_oral_viva_$vivaIdx")
                                    ) {
                                        if (isEvaluatingMap[vivaIdx] == true) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onTertiary)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Evaluating...", fontSize = 11.sp)
                                        } else {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Evaluate Technical Accuracy", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            val evaluationText = aiEvaluationMap[vivaIdx]
                            if (!evaluationText.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Psychology,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Gemini AI Oral Viva Evaluation",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = evaluationText,
                                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = {
                                onAskAiAboutExam("What follow-up viva questions might an examiner ask after: '${viva.question}'?")
                            },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Examiner Follow-ups", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GateLogicView(
    topic: SyllabusTopic,
    onAskAiAboutExam: (String) -> Unit
) {
    val gateLogicItems = remember(topic.id) {
        listOf(
            Triple(
                "🎓 GATE Question: Bandwidth & Delay Product",
                "A 100 Mbps link has a propagation delay of 20 ms. What is the bandwidth-delay product in bits?",
                "Formula: BDP = Bandwidth × Delay = (100 × 10^6 bps) × (20 × 10^-3 s) = 2,000,000 bits (2 Mb). This represents the maximum volume of unacknowledged data in transit."
            ),
            Triple(
                "🧠 Logic & Resolution: CSMA/CD Minimum Frame Size",
                "Why is minimum frame size mandatory in CSMA/CD Ethernet? Express using round-trip propagation logic.",
                "Frame transmission time (T_frame) must be ≥ 2 × T_prop (round-trip propagation delay). L_min = 2 × R × d / v. If the frame is too short, the sender finishes sending before collision signal returns!"
            ),
            Triple(
                "⚙️ Proof & Unification: TCP Sliding Window Efficiency",
                "Derive maximum throughput efficiency for Stop-and-Wait protocol where T_t = transmission time and T_p = propagation time.",
                "Efficiency η = T_t / (T_t + 2·T_p) = 1 / (1 + 2a), where a = T_p / T_t. For sliding window with window size W, η = min(1, W / (1 + 2a))."
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🎓 GATE & Conceptual Logic Questions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Advanced numericals, resolution proofs, and GATE CS exam problems for ${topic.topicTitle}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(gateLogicItems) { (title, question, proof) ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = question,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = proof,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            modifier = Modifier.padding(10.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onAskAiAboutExam("Solve and prove step-by-step with full derivation: '$question'")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ask AI GATE Derivation Step-by-Step", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}

@Composable
private fun FormulasSheetView(
    topic: SyllabusTopic,
    onAskAiAboutExam: (String) -> Unit
) {
    val formulasList = remember(topic.id) {
        listOf(
            "Transmission Delay (T_t)" to "T_t = Length of Packet (L) / Bandwidth (R)",
            "Propagation Delay (T_p)" to "T_p = Distance (d) / Propagation Speed (v)",
            "Total Delay" to "T_total = T_proc + T_queue + T_t + T_p",
            "Throughput Efficiency" to "η = 1 / (1 + 2a) where a = T_p / T_t",
            "Maximum Window Size (W)" to "W ≥ 1 + 2a (for 100% link utilization)",
            "Subnet Mask Hosts" to "Usable Hosts = 2^(32 - CIDR) - 2"
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📚 Exam Revision: Formula & Definition Sheet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Quick definitions, key formulas, and protocol rules for fast revision.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(formulasList) { (name, formula) ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formula,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = {
                            onAskAiAboutExam("Explain the formula '$formula' ($name) with a solved numerical example.")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Numerical Example", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
