# Tanish — Android ilova TZ (1-versiya)

Nov 1, 2026 · @Azizbek

## 1. Umumiy ma'lumot

**Tanish** (ishchi nom) — xotirasi zaiflashgan bemorga kamera orqali yaqin odamlarini tanishga yordam beradigan Android ilova. Bemor kamerani odamga qaratadi, ekranda katta harflar bilan "Bu — qizingiz Malika" chiqadi va qarindoshning oldindan yozib olingan ovozi eshitiladi.

**Foydalanuvchilar:**

| Kim | Vazifasi | Talab |
| --- | --- | --- |
| Bemor | Ilovadan foydalanadi | Bitta katta tugma, o'qish oson, chalg'itadigan narsa yo'q |
| Qarovchi (oila a'zosi) | Odamlarni qo'shadi, sozlaydi | PIN bilan himoyalangan oddiy sozlash bo'limi |

**1-versiyaga kiradi:**

- Qarovchi rejimida odam qo'shish: suratga olish, ism, kimligi, ovoz yozuvi
- Bemor rejimida "Bu kim?" tugmasi va yuzni tanish
- Katta yozuv + qarindoshning yozilgan ovozi
- "Notanish odam" va "Aniq bilmayman" holatlari
- Hammasi telefonda, internetsiz ishlaydi

**1-versiyaga kirmaydi** (keyingi versiyalarga qoldiriladi): buyumlarni tanish, dori eslatmalari, kiosk rejimi, yiqilishni aniqlash, Telegram xabarnoma, bulutga zaxira, ovoz orqali odamni tanish.

## 2. Texnik talablar

Ilova Kotlin'da, Jetpack Compose bilan yoziladi. Barcha hisob-kitoblar telefonda bajariladi va internet ruxsati so'ralmaydi.

| Qism | Texnologiya | Izoh |
| --- | --- | --- |
| Til | Kotlin | Java ishlatilmaydi |
| Interfeys | Jetpack Compose, Material 3 | Katta shrift, yuqori kontrast |
| Kamera | CameraX (Preview + ImageAnalysis) | Faqat old va orqa kamerani almashtirish |
| Yuzni topish | Google ML Kit Face Detection (on-device) | Kadrdagi yuzni topib, qirqib beradi |
| Yuz izi (embedding) | MobileFaceNet `.tflite` modeli, TensorFlow Lite / LiteRT orqali | Ochiq litsenziyali model, `assets/` papkasida |
| Ma'lumotlar bazasi | Room | Odamlar va ularning yuz izlari |
| Fayllar | Ilovaning ichki xotirasi (`filesDir`) | Rasmlar va ovoz yozuvlari |
| Ovoz yozish / ijro | MediaRecorder, MediaPlayer | `.m4a` format |
| PIN saqlash | EncryptedSharedPreferences yoki DataStore + xesh | PIN ochiq holda saqlanmaydi |
| Arxitektura | MVVM, bitta modul (`app`) | ViewModel + Repository |
| Yig'ish | Gradle (Kotlin DSL), GitHub Actions | Har push'da debug APK artefakt sifatida chiqadi |

**Qurilma talablari:** minSdk 26 (Android 8.0), targetSdk — eng so'nggi barqaror versiya. Asosiy sinov qurilmasi: Xiaomi 12T.

**Ruxsatlar:** `CAMERA`, `RECORD_AUDIO`. `INTERNET` ruxsati qo'shilmaydi.

## 3. Ishlash tamoyili

Ilova modelni o'qitmaydi: har bir yuzdan raqamli iz oladi va uni bazadagi izlar bilan solishtiradi. Ism faqat ishonch yuqori bo'lganda aytiladi, shubha bo'lsa "Aniq bilmayman" chiqadi.

&#91;embedded content: tanish jarayoni · 4 qadam, 2 qaror, 3 natija\]

**Qadamlar:**

1. **Kadr.** CameraX `ImageAnalysis` faqat oxirgi kadrni oladi (`STRATEGY_KEEP_ONLY_LATEST`), sekundiga taxminan 5 kadr yetarli.
2. **Yuzni topish.** ML Kit kadrdagi eng katta yuzni oladi. Juda kichik, qattiq burilgan yoki xira yuz o'tkazib yuboriladi.
3. **Yuz izi.** Yuz qirqiladi, model kirish o'lchamiga keltiriladi va L2 bo'yicha normallashtirilgan vektor olinadi.
4. **Solishtirish.** Kosinus o'xshashlik hisoblanadi. Har bir odam uchun uning namunalari ichidagi eng yuqori qiymat olinadi. Eng yaqin odam — s1, ikkinchisi — s2.

**Qaror qoidasi:**

| Holat | Shart | Natija |
| --- | --- | --- |
| Juda uzoq | s1 pastki chegaradan kichik | Notanish |
| Ishonchli | s1 yuqori chegaradan katta, s1 − s2 farq chegarasidan katta va oxirgi 5 kadrdan kamida 4 tasida bir xil odam | Tanildi |
| Qolgan hamma holat | — | Aniq bilmayman |

**Chegaralar:** aniq qiymatlar tanlangan modelga bog'liq. Claude Code boshlang'ich qiymatlarni model hujjatiga qarab tanlaydi va bitta konstantalar faylida saqlaydi. Sozlamalardagi "Qat'iy / O'rtacha / Yumshoq" tanlovi yuqori chegarani suradi. Yakuniy qiymatlar real odamlarda "Tanishni sinash" ekrani orqali aniqlanadi.

Natija aniqlangan zahoti kamera to'xtatiladi.

## 4. Ma'lumotlar modeli va maxfiylik

Bazada ikkita jadval bo'ladi. Rasm va ovozlarning o'zi fayl sifatida saqlanadi, bazada faqat ularning yo'li turadi.

**`Person` jadvali** — bitta tanish odam:

| Maydon | Turi | Misol |
| --- | --- | --- |
| id | Long (avto) | 1 |
| name | String | Malika |
| relation | String | qizingiz |
| displayText | String | Bu — qizingiz Malika |
| photoPath | String | Ekranda ko'rsatiladigan asosiy rasm |
| voicePath | String? | Qarindoshning ovoz yozuvi (bo'lmasligi mumkin) |
| createdAt | Long | Qo'shilgan vaqt |

**`FaceSample` jadvali** — bitta odamning bitta yuz izi (har bir odamga 10–15 ta):

| Maydon | Turi | Izoh |
| --- | --- | --- |
| id | Long (avto) |  |
| personId | Long | `Person.id` ga bog'langan, odam o'chirilsa birga o'chadi |
| embedding | ByteArray | Normallashtirilgan float massivi |

**Sozlamalar** (DataStore): PIN xeshi, ishonch chegarasi, shrift o'lchami, ovoz yoqilgan yoki o'chirilgani.

**Maxfiylik qoidalari:**

- Hech qanday ma'lumot telefondan tashqariga chiqmaydi. Internet ruxsati yo'q.
- Yuz izlari va rasmlar faqat ilovaning ichki xotirasida saqlanadi, galereyada ko'rinmaydi.
- `android:allowBackup="false"` — ma'lumotlar avtomatik bulut zaxirasiga tushmaydi.
- Odam o'chirilsa, uning barcha rasmlari, ovozi va yuz izlari to'liq o'chiriladi.
- Ilovani birinchi ochganda qisqa ogohlantirish: "Bu ilova yordamchi vosita, tibbiy qurilma emas. Rasmlar faqat shu telefonda saqlanadi."

## 5. Ekranlar

Ilova bemor rejimida ochiladi. Qarovchi rejimiga faqat PIN orqali kiriladi. Butun interfeys o'zbek tilida (lotin yozuvi).

**Bemor rejimi:**

1. **Bosh ekran.** Ekranning katta qismini egallagan bitta tugma: "Bu kim?" (shrift kamida 32sp). Yuqori o'ng burchakda kichik, ko'zga tashlanmaydigan qulf belgisi — qarovchi rejimiga kirish uchun uzoq bosiladi (3 soniya), keyin PIN so'raladi.
2. **Kamera ekrani.** To'liq ekran kamera, topilgan yuz atrofida ramka. Pastda "Orqaga" tugmasi. Yuz topilmasa: "Kamerani yuzga qarating" degan yozuv.
3. **Natija ekrani.** Odamning asosiy rasmi, ostida katta harflar bilan `displayText` (kamida 40sp). Ovoz yozuvi bo'lsa, avtomatik ijro etiladi; "Yana eshitish" tugmasi. 10 soniyadan keyin yoki tugma bosilganda bosh ekranga qaytadi.
4. **Notanish / Aniq bilmayman.** "Bu odamni tanimayman" yoki "Aniq bilmayman, yaqinroq qarating" — xotirjam ohangda, qizil rang va qo'rqituvchi belgilarsiz.

**Qarovchi rejimi:**

1. **PIN ekrani.** Birinchi ochilishda PIN o'rnatiladi (4–6 raqam), keyin har safar so'raladi.
2. **Odamlar ro'yxati.** Har bir odam: rasm, ism, kimligi, nechta yuz namunasi borligi. "Odam qo'shish" tugmasi.
3. **Odam qo'shish / tahrirlash.** Ism, kimligi, ekranda chiqadigan matn (avtomatik to'ldiriladi, o'zgartirish mumkin). Keyin yuz namunalarini olish va ovoz yozish.
4. **Yuz namunalarini olish.** Kamera ochiladi, ilova ko'rsatma beradi: "To'g'riga qarang", "Biroz chapga", "Biroz o'ngga", "Yuqoriga", "Pastga". Har bir namuna faqat sifatli yuz topilganda saqlanadi. Hisoblagich: "8 / 12". Kamida 10 ta namuna olinmaguncha saqlash tugmasi faol bo'lmaydi.
5. **Ovoz yozish.** "Yozishni boshlash" / "To'xtatish" / "Tinglash" / "Qayta yozish". Maksimum 15 soniya. Ovoz ixtiyoriy.
6. **Sozlamalar.** Ishonch chegarasi ("Qat'iy" / "O'rtacha" / "Yumshoq" — uchta tanlov, raqam emas), shrift o'lchami, ovozni yoqish/o'chirish, PIN'ni o'zgartirish, "Tanishni sinash" ekrani (har bir aniqlashda o'xshashlik foizini ko'rsatadi — faqat qarovchi uchun).

## 6. Bosqichlar

Ilova 7 bosqichda quriladi. Har bir bosqich Claude Code'ga alohida beriladi. Keyingi bosqichga faqat telefondagi tekshiruv to'liq o'tgandan keyin o'tiladi.

### 0-bosqich. Loyiha skeleti va xatolar jurnali

**Claude Code'ga:** "Ushbu TZ asosida Kotlin + Compose loyihasini yarat. GitHub Actions workflow qo'sh: har push'da debug APK yig'ib, artefakt sifatida yuklasin. Ilovaga xatolar jurnalini qo'sh: barcha xatolar va ilova yopilishlari (crash) ichki faylga yozilsin, qarovchi rejimida 'Jurnal' ekranida ko'rinsin va 'Nusxa olish' tugmasi bo'lsin. Hozircha bitta bo'sh ekran yetarli."

**Telefonda tekshirish:**

- [ ] GitHub Actions yashil, APK yuklab olinadi
- [ ] APK Xiaomi 12T'ga o'rnatiladi va ochiladi
- [ ] Jurnal ekrani ochiladi, nusxa olish ishlaydi

### 1-bosqich. Ekranlar va PIN

**Claude Code'ga:** "5-bo'limdagi barcha ekranlarni kamerasiz va tanishsiz yarat: bemor bosh ekrani, qulf belgisini uzoq bosish, PIN o'rnatish va kiritish, odamlar ro'yxati, odam qo'shish formasi, sozlamalar. Room bazasini 4-bo'limdagidek yarat. Odam qo'shish hozircha faqat ism, kimligi va galereyadan bitta rasm bilan ishlasin."

**Telefonda tekshirish:**

- [ ] Birinchi ochilishda PIN o'rnatiladi
- [ ] Qulfni 3 soniya bosish → PIN → qarovchi rejimi
- [ ] Noto'g'ri PIN qabul qilinmaydi
- [ ] Odam qo'shiladi, ro'yxatda ko'rinadi, ilova qayta ochilganda saqlanib qoladi
- [ ] Odam o'chiriladi

### 2-bosqich. Kamera va yuzni topish

**Claude Code'ga:** "CameraX va ML Kit Face Detection'ni ulab, 'Bu kim?' tugmasidan keyin ochiladigan kamera ekranini yarat. Topilgan yuz atrofida ramka chizilsin. Kamera ruxsati so'ralsin, rad etilsa tushunarli xabar chiqsin. Kamera faqat shu ekranda ishlasin, ekrandan chiqilganda darhol to'xtasin."

**Telefonda tekshirish:**

- [ ] Ruxsat so'raladi; rad etilsa ilova yopilmaydi
- [ ] Yuz atrofida ramka chiqadi va yuz bilan birga siljiydi
- [ ] Yuz yo'q bo'lsa "Kamerani yuzga qarating" chiqadi
- [ ] Orqaga qaytganda kamera o'chadi (yashil indikator yo'qoladi)

### 3-bosqich. Yuz izi va odamni ro'yxatga olish

**Claude Code'ga:** "MobileFaceNet `.tflite` modelini ulab, yuz qirqimidan embedding olish funksiyasini yoz (modelning kirish o'lchami va normalizatsiyasini model hujjatiga qarab sozla, embedding'ni L2 bo'yicha normallashtir). 5-bo'limdagi 'Yuz namunalarini olish' ekranini yarat: ko'rsatmalar, hisoblagich, kamida 10 namuna. Namunalarni `FaceSample` jadvaliga saqla. Model faylining manbasi va litsenziyasini README'ga yoz."

**Telefonda tekshirish:**

- [ ] Ko'rsatmalar navbat bilan chiqadi, hisoblagich oshadi
- [ ] Yuz bo'lmasa yoki xira bo'lsa namuna olinmaydi
- [ ] 10 tadan kam namunada saqlash tugmasi faol emas
- [ ] Ro'yxatda odamning namunalar soni to'g'ri ko'rinadi

### 4-bosqich. Tanish va natija

**Claude Code'ga:** "Kamera ekranida yuz topilganda embedding ol va 3-bo'limdagi qoida bo'yicha bazadagi barcha namunalar bilan solishtir. Bir necha ketma-ket kadrning natijasini birlashtir (bitta kadr bilan qaror qilma). Natijaga qarab 'Natija', 'Notanish' yoki 'Aniq bilmayman' ekranini ko'rsat. Qarovchi sozlamalariga 'Tanishni sinash' ekranini qo'sh — u eng yaqin odamni va o'xshashlik qiymatini ko'rsatsin."

**Telefonda tekshirish:**

- [ ] Ro'yxatga olingan har bir odam kamida 10 urinishdan 9 tasida to'g'ri taniladi (yaxshi yorug'likda)
- [ ] Ro'yxatda yo'q odam 10 urinishdan 10 tasida boshqa odam ismi bilan atalmaydi
- [ ] Qorong'i xonada noto'g'ri ism chiqmaydi ("Aniq bilmayman" chiqishi normal)
- [ ] Rasm/ekrandagi suratga qaratilganda natija qayd etiladi (bu 1-versiyada bloklanmaydi, faqat bilish uchun)

### 5-bosqich. Ovoz

**Claude Code'ga:** "Odam tahrirlash ekraniga ovoz yozish qismini qo'sh (5-bo'lim). Natija ekranida ovoz avtomatik ijro etilsin, 'Yana eshitish' tugmasi bo'lsin. Ovoz yo'q odam uchun hech narsa ijro etilmasin. Sozlamalarda ovozni o'chirish imkoni bo'lsin."

**Telefonda tekshirish:**

- [ ] Ovoz yoziladi, tinglanadi, qayta yoziladi
- [ ] Natija ekranida ovoz o'zi ijro etiladi
- [ ] Ovoz o'chirilganda ijro etilmaydi

### 6-bosqich. Sayqal

**Claude Code'ga:** "Birinchi ochilishdagi ogohlantirishni qo'sh (4-bo'lim). Ishonch chegarasi tanlovini ulab ber. Shrift o'lchami sozlamasini ulab ber. Barcha matnlarni `strings.xml`'ga chiqar. 7-bo'limdagi qabul mezonlarini birma-bir tekshirib, topilgan muammolarni tuzat."

**Telefonda tekshirish:** 7-bo'limdagi barcha qabul mezonlari.

## 7. Nofunksional talablar va qabul mezonlari

**Nofunksional talablar:**

| Talab | Mezon |
| --- | --- |
| Tezlik | "Bu kim?" bosilgandan natijagacha yaxshi yorug'likda 3 soniyadan kam |
| Internetsiz ishlash | Samolyot rejimida hamma funksiya ishlaydi |
| Batareya | Kamera faqat kamera ekranida ishlaydi; fonda hech narsa ishlamaydi |
| O'qish qulayligi | Asosiy matnlar kamida 32sp, kontrast yuqori, tugmalar kamida 64dp |
| Til | Barcha matnlar o'zbekcha (lotin), `strings.xml` da |
| Barqarorlik | Ruxsat rad etilsa, kamera band bo'lsa yoki model yuklanmasa ilova yopilmaydi, tushunarli xabar chiqadi |
| Ekran burilishi | Faqat vertikal (portrait) holat |

**Qabul mezonlari** — 1-versiya tayyor deb hisoblanadi, agar:

- [ ] 3 ta odam ro'yxatga olingan, har biri 10 urinishdan kamida 9 tasida to'g'ri tanilsa
- [ ] Ro'yxatda yo'q 3 ta odamning hech biri boshqa odam ismi bilan atalmasa
- [ ] Ovozli odamda ovoz o'zi ijro etilsa
- [ ] Bemor qarovchi rejimiga PIN'siz kira olmasa
- [ ] Samolyot rejimida hammasi ishlasa
- [ ] 30 daqiqa davomida qayta-qayta foydalanishda ilova bir marta ham yopilib qolmasa
- [ ] Ilova o'chirib-yoqilgandan keyin barcha odamlar saqlanib qolsa
- [ ] Odam o'chirilganda uning fayllari ham o'chgani tekshirilgan bo'lsa

## 8. Claude Code uchun ish qoidalari

Bu bo'limni har bir bosqich topshirig'ining boshiga qo'shib yuboring.

1. Faqat joriy bosqichni bajar. Keyingi bosqichlarning funksiyalarini oldindan qo'shma.
2. Har bosqich oxirida qisqa hisobot yoz: nima qilindi, qaysi fayllar o'zgardi, telefonda nimani tekshirish kerak.
3. Kodni push qilishdan oldin loyiha yig'ilishini tekshir. GitHub Actions qizil bo'lsa, tuzatmaguncha bosqichni tugatma.
4. Yangi kutubxona qo'shsang, nima uchunligini hisobotda yoz. Faqat barqaror (stable) versiyalardan foydalan.
5. Hech qachon `INTERNET` ruxsatini, analitika yoki reklama kutubxonalarini qo'shma.
6. Har bir xato (`try/catch`) xatolar jurnaliga yozilsin. Foydalanuvchiga texnik matn emas, oddiy o'zbekcha xabar ko'rsatilsin.
7. Bemor ko'radigan ekranlarda qizil rang, ogohlantirish belgilari va qo'rqituvchi so'zlar ishlatilmasin.
8. Ishonchsiz holatda doim "Aniq bilmayman"ni tanla. Noto'g'ri ism aytish ism aytmaslikdan yomonroq.
9. Men xato haqida yozsam, avval sababini tushuntir, keyin tuzat. Xatoni yashirish uchun funksiyani o'chirib qo'yma.

**Xato haqida xabar berish shabloni** (siz Claude Code'ga yozasiz):

```markdown
Bosqich: 3
Nima qildim: Odam qo'shish → Yuz namunalari → 4-namunada
Nima bo'ldi: ilova yopilib qoldi
Kutgan edim: hisoblagich 5 ga o'tishi
Jurnal: (Jurnal ekranidan nusxa)
```
