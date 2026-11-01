# Tanish

Xotirasi zaiflashgan bemorga kamera orqali yaqin odamlarini tanishga yordam beradigan Android ilova.
To'liq texnik topshiriq: `Tanish — Android ilova TZ (1-versiya).md`.

## Holat

| Bosqich | Holat |
| --- | --- |
| 0. Loyiha skeleti va xatolar jurnali | Bajarildi |
| 1. Ekranlar va PIN | Bajarildi, telefonda tekshirish kutilmoqda |
| 2. Kamera va yuzni topish | Bajarildi, telefonda tekshirish kutilmoqda |
| 3. Yuz izi va ro'yxatga olish | Bajarildi, telefonda tekshirish kutilmoqda |
| 4. Tanish va natija | Bajarildi, telefonda tekshirish kutilmoqda |
| 5. Ovoz | Bajarildi, telefonda tekshirish kutilmoqda |
| 6. Sayqal | Bajarildi, telefonda tekshirish kutilmoqda |

## Yig'ish

Har push'da GitHub Actions (`.github/workflows/android.yml`) debug APK yig'adi.
APK: Actions → oxirgi ishga tushirish → **Artifacts** → `tanish-debug-apk`.

## Tuzilishi

| Papka | Vazifasi |
| --- | --- |
| `data/` | Room bazasi (`Person`, `FaceSample`), fayllar, DataStore sozlamalari |
| `face/` | Yuzni topish (ML Kit), yuz izi (MobileFaceNet), solishtirish va qaror qoidasi |
| `face/RecognitionConfig.kt` | **Barcha chegaralar** (ishonch, sifat, kadrlar soni) — bitta faylda |
| `audio/` | Ovoz yozish va ijro |
| `security/` | PIN xeshi (PBKDF2 + tuz) |
| `ui/` | Ekranlar, ViewModel'lar, navigatsiya |
| `log/` | Xatolar jurnali (`filesDir/logs/jurnal.txt`) |

## Yuz modeli

- Fayl: `app/src/main/assets/MobileFaceNet.tflite` (5.2 MB)
- Manba: [syaringan357/Android-MobileFaceNet-MTCNN-FaceAntiSpoofing](https://github.com/syaringan357/Android-MobileFaceNet-MTCNN-FaceAntiSpoofing)
  (commit `d635649`), litsenziya — **MIT** (`third_party/MobileFaceNet-Android-LICENSE.txt`)
- Asl model: [sirius-ai/MobileFaceNet_TF](https://github.com/sirius-ai/MobileFaceNet_TF), litsenziya — **Apache-2.0**
- Kirish: `[2, 112, 112, 3]` float32, RGB, `(piksel − 127.5) / 128`. Chiqish: `[2, 192]`.
  Ikki o'ringa yuz va uning ko'zgudagi aksi beriladi, natija qo'shilib L2 bo'yicha normallashtiriladi.

## Chegaralar (boshlang'ich)

Kosinus o'xshashlik. Muallif kodida bir xil odam chegarasi L2² < 0.8 (≈ kosinus > 0.6); biz qattiqroq olamiz:

| Parametr | Qiymat |
| --- | --- |
| Pastki chegara (bundan past — "Notanish") | 0.45 |
| Yuqori chegara: Qat'iy / O'rtacha / Yumshoq | 0.75 / 0.70 / 0.65 |
| s1 − s2 farqi | 0.08 |
| Ketma-ket kadrlar | oxirgi 5 tadan 4 tasida bir xil odam |

Yakuniy qiymatlar "Tanishni sinash" ekrani orqali real odamlarda aniqlanadi.

## Maxfiylik

- `INTERNET` ruxsati yo'q. ML Kit qo'shadigan tarmoq ruxsatlari manifestda majburan olib tashlangan.
- `allowBackup="false"`, bulut zaxirasi va qurilmalararo ko'chirish o'chirilgan.
- Rasm, ovoz va yuz izlari faqat ilovaning ichki xotirasida. Odam o'chirilsa, hammasi o'chiriladi.

## Kutubxonalar

| Kutubxona | Nima uchun |
| --- | --- |
| core-ktx 1.15.0, lifecycle 2.8.7, activity-compose 1.9.3 | Android asoslari, ViewModel, hayot sikli |
| Compose BOM 2024.12.01 (ui, material3, material-icons-core) | Interfeys |
| navigation-compose 2.8.5 | Ekranlar orasida o'tish |
| Room 2.6.1 (+ KSP 2.1.0-1.0.29) | Odamlar va yuz izlari bazasi |
| datastore-preferences 1.1.1 | Sozlamalar va PIN xeshi |
| CameraX 1.4.1 | Kamera (Preview + ImageAnalysis) |
| ML Kit face-detection 16.1.7 (bundled) | Yuzni topish, telefonda, internetsiz |
| tensorflow-lite 2.16.1 | MobileFaceNet modelini ishga tushirish |
