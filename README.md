# إنجاز | ENJAZ

تطبيق Android أصلي (Kotlin + Jetpack Compose) لإدارة الدراسة والمهام بشخصية قيادية صارمة.

## البناء عبر GitHub Actions
1. أنشئ مستودعًا باسم `enjaz` على GitHub ثم ادفع الملفات (الأوامر أدناه).
2. تبويب Actions ← Build APK ← بعد النجاح نزّل artifact باسم `enjaz-debug-apk`.

## من Termux
```
pkg install git unzip -y
termux-setup-storage
cd ~/storage/downloads
unzip -o enjaz-phase3.zip && cd enjaz
git init && git branch -M main
git add . && git commit -m "enjaz v0.3"
git remote add origin https://github.com/USERNAME/enjaz.git
git push -u origin main --force
```

## قناة Telegram
تُضبط من الإعدادات داخل التطبيق (لا يوجد رابط مدمج).
