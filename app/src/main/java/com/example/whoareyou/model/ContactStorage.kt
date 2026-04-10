package com.example.whoareyou.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 로컬 디바이스에 저장하는 두 가지 데이터를 관리한다:
 *   1. CustomContact  – 사용자가 앱에서 직접 등록한 개인 연락처 (SharedPreferences)
 *   2. CallRecord     – 후아유가 식별한 수신/발신 통화 이력 (SharedPreferences, 최대 200건)
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * 보안 정책 변경 (2024-04):
 *   이전: 임직원 전화번호 전체를 앱에 다운로드·저장 후 로컬에서 검색
 *   현재: 임직원 번호는 기기에 저장 금지 → 수신 전화 발생 시 서버 API 실시간 조회
 *         (WhoAreYouScreeningService → ApiClient.api.lookupByPhoneNumber())
 *
 * 따라서 이 클래스에서 EmployeeRepository / findCallerInfo() 를 완전히 제거한다.
 * 커스텀 연락처 조회는 findCustomContact() 로 대체되며,
 * 임직원 조회는 반드시 네트워크 레이어(ApiClient)를 통해 수행해야 한다.
 * ──────────────────────────────────────────────────────────────────────────────
 */
object ContactStorage {

    private const val PREF_NAME       = "whoareyou_storage"
    private const val KEY_CONTACTS    = "custom_contacts"
    private const val KEY_CALL_RECORDS = "call_records"
    private const val MAX_CALL_RECORDS = 200

    // ── Custom Contacts ────────────────────────────────────────────────────────

    /** SharedPreferences 에서 커스텀 연락처 전체 목록을 반환한다. */
    fun getContacts(context: Context): MutableList<CustomContact> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json  = prefs.getString(KEY_CONTACTS, "[]") ?: "[]"
        return parseContacts(json)
    }

    /**
     * 커스텀 연락처를 저장(추가 또는 수정)한다.
     *   - id 가 이미 존재하면 해당 항목을 덮어쓴다.
     *   - 신규 항목은 목록 맨 앞에 삽입한다.
     */
    fun saveContact(context: Context, contact: CustomContact) {
        val list = getContacts(context)
        val idx  = list.indexOfFirst { it.id == contact.id }
        if (idx >= 0) list[idx] = contact else list.add(0, contact)
        saveContacts(context, list)
    }

    /** id 에 해당하는 커스텀 연락처를 삭제한다. */
    fun deleteContact(context: Context, id: String) {
        val list = getContacts(context).filter { it.id != id }
        saveContacts(context, list)
    }

    private fun saveContacts(context: Context, list: List<CustomContact>) {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(JSONObject().apply {
                put("id",    c.id)
                put("name",  c.name)
                put("phone", c.phone)
                put("note",  c.note)
            })
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CONTACTS, arr.toString()).apply()
    }

    private fun parseContacts(json: String): MutableList<CustomContact> {
        val result = mutableListOf<CustomContact>()
        val arr    = JSONArray(json)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            result.add(
                CustomContact(
                    id    = o.optString("id",    UUID.randomUUID().toString()),
                    name  = o.optString("name"),
                    phone = o.optString("phone"),
                    note  = o.optString("note")
                )
            )
        }
        return result
    }

    // ── Call Records ───────────────────────────────────────────────────────────

    /** 저장된 통화 기록 전체를 반환한다 (최신 순). */
    fun getCallRecords(context: Context): List<CallRecord> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json  = prefs.getString(KEY_CALL_RECORDS, "[]") ?: "[]"
        return parseCallRecords(json)
    }

    /**
     * 새 통화 기록을 목록 맨 앞에 추가한다.
     * 최대 200건을 초과하면 오래된 항목을 자동 삭제한다.
     */
    fun addCallRecord(context: Context, record: CallRecord) {
        val list    = getCallRecords(context).toMutableList()
        list.add(0, record)
        // 최대 200건 유지: 오래된 기록 자동 삭제
        val trimmed = list.take(MAX_CALL_RECORDS)
        val arr     = JSONArray()
        trimmed.forEach { r ->
            arr.put(JSONObject().apply {
                put("id",              r.id)
                put("callerName",      r.callerName)
                put("callerTeam",      r.callerTeam)
                put("callerJob",       r.callerJob)
                put("phone",           r.phone)
                put("timestampMs",     r.timestampMs)
                put("durationSeconds", r.durationSeconds)
                put("callType",        r.callType)
            })
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CALL_RECORDS, arr.toString()).apply()
    }

    /** 모든 통화 기록을 삭제한다. */
    fun clearCallRecords(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_CALL_RECORDS).apply()
    }

    private fun parseCallRecords(json: String): List<CallRecord> {
        val result = mutableListOf<CallRecord>()
        val arr    = JSONArray(json)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            result.add(
                CallRecord(
                    id              = o.optString("id",       UUID.randomUUID().toString()),
                    callerName      = o.optString("callerName"),
                    callerTeam      = o.optString("callerTeam"),
                    callerJob       = o.optString("callerJob"),
                    phone           = o.optString("phone"),
                    timestampMs     = o.optLong("timestampMs"),
                    durationSeconds = o.optLong("durationSeconds"),
                    callType        = o.optString("callType", "incoming")
                )
            )
        }
        return result
    }

    // ── 커스텀 연락처 단독 조회 ────────────────────────────────────────────────
    //
    // 보안 정책에 따라 임직원 번호는 기기에 저장하지 않으므로
    // 이 메서드는 사용자가 직접 등록한 커스텀 연락처만 조회한다.
    //
    // 임직원 조회는 WhoAreYouScreeningService 에서 수신 전화 발생 시
    // ApiClient.api.lookupByPhoneNumber() 를 통해 실시간으로 수행한다.

    /**
     * 커스텀 연락처에서만 번호를 조회한다. (임직원은 API로 실시간 조회)
     *
     * @param rawNumber 수신/발신 전화번호 (국가코드 포함 가능)
     * @return 일치하는 커스텀 연락처가 있으면 Triple(name, note, "") 반환, 없으면 null
     *         - first  : 연락처 이름
     *         - second : 메모(note)
     *         - third  : 빈 문자열 (직위 미사용 – 임직원 API 응답 구조와 인터페이스 통일)
     */
    fun findCustomContact(context: Context, rawNumber: String): Triple<String, String, String>? {
        val normalized = normalizePhone(rawNumber)
        val contact    = getContacts(context).firstOrNull {
            normalizePhone(it.phone) == normalized
        }
        return if (contact != null) Triple(contact.name, contact.note, "") else null
    }

    // ── 번호 정규화 유틸리티 ───────────────────────────────────────────────────

    /**
     * 전화번호에서 숫자만 추출하고 국가코드(+82) 를 국내 형식(0으로 시작)으로 변환한다.
     *   예) "+82-10-1234-5678" → "01012345678"
     *       "010-1234-5678"   → "01012345678"
     */
    private fun normalizePhone(phone: String): String {
        var num = phone.replace(Regex("[^0-9]"), "")
        // 국제전화 +82 접두사를 국내 번호 형식으로 변환
        if (num.startsWith("82") && num.length > 10) num = "0" + num.substring(2)
        return num
    }
}
