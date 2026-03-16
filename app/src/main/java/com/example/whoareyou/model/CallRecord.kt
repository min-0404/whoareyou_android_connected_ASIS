package com.example.whoareyou.model

data class CallRecord(
    val id: String,
    val callerName: String,
    val callerTeam: String,
    val callerJob: String,
    val phone: String,
    val timestampMs: Long,      // System.currentTimeMillis()
    val durationSeconds: Long,
    val callType: String = "incoming"   // "incoming"(수신), "outgoing"(발신), "missed"(부재중)
)
