package com.enjaz.app.notify

import android.content.Context
import com.enjaz.app.data.AppSettings
import com.enjaz.app.data.QuickItemsRepository

/** تذكير ساخر بعنصر واحد في كل تشغيل، وفق الفاصل الذي اختاره المستخدم. */
object QuickItemReminder {
    private val messages = listOf(
        "«{title}» لسه هنا. أنت ضفته عشان تعمله ولا عشان يبقى من مقتنيات التطبيق؟",
        "فاكر «{title}»؟ هو فاكرَك على فكرة، ومستنيك.",
        "فيه حاجة اسمها «{title}» مستنياك. نبدأ ولا نحدد لها ذكرى سنوية؟",
        "«{title}» بقاله شوية في الانتظار. نتحرك قبل ما يطلب نقل كفالة؟",
        "مش هنفكرك كل مرة… بس إحنا أصلًا بنفكرك كل مرة. خلّص «{title}».",
        "المهمة «{title}» لم تختفِ. أنت بس عامل نفسك مش شايفها.",
        "«{title}» محتاج منك خمس دقايق بداية. مش محتاج اجتماع مجلس إدارة.",
        "لسه ما خلصتش «{title}»؟ طب نعتبرها مهمة ولا مشروع قومي؟"
    )

    fun run(ctx: Context) {
        val settings = AppSettings(ctx)
        if (!settings.notifEnabled || !settings.nQuick || !Notifier.canNotify(ctx)) return
        val state = NotifState(ctx)
        if (state.countToday() >= 5) return
        val repo = QuickItemsRepository(ctx)
        val now = System.currentTimeMillis()
        val due = repo.items.filter { item ->
            !item.done && now - item.lastReminderAt >= item.reminderHours * 60L * 60L * 1000L
        }.sortedBy { it.lastReminderAt }
        val item = due.firstOrNull() ?: return
        val body = messages.random().replace("{title}", item.title)
        val id = 20000 + (item.id % 10000L).toInt()
        if (Notifier.send(ctx, id, "إنجاز · فاكرينك بحاجة", body)) {
            repo.markReminded(item.id, now)
            state.markSent("quick_item_${item.id}")
        }
    }
}
