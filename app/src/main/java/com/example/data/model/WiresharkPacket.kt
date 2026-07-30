package com.example.data.model

data class PacketHeaderField(
    val fieldName: String,
    val hexValue: String,
    val decodedValue: String,
    val description: String
)

data class PacketLayer(
    val layerName: String,
    val protocol: String,
    val fields: List<PacketHeaderField>
)

data class WiresharkPacketSample(
    val id: String,
    val packetName: String,
    val protocol: String,
    val sourceIp: String,
    val destinationIp: String,
    val lengthBytes: Int,
    val summaryText: String,
    val hexDumpPreview: String,
    val layers: List<PacketLayer>
)
