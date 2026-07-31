package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.AppHeader
import com.example.ui.components.BottomNavBar
import com.example.ui.components.ChatHistoryDrawerContent
import com.example.ui.components.LiveVoiceDialog
import com.example.ui.components.NavTab
import kotlinx.coroutines.launch
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ExamPrepScreen
import com.example.ui.screens.SavedNotesScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SyllabusScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.NetChatTheme
import com.example.ui.viewmodel.NetChatViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: NetChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            com.example.data.remote.FirebaseManager.init(applicationContext)
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Firebase init error: ${e.message}")
        }
        enableEdgeToEdge()

        setContent {
            NetChatTheme {
                var showSplash by rememberSaveable { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(
                        onSplashFinished = { showSplash = false }
                    )
                } else {
                    val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: NavTab.CHAT.route
                val currentTab = NavTab.values().firstOrNull { it.route == currentRoute } ?: NavTab.CHAT

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val coroutineScope = rememberCoroutineScope()

                BackHandler(enabled = drawerState.isOpen) {
                    coroutineScope.launch { drawerState.close() }
                }

                fun navigateToTab(tab: NavTab) {
                    if (tab.route != currentRoute) {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }

                val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
                val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
                val topics by viewModel.syllabusTopics.collectAsStateWithLifecycle()
                val selectedTopic by viewModel.selectedTopic.collectAsStateWithLifecycle()
                val subnetResult by viewModel.subnetResult.collectAsStateWithLifecycle()
                val wiresharkSamples by viewModel.wiresharkSamples.collectAsStateWithLifecycle()
                val selectedPacket by viewModel.selectedPacket.collectAsStateWithLifecycle()
                val commandCheatsheet by viewModel.commandCheatsheet.collectAsStateWithLifecycle()
                val bookmarkedMessages by viewModel.bookmarkedMessages.collectAsStateWithLifecycle()
                val savedNotes by viewModel.savedNotes.collectAsStateWithLifecycle()
                val quizResults by viewModel.quizResults.collectAsStateWithLifecycle()
                val uploadedDocuments by viewModel.uploadedDocuments.collectAsStateWithLifecycle()

                val firebaseUser by viewModel.firebaseUser.collectAsStateWithLifecycle()
                val isLiveVoiceActive by viewModel.isLiveVoiceActive.collectAsStateWithLifecycle()
                val liveVoiceTranscript by viewModel.liveVoiceTranscript.collectAsStateWithLifecycle()

                val isVideoAnalyzing by viewModel.isVideoAnalyzing.collectAsStateWithLifecycle()
                val videoAnalysisResult by viewModel.videoAnalysisResult.collectAsStateWithLifecycle()
                val selectedVideoTitle by viewModel.selectedVideoTitle.collectAsStateWithLifecycle()

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ChatHistoryDrawerContent(
                            messages = messages,
                            onSelectHistoryMessage = { selectedMsg ->
                                coroutineScope.launch { drawerState.close() }
                                navigateToTab(NavTab.CHAT)
                                viewModel.sendMessage(selectedMsg.text, selectedMsg.topicTag)
                            },
                            onNewChat = {
                                coroutineScope.launch { drawerState.close() }
                                navigateToTab(NavTab.CHAT)
                                viewModel.clearChat()
                            },
                            onClearHistory = {
                                viewModel.clearChat()
                            }
                        )
                    }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            AppHeader(
                                currentTabTitle = currentTab.label,
                                firebaseUser = firebaseUser,
                                onOpenDrawer = {
                                    coroutineScope.launch { drawerState.open() }
                                },
                                onClearChat = if (currentTab == NavTab.CHAT) {
                                    { viewModel.clearChat() }
                                } else null,
                                onOpenVoiceDialog = { viewModel.setLiveVoiceActive(true) },
                                onSignInAnonymously = { viewModel.signInAnonymously() },
                                onSignOut = { viewModel.signOutFirebase() },
                                onReplaySplash = {
                                    viewModel.setLiveVoiceActive(true)
                                }
                            )
                        },
                        bottomBar = {
                            BottomNavBar(
                                selectedTab = currentTab,
                                onTabSelected = { tab -> navigateToTab(tab) }
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (isLiveVoiceActive) {
                                LiveVoiceDialog(
                                    transcript = liveVoiceTranscript,
                                    isGenerating = isGenerating,
                                    onDismiss = { viewModel.setLiveVoiceActive(false) },
                                    onSendVoiceQuery = { query -> viewModel.sendLiveVoiceMessage(query) }
                                )
                            }

                            NavHost(
                                navController = navController,
                                startDestination = NavTab.CHAT.route,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                composable(NavTab.CHAT.route) {
                                    ChatScreen(
                                        messages = messages,
                                        isGenerating = isGenerating,
                                        onSendMessage = { prompt, context ->
                                            viewModel.sendMessage(prompt, context)
                                        },
                                        onToggleBookmark = { id, bookmarked ->
                                            viewModel.toggleBookmark(id, bookmarked)
                                        },
                                        onUploadPdf = { uri ->
                                            viewModel.uploadDocumentFromUri(uri)
                                        }
                                    )
                                }
                                composable(NavTab.SYLLABUS.route) {
                                    SyllabusScreen(
                                        topics = topics,
                                        selectedTopic = selectedTopic,
                                        uploadedDocuments = uploadedDocuments,
                                        onTopicSelected = { viewModel.selectTopic(it) },
                                        onAskAiAboutTopic = { query ->
                                            navigateToTab(NavTab.CHAT)
                                            viewModel.sendMessage(query, selectedTopic?.topicTitle)
                                        },
                                        onUploadPdf = { uri ->
                                            viewModel.uploadDocumentFromUri(uri)
                                        },
                                        onAskAiAboutDoc = { doc ->
                                            navigateToTab(NavTab.CHAT)
                                            viewModel.askAiAboutDocument(doc)
                                        },
                                        onDeleteDoc = { doc ->
                                            viewModel.deleteUploadedDocument(doc)
                                        }
                                    )
                                }
                                composable(NavTab.TOOLS.route) {
                                    ToolsScreen(
                                        subnetResult = subnetResult,
                                        onCalculateSubnet = { ip, cidr ->
                                            viewModel.calculateSubnet(ip, cidr)
                                        },
                                        wiresharkSamples = wiresharkSamples,
                                        selectedPacket = selectedPacket,
                                        onSelectWiresharkPacket = { viewModel.selectWiresharkPacket(it) },
                                        commandCheatsheet = commandCheatsheet,
                                        onAskAiAboutTool = { query ->
                                            navigateToTab(NavTab.CHAT)
                                            viewModel.sendMessage(query, "Tool Analysis")
                                        },
                                        isVideoAnalyzing = isVideoAnalyzing,
                                        videoAnalysisResult = videoAnalysisResult,
                                        selectedVideoTitle = selectedVideoTitle,
                                        onAnalyzeVideo = { uri, sampleTitle, customPrompt ->
                                            viewModel.analyzeVideo(uri, sampleTitle, customPrompt)
                                        }
                                    )
                                }
                                composable(NavTab.EXAM_PREP.route) {
                                    ExamPrepScreen(
                                        topics = topics,
                                        onRecordQuizResult = { topicTitle, score, total ->
                                            viewModel.recordQuizResult(topicTitle, score, total)
                                        },
                                        onAskAiAboutExam = { query ->
                                            navigateToTab(NavTab.CHAT)
                                            viewModel.sendMessage(query, "Exam Prep")
                                        }
                                    )
                                }
                                composable(NavTab.SAVED_NOTES.route) {
                                    SavedNotesScreen(
                                        bookmarkedMessages = bookmarkedMessages,
                                        savedNotes = savedNotes,
                                        quizResults = quizResults,
                                        onToggleBookmark = { id, bookmarked ->
                                            viewModel.toggleBookmark(id, bookmarked)
                                        },
                                        onSaveStudyNote = { title, unit, content ->
                                            viewModel.saveStudyNote(title, unit, content)
                                        },
                                        onDeleteStudyNote = { note ->
                                            viewModel.deleteStudyNote(note)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}




