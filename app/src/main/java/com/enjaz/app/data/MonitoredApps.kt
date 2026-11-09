package com.enjaz.app.data

/** التطبيقات المراقَبة افتراضيًا. يستطيع المستخدم استثناء أي منها أو إضافة تطبيقات أخرى. */
val PRESET_APPS: Map<String, String> = mapOf(
    "com.zhiliaoapp.musically" to "TikTok",
    "com.ss.android.ugc.trill" to "TikTok",
    "com.facebook.katana" to "Facebook",
    "com.instagram.android" to "Instagram",
    "com.google.android.youtube" to "YouTube",
    "com.snapchat.android" to "Snapchat",
    "com.twitter.android" to "X",
    "org.telegram.messenger" to "Telegram",
    "org.telegram.messenger.web" to "Telegram"
)

data class AppEntry(val pkg: String, val label: String)
