<div align="center">
  <img width="1200" height="475" alt="IPTV Player EGYPT Banner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
  
  # 🎬 IPTV Player EGYPT
  ### التطبيق المصري الأول لمشاهدة الأفلام، المسلسلات والقنوات التلفزيونية 🇪🇬✨
  
  <p align="center">
    <img src="https://img.shields.io/badge/Platform-Android-brightgreen?style=for-the-badge&logo=android" alt="Android" />
    <img src="https://img.shields.io/badge/Language-Kotlin%20%2F%20Java-orange?style=for-the-badge" alt="Languages" />
    <img src="https://img.shields.io/badge/Status-Active-blue?style=for-the-badge" alt="Status" />
  </p>
</div>

---

## 📌 عن التطبيق (About The Project)

**IPTV Player EGYPT** هو تطبيق أندرويد متكامل ومصمم خصيصاً للجمهور المصري والعربي لعرض وبث الأفلام، المسلسلات، والقنوات الحية بأعلى جودة ممكنة وبتجربة مستخدم سلسة وسريعة (UI/UX) وبدون تعقيدات.

### ✨ المميزات الرئيسية:
* 📺 **بث مباشر بدون تقطيع:** دعم كامل لملفات وقوائم IPTV وتشغيل القنوات بجودات متعددة.
* 🎬 **مكتبة ضخمة:** أقسام مخصصة لأحدث الأفلام والمسلسلات العربية والأجنبية المترجمة.
* 🚀 **أداء صاروخي:** واجهة مستخدم خفيفة وسريعة متوافقة مع جميع أجهزة الأندرويد والشاشات الذكية.
* 🔍 **بحث ذكي:** اعثر على فيلمك أو قناتك المفضلة في ثوانٍ معدودة.

---

## 🚀 التشغيل والتثبيت محلياً (Run Locally)

إذا كنت مطوراً وتريد التعديل على التطبيق أو تشغيله على جهازك، اتبع الخطوات التالية:

### 🛠 المتطلبات الأساسية (Prerequisites):
تحتاج إلى تثبيت برنامج **[Android Studio](https://developer.android.com/studio)** على جهازك.

### 🏃‍♂️ خطوات التشغيل:

1. **تحميل المشروع:** قم بعمل `Clone` للمستودع أو تحميله كملف المضغوط `ZIP`.
2. **فتح المشروع:** افتح برنامج Android Studio، واختر **Open** ثم حدد مجلد هذا المشروع.
3. **التحديث التلقائي:** انتظر قليلاً حتى يقوم الأندرويد ستوديو بعمل Sync وحل أي تعارضات في المكتبات تلقائياً.
4. **إعداد مفاتيح البيئة:**
   * قم بإنشاء ملف جديد باسم `.env` في المجلد الرئيسي للمشروع.
   * أضف مفتاح الـ API الخاص بك داخل الملف كالتالي (يمكنك الاستعانة بملف `.env.example` كدليل):
     ```env
     GEMINI_API_KEY=your_api_key_here
     ```
5. **تعديل الـ Gradle:** قم بإزالة هذا السطر من ملف `build.gradle.kts` الخاص بالتطبيق لتجنب مشاكل التوقيع أثناء التطوير:
   ```kotlin
   signingConfig = signingConfigs.getByName("debugConfig")
