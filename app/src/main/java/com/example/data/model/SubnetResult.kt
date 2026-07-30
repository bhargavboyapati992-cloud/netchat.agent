package com.example.data.model

data class SubnetResult(
    val ipAddress: String,
    val cidrPrefix: Int,
    val ipClass: String,
    val networkAddress: String,
    val broadcastAddress: String,
    val subnetMask: String,
    val wildcardMask: String,
    val firstUsableHost: String,
    val lastUsableHost: String,
    val totalUsableHosts: Long,
    val binarySubnetMask: String,
    val binaryIpAddress: String,
    val isPrivateIp: Boolean
)
