package com.example.data.model

data class CommandItem(
    val category: String, // "Cisco Packet Tracer Router/Switch", "Windows/Linux Network Utilities", "Wireshark Filters"
    val command: String,
    val purpose: String,
    val exampleUsage: String,
    val expectedOutput: String
)
