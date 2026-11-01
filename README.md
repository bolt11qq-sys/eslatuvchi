# Tanish

Xotirasi zaiflashgan bemorga kamera orqali yaqin odamlarini tanishga yordam beradigan Android ilova.
To'liq texnik topshiriq: `Tanish — Android ilova TZ (1-versiya).md`.

## Holat

| Bosqich | Holat |
| --- | --- |
| 0. Loyiha skeleti va xatolar jurnali | Bajarildi, telefonda tekshirish kutilmoqda |
| 1–6 | Boshlanmagan |

## Yig'ish

Har push'da GitHub Actions (`.github/workflows/android.yml`) debug APK yig'adi.
APK: Actions → oxirgi ishga tushirish → **Artifacts** → `tanish-debug-apk`.

Mahalliy yig'ish (JDK 17 va Android SDK kerak):

```
./gradlew assembleDebug
```

## Texnik

- Kotlin, Jetpack Compose, Material 3, minSdk 26, targetSdk 35
- Paket: `uz.tanish`
- `INTERNET` ruxsati yo'q, `allowBackup="false"`, bulut zaxirasi va qurilmalararo ko'chirish o'chirilgan
- Xatolar jurnali: `filesDir/logs/jurnal.txt` (maksimum 256 KB, eski yozuvlar avtomatik qisqartiriladi)

## Kutubxonalar

| Kutubxona | Nima uchun |
| --- | --- |
| androidx.core:core-ktx 1.15.0 | Android API'lari uchun Kotlin kengaytmalari |
| androidx.lifecycle:lifecycle-runtime-ktx 2.8.7 | Activity hayot sikli |
| androidx.activity:activity-compose 1.9.3 | Compose'ni Activity'ga ulash, orqaga tugmasi |
| Compose BOM 2024.12.01 (ui, material3, material-icons-core) | Interfeys; qulf belgisi uchun ikonka |
