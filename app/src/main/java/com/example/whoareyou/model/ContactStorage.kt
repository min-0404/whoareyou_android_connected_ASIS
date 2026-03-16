package com.example.whoareyou.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object ContactStorage {

    private const val PREF_NAME = "whoareyou_storage"
    private const val KEY_CONTACTS = "custom_contacts"
    private const val KEY_CALL_RECORDS = "call_records"
    private const val MAX_CALL_RECORDS = 200

    // ── Custom Contacts ────────────────────────────────────────

    fun getContacts(context: Context): MutableList<CustomContact> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CONTACTS, "[]") ?: "[]"
        return parseContacts(json)
    }

    fun saveContact(context: Context, contact: CustomContact) {
        val list = getContacts(context)
        val idx = list.indexOfFirst { it.id == contact.id }
        if (idx >= 0) list[idx] = contact else list.add(0, contact)
        saveContacts(context, list)
    }

    fun deleteContact(context: Context, id: String) {
        val list = getContacts(context).filter { it.id != id }
        saveContacts(context, list)
    }

    private fun saveContacts(context: Context, list: List<CustomContact>) {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("phone", c.phone)
                put("note", c.note)
            })
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CONTACTS, arr.toString()).apply()
    }

    private fun parseContacts(json: String): MutableList<CustomContact> {
        val result = mutableListOf<CustomContact>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            result.add(CustomContact(
                id = o.optString("id", UUID.randomUUID().toString()),
                name = o.optString("name"),
                phone = o.optString("phone"),
                note = o.optString("note")
            ))
        }
        return result
    }

    // ── Call Records ───────────────────────────────────────────

    fun getCallRecords(context: Context): List<CallRecord> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CALL_RECORDS, "[]") ?: "[]"
        return parseCallRecords(json)
    }

    fun addCallRecord(context: Context, record: CallRecord) {
        val list = getCallRecords(context).toMutableList()
        list.add(0, record)
        // 최대 200건 유지
        val trimmed = list.take(MAX_CALL_RECORDS)
        val arr = JSONArray()
        trimmed.forEach { r ->
            arr.put(JSONObject().apply {
                put("id", r.id)
                put("callerName", r.callerName)
                put("callerTeam", r.callerTeam)
                put("callerJob", r.callerJob)
                put("phone", r.phone)
                put("timestampMs", r.timestampMs)
                put("durationSeconds", r.durationSeconds)
                put("callType", r.callType)
            })
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CALL_RECORDS, arr.toString()).apply()
    }

    fun clearCallRecords(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_CALL_RECORDS).apply()
    }

    private fun parseCallRecords(json: String): List<CallRecord> {
        val result = mutableListOf<CallRecord>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            result.add(CallRecord(
                id = o.optString("id", UUID.randomUUID().toString()),
                callerName = o.optString("callerName"),
                callerTeam = o.optString("callerTeam"),
                callerJob = o.optString("callerJob"),
                phone = o.optString("phone"),
                timestampMs = o.optLong("timestampMs"),
                durationSeconds = o.optLong("durationSeconds"),
                callType = o.optString("callType", "incoming")
            ))
        }
        return result
    }

    // ── 번호 조회 (임직원 + 커스텀 연락처 통합) ─────────────────

    fun findCallerInfo(context: Context, rawNumber: String): Triple<String, String, String>? {
        val normalized = normalizePhone(rawNumber)

        // 1. 임직원 목록에서 검색
        val emp = EmployeeRepository.findByPhoneNumber(rawNumber)
        if (emp != null) return Triple(emp.name, emp.team, emp.jobTitle)

        // 2. 커스텀 연락처에서 검색
        val contact = getContacts(context).firstOrNull {
            normalizePhone(it.phone) == normalized
        }
        if (contact != null) return Triple(contact.name, "", contact.note)

        return null
    }

    private fun normalizePhone(phone: String): String {
        var num = phone.replace(Regex("[^0-9]"), "")
        if (num.startsWith("82") && num.length > 10) num = "0" + num.substring(2)
        return num
    }
}
