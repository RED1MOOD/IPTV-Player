package com.example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

object LanguageTranslation {
    private val arTranslations = mapOf(
        "app_title" to "مشغل قنوات IPTV",
        "live_tv" to "البث المباشر",
        "movies" to "الأفلام VOD",
        "series" to "المسلسلات VOD",
        "settings" to "الإعدادات",
        "splash_title" to "مشغل قنوات IPTV",
        "splash_sub" to "معمارية نظام تشغيل بروتوكول Xtream",
        "splash_authenticating" to "جاري الاتصال والتحقق من الخادم...",
        "splash_retry" to "إعادة محاولة الاتصال بالخادم",
        "login_title" to "إعدادات الخادم وبيانات الدخول",
        "login_sub" to "قم بتهيئة بيانات اتصال مخصصة في حال وجود ضغط على السيرفر الافتراضي أو انتهاء صلاحيته.",
        "login_host_url" to "الرابط الرئيسي للخادم",
        "login_username" to "اسم المستخدم Xtream",
        "login_password" to "كلمة المرور Xtream",
        "login_button" to "تشغيل مشغل القنوات",
        "search_hint" to "ابحث عن القنوات والمحطات المباشرة...",
        "empty_category" to "قائمة الفئة فارغة أو فارغة من البث حالياً.",
        "no_channels_found" to "لم يتم العثور على أي قنوات تطابق: ",
        "app_config_panel" to "لوحة إعدادات التطبيق",
        "account_privileges" to "امتيازات وتفاصيل الحساب",
        "account_info_sub" to "تفاصيل حالة اشتراك الحساب النشط ومعلومات بروتوكولات ترميز الفيديو",
        "username" to "اسم المستخدم",
        "base_server" to "خادم الاتصال",
        "expiration_date" to "تاريخ انتهاء الاشتراك",
        "format_protocols" to "بروتوكولات البث المعتمدة",
        "local_persisted" to "بيانات القنوات المخزنة محلياً",
        "fav_count" to "القنوات المفضلة",
        "db_state" to "حالة قاعدة البيانات المحلية",
        "channels_saved" to "قنوات محفوظة ومفضلة",
        "db_stable_log" to "مؤمن / مستقر تلقائي (Room)",
        "sync_button" to "تحديث ومزامنة بيانات الحساب",
        "cinema_placeholder_text" to "يتم الآن تجميع وفهرسة قوائم الأفلام والمسلسلات الحصرية المميزة من عقد خوادم Xtream API.",
        "adv_tools_title" to "أدوات التشغيل المتقدمة",
        "playback_speed" to "سرعة تشغيل الفيديو",
        "audio_amp" to "تضخيم وتقوية الصوت",
        "amp_sub" to "يرفع كفاءة الصوت الضعيف ويقوي ترددات السماعة تلقائياً.",
        "voice_pitch" to "نبرة وتردد صوت البث",
        "pitch_deep" to "عميق",
        "pitch_sharp" to "حاد",
        "pitch_normal" to "طبيعي",
        "resolution_limit" to "جودة البث الأقصى المسموح بها",
        "quality_auto" to "تلقائي / Auto",
        "sleep_timer" to "مؤقت النوم التلقائي",
        "sleep_off" to "إيقاف",
        "aspect_ratio" to "أبعاد ونسبة أبعاد الشاشة",
        "live_channel_default" to "بث مباشر",
        "lock_controls" to "قفل أزرار التحكم",
        "unlock_controls" to "إلغاء قفل أزرار التحكم",
        "current_playing_epg" to "البرنامج المعروض حالياً",
        "upcoming_live" to "المعروض التالي بالجدول",
        "audio_tracks" to "اختيار مسار الصوت والدبلجة المتاحة",
        "epg_guide" to "دليل الجدول الزمني للقناة (EPG)",
        "lang_selection_title" to "لغة واجهة التطبيق / Application Interface Language",
        "lang_selection_sub" to "اختر لغة العرض المفضلة لواجهة المستخدم وكافة التبويبات والمشغلات",
        "lang_ar" to "العربية (Arabic)",
        "lang_en" to "English (الأندونيسية/الإنجليزية)",
        "custom_billing" to "إجراءات مخصصة",
        "congested" to "مزدحم",
        "active" to "نشط ومفعل",
        "all_channels" to "📺 كل القنوات المباشرة",
        "favorites" to "⭐ القنوات المفضلة",
        "last_watched" to "🕒 آخر القنوات مشاهدة",
        "seconds" to "ثانية",
        "minutes" to "دقيقة"
    )

    private val enTranslations = mapOf(
        "app_title" to "IPTV Stream Player",
        "live_tv" to "Live TV",
        "movies" to "Movies VOD",
        "series" to "Series VOD",
        "settings" to "Settings",
        "splash_title" to "IPTV STREAM PLAYER",
        "splash_sub" to "Xtream Codes Engine Architecture",
        "splash_authenticating" to "Authenticating with server...",
        "splash_retry" to "Retry Server Authentication",
        "login_title" to "Server Settings & Credentials",
        "login_sub" to "Configure custom billing credentials if default access is congested or offline.",
        "login_host_url" to "Server Host Base URL",
        "login_username" to "Xtream Username",
        "login_password" to "Xtream Password",
        "login_button" to "Launch IPTV Player",
        "search_hint" to "Search channels and live streams...",
        "empty_category" to "This category is currently empty or has no streams.",
        "no_channels_found" to "No channels found matching: ",
        "app_config_panel" to "Application Configuration Panel",
        "account_privileges" to "ACCOUNT PRIVILEGES",
        "account_info_sub" to "Account Subscription Status & Codecs info",
        "username" to "Username",
        "base_server" to "Base Server",
        "expiration_date" to "Expiration Date",
        "format_protocols" to "Format Protocols",
        "local_persisted" to "LOCAL PERSISTED STATUS",
        "fav_count" to "Favorites Count",
        "db_state" to "Database State",
        "channels_saved" to "Channels Saved",
        "db_stable_log" to "Encrypted / Stable (Room)",
        "sync_button" to "Sync Account Subscriptions",
        "cinema_placeholder_text" to "Premium selection lists are being indexed by Xtream API nodes.",
        "adv_tools_title" to "Advanced IPTV Tools",
        "playback_speed" to "Playback Speed",
        "audio_amp" to "Audio Amplification",
        "amp_sub" to "Reduces quiet audio & boosts output dynamically.",
        "voice_pitch" to "Voice Pitch Tuning",
        "pitch_deep" to "Deep",
        "pitch_sharp" to "Sharp",
        "pitch_normal" to "Normal",
        "resolution_limit" to "Resolution Limit",
        "quality_auto" to "Auto / تلقائي",
        "sleep_timer" to "Sleep Timer",
        "sleep_off" to "Off",
        "aspect_ratio" to "Aspect Ratio",
        "live_channel_default" to "Live Channel",
        "lock_controls" to "Lock Controls",
        "unlock_controls" to "Unlock Controls",
        "current_playing_epg" to "Current Playing EPG Program",
        "upcoming_live" to "Upcoming Live Programs",
        "audio_tracks" to "Audio Tracks Selection",
        "epg_guide" to "EPG Timeline Guide",
        "lang_selection_title" to "Application Interface Language / لغة واجهة التطبيق",
        "lang_selection_sub" to "Choose your preferred user interface display language and alignments",
        "lang_ar" to "العربية (Arabic)",
        "lang_en" to "English",
        "custom_billing" to "Custom Credentials",
        "congested" to "Congested",
        "active" to "Active",
        "all_channels" to "📺 All Channels",
        "favorites" to "⭐ Favorites",
        "last_watched" to "🕒 Last Watched",
        "seconds" to "seconds",
        "minutes" to "minutes"
    )

    fun translate(key: String, lang: String): String {
        return if (lang == "ar") {
            arTranslations[key] ?: enTranslations[key] ?: key
        } else {
            enTranslations[key] ?: key
        }
    }
}

@Composable
fun LocalizedLayout(
    viewModel: IPTVViewModel,
    content: @Composable () -> Unit
) {
    val lang by viewModel.appLanguage.collectAsState()
    val isRtl = lang == "ar"
    val direction = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        content()
    }
}
