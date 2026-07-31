package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommandItem
import com.example.data.model.SubnetResult
import com.example.data.model.WiresharkPacketSample

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CircularProgressIndicator

@Composable
fun ToolsScreen(
    subnetResult: SubnetResult?,
    onCalculateSubnet: (String, Int) -> Unit,
    wiresharkSamples: List<WiresharkPacketSample>,
    selectedPacket: WiresharkPacketSample?,
    onSelectWiresharkPacket: (WiresharkPacketSample) -> Unit,
    commandCheatsheet: List<CommandItem>,
    onAskAiAboutTool: (String) -> Unit,
    isVideoAnalyzing: Boolean = false,
    videoAnalysisResult: String? = null,
    selectedVideoTitle: String = "",
    onAnalyzeVideo: (Uri?, String?, String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var toolSubTab by remember { mutableIntStateOf(0) } // 0 = Subnet, 1 = Wireshark, 2 = CLI Cheatsheet, 3 = Video AI

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tool Sub-Tabs
        TabRow(
            selectedTabIndex = toolSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = (toolSubTab == 0),
                onClick = { toolSubTab = 0 },
                text = { Text("Subnet", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tool_tab_subnet")
            )
            Tab(
                selected = (toolSubTab == 1),
                onClick = { toolSubTab = 1 },
                text = { Text("Wireshark", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tool_tab_wireshark")
            )
            Tab(
                selected = (toolSubTab == 2),
                onClick = { toolSubTab = 2 },
                text = { Text("CLI Commands", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tool_tab_cli")
            )
            Tab(
                selected = (toolSubTab == 3),
                onClick = { toolSubTab = 3 },
                text = { Text("Video AI", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tool_tab_video_ai")
            )
        }

        when (toolSubTab) {
            0 -> SubnetCalculatorTool(subnetResult, onCalculateSubnet, onAskAiAboutTool)
            1 -> WiresharkAnalyzerTool(wiresharkSamples, selectedPacket, onSelectWiresharkPacket, onAskAiAboutTool)
            2 -> CliCheatsheetTool(commandCheatsheet, onAskAiAboutTool)
            3 -> VideoAnalysisTool(
                isVideoAnalyzing = isVideoAnalyzing,
                videoAnalysisResult = videoAnalysisResult,
                selectedVideoTitle = selectedVideoTitle,
                onAnalyzeVideo = onAnalyzeVideo,
                onAskAiAboutTool = onAskAiAboutTool
            )
        }
    }
}

@Composable
private fun SubnetCalculatorTool(
    subnetResult: SubnetResult?,
    onCalculateSubnet: (String, Int) -> Unit,
    onAskAiAboutTool: (String) -> Unit
) {
    var ipInput by remember { mutableStateOf("192.168.1.100") }
    var cidrInput by remember { mutableIntStateOf(24) }

    val quickCidrs = listOf(8, 16, 24, 26, 27, 28, 30)

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
                        text = "🧮 IP Subnet & VLSM Calculator",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = {
                            ipInput = it
                            onCalculateSubnet(ipInput, cidrInput)
                        },
                        label = { Text("IPv4 Address") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ip_input_field"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "CIDR Prefix Mask: /$cidrInput",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Slider(
                        value = cidrInput.toFloat(),
                        onValueChange = {
                            cidrInput = it.toInt()
                            onCalculateSubnet(ipInput, cidrInput)
                        },
                        valueRange = 1f..30f,
                        steps = 28,
                        modifier = Modifier.testTag("cidr_slider")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickCidrs.forEach { c ->
                            FilterChip(
                                selected = (cidrInput == c),
                                onClick = {
                                    cidrInput = c
                                    onCalculateSubnet(ipInput, cidrInput)
                                },
                                label = { Text("/$c", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }

        if (subnetResult != null) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${subnetResult.ipClass} • ${if (subnetResult.isPrivateIp) "Private RFC1918" else "Public IP"}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.tertiary
                            )

                            Button(
                                onClick = {
                                    onAskAiAboutTool("Explain step-by-step how subnetting works for ${subnetResult.ipAddress}/${subnetResult.cidrPrefix} including network address ${subnetResult.networkAddress}, broadcast address ${subnetResult.broadcastAddress}, and total ${subnetResult.totalUsableHosts} hosts.")
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ask AI Explanation", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        SubnetDetailRow("Network Address", subnetResult.networkAddress)
                        SubnetDetailRow("Broadcast Address", subnetResult.broadcastAddress)
                        SubnetDetailRow("Subnet Mask", "${subnetResult.subnetMask} (/$cidrInput)")
                        SubnetDetailRow("Wildcard Mask", subnetResult.wildcardMask)
                        SubnetDetailRow("Usable Host Range", "${subnetResult.firstUsableHost}  ➔  ${subnetResult.lastUsableHost}")
                        SubnetDetailRow("Total Usable Hosts", "${subnetResult.totalUsableHosts} hosts")

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Binary Subnet Mask:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Text(
                                text = subnetResult.binarySubnetMask,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.padding(8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubnetDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun WiresharkAnalyzerTool(
    wiresharkSamples: List<WiresharkPacketSample>,
    selectedPacket: WiresharkPacketSample?,
    onSelectWiresharkPacket: (WiresharkPacketSample) -> Unit,
    onAskAiAboutTool: (String) -> Unit
) {
    val packet = selectedPacket ?: wiresharkSamples.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Select Wireshark Packet Capture Sample:",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                wiresharkSamples.forEach { sample ->
                    FilterChip(
                        selected = (packet?.id == sample.id),
                        onClick = { onSelectWiresharkPacket(sample) },
                        label = { Text(sample.packetName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        if (packet != null) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = packet.packetName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${packet.sourceIp} ➔ ${packet.destinationIp} (${packet.lengthBytes} Bytes)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {
                                    onAskAiAboutTool("Analyze the Wireshark packet capture sample '${packet.packetName}' (${packet.summaryText}) and break down Layer 2, Layer 3, Layer 4, and Layer 7 fields.")
                                },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ask AI Analysis", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Packet Summary Bar
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Info: ${packet.summaryText}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(10.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Layers Accordion
                        Text(
                            text = "Layer-by-Layer Header Inspection:",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        packet.layers.forEach { layer ->
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "▸ ${layer.layerName} [${layer.protocol}]",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    layer.fields.forEach { field ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(field.fieldName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${field.decodedValue} (${field.hexValue})", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Hex Dump Preview
                        Text(
                            text = "Hexadecimal Raw Frame Dump:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Text(
                                text = packet.hexDumpPreview,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.padding(10.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CliCheatsheetTool(
    commandCheatsheet: List<CommandItem>,
    onAskAiAboutTool: (String) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "⚡ Cisco Packet Tracer & Network CLI Cheatsheet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(commandCheatsheet) { item ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Command", item.command)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Command copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Command", modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.purpose,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = item.command,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Expected Output: ${item.expectedOutput}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoAnalysisTool(
    isVideoAnalyzing: Boolean,
    videoAnalysisResult: String?,
    selectedVideoTitle: String,
    onAnalyzeVideo: (Uri?, String?, String) -> Unit,
    onAskAiAboutTool: (String) -> Unit
) {
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedSampleTitle by remember { mutableStateOf("TCP 3-Way Handshake Animation") }
    var customPrompt by remember { mutableStateOf("") }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            selectedSampleTitle = "Uploaded Video"
        }
    }

    val sampleVideos = listOf(
        "TCP 3-Way Handshake Animation",
        "Wireshark Packet Capture Tutorial",
        "Subnetting & CIDR Calculation Visual Guide",
        "OSPF Link State Routing Demo"
    )

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎬 Gemini Pro Video Understanding",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Text(
                                text = "gemini-3.1-pro-preview",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Upload any lecture video or choose a sample network animation. Gemini 3.1 Pro will extract key concepts, timestamps, formulas, and viva Q&A!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Select Sample Video or Upload MP4/WEBM:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sampleVideos.forEach { title ->
                            FilterChip(
                                selected = (selectedUri == null && selectedSampleTitle == title),
                                onClick = {
                                    selectedUri = null
                                    selectedSampleTitle = title
                                },
                                label = { Text(title, fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { videoPickerLauncher.launch("video/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_video_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedUri != null) "Uploaded Video selected" else "📁 Choose Video File from Storage (*.mp4, *.webm)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customPrompt,
                        onValueChange = { customPrompt = it },
                        placeholder = { Text("Specific question or prompt (e.g. Extract timestamps and 3-way handshake steps)", fontSize = 12.sp) },
                        label = { Text("Optional Custom Prompt") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_prompt_input"),
                        singleLine = false,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            onAnalyzeVideo(selectedUri, if (selectedUri != null) "Uploaded Device Video" else selectedSampleTitle, customPrompt)
                        },
                        enabled = !isVideoAnalyzing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analyze_video_gemini_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isVideoAnalyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gemini 3.1 Pro Analyzing Video...", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyze Video with Gemini Pro", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (videoAnalysisResult != null) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✨ Gemini Pro Video Analysis Results",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Button(
                                onClick = {
                                    onAskAiAboutTool("Here is my Gemini 3.1 Pro video analysis for '$selectedVideoTitle':\n\n$videoAnalysisResult\n\nPlease give me 3 practice viva questions based on this video.")
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ask Tutor", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = videoAnalysisResult,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
