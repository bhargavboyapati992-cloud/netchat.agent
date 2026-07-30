package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.QuizResultEntity
import com.example.data.local.StudyNoteEntity
import com.example.data.local.UploadedDocumentEntity
import com.example.data.model.CommandItem
import com.example.data.model.SubnetResult
import com.example.data.model.SyllabusTopic
import com.example.data.model.WiresharkPacketSample
import com.example.data.repository.NetChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NetChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = NetChatRepository(db.netChatDao())

    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.allChatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedMessages: StateFlow<List<ChatMessageEntity>> = repository.bookmarkedMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedNotes: StateFlow<List<StudyNoteEntity>> = repository.allStudyNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quizResults: StateFlow<List<QuizResultEntity>> = repository.allQuizResults
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uploadedDocuments: StateFlow<List<UploadedDocumentEntity>> = repository.allUploadedDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _syllabusTopics = MutableStateFlow<List<SyllabusTopic>>(emptyList())
    val syllabusTopics: StateFlow<List<SyllabusTopic>> = _syllabusTopics.asStateFlow()

    private val _selectedTopic = MutableStateFlow<SyllabusTopic?>(null)
    val selectedTopic: StateFlow<SyllabusTopic?> = _selectedTopic.asStateFlow()

    private val _subnetResult = MutableStateFlow<SubnetResult?>(null)
    val subnetResult: StateFlow<SubnetResult?> = _subnetResult.asStateFlow()

    private val _wiresharkSamples = MutableStateFlow<List<WiresharkPacketSample>>(emptyList())
    val wiresharkSamples: StateFlow<List<WiresharkPacketSample>> = _wiresharkSamples.asStateFlow()

    private val _selectedPacket = MutableStateFlow<WiresharkPacketSample?>(null)
    val selectedPacket: StateFlow<WiresharkPacketSample?> = _selectedPacket.asStateFlow()

    private val _commandCheatsheet = MutableStateFlow<List<CommandItem>>(emptyList())
    val commandCheatsheet: StateFlow<List<CommandItem>> = _commandCheatsheet.asStateFlow()

    // Firebase Auth State
    val firebaseUser = com.example.data.remote.FirebaseManager.authStateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.data.remote.FirebaseManager.currentUser)

    // Voice Assistant State
    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing.asStateFlow()

    private val _isLiveVoiceActive = MutableStateFlow(false)
    val isLiveVoiceActive: StateFlow<Boolean> = _isLiveVoiceActive.asStateFlow()

    private val _liveVoiceTranscript = MutableStateFlow("")
    val liveVoiceTranscript: StateFlow<String> = _liveVoiceTranscript.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val topics = repository.getSyllabusTopics()
                _syllabusTopics.value = topics
                if (topics.isNotEmpty()) {
                    _selectedTopic.value = topics.first()
                }

                val wireshark = repository.getWiresharkSamples()
                _wiresharkSamples.value = wireshark
                if (wireshark.isNotEmpty()) {
                    _selectedPacket.value = wireshark.first()
                }

                _commandCheatsheet.value = repository.getCommandCheatsheet()

                // Initial calculation for Subnet calculator
                calculateSubnet("192.168.1.100", 24)

                seedSyllabusDatabaseInternal()
            } catch (e: Throwable) {
                android.util.Log.e("NetChatViewModel", "Error loading initial data: ${e.message}")
            }
        }
    }

    fun sendMessage(prompt: String, topicContext: String? = null) {
        if (prompt.isBlank() || _isGenerating.value) return

        viewModelScope.launch {
            _isGenerating.value = true
            // Save user message
            repository.saveChatMessage(sender = "user", text = prompt.trim(), topicTag = topicContext)

            // Query AI tutor
            val responseText = repository.queryTutor(prompt.trim(), topicContext)

            // Save AI message
            repository.saveChatMessage(sender = "ai", text = responseText, topicTag = topicContext)
            _isGenerating.value = false
        }
    }

    fun toggleBookmark(id: Long, isBookmarked: Boolean) {
        viewModelScope.launch {
            repository.toggleBookmark(id, isBookmarked)
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
        }
    }

    fun selectTopic(topic: SyllabusTopic) {
        _selectedTopic.value = topic
    }

    fun calculateSubnet(ip: String, cidr: Int) {
        viewModelScope.launch {
            _subnetResult.value = repository.calculateSubnet(ip, cidr)
        }
    }

    fun selectWiresharkPacket(packet: WiresharkPacketSample) {
        _selectedPacket.value = packet
    }

    fun saveStudyNote(title: String, unit: String, content: String, noteType: String = "NOTE") {
        viewModelScope.launch {
            repository.saveStudyNote(title, unit, content, noteType)
        }
    }

    fun deleteStudyNote(note: StudyNoteEntity) {
        viewModelScope.launch {
            repository.deleteStudyNote(note)
        }
    }

    fun recordQuizResult(topicTitle: String, score: Int, totalQuestions: Int) {
        viewModelScope.launch {
            repository.saveQuizResult(topicTitle, score, totalQuestions)
        }
    }

    fun uploadDocumentFromUri(uri: Uri, category: String = "Syllabus Book") {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val contentResolver = context.contentResolver

            var fileName = "document.pdf"
            var fileSizeStr = "1.5 MB"
            var textContent = ""
            var pageCount = 10

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: "Syllabus_Document.pdf"
                    if (sizeIndex != -1) {
                        val sizeBytes = cursor.getLong(sizeIndex)
                        if (sizeBytes > 0) {
                            fileSizeStr = String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0))
                            pageCount = (sizeBytes / 40000L).toInt().coerceIn(1, 250)
                        }
                    }
                }
            }

            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    val mimeType = contentResolver.getType(uri) ?: ""
                    if (fileName.endsWith(".txt", ignoreCase = true) || mimeType.contains("text")) {
                        textContent = String(bytes, Charsets.UTF_8).take(8000)
                    } else {
                        // Attempt to extract readable text content from PDF bytes stream
                        val rawText = String(bytes, Charsets.ISO_8859_1)
                        val printableRegex = Regex("[a-zA-Z0-9\\s,.!?:;()\\-\\/]{4,}")
                        val matches = printableRegex.findAll(rawText)
                            .map { it.value.trim() }
                            .filter { it.length > 5 && !it.startsWith("%PDF") && !it.contains("obj") && !it.contains("endobj") }
                            .take(100)
                            .joinToString("\n")

                        if (matches.isNotBlank() && matches.length > 50) {
                            textContent = "PDF Title: $fileName ($fileSizeStr)\nExtracted Syllabus Content:\n${matches.take(6000)}"
                        } else {
                            textContent = "Uploaded Syllabus PDF Document: $fileName ($fileSizeStr, ~$pageCount pages). Contains Computer Networks course materials, unit topics, formulas, and exam questions."
                        }
                    }
                }
            } catch (e: Exception) {
                textContent = "Uploaded Document: $fileName ($fileSizeStr)"
            }

            val docTitle = fileName.substringBeforeLast(".")
                .replace("_", " ")
                .replace("-", " ")
                .split(" ")
                .joinToString(" ") { word -> word.replaceFirstChar { char -> char.uppercase() } }

            repository.saveUploadedDocument(
                title = if (docTitle.isBlank()) "Syllabus Book" else docTitle,
                fileName = fileName,
                fileType = if (fileName.endsWith(".txt", true)) "TXT" else "PDF",
                fileSizeFormatted = fileSizeStr,
                extractedText = textContent,
                category = category,
                pageCount = pageCount
            )
        }
    }

    fun seedSyllabusDatabase() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            seedSyllabusDatabaseInternal()
        }
    }

    private suspend fun seedSyllabusDatabaseInternal() {
        try {
            val existingDocs = db.netChatDao().getUploadedDocumentsList()
            val filenames = existingDocs.map { it.fileName }

            // Document 1: DCCN Lecture Notes & University Papers (R22A0411 MRCET)
            if (!filenames.contains("DCCN_R22A0411_Lecture_Notes_MRCET.pdf")) {
                repository.saveUploadedDocument(
                    title = "DCCN (R22A0411) Lecture Notes & Exam Papers - MRCET",
                    fileName = "DCCN_R22A0411_Lecture_Notes_MRCET.pdf",
                    fileType = "PDF",
                    fileSizeFormatted = "12.5 MB",
                    extractedText = """
                    DATA COMMUNICATIONS AND COMPUTER NETWORKS (R22A0411) LECTURE NOTES
                    III B.TECH - I SEM ECE (2025-2026) - MALLA REDDY COLLEGE OF ENGINEERING & TECHNOLOGY (MRCET)
                    Prepared by Mr E. MAHENDER REDDY, Department of Electronics & Communication Engineering

                    COURSE OBJECTIVES & OUTCOMES:
                    1. Fundamentals of Data Communication Networks (Topologies, OSI & TCP/IP Reference Models).
                    2. Data Link Layer Protocols, Error Detection (CRC-32, Checksum), Framing, Sliding Window, ALOHA, CSMA/CD, CSMA/CA, Ethernet MAC.
                    3. Network Layer Design, Distance Vector (RIP), Link State (OSPF), Path Vector (BGP), IPv4/IPv6, CIDR Subnetting, Fragmentation, ICMP, IGMP.
                    4. Transport Layer Services, Port Addressing, TCP 3-Way Handshake, Flow Control, Congestion Control (Slow Start, Fast Retransmit), UDP, SCTP.
                    5. Application Layer Services (HTTP, HTTPS, DNS, DHCP, FTP, SMTP, POP3, IMAP, SNMP, WWW).

                    UNIT-I: INTRODUCTION TO DATA COMMUNICATIONS
                    - Components: Message, Sender, Receiver, Medium, Protocol.
                    - Data Flow: Simplex (unidirectional), Half-Duplex (alternating), Full-Duplex (simultaneous).
                    - Network Criteria: Performance (Throughput & Delay), Reliability, Security.
                    - Physical Structures: Point-to-Point, Multipoint. Topologies: Mesh (n(n-1)/2 links), Star (hub/switch), Bus (backbone & drop lines), Ring (repeaters), Hybrid.
                    - Categories of Networks: LAN (10Mbps-10Gbps), MAN (Cable TV, IEEE 802.16), WAN (Switched/Packet-switched subnet).
                    - Multiplexing: FDM, TDM, WDM.
                    - Reference Models: OSI 7-Layer Model (Physical, Data Link, Network, Transport, Session, Presentation, Application) vs. TCP/IP 4-Layer Model (Host-to-Network, Internet, Transport, Application).
                    - Physical Layer Media: Guided (Twisted-pair UTP/STP 10Base-T/100Base-T, Coaxial RG-59, Fiber Optic 100Base-FX/1000Base-X) and Unguided (Radio waves omnidirectional, Microwaves parabolic/horn, Infrared IrDA).
                    - Switching: Circuit Switching (Setup, Transfer, Teardown), Packet Switching (Datagram vs Virtual Circuit).

                    UNIT-II: DATA LINK LAYER
                    - Design Issues: Framing (Character count, Byte stuffing ESC/FLAG, Bit stuffing 01111110), Error Control (CRC-32 Generator Polynomial, Checksum 16-bit complement), Flow Control.
                    - Elementary Protocols: Simplest Protocol, Stop-and-Wait ARQ, Go-Back-N ARQ (Send window 2^m-1, Receive window 1), Selective Repeat ARQ (Send window 2^(m-1), Receive window 2^(m-1)), Piggybacking.
                    - Multiple Access Protocols: ALOHA (Pure ALOHA throughput 18.4% S=G*e^-2G; Slotted ALOHA throughput 36.8% S=G*e^-G), CSMA (1-persistent, Non-persistent, p-persistent), CSMA/CD (Ethernet min frame size L_min >= 2*R*d/v), CSMA/CA (Wireless IFS, Contention Window, ACK), Collision Free Protocols, Reservation, Polling, Token Passing.
                    - Ethernet: Standard Ethernet 802.3 MAC Frame (Preamble 7 bytes, SFD 1 byte, DA 6 bytes, SA 6 bytes, Length/Type 2 bytes, Data 46-1500 bytes, CRC 4 bytes).

                    UNIT-III: ETHERNET & WIRELESS LANs
                    - Fast Ethernet (802.3u 100Mbps: 100Base-TX, 100Base-FX, 100Base-T4), Gigabit Ethernet (802.3z 1Gbps: 1000Base-SX, 1000Base-LX, 1000Base-CX, 1000Base-T).
                    - Wireless LAN (IEEE 802.11): Architecture BSS, ESS, AP, Ad-hoc vs Infrastructure, DCF & PCF MAC, RTS/CTS handshaking solving Hidden Station and Exposed Station problems, Physical layers (FHSS, DSSS, OFDM, 802.11a/b/g).
                    - Bluetooth (IEEE 802.15): Piconet (1 Primary, max 7 active Secondaries), Scatternet, FHSS 1600 hops/sec, TDD-TDMA.
                    - Connecting Devices: Passive Hubs, Repeaters, Active Hubs, Bridges (Transparent learning bridge, Spanning Tree Protocol 802.1D), Switches, Routers, Gateways.
                    - VLANs (IEEE 802.1Q): Port/MAC/IP based membership, Frame tagging.
                    - Cellular Telephony: 1G AMPS (FDMA), 2G GSM (TDMA/FDMA 13Kbps voice), IS-95 (CDMA), 3G (IMT-2000 2Mbps), 4G/5G.
                    - Satellite Networks: GEO (35,786 km), MEO (10,000 km GPS), LEO (500-1500 km Iridium 66 satellites, Teledesic 288 satellites), VSAT (Ku-band 1-2m dish).

                    UNIT-IV: NETWORK LAYER
                    - Logical Addressing: IPv4 32-bit (Binary & Dotted-Decimal notation), Classful (Class A, Class B, Class C, Class D Multicast, Class E Reserved), NetID & HostID, Default Masks (/8, /16, /24), Subnetting & Supernetting, Classless CIDR notation (x.y.z.t/n).
                    - IPv6: 128-bit address space (32 hexadecimal digits separated by colons), Abbreviation rules, Dual-stack & Tunneling transition.
                    - Address Mapping & Control: ARP (IP to MAC), RARP, BOOTP, DHCP (Dynamic IP lease), ICMP (Echo, Destination Unreachable, Time Exceeded), IGMP (Multicast group membership).
                    - Routing Algorithms & Protocols: Distance Vector Routing (RIP hop count max 15, 30s periodic updates, Count-to-Infinity problem), Link State Routing (OSPF Dijkstra shortest path algorithm, LSPs, Flooding, Area border routers), Path Vector Routing (BGP Autonomous Systems interdomain routing).
                    - Multicast Routing: MOSPF, DVMRP, PIM-DM, PIM-SM, Reverse Path Forwarding (RPF), RPB, RPM (Pruning & Grafting).

                    UNIT-V: TRANSPORT & APPLICATION LAYERS
                    - Transport Layer: Process-to-Process Delivery, Socket Addresses (IP:Port), Ephemeral vs Well-known Ports (0-1023), Multiplexing & Demultiplexing.
                    - UDP: User Datagram Protocol (RFC 768), 8-byte header (Source Port, Dest Port, Length, Checksum), Connectionless, Unreliable.
                    - TCP: Transmission Control Protocol, Reliable Byte Stream, Full-Duplex, 20-byte header, 3-Way Handshake (SYN, SYN+ACK, ACK), Connection Teardown (FIN, ACK), Sliding Window Flow Control, Congestion Control (Slow Start cwnd=1 MSS, Congestion Avoidance, Fast Retransmit, Fast Recovery).
                    - SCTP: Stream Control Transmission Protocol, Multihoming, Multistreaming, 4-Way Cookie Handshake.
                    - Quality of Service (QoS): Traffic Descriptors (Average/Peak Rate, Burst Size), Leaky Bucket & Token Bucket algorithms, RSVP, DiffServ (DSCP).
                    - Application Layer: DNS, WWW, HTTP/HTTPS, FTP, SMTP, POP3, IMAP, SNMP, SMI, MIB.

                    REGULAR EXAMINATIONS NOVEMBER 2024 (R22A0410) & MODEL QUESTIONS:
                    - Q1: Network Topologies, Transmission Modes, Bit Stuffing, Framing, Routing, Network Layer Functions, TCP Header Flags, Flow Control, DNS, HTTP properties.
                    - Q2: Compare OSI and TCP/IP Reference Models. Hamming Code construction for message 1011001 with error detection/correction.
                    - Q3: CRC Error Detection with Dataword 1001 and Divisor 1011. CSMA/CD working principle.
                    - Q4: IPv4 Classes & Subnetting vs IPv6. Transition mechanisms IPv4 to IPv6.
                    - Q5: TCP 3-Way Handshake connection establishment and termination scenarios. TCP Segment Header diagram & fields.
                    - Q6: DNS architecture & resolution. WWW Client-Server architecture and HTTP request/response format.
                    """.trimIndent(),
                    category = "Lecture Notes & Papers",
                    pageCount = 335
                )
            }

            // Document 2: Textbooks & Reference Links
            if (!filenames.contains("Computer_Networks_Textbooks_Reference_Links.pdf")) {
                repository.saveUploadedDocument(
                    title = "Computer Networks Textbooks & GitHub Download Links",
                    fileName = "Computer_Networks_Textbooks_Reference_Links.pdf",
                    fileType = "PDF",
                    fileSizeFormatted = "1.1 MB",
                    extractedText = """
                    RECOMMENDED TEXTBOOKS, REFERENCE BOOKS & DIRECT DOWNLOAD LINKS:

                    PRIMARY TEXTBOOKS:
                    1. Kurose, James F. & Ross, Keith W. — "Computer Networking: A Top-Down Approach", 6th & 7th Editions, Pearson.
                    2. Forouzan, Behrouz A. — "Data Communications and Networking", 4th & 5th Editions, McGraw-Hill Education / TMH, 2014-2017.

                    RECOMMENDED REFERENCE BOOKS:
                    1. Tanenbaum, Andrew S. & Wetherall, David J. — "Computer Networks", 4th & 5th Editions, Pearson Education, 2014.
                    2. Stallings, William — "Data and Computer Communications", 10th Edition, Pearson Education, 2017.
                    3. Halsall, Fred — "Computer Networking and the Internet", 5th Edition, Addison Wesley, 2005.

                    DIRECT ONLINE REFERENCE LINKS & DOWNLOAD REPOSITORIES:
                    1. Kurose & Ross Computer Networking Top-Down Approach 6th Edition PDF Repository:
                       https://github.com/kowsertusher/Book/blob/master/Computer.Networking%20A%20Top-Down%20Approach%206th%20Edition.pdf
                    2. Kurose James F & Ross Keith W 2017 Top-Down Pearson Edition PDF:
                       https://github.com/TimorYang/Computer-Networking-Keith-Ross/blob/main/book/Kurose%2C%20James%20F._Ross%2C%20Keith%20W%20-%20Computer%20networking_%20a%20top-down%20approach-Pearson%20(2017).pdf
                    3. Computer Networks A Top-Down Approach Dokumen Online Repository:
                       https://dokumen.pub/computer-networks-a-top-down-approach-9781259001567-1259001563.html
                    """.trimIndent(),
                    category = "Textbooks & References",
                    pageCount = 5
                )
            }

            // Document 3: NPTEL MOOCs Computer Networks Online Courses
            if (!filenames.contains("NPTEL_MOOCs_Computer_Networks_Courses.pdf")) {
                repository.saveUploadedDocument(
                    title = "NPTEL MOOCs Computer Networks Online Courses",
                    fileName = "NPTEL_MOOCs_Computer_Networks_Courses.pdf",
                    fileType = "PDF",
                    fileSizeFormatted = "0.8 MB",
                    extractedText = """
                    OFFICIAL NPTEL & MOOCs ONLINE VIDEO COURSES FOR COMPUTER NETWORKS:

                    1. Course Name: Computer Networks and Internet Protocol
                       Instructors: Prof. Soumya Kanti Ghosh, Prof. Sandip Chakraborty
                       Institute: Indian Institute of Technology (IIT) Kharagpur
                       Official NPTEL Link: https://onlinecourses.nptel.ac.in/noc25_cs15/preview
                       Coverage: Complete Internet Protocol Stack, OSI Layers, Routing, TCP/UDP, Socket Programming, Network Security.

                    2. Course Name: Advanced Computer Networks
                       Instructors: Prof. Neminath Hubballi, Prof. Sameer G Kulkarni
                       Institute: IIT Indore & IIT Gandhinagar
                       Official NPTEL Link: https://onlinecourses.nptel.ac.in/noc26_cs60/preview
                       Coverage: Advanced Switching, SDN (Software Defined Networking), Data Center Networking, High-Speed Routers, BGP Policy Routing.

                    3. Course Name: Introduction to Computer and Network Performance Analysis using Queuing Systems
                       Instructor: Prof. Varsha Apte
                       Institute: IIT Bombay
                       Official NPTEL Link: https://onlinecourses.nptel.ac.in/noc25_cs126/preview
                       Coverage: M/M/1 Queuing Models, Network Throughput, Delay Calculations, Packet Loss Analysis, Simulation & Performance Modeling.
                    """.trimIndent(),
                    category = "NPTEL & Video Courses",
                    pageCount = 3
                )
            }
        } catch (e: Throwable) {
            android.util.Log.e("NetChatViewModel", "Error seeding syllabus database: ${e.message}")
        }
    }

    fun deleteUploadedDocument(doc: UploadedDocumentEntity) {
        viewModelScope.launch {
            repository.deleteUploadedDocument(doc)
        }
    }

    fun askAiAboutDocument(doc: UploadedDocumentEntity) {
        val prompt = "I uploaded my syllabus book/PDF titled '${doc.title}'. Please analyze its contents, outline the main exam topics, key formulas/protocol steps, and suggest 5 probable viva questions for my university exam."
        sendMessage(prompt, topicContext = doc.title)
    }

    // Firebase Auth Handlers
    fun signInAnonymously() {
        viewModelScope.launch {
            com.example.data.remote.FirebaseManager.signInAnonymously()
        }
    }

    fun signOutFirebase() {
        com.example.data.remote.FirebaseManager.signOut()
    }

    // Voice & Audio Methods
    fun transcribeAndSendAudio(audioBase64: String) {
        viewModelScope.launch {
            _isTranscribing.value = true
            val text = repository.transcribeVoiceAudio(audioBase64)
            _isTranscribing.value = false
            if (text.isNotBlank()) {
                sendMessage(text)
            }
        }
    }

    fun setLiveVoiceActive(active: Boolean) {
        _isLiveVoiceActive.value = active
        if (!active) {
            _liveVoiceTranscript.value = ""
        }
    }

    fun sendLiveVoiceMessage(input: String, isAudio: Boolean = false) {
        viewModelScope.launch {
            _isGenerating.value = true
            _liveVoiceTranscript.value = "Listening & thinking..."
            val response = repository.queryLiveVoice(input, isAudio)
            _liveVoiceTranscript.value = response
            _isGenerating.value = false
            // Also add to chat log
            repository.saveChatMessage(sender = "user", text = if (isAudio) "🎤 Voice Message" else input, topicTag = "Live Voice")
            repository.saveChatMessage(sender = "ai", text = response, topicTag = "Live Voice")
        }
    }
}
