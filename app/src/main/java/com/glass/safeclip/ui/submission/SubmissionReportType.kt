package com.glass.safeclip.ui.submission

object SubmissionReportType {
    val Options = listOf(
        "교통위반(고속도로 포함)",
        "이륜차 위반",
        "난폭/보복운전",
        "버스전용차로 위반(고속도로제외)",
        "번호판 규정 위반",
        "불법등화, 반사판(지) 가림·손상",
        "불법 튜닝, 해체, 조작",
        "기타 자동차 안전 기준 위반",
        "기타 문의/신고"
    )

    fun normalize(value: String): String {
        return value.trim().takeIf { it in Options }.orEmpty()
    }
}
