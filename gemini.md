# YTChannelCLI

**YouTube Channel Scraper CLI Tool**

Dokumentasi ini menjelaskan tujuan, arsitektur, dan cara penggunaan tool command-line (CLI) untuk mengambil data video dari channel YouTube menggunakan NewPipeExtractor.

## 1. Tujuan Proyek

Membuat aplikasi CLI mandiri (`.jar`) yang dapat dieksekusi dari terminal atau workflow n8n dengan fitur:

1. Menerima URL channel YouTube sebagai input
2. Mengambil daftar video terbaru dari channel tersebut
3. Menghasilkan output JSON berisi metadata video (judul, URL, durasi, dll.)
4. Resilient terhadap error - mengeluarkan pesan error terstruktur dalam JSON tanpa crash

## 2. Arsitektur & Teknologi

- **Bahasa:** Java 11+
- **Build Tool:** Gradle
- **Library Utama:** `com.github.TeamNewPipe:NewPipeExtractor:v0.26.1` (via JitPack)
- **Packaging:** Gradle Shadow Plugin untuk membuat fat JAR dengan semua dependensi

NewPipeExtractor digunakan sebagai library eksternal (seperti menggunakan package di Laravel via Composer), bukan sebagai bagian dari source code proyek.

**Penting:** Metadata yang tersedia dari NewPipeExtractor bervariasi tergantung channel dan kondisi YouTube. Tidak semua field selalu tersedia untuk setiap video. Aplikasi harus dirancang untuk mengembalikan metadata yang tersedia saja dan menangani field yang kosong/null dengan graceful.

## 3. Struktur Proyek

```
C:\projects\YTChannelCLI\
├── build.gradle              # Konfigurasi build & dependensi
├── gradlew                   # Gradle wrapper (Linux/macOS)
├── gradlew.bat               # Gradle wrapper (Windows)
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gemini.md                 # Dokumentasi ini
└── src/
    └── main/
        └── java/
            └── com/
                └── ytchannelcli/
                    └── GetLatestVideo.java
```

**Catatan:** Package name `com.ytchannelcli` lebih tepat daripada `org.schabi.newpipe.cli` karena kita tidak memodifikasi source code NewPipeExtractor, melainkan menggunakannya sebagai library.

## 4. Setup & Build

### Prasyarat

- JDK 11 atau lebih tinggi terinstall
- Koneksi internet (untuk download dependensi)

### Langkah-langkah

1. **Buat Struktur Proyek**

   ```bash
   mkdir -p C:\projects\YTChannelCLI\src\main\java\com\ytchannelcli
   cd C:\projects\YTChannelCLI
   ```

2. **Buat File `build.gradle`**

   Letakkan file konfigurasi Gradle di root proyek dengan konten yang mendefinisikan dependensi NewPipeExtractor.

3. **Buat Gradle Wrapper** (hanya sekali)

   ```bash
   gradle wrapper --gradle-version 8.5
   ```

4. **Build Fat JAR**

   ```bash
   ./gradlew shadowJar
   ```

   Output: `build/libs/ytchannelcli-1.0.0.jar`

## 5. Cara Penggunaan

### Sintaks Dasar

```bash
java -jar build/libs/ytchannelcli-1.0.0.jar "<URL_CHANNEL>" [jumlah_video]
```

### Contoh Penggunaan

Mengambil 5 video terbaru:

```bash
java -jar build/libs/ytchannelcli-1.0.0.jar "https://www.youtube.com/@WorldofTeyvat" 5
```

### Format Output

**Sukses** (stdout):

```json
[
  {
    "title": "Video Title 1",
    "url": "https://youtube.com/watch?v=...",
    "duration_seconds": 1300,
    "upload_date": "2024-10-01",
    "view_count": 10000,
    "thumbnail_url": "https://..."
  },
  {
    "title": "Video Title 2",
    "url": "https://youtube.com/watch?v=...",
    "duration_seconds": null,
    "upload_date": "2024-09-28",
    "view_count": null,
    "thumbnail_url": "https://..."
  }
]
```

**Catatan:**

- Beberapa metadata mungkin `null` atau tidak tersedia tergantung dari data yang bisa diekstrak NewPipeExtractor
- Field yang tersedia bisa berbeda antar video atau channel
- Selalu cek ketersediaan data sebelum memproses di workflow

**Error** (stderr):

```json
{
  "error": "Extraction failed",
  "message": "Unable to fetch channel data",
  "cause": "java.net.SocketTimeoutException"
}
```

## 6. Integrasi dengan n8n

Aplikasi ini dapat dipanggil dari n8n menggunakan node "Execute Command":

```javascript
java -jar /path/to/ytchannelcli-1.0.0.jar "{{ $json.channelUrl }}" 10
```

Parse output JSON untuk workflow selanjutnya.

## 7. Metadata & Data Availability

### Prinsip Pengembangan

Saat mengimplementasikan `GetLatestVideo.java`, ikuti prinsip berikut:

1. **Riset Field yang Tersedia**: Gunakan debugger atau logging untuk melihat method apa saja yang tersedia di objek `StreamInfoItem` dari NewPipeExtractor
2. **Defensive Programming**: Selalu cek `null` sebelum mengakses field
3. **Return What's Available**: Kembalikan hanya metadata yang berhasil diekstrak, skip field yang null/error
4. **Jangan Crash**: Gunakan try-catch untuk setiap field ekstraksi, jangan biarkan satu field yang error menggagalkan seluruh proses

### Contoh Implementasi

```java
// ✅ BENAR - Handle metadata yang mungkin tidak tersedia
JSONObject video = new JSONObject();
video.put("title", item.getName()); // Required field

// Optional fields dengan null handling
try {
    long duration = item.getDuration();
    if (duration > 0) {
        video.put("duration_seconds", duration);
    }
} catch (Exception e) {
    // Skip jika tidak tersedia
}

try {
    long views = item.getViewCount();
    if (views >= 0) {
        video.put("view_count", views);
    }
} catch (Exception e) {
    // Skip jika tidak tersedia
}
```

### Field yang Umumnya Tersedia

- ✅ `title` - Hampir selalu tersedia
- ✅ `url` - Hampir selalu tersedia
- ⚠️ `duration` - Kadang tidak tersedia untuk live stream
- ⚠️ `view_count` - Kadang disembunyikan oleh creator
- ⚠️ `upload_date` - Format bisa bervariasi
- ⚠️ `thumbnail` - Biasanya tersedia, tapi resolusi bervariasi
- ⚠️ `description` - Tidak selalu lengkap di list view

## 8. Troubleshooting

### Error: "Could not find or load main class"

Pastikan `mainClassName` di `build.gradle` sesuai dengan package class Anda.

### Error saat download dependensi

Periksa koneksi internet dan pastikan repository JitPack dapat diakses.

### Output kosong atau error parsing

Verifikasi URL channel valid dan dapat diakses. Beberapa channel mungkin memiliki proteksi tambahan.

---
## **Fase 2: Service CLI yang Lebih Lengkap**

Mengembangkan tool ini menjadi service CLI yang lebih komprehensif untuk otomatisasi n8n. Tool ini akan menggunakan sub-command untuk setiap fungsionalitas.

### **Arsitektur & Teknologi Tambahan**

*   **CLI Framework:** `info.picocli:picocli` untuk parsing argumen dan sub-command yang canggih.
*   **JSON Library:** `org.json:json` untuk membuat output JSON yang robust.
*   **Struktur Kode:** Aplikasi utama (`App.java`) akan bertindak sebagai dispatcher yang memanggil kelas command yang sesuai (`GetVideosCommand.java`, `GetDetailsCommand.java`, dll).

### **Struktur Proyek (Diperbarui)**

Nama file utama diubah menjadi `App.java` untuk merefleksikan peranannya sebagai entry point utama.

```
C:\projects\YTChannelCLI\
└── src/
    └── main/
        └── java/
            └── com/
                └── ytchannelcli/
                    ├── App.java              # Main class & dispatcher
                    └── commands/
                        ├── GetVideos.java
                        ├── GetDetails.java
                        ├── Search.java
                        └── GetPlaylist.java
```

### **Sintaks CLI**

```bash
# Format umum
java -jar yt-cli.jar <command> [options]

# Contoh: Mengambil 5 video terbaru
java -jar yt-cli.jar get-videos --url "https://www.youtube.com/@channel" --limit 5

# Contoh: Mengambil detail video
java -jar yt-cli.jar get-details --url "https://www.youtube.com/watch?v=xxxx"

# Contoh: Mencari video
java -jar yt-cli.jar search --query "tutorial n8n" --type video

# Contoh: Mengambil item playlist
java -jar yt-cli.jar get-playlist --url "https://www.youtube.com/playlist?list=xxxx"
```

### **Prinsip Eksplorasi `NewPipeExtractor`**

Sesuai permintaan, implementasi akan melibatkan eksplorasi aktif terhadap library `NewPipeExtractor`. Jika sebuah fungsionalitas (misal: search) tidak secara langsung terekspos di API level atas, saya akan mencoba mencari cara untuk menggunakannya. Jika tidak memungkinkan, saya akan memberitahu Anda keterbatasan tersebut.
---
## **Status Proyek (9 Oktober 2025)**

Sesi ini fokus pada implementasi awal dari arsitektur Fase 2.

### **Progress Selesai**

1.  **Dokumentasi Desain**: `gemini.md` diperbarui dengan rencana arsitektur CLI (sub-command, picocli, dll).
2.  **Struktur Proyek**: Direktori `src/main/java/com/ytchannelcli/commands` telah dibuat.
3.  **Konfigurasi Build**: `build.gradle` telah dibuat dan berisi semua dependensi yang diperlukan (`NewPipeExtractor`, `picocli`, `org.json`, `shadow`).
4.  **Kode Aplikasi**:
    *   `App.java`: Kerangka utama aplikasi yang berfungsi sebagai dispatcher perintah telah dibuat.
    *   `commands/GetVideos.java`: Implementasi untuk sub-command `get-videos` telah selesai.

### **Status Saat Ini: BLOKIR**

Proses pembuatan Gradle wrapper gagal.

*   **Error**: `Gradle requires JVM 17 or later to run. Your build is currently configured to use JVM 11.`
*   **Penyebab**: Perintah `gradle` yang terinstal di sistem Anda adalah versi baru yang memerlukan Java 17+ untuk berjalan, sedangkan environment `JAVA_HOME` sistem Anda menunjuk ke Java 11.
*   **Solusi**: Pengguna perlu menginstal JDK 17+ dan memastikan variabel `JAVA_HOME` menunjuk ke sana.

### **Langkah Selanjutnya**

1.  **Action dari Anda**: Install Gradle di sistem operasi Windows Anda dan pastikan `gradle` bisa diakses dari terminal (verifikasi dengan `gradle -v`).
2.  **Action dari Saya**: Setelah Anda mengkonfirmasi instalasi Gradle, saya akan menjalankan kembali `gradle wrapper --gradle-version 8.5`.
3.  **Action dari Saya**: Setelah wrapper berhasil dibuat, saya akan menjalankan `gradlew.bat shadowJar` untuk membangun file `.jar` final.
