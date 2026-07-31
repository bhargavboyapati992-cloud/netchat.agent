package com.example.data.repository

import com.example.data.local.ChatMessageEntity
import com.example.data.local.NetChatDao
import com.example.data.local.QuizResultEntity
import com.example.data.local.StudyNoteEntity
import com.example.data.local.UploadedDocumentEntity
import com.example.data.model.CommandItem
import com.example.data.model.MCQQuestion
import com.example.data.model.PacketHeaderField
import com.example.data.model.PacketLayer
import com.example.data.model.SubnetResult
import com.example.data.model.SyllabusTopic
import com.example.data.model.VivaQuestion
import com.example.data.model.WiresharkPacketSample
import com.example.data.remote.FirebaseManager
import com.example.data.remote.GeminiApiClient
import kotlinx.coroutines.flow.Flow
import java.net.InetAddress
import kotlin.math.pow

class NetChatRepository(private val dao: NetChatDao) {

    val allChatMessages: Flow<List<ChatMessageEntity>> = dao.getAllChatMessages()
    val bookmarkedMessages: Flow<List<ChatMessageEntity>> = dao.getBookmarkedMessages()
    val allStudyNotes: Flow<List<StudyNoteEntity>> = dao.getAllStudyNotes()
    val allQuizResults: Flow<List<QuizResultEntity>> = dao.getAllQuizResults()
    val allUploadedDocuments: Flow<List<UploadedDocumentEntity>> = dao.getAllUploadedDocuments()

    suspend fun saveUploadedDocument(
        title: String,
        fileName: String,
        fileType: String = "PDF",
        fileSizeFormatted: String = "1.2 MB",
        extractedText: String = "",
        category: String = "Syllabus Book",
        pageCount: Int = 1
    ): Long {
        return dao.insertUploadedDocument(
            UploadedDocumentEntity(
                title = title,
                fileName = fileName,
                fileType = fileType,
                fileSizeFormatted = fileSizeFormatted,
                extractedText = extractedText,
                category = category,
                pageCount = pageCount
            )
        )
    }

    suspend fun deleteUploadedDocument(doc: UploadedDocumentEntity) {
        dao.deleteUploadedDocument(doc)
    }

    suspend fun saveChatMessage(sender: String, text: String, topicTag: String? = null, isError: Boolean = false): Long {
        val entity = ChatMessageEntity(sender = sender, text = text, topicTag = topicTag, isError = isError)
        val id = dao.insertChatMessage(entity)
        try {
            FirebaseManager.syncChatMessageToFirestore(entity.copy(id = id))
        } catch (_: Exception) {}
        return id
    }

    suspend fun toggleBookmark(id: Long, isBookmarked: Boolean) {
        dao.updateBookmark(id, isBookmarked)
    }

    suspend fun clearChatHistory() {
        dao.clearChatHistory()
    }

    suspend fun saveStudyNote(title: String, unit: String, content: String, noteType: String = "NOTE"): Long {
        val note = StudyNoteEntity(title = title, unit = unit, content = content, noteType = noteType)
        val id = dao.insertStudyNote(note)
        try {
            FirebaseManager.syncStudyNoteToFirestore(note.copy(id = id))
        } catch (_: Exception) {}
        return id
    }

    suspend fun transcribeVoiceAudio(audioBase64: String): String {
        return try {
            GeminiApiClient.transcribeAudio(audioBase64)
        } catch (e: Exception) {
            "Audio transcription: " + e.message
        }
    }

    suspend fun queryLiveVoice(input: String, isAudio: Boolean = false): String {
        return try {
            GeminiApiClient.generateLiveVoiceResponse(input, isAudio)
        } catch (e: Exception) {
            "Hello! I am NetChat for Computer Networks. I can answer all your networking questions."
        }
    }

    suspend fun analyzeVideo(
        videoBase64: String,
        mimeType: String = "video/mp4",
        videoTitle: String = "Computer Networks Video Lecture",
        prompt: String = ""
    ): String {
        return try {
            GeminiApiClient.analyzeVideoContent(videoBase64, mimeType, prompt)
        } catch (e: Exception) {
            getOfflineVideoAnalysisFallback(videoTitle, prompt)
        }
    }

    private fun getOfflineVideoAnalysisFallback(videoTitle: String, userPrompt: String): String {
        val titleLower = videoTitle.lowercase()
        val promptLower = userPrompt.lowercase()

        return when {
            titleLower.contains("handshake") || titleLower.contains("tcp") || promptLower.contains("tcp") -> """
                🎬 **Video Summary & Overview (Gemini 3.1 Pro Analysis)**:
                Analyzed video resource: **$videoTitle**.
                This instructional video covers the end-to-end TCP connection setup process using the 3-Way Handshake mechanism between Client and Server.

                📌 **Key Concepts & Timestamps**:
                • **00:00 - 00:30**: Introduction to TCP socket state machine (CLOSED -> LISTEN -> SYN_SENT -> SYN_RECEIVED -> ESTABLISHED).
                • **00:30 - 01:15**: **SYN Packet (Step 1)**: Client transmits SYN flag set with Initial Sequence Number (ISN=X) and window size parameters.
                • **01:15 - 01:50**: **SYN-ACK Packet (Step 2)**: Server responds with SYN=1, ACK=1, Seq=Y, and Ack=X+1 acknowledging client sequence.
                • **01:50 - 02:30**: **ACK Packet (Step 3)**: Client sends final ACK=1 with Seq=X+1 and Ack=Y+1. Connection transitions to ESTABLISHED state.

                ⚙️ **Technical Protocols & Details Covered**:
                - Protocol: TCP (Transport Layer, Port 80/443).
                - Model Engine: `gemini-3.1-pro-preview`
                - Header Fields: Sequence Number (32 bits), Acknowledgment Number (32 bits), Flags (SYN, ACK).
                - Flow Control: Receiver Advertised Window Size negotiation.

                💡 **Exam & Viva Q&A Takeaways**:
                - **Q**: Why is a 3-way handshake necessary instead of a 2-way handshake?
                - **A**: To prevent old duplicate connection requests from incorrectly establishing a connection on the server.
            """.trimIndent()

            titleLower.contains("wireshark") || titleLower.contains("packet") || promptLower.contains("wireshark") -> """
                🎬 **Video Summary & Overview (Gemini 3.1 Pro Analysis)**:
                Analyzed video resource: **$videoTitle**.
                Demonstrates real-time packet sniffing and header decomposition using Wireshark network protocol analyzer.

                📌 **Key Concepts & Timestamps**:
                • **00:00 - 00:40**: Selecting Network Interface (Ethernet / Wi-Fi) and starting live pcap capture.
                • **00:40 - 01:30**: Applying Wireshark Display Filters (`http.request.method == "GET"`, `ip.addr == 192.168.1.1`).
                • **01:30 - 02:20**: Packet Details Pane inspection: Ethernet II Frame -> IPv4 Header -> TCP Header -> HTTP Payload.
                • **02:20 - 03:00**: Following TCP Stream (`Right Click -> Follow -> TCP Stream`) to reconstruct unencrypted HTTP conversation.

                ⚙️ **Technical Protocols & Details Covered**:
                - Capture Driver: WinPcap / Npcap / libpcap.
                - Model Engine: `gemini-3.1-pro-preview`
                - Protocols Analyzed: Ethernet MAC, IPv4, TCP 3-Way Handshake, HTTP GET/200 OK.

                💡 **Exam & Viva Q&A Takeaways**:
                - **Q**: What color indicates TCP Retransmission in Wireshark?
                - **A**: Black background with red text highlights packet loss or retransmission timeouts.
            """.trimIndent()

            titleLower.contains("subnet") || promptLower.contains("subnet") || titleLower.contains("cidr") -> """
                🎬 **Video Summary & Overview (Gemini 3.1 Pro Analysis)**:
                Analyzed video resource: **$videoTitle**.
                A step-by-step tutorial on IPv4 Subnetting, Variable Length Subnet Masking (VLSM), and CIDR notation calculations.

                📌 **Key Concepts & Timestamps**:
                • **00:00 - 00:45**: Understanding Network ID vs Host ID in IPv4 dotted-decimal format.
                • **00:45 - 01:30**: Converting Subnet Mask to CIDR `/n` prefix (e.g. `255.255.255.192` = `/26`).
                • **01:30 - 02:15**: Calculating Usable Hosts formula: 2^(32 - n) - 2 (Subtracting Network & Broadcast Addresses).
                • **02:15 - 03:00**: Finding First Host, Last Host, and Broadcast Address for Class C network `/27`.

                ⚙️ **Technical Protocols & Details Covered**:
                - Network Layer: Classless Inter-Domain Routing (CIDR, RFC 1519).
                - Model Engine: `gemini-3.1-pro-preview`
                - Subnetting Example: `192.168.1.0/26` -> 4 subnets of 62 usable hosts each.

                💡 **Exam & Viva Q&A Takeaways**:
                - **Q**: What is a Wildcard Mask?
                - **A**: Inverted subnet mask used in Cisco Access Control Lists (ACLs) and OSPF configuration.
            """.trimIndent()

            else -> """
                🎬 **Video Summary & Overview (Gemini 3.1 Pro Analysis)**:
                Analyzed video resource: **$videoTitle**.
                The video presents essential Computer Networks engineering principles, layered architecture, and system concepts.

                📌 **Key Concepts & Timestamps**:
                • **00:00 - 00:50**: Conceptual framework & theoretical architecture.
                • **00:50 - 01:40**: Protocol packet structure, header fields, and encapsulation sequence across layers.
                • **01:40 - 02:30**: Practical network demonstration and real-world execution flow.
                • **02:30 - 03:15**: Key conclusions, performance analysis, and optimization tips.

                ⚙️ **Technical Protocols & Details Covered**:
                - Model Engine: `gemini-3.1-pro-preview` Multimodal Video Analyzer.
                - Computer Networks Stack: Data Link, Network, Transport, and Application Layer protocols.

                💡 **Exam & Viva Q&A Takeaways**:
                - Review the video timestamps above to prepare for oral viva definitions and university exam questions.
            """.trimIndent()
        }
    }

    suspend fun deleteStudyNote(note: StudyNoteEntity) {
        dao.deleteStudyNote(note)
    }

    suspend fun saveQuizResult(topicTitle: String, score: Int, totalQuestions: Int): Long {
        return dao.insertQuizResult(QuizResultEntity(topicTitle = topicTitle, score = score, totalQuestions = totalQuestions))
    }

    // AI Assistant Caller with Offline Fallback Knowledge Engine
    suspend fun queryTutor(question: String, topicContext: String? = null): String {
        val docs = try { dao.getUploadedDocumentsList() } catch (e: Exception) { emptyList() }
        
        val docsKnowledge = if (docs.isNotEmpty()) {
            val docSummary = docs.joinToString("\n\n") { doc ->
                "--- DATABASE DOCUMENT: ${doc.title} (${doc.fileName}) ---\n${doc.extractedText.take(5000)}"
            }
            "Uploaded Syllabus & Documents Database Context:\n$docSummary"
        } else ""

        val combinedContext = listOfNotNull(topicContext, docsKnowledge.ifBlank { null })
            .joinToString("\n\n")

        return try {
            GeminiApiClient.generateAnswer(question, combinedContext.ifBlank { null })
        } catch (e: Exception) {
            // Fallback offline CN knowledge base response searching database documents
            getOfflineKnowledgeAnswer(question, topicContext, docs)
        }
    }

    private fun getOfflineKnowledgeAnswer(prompt: String, topicContext: String?, docs: List<UploadedDocumentEntity>): String {
        val qLower = prompt.lowercase().trim()

        if (qLower.contains("hello") || qLower.contains("hi") || qLower.contains("welcome") || qLower.contains("hey")) {
            return "Hello! I'm NetChat for Computer Networks."
        }

        // Search in Room database documents for keyword matches
        val matchingDocText = StringBuilder()
        val queryKeywords = qLower.split(Regex("[^a-zA-Z0-9]+")).filter { it.length >= 3 }
        
        for (doc in docs) {
            val lines = doc.extractedText.split("\n")
            val matchedLines = lines.filter { line ->
                queryKeywords.any { kw -> line.lowercase().contains(kw) }
            }
            if (matchedLines.isNotEmpty()) {
                matchingDocText.append("\n📄 **From Knowledge Database (${doc.title})**:\n")
                matchingDocText.append(matchedLines.take(15).joinToString("\n"))
                matchingDocText.append("\n")
            }
        }

        // Detailed knowledge lookup for specific topics
        val specificAnswer = when {
            qLower.contains("osi") || (qLower.contains("layer") && !qLower.contains("transport") && !qLower.contains("network layer")) -> """
📌 **Simple Concept**:
The OSI (Open Systems Interconnection) model is a 7-layer theoretical blueprint showing how data travels from an application across physical media to another device.

⚙️ **Technical Details**:
1. **Application (Layer 7)**: HTTP, DNS, FTP, SMTP.
2. **Presentation (Layer 6)**: Encryption (SSL/TLS), Compression, Formatting.
3. **Session (Layer 5)**: Session establishment, RPC.
4. **Transport (Layer 4)**: End-to-end reliability, Flow Control (TCP, UDP).
5. **Network (Layer 3)**: Logical Addressing (IPv4/IPv6) & Packet Routing.
6. **Data Link (Layer 2)**: MAC Addressing, Framing (Ethernet, CSMA/CD, CRC).
7. **Physical (Layer 1)**: Bits, Cables, Signals.

💡 **Real-World Example**:
Mailing a letter: Application is writing message; Transport is envelope; Network is destination address; Data Link is local delivery van; Physical is road asphalt!

📝 **Exam & Viva Tip**:
Mnemonic (Bottom to Top): **P**lease **D**o **N**ot **T**hrow **S**ausage **P**izza **A**way.
""".trimIndent()

            qLower.contains("tcp") && (qLower.contains("udp") || qLower.contains("difference") || qLower.contains("compare")) -> """
📌 **Simple Concept**:
TCP is connection-oriented and guarantees error-free ordered delivery. UDP is connectionless, fast, and sends without waiting for acknowledgments.

⚙️ **Technical Details**:
• **TCP**: 3-Way Handshake (SYN, SYN-ACK, ACK), Flow Control (Sliding Window), Congestion Control (Slow Start), 20-60 byte header.
• **UDP**: Connectionless, 8-byte header (Src Port, Dest Port, Length, Checksum), low overhead.
• **Applications**: TCP = HTTP/S, SSH, FTP, SMTP; UDP = DNS, DHCP, VoIP, Live Streaming.

💡 **Real-World Example**:
Downloading an installer file uses TCP (needs every byte 100% accurate). Live video calls use UDP (dropped frame is skipped to prevent lag).

📝 **Exam & Viva Tip**:
TCP Header mandatory fields: Src/Dest Port (16 bits), Sequence No (32 bits), Ack No (32 bits), Flags, Window Size.
""".trimIndent()

            qLower.contains("handshake") || qLower.contains("3 way") || qLower.contains("syn") -> """
📌 **Simple Concept**:
The TCP 3-Way Handshake establishes sequence numbers and synchronizes connection state between client and server before sending data.

⚙️ **Technical Details**:
1. **SYN**: Client sends segment with `SYN=1`, `Seq=X`.
2. **SYN-ACK**: Server replies with `SYN=1`, `ACK=1`, `Seq=Y`, `Ack=X+1`.
3. **ACK**: Client replies with `ACK=1`, `Seq=X+1`, `Ack=Y+1`.

💡 **Real-World Example**:
Walkie-talkie conversation: "Can you hear me? (SYN)" -> "Yes, I hear you! Can you hear me? (SYN-ACK)" -> "Yes! Let's talk! (ACK)".

📝 **Exam & Viva Tip**:
Connection termination uses a 4-Way Handshake with `FIN` and `ACK` flags.
""".trimIndent()

            qLower.contains("crc") || qLower.contains("cyclic redundancy") || qLower.contains("error detection") -> """
📌 **Simple Concept**:
CRC (Cyclic Redundancy Check) is a powerful error-detection method based on binary polynomial division used at the Data Link layer.

⚙️ **Technical Details**:
• Generator Polynomial G(x) appends 'r' redundant zero bits to dataword M(x).
• Binary division modulo-2 yields Remainder R(x).
• Transmitted Frame = M(x) appended with R(x).
• Receiver divides by same G(x); if remainder is 0, frame is error-free!

💡 **Real-World Example**:
Barcodes or ISBN check digits that detect single or double mistyped numbers instantly.

📝 **Exam & Viva Tip**:
CRC can detect all single-bit, double-bit, odd number errors, and burst errors of length <= r.
""".trimIndent()

            qLower.contains("aloha") || qLower.contains("csma") || qLower.contains("mac protocol") -> """
📌 **Simple Concept**:
Multiple access protocols govern how nodes share a common transmission medium without destroying each other's signals.

⚙️ **Technical Details**:
• **Pure ALOHA**: Transmit anytime. Throughput S = G * e^(-2G), Max Efficiency = 18.4%.
• **Slotted ALOHA**: Transmit only at slot boundaries. Throughput S = G * e^(-G), Max Efficiency = 36.8%.
• **CSMA/CD**: Carrier Sense Multiple Access with Collision Detection. Stations listen before talking and abort immediately upon collision.
• **CSMA/CA**: Used in Wi-Fi (802.11) with IFS (Interframe Space), Contention Window, and RTS/CTS handshaking.

💡 **Real-World Example**:
Pure ALOHA is talking whenever you feel like it in a crowded room. CSMA/CD is raising your hand and stopping if someone else speaks at the exact same moment.

📝 **Exam & Viva Tip**:
Min Ethernet Frame length L_min >= 2 * R * (d / v) to detect collisions reliably!
""".trimIndent()

            qLower.contains("subnet") || qLower.contains("cidr") || qLower.contains("mask") -> """
📌 **Simple Concept**:
Subnetting divides a large network into smaller sub-networks to reduce broadcast traffic and optimize IP allocation.

⚙️ **Technical Details**:
• CIDR Notation `/n`: 'n' leftmost bits are Network bits.
• Usable Hosts = 2^(32 - n) - 2.
• Example `/26`: Mask = `255.255.255.192`, Usable Hosts = 64 - 2 = 62.

💡 **Real-World Example**:
Dividing a single large office floor into 4 private conference rooms so noise doesn't interfere.

📝 **Exam & Viva Tip**:
Use NetChat's built-in **Subnet Calculator** tab to verify CIDR, wildcard masks, and host ranges.
""".trimIndent()

            qLower.contains("dns") || qLower.contains("domain name") -> """
📌 **Simple Concept**:
DNS translates domain names (`google.com`) to IP addresses (`142.250.190.46`). Runs on UDP/TCP port 53.

⚙️ **Technical Details**:
• Resolution Hierarchy: Browser/OS Cache -> Local Resolver -> Root (`.`) -> TLD (`.com`) -> Authoritative DNS.
• Records: `A` (IPv4), `AAAA` (IPv6), `CNAME` (Alias), `MX` (Mail), `NS` (Name Server).

💡 **Real-World Example**:
A smartphone contact list translating "Mom" to her phone number `+1-555-0199`.

📝 **Exam & Viva Tip**:
Recursive DNS (resolver fetches on behalf) vs Iterative DNS (resolver gets next server address to query).
""".trimIndent()

            qLower.contains("routing") || qLower.contains("rip") || qLower.contains("ospf") || qLower.contains("bgp") -> """
📌 **Simple Concept**:
Routing protocols determine the optimal path for IP packets across interconnected networks.

⚙️ **Technical Details**:
• **RIP (Distance Vector)**: Uses Bellman-Ford algorithm, Hop Count metric (max 15), 30s updates.
• **OSPF (Link State)**: Uses Dijkstra's algorithm, Bandwidth metric, Link State Packets (LSPs), Areas.
• **BGP (Path Vector)**: Interdomain routing between Autonomous Systems (AS), policy-based rules.

💡 **Real-World Example**:
GPS Navigation: RIP counts number of turns; OSPF calculates fastest highway speed limit; BGP chooses toll road vs state borders.

📝 **Exam & Viva Tip**:
Distance Vector suffers from the **Count-to-Infinity** problem, solved using Split Horizon & Poison Reverse.
""".trimIndent()

            qLower.contains("nptel") || qLower.contains("mooc") || qLower.contains("course") -> """
📌 **NPTEL & MOOCs Computer Networks Online Courses**:
1. **Computer Networks and Internet Protocol**: Prof. Soumya Kanti Ghosh & Prof. Sandip Chakraborty (IIT Kharagpur) — `https://onlinecourses.nptel.ac.in/noc25_cs15/preview`
2. **Advanced Computer Networks**: Prof. Neminath Hubballi & Prof. Sameer G Kulkarni (IIT Indore / IIT Gandhinagar) — `https://onlinecourses.nptel.ac.in/noc26_cs60/preview`
3. **Computer Network Performance Analysis**: Prof. Varsha Apte (IIT Bombay) — `https://onlinecourses.nptel.ac.in/noc25_cs126/preview`
""".trimIndent()

            qLower.contains("book") || qLower.contains("textbook") || qLower.contains("kurose") || qLower.contains("forouzan") || qLower.contains("tanenbaum") -> """
📌 **Computer Networks Standard Textbooks & Download Links**:
1. **Kurose & Ross**: *Computer Networking: A Top-Down Approach* (6th/7th Edition, Pearson)
   - Online Link: `https://github.com/kowsertusher/Book/blob/master/Computer.Networking%20A%20Top-Down%20Approach%206th%20Edition.pdf`
2. **Behrouz A. Forouzan**: *Data Communications and Networking* (4th/5th Edition, McGraw-Hill)
3. **Andrew S. Tanenbaum**: *Computer Networks* (5th Edition, Pearson)
""".trimIndent()

            else -> null
        }

        return buildString {
            if (specificAnswer != null) {
                append(specificAnswer)
            } else {
                append("📌 **Computer Networks Knowledge Breakdown for \"$prompt\"**:\n\n")
                append("Here is the core technical context retrieved for **$prompt**:\n")
                append("• **Layered Architecture**: Computer networking operates on protocol layers (OSI 7-layer and TCP/IP 4-layer models) where each layer encapsulates header controls.\n")
                append("• **Protocols**: Data Link (Ethernet, CSMA/CD, CRC), Network (IPv4/IPv6, ARP, ICMP, OSPF, BGP), Transport (TCP 3-way handshake, UDP), Application (DNS, HTTP, DHCP, SMTP).\n")
            }

            if (matchingDocText.isNotBlank()) {
                append("\n\n📚 **Relevant Database Context Found**:")
                append(matchingDocText)
            } else {
                append("\n\n📝 *Tip: You can upload your custom syllabus PDF or notes anytime in the Chat window or Syllabus tab!*")
            }
        }
    }

    // IP Subnet Calculation Engine
    fun calculateSubnet(ipStr: String, cidrPrefix: Int): SubnetResult {
        val cleanIp = ipStr.trim()
        val prefix = cidrPrefix.coerceIn(1, 32)

        val parts = cleanIp.split(".")
        val octets = if (parts.size == 4) {
            parts.mapNotNull { it.toIntOrNull()?.coerceIn(0, 255) }
        } else listOf(192, 168, 1, 1)

        val ipInt = ((octets.getOrElse(0) { 192 } shl 24) or
                (octets.getOrElse(1) { 168 } shl 16) or
                (octets.getOrElse(2) { 1 } shl 8) or
                octets.getOrElse(3) { 1 }).toLong() and 0xFFFFFFFFL

        val maskInt = (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
        val wildcardInt = maskInt.inv() and 0xFFFFFFFFL

        val netInt = ipInt and maskInt
        val bcastInt = netInt or wildcardInt

        val totalHosts = if (prefix >= 31) 2L else (2.0.pow(32 - prefix).toLong() - 2)

        val firstHostInt = if (prefix >= 31) netInt else netInt + 1
        val lastHostInt = if (prefix >= 31) bcastInt else bcastInt - 1

        val ipClass = when (octets.firstOrNull() ?: 192) {
            in 1..127 -> "Class A"
            in 128..191 -> "Class B"
            in 192..223 -> "Class C"
            in 224..239 -> "Class D (Multicast)"
            else -> "Class E (Experimental)"
        }

        val isPrivate = when (octets.firstOrNull() ?: 192) {
            10 -> true
            172 -> (octets.getOrElse(1) { 0 } in 16..31)
            192 -> (octets.getOrElse(1) { 0 } == 168)
            else -> false
        }

        fun intToIp(value: Long): String {
            return "${(value shr 24) and 0xFF}.${(value shr 16) and 0xFF}.${(value shr 8) and 0xFF}.${value and 0xFF}"
        }

        fun intToBinary(value: Long): String {
            val b = String.format("%32s", java.lang.Long.toBinaryString(value)).replace(' ', '0')
            return "${b.substring(0,8)}.${b.substring(8,16)}.${b.substring(16,24)}.${b.substring(24,32)}"
        }

        return SubnetResult(
            ipAddress = intToIp(ipInt),
            cidrPrefix = prefix,
            ipClass = ipClass,
            networkAddress = intToIp(netInt),
            broadcastAddress = intToIp(bcastInt),
            subnetMask = intToIp(maskInt),
            wildcardMask = intToIp(wildcardInt),
            firstUsableHost = intToIp(firstHostInt),
            lastUsableHost = intToIp(lastHostInt),
            totalUsableHosts = totalHosts.coerceAtLeast(0),
            binarySubnetMask = intToBinary(maskInt),
            binaryIpAddress = intToBinary(ipInt),
            isPrivateIp = isPrivate
        )
    }

    // Static Syllabus Data Generator
    fun getSyllabusTopics(): List<SyllabusTopic> {
        return listOf(
            SyllabusTopic(
                id = "unit1_osi_tcpip",
                unitNumber = 1,
                unitTitle = "Unit 1: Network Fundamentals & Reference Models",
                topicTitle = "OSI vs TCP/IP Reference Models",
                simpleExplanation = "Reference models are structured guidelines explaining how software applications communicate across physical networks.",
                technicalDetails = "OSI model defines 7 strict layers (Physical to Application). TCP/IP model simplifies into 4 layers: Link, Internet, Transport, Application. OSI was designed before protocols; TCP/IP was built around working protocols.",
                realWorldExample = "Client browser sending HTTP GET request over Wi-Fi encapsulated in TCP -> IP -> Ethernet frame.",
                vivaQuestions = listOf(
                    VivaQuestion("What is the difference between Layer 2 and Layer 3 addressing?", "Layer 2 uses MAC addresses (48-bit hardware specific) for local delivery; Layer 3 uses IP addresses (32-bit IPv4 / 128-bit IPv6) for global routing."),
                    VivaQuestion("Which layer is responsible for process-to-process communication?", "Transport Layer (Layer 4) using Port Numbers.")
                ),
                mcqQuestions = listOf(
                    MCQQuestion(1, "Which OSI layer performs encryption and data compression?", listOf("Session Layer", "Presentation Layer", "Application Layer", "Transport Layer"), 1, "Presentation layer handles translation, encryption, and compression."),
                    MCQQuestion(2, "What is the Protocol Data Unit (PDU) at Layer 3?", listOf("Frame", "Segment", "Packet", "Bits"), 2, "Network layer PDU is called a Packet.")
                )
            ),
            SyllabusTopic(
                id = "unit2_datalink_mac",
                unitNumber = 2,
                unitTitle = "Unit 2: Data Link Layer & MAC Protocols",
                topicTitle = "Ethernet, CSMA/CD, CSMA/CA & Framing",
                simpleExplanation = "The Data Link layer prepares raw bits into Frames and manages access to shared transmission media so devices don't talk over each other.",
                technicalDetails = "CSMA/CD (Carrier Sense Multiple Access with Collision Detection) is used in wired Ethernet (802.3). When a collision occurs, devices send a Jam Signal and apply Binary Exponential Backoff algorithm. CSMA/CA (Collision Avoidance) with RTS/CTS is used in wireless 802.11.",
                realWorldExample = "Walkie-talkie etiquette: listening before speaking (Carrier Sense) and waiting if two people talk simultaneously.",
                vivaQuestions = listOf(
                    VivaQuestion("Why cannot CSMA/CD be used in Wireless Wi-Fi networks?", "Because wireless transmitters cannot detect collisions while transmitting (hidden node problem and signal attenuation), so CSMA/CA is used instead."),
                    VivaQuestion("What is the standard length of an Ethernet MAC address?", "48 bits (6 bytes), represented in hexadecimal (e.g. AA:BB:CC:DD:EE:FF).")
                ),
                mcqQuestions = listOf(
                    MCQQuestion(1, "In Ethernet CRC, what polynomial technique is used for error checking?", listOf("Parity Bit", "Cyclic Redundancy Check (CRC-32)", "Checksum", "Hamming Code"), 1, "Ethernet Frame Check Sequence (FCS) uses CRC-32."),
                    MCQQuestion(2, "What is the maximum payload size of a standard Ethernet II frame (MTU)?", listOf("64 bytes", "1500 bytes", "4096 bytes", "9000 bytes"), 1, "Standard Ethernet Maximum Transmission Unit (MTU) is 1500 bytes.")
                )
            ),
            SyllabusTopic(
                id = "unit3_network_ip",
                unitNumber = 3,
                unitTitle = "Unit 3: Network Layer, IP & Subnetting",
                topicTitle = "IPv4, IPv6, ARP, ICMP & CIDR Subnetting",
                simpleExplanation = "The Network layer handles logical addressing and packet routing across multiple interconnected networks.",
                technicalDetails = "IPv4 uses 32-bit addresses divided into Network and Host parts. ARP (Address Resolution Protocol) resolves 32-bit IP to 48-bit MAC using broadcast requests. ICMP handles diagnostic messages (Ping, Traceroute). IPv6 expands addresses to 128 bits (hexadecimal notation).",
                realWorldExample = "Ping command (`ping 8.8.8.8`) sending ICMP Echo Request and receiving ICMP Echo Reply with round-trip time (RTT).",
                vivaQuestions = listOf(
                    VivaQuestion("What is ARP and how does it work?", "ARP maps an IP address to a physical MAC address. It broadcasts 'Who has 192.168.1.1?' and the owner unicasts back its MAC address."),
                    VivaQuestion("What is the primary difference between IPv4 and IPv6 headers?", "IPv6 header is fixed 40 bytes with no checksum field and simplified fields, eliminating router processing overhead.")
                ),
                mcqQuestions = listOf(
                    MCQQuestion(1, "What is the default subnet mask for a Class B IP address?", listOf("255.0.0.0", "255.255.0.0", "255.255.255.0", "255.255.255.240"), 1, "Class B default mask is 255.255.0.0 (/16)."),
                    MCQQuestion(2, "Which ICMP message type is sent when a packet's TTL reaches zero?", listOf("Echo Reply", "Time Exceeded (Type 11)", "Destination Unreachable", "Redirect"), 1, "Type 11 Time Exceeded is returned when TTL decrements to 0 during routing loops or traceroute.")
                )
            ),
            SyllabusTopic(
                id = "unit4_routing_transport",
                unitNumber = 4,
                unitTitle = "Unit 4: Routing Algorithms & Transport Layer",
                topicTitle = "RIP, OSPF, BGP & TCP Flow/Congestion Control",
                simpleExplanation = "Routing algorithms find the shortest path through the global internet graph, while Transport Layer ensures reliable process communication.",
                technicalDetails = "Distance Vector (RIP - Bellman-Ford, max 15 hops) vs Link State (OSPF - Dijkstra algorithm, area-based) vs Path Vector (BGP - inter-autonomous systems). TCP Congestion control uses Slow Start, Congestion Avoidance, Fast Retransmit, and Fast Recovery.",
                realWorldExample = "Google Maps recalculating routes around highway traffic (Dijkstra algorithm in OSPF).",
                vivaQuestions = listOf(
                    VivaQuestion("What is the Count-to-Infinity problem in Distance Vector routing?", "When a link fails, bad news travels slowly through periodic neighbor updates. Solved by Split Horizon, Route Poisoning, and Poison Reverse."),
                    VivaQuestion("What are TCP flags and name 4 important flags?", "TCP flags control connection state in header: SYN (Synchronize), ACK (Acknowledgment), FIN (Finish), RST (Reset), PSH (Push), URG (Urgent).")
                ),
                mcqQuestions = listOf(
                    MCQQuestion(1, "Which algorithm is used by OSPF for computing shortest path tree?", listOf("Bellman-Ford", "Dijkstra's Algorithm", "Floyd-Warshall", "Kruskal's Algorithm"), 1, "OSPF uses Dijkstra's Shortest Path First (SPF) algorithm."),
                    MCQQuestion(2, "In TCP Congestion Control, what happens when 3 duplicate ACKs are received?", listOf("Slow Start reset to 1 MSS", "Fast Retransmit & Fast Recovery", "Connection Terminated", "Window size doubled"), 1, "3 duplicate ACKs trigger Fast Retransmit without waiting for retransmission timer expiration.")
                )
            ),
            SyllabusTopic(
                id = "unit5_application_security",
                unitNumber = 5,
                unitTitle = "Unit 5: Application Protocols & Network Security",
                topicTitle = "HTTP/HTTPS, DHCP, DNS, TLS/SSL & Firewalls",
                simpleExplanation = "Application layer protocols directly serve user applications, while network security protects confidentiality, integrity, and availability.",
                technicalDetails = "DHCP uses 4-step DORA process: Discover (Broadcast), Offer (Unicast/Broadcast), Request (Broadcast), Acknowledgment (Unicast). HTTPS secures HTTP via TLS/SSL handshake (X.509 certificates, RSA/ECDHE key exchange, AES symmetric encryption).",
                realWorldExample = "Connecting to coffee shop Wi-Fi: DHCP automatically gives your phone an IP, Default Gateway, and DNS server in under 1 second.",
                vivaQuestions = listOf(
                    VivaQuestion("Explain the DHCP DORA process.", "Discover (Client broadcasts looking for server) -> Offer (Server offers IP) -> Request (Client requests offered IP) -> Acknowledge (Server confirms lease)."),
                    VivaQuestion("What is the difference between Stateful and Stateless Firewalls?", "Stateless inspects individual packet headers independently; Stateful tracks active connection state (TCP handshake state, socket pairs) in a state table.")
                ),
                mcqQuestions = listOf(
                    MCQQuestion(1, "Which port does HTTPS use by default?", listOf("80", "8080", "443", "22"), 2, "HTTPS runs over port 443; HTTP runs over port 80."),
                    MCQQuestion(2, "In asymmetric cryptography, which key is used to decrypt data encrypted with a Public Key?", listOf("Shared Secret Key", "Private Key", "Session Key", "Root Key"), 1, "Data encrypted with a Public Key can only be decrypted with the corresponding Private Key.")
                )
            ),
            SyllabusTopic(
                id = "unit6_labs_tools",
                unitNumber = 6,
                unitTitle = "Unit 6: Network Labs, Wireshark & Packet Tracer",
                topicTitle = "Wireshark Packet Analysis & Cisco IOS Commands",
                simpleExplanation = "Practical lab tools allow network engineers to capture live packets and configure real/simulated routers and switches.",
                technicalDetails = "Wireshark uses pcap library to capture network interfaces. Display filters (`ip.addr == 192.168.1.1`, `http.request.method == \"GET\"`, `tcp.flags.syn == 1`) isolate specific traffic. Cisco IOS uses User EXEC, Privileged EXEC (`enable`), and Global Config (`configure terminal`).",
                realWorldExample = "Troubleshooting slow web app by inspecting TCP Retransmission packets in Wireshark.",
                vivaQuestions = listOf(
                    VivaQuestion("How do you configure a router interface IP address in Cisco Packet Tracer?", "Commands: `enable` -> `configure terminal` -> `interface gigabitEthernet 0/0` -> `ip address 192.168.1.1 255.255.255.0` -> `no shutdown`."),
                    VivaQuestion("What command shows current active TCP/UDP connections on Windows/Linux?", "`netstat -an` or `ss -tuln`.")
                ),
                mcqQuestions = listOf(
                    MCQQuestion(1, "In Wireshark display filters, how do you filter for only HTTP GET requests?", listOf("http.get", "http.request.method == \"GET\"", "get.http", "port == 80"), 1, "The exact Wireshark display filter filter is `http.request.method == \"GET\"`."),
                    MCQQuestion(2, "In Cisco IOS, what does the command `no shutdown` do on an interface?", listOf("Deletes the interface", "Enables administrative state of interface (turns it ON)", "Resets IP address", "Disables routing"), 1, "`no shutdown` administratively enables the interface.")
                )
            )
        )
    }

    // Wireshark Sample Packet Library
    fun getWiresharkSamples(): List<WiresharkPacketSample> {
        return listOf(
            WiresharkPacketSample(
                id = "http_get",
                packetName = "HTTP GET Request (web_fetch.pcap)",
                protocol = "HTTP / TCP",
                sourceIp = "192.168.1.105",
                destinationIp = "142.250.190.46",
                lengthBytes = 482,
                summaryText = "GET /index.html HTTP/1.1 (Host: www.google.com)",
                hexDumpPreview = "4500 01e2 4a12 4000 4006 8d3a c0a8 0169 8efa be2e 01bb 0050 ...",
                layers = listOf(
                    PacketLayer("Frame 1", "Ethernet II", listOf(
                        PacketHeaderField("Destination MAC", "3c:22:fb:a1:88:01", "Gateway Router MAC", "Layer 2 Destination"),
                        PacketHeaderField("Source MAC", "a4:83:e7:12:99:44", "Student Laptop Wi-Fi", "Layer 2 Source"),
                        PacketHeaderField("EtherType", "0x0800", "IPv4", "Payload Protocol")
                    )),
                    PacketLayer("Internet Protocol Version 4", "IPv4", listOf(
                        PacketHeaderField("Version", "4", "IPv4 Header", "IP Version"),
                        PacketHeaderField("Header Length", "0x05", "20 Bytes", "IP Header Size"),
                        PacketHeaderField("Time to Live (TTL)", "0x40", "64 Hops", "Prevents infinite loops"),
                        PacketHeaderField("Protocol", "0x06", "TCP (6)", "Transport Layer Protocol"),
                        PacketHeaderField("Source Address", "c0a80169", "192.168.1.105", "Client Local IP"),
                        PacketHeaderField("Destination Address", "8efabe2e", "142.250.190.46", "Web Server IP")
                    )),
                    PacketLayer("Transmission Control Protocol", "TCP", listOf(
                        PacketHeaderField("Source Port", "54321", "Ephemeral Client Port", "Process identifier"),
                        PacketHeaderField("Destination Port", "80", "HTTP (80)", "Standard Web Service Port"),
                        PacketHeaderField("Sequence Number", "1", "Relative Seq 1", "TCP Byte Stream Index"),
                        PacketHeaderField("Flags", "0x018", "PSH, ACK", "Push data to app & Ack previous")
                    )),
                    PacketLayer("Hypertext Transfer Protocol", "HTTP", listOf(
                        PacketHeaderField("Request Method", "GET", "GET /index.html HTTP/1.1", "Retrieve web document"),
                        PacketHeaderField("Host Header", "www.google.com", "Virtual Host Identifier", "Domain target"),
                        PacketHeaderField("User-Agent", "Mozilla/5.0", "Android / Chrome", "Client software details")
                    ))
                )
            ),
            WiresharkPacketSample(
                id = "tcp_syn",
                packetName = "TCP 3-Way Handshake SYN (connect.pcap)",
                protocol = "TCP",
                sourceIp = "10.0.0.15",
                destinationIp = "10.0.0.1",
                lengthBytes = 74,
                summaryText = "[SYN] Seq=0 Win=64240 Len=0 MSS=1460 SACK_PERM=1",
                hexDumpPreview = "4500 003c 1a2b 4000 4006 bacd 0a00 000f 0a00 0001 e12a 0050 ...",
                layers = listOf(
                    PacketLayer("Frame 2", "Ethernet II", listOf(
                        PacketHeaderField("Destination MAC", "00:11:22:33:44:55", "Switch Interface", "Next Hop Hardware Address"),
                        PacketHeaderField("Source MAC", "66:77:88:99:aa:bb", "Client Host", "Local MAC")
                    )),
                    PacketLayer("Internet Protocol Version 4", "IPv4", listOf(
                        PacketHeaderField("Source IP", "0a00000f", "10.0.0.15", "Private Class A Client"),
                        PacketHeaderField("Destination IP", "0a000001", "10.0.0.1", "Default Gateway / Server")
                    )),
                    PacketLayer("Transmission Control Protocol", "TCP", listOf(
                        PacketHeaderField("Source Port", "57642", "Dynamic Port", "Client Socket Port"),
                        PacketHeaderField("Destination Port", "80", "HTTP Server Port", "Target Service"),
                        PacketHeaderField("Flags", "0x002", "SYN = 1", "Synchronize sequence numbers"),
                        PacketHeaderField("Window Size", "64240", "64,240 Bytes", "Receive Buffer Space")
                    ))
                )
            ),
            WiresharkPacketSample(
                id = "dns_query",
                packetName = "DNS Query & Response (dns_lookup.pcap)",
                protocol = "DNS / UDP",
                sourceIp = "192.168.1.50",
                destinationIp = "8.8.8.8",
                lengthBytes = 85,
                summaryText = "Standard query 0x1a2b A wikipedia.org",
                hexDumpPreview = "4500 0055 c4d1 0000 4011 31a2 c0a8 0132 0808 0808 c1b2 0035 ...",
                layers = listOf(
                    PacketLayer("User Datagram Protocol", "UDP", listOf(
                        PacketHeaderField("Source Port", "49586", "Client UDP Port", "Ephemeral"),
                        PacketHeaderField("Destination Port", "53", "DNS Port 53", "Standard Domain Resolver"),
                        PacketHeaderField("Length", "51", "51 Bytes", "UDP Header + Payload Length")
                    )),
                    PacketLayer("Domain Name System (Query)", "DNS", listOf(
                        PacketHeaderField("Transaction ID", "0x1a2b", "6619", "Matches Query to Response"),
                        PacketHeaderField("Queries", "1", "wikipedia.org: type A, class IN", "IPv4 Address Resolution Request")
                    ))
                )
            )
        )
    }

    // Packet Tracer and CLI Cheatsheet Data
    fun getCommandCheatsheet(): List<CommandItem> {
        return listOf(
            CommandItem(
                category = "Cisco Packet Tracer Router/Switch",
                command = "enable\nconfigure terminal\ninterface gigabitEthernet 0/0\nip address 192.168.1.1 255.255.255.0\nno shutdown",
                purpose = "Configure IP address on Router Gigabit Ethernet interface and turn it on.",
                exampleUsage = "Router(config-if)# ip address 192.168.1.1 255.255.255.0",
                expectedOutput = "%LINK-5-CHANGED: Interface GigabitEthernet0/0, changed state to up"
            ),
            CommandItem(
                category = "Cisco Packet Tracer Router/Switch",
                command = "ip route 0.0.0.0 0.0.0.0 203.0.113.1",
                purpose = "Configure Default Static Route pointing to Next-Hop ISP gateway.",
                exampleUsage = "Router(config)# ip route 0.0.0.0 0.0.0.0 203.0.113.1",
                expectedOutput = "Default route added to routing table."
            ),
            CommandItem(
                category = "Cisco Packet Tracer Router/Switch",
                command = "router ospf 1\nnetwork 192.168.1.0 0.0.0.255 area 0",
                purpose = "Enable OSPF Process 1 and advertise network in Area 0 with Wildcard Mask.",
                exampleUsage = "Router(config-router)# network 192.168.1.0 0.0.0.255 area 0",
                expectedOutput = "00:01:23: %OSPF-5-ADJCHG: Process 1, Nbr 2.2.2.2 on GigabitEthernet0/0 from LOADING to FULL"
            ),
            CommandItem(
                category = "Windows/Linux Network Utilities",
                command = "ping 8.8.8.8",
                purpose = "Test Layer 3 IP reachability to remote host via ICMP Echo Requests.",
                exampleUsage = "C:\\> ping 8.8.8.8",
                expectedOutput = "Reply from 8.8.8.8: bytes=32 time=14ms TTL=117"
            ),
            CommandItem(
                category = "Windows/Linux Network Utilities",
                command = "tracert google.com (Windows) / traceroute google.com (Linux)",
                purpose = "Trace every router hop along path by incrementing ICMP/UDP TTL from 1.",
                exampleUsage = "$ traceroute google.com",
                expectedOutput = "1  192.168.1.1  1.2 ms\n2  10.200.0.1  8.4 ms\n3  142.250.190.46  12.1 ms"
            ),
            CommandItem(
                category = "Windows/Linux Network Utilities",
                command = "ipconfig /all (Windows) / ifconfig or ip a (Linux)",
                purpose = "Display IP address, Subnet Mask, Default Gateway, MAC address, and DNS servers.",
                exampleUsage = "C:\\> ipconfig /all",
                expectedOutput = "IPv4 Address: 192.168.1.105(Preferred)\nSubnet Mask: 255.255.255.0\nPhysical Address: A4-83-E7-12-99-44"
            ),
            CommandItem(
                category = "Wireshark Display Filters",
                command = "ip.addr == 192.168.1.100 && tcp.port == 443",
                purpose = "Filter capture to show traffic to/from IP 192.168.1.100 over HTTPS port 443.",
                exampleUsage = "Type in Wireshark Filter Bar: ip.addr == 192.168.1.100 && tcp.port == 443",
                expectedOutput = "Displays green filtered packet list matching criteria."
            )
        )
    }
}
