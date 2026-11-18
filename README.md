# Remote Laptop Bluetooth (Android ↔ Windows 10/11)

Aplikasi Android ini mengubah HP menjadi touchpad, keyboard teks, pengendali media, dan tombol kunci Windows. Koneksi memakai Bluetooth Classic SPP (Serial Port Profile), bukan Wi-Fi.

## Isi proyek
- `app/`: proyek Android Kotlin.
- `windows_server/server.py`: penerima perintah di laptop Windows.
- `windows_server/requirements.txt`: dependensi Python.

## 1. Siapkan laptop Windows
1. Aktifkan Bluetooth di Windows dan Android.
2. Pasangkan HP dan laptop melalui Settings → Bluetooth & devices → Add device.
3. Di Windows buka **More Bluetooth settings** → tab **COM Ports** → **Add** → pilih **Incoming (device initiates the connection)**. Catat nomor COM, misalnya `COM6`.
   - Jika menu COM Ports tidak tersedia, driver Bluetooth laptop mungkin tidak menyediakan SPP. Gunakan adaptor Bluetooth USB yang mendukung Serial Port Profile.
4. Install Python 3.10 atau lebih baru.
5. Buka PowerShell di folder `windows_server` lalu jalankan:
   ```powershell
   py -m pip install -r requirements.txt
   py server.py --port COM6
   ```
   Ganti `COM6` sesuai port Incoming yang dibuat. Biarkan server berjalan.

## 2. Build APK
1. Install **Android Studio** versi stabil terbaru dan Android SDK Platform 35.
2. Buka folder `RemoteLaptopBluetooth` melalui Android Studio.
3. Tunggu Gradle Sync selesai. Gunakan JDK 17 (Android Studio biasanya menyediakannya).
4. Pilih **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. APK debug akan dibuat di:
   `app/build/outputs/apk/debug/app-debug.apk`
6. Pindahkan APK ke Android dan izinkan instalasi dari sumber tersebut jika diminta.

Alternatif terminal dari folder proyek (dengan Gradle terpasang):
```powershell
gradle assembleDebug
```

## 3. Cara memakai
1. Jalankan `server.py` di laptop.
2. Pastikan HP sudah dipasangkan dengan laptop pada pengaturan Bluetooth Android.
3. Buka Remote Laptop, pilih nama laptop pada daftar perangkat, lalu tekan **Hubungkan perangkat**.
4. Gunakan area putih sebagai touchpad. Tombol di bawahnya untuk klik, scroll, mengetik teks, media, dan mengunci laptop.

## Keamanan dan batasan
- Aplikasi hanya mengirim perintah ke perangkat Bluetooth yang sudah dipasangkan; jangan pasangkan perangkat yang tidak dikenal.
- Server hanya menerima daftar perintah yang diizinkan dan membatasi gerakan mouse/scroll serta panjang teks.
- Tombol sistem yang tersedia hanya **Lock**. Shutdown/restart sengaja tidak disediakan agar tidak terjadi pemadaman tanpa sengaja.
- `pyautogui.FAILSAFE` aktif: arahkan mouse ke sudut kiri atas layar laptop untuk menghentikan gerakan otomatis.
- Jangan jalankan server sebagai Administrator. Tutup server dengan Ctrl+C saat selesai.
- Bluetooth SPP membutuhkan dukungan pada driver/adaptor Windows. Sebagian perangkat Bluetooth modern tidak menyediakan COM port SPP; proyek ini tidak dapat mengatasi ketiadaan dukungan tersebut.
- Build ini adalah source project, bukan APK yang sudah dikompilasi. APK perlu dibangun dengan Android Studio/Gradle.


## Build APK online tanpa memasang JDK/Android SDK di laptop

Proyek ini sudah menyertakan workflow GitHub Actions di `.github/workflows/build-apk.yml`.
JDK 17, Android SDK, dan Gradle dipasang sementara di komputer cloud GitHub Actions, bukan di laptop pengguna.

### A. Buat akun GitHub
1. Buka https://github.com/ lalu pilih **Sign up**.
2. Daftarkan email, verifikasi, dan selesaikan pembuatan akun.
3. Login ke GitHub.

### B. Buat repository dan unggah proyek dari VS Code
1. Buat repository baru di https://github.com/new dengan nama `RemoteLaptopBluetooth`.
2. Pilih **Public** atau **Private** (keduanya dapat menjalankan workflow; kuota/ketentuan GitHub berlaku).
3. Jangan centang opsi untuk menambahkan README, .gitignore, atau License karena proyek ini sudah memilikinya.
4. Buka folder proyek di VS Code.
5. Buka Terminal → New Terminal, lalu jalankan (ganti URL dengan URL repository milik sendiri):

   ```powershell
   git init
   git add .
   git commit -m "Initial Remote Laptop Bluetooth project"
   git branch -M main
   git remote add origin https://github.com/USERNAME/RemoteLaptopBluetooth.git
   git push -u origin main
   ```

   Jika Git belum terpasang, instal dari https://git-scm.com/download/win lalu buka ulang VS Code.
   Saat diminta login, ikuti jendela browser Git Credential Manager.

### C. Jalankan build online
1. Buka repository GitHub.
2. Pilih tab **Actions**.
3. Pilih workflow **Build Remote Laptop APK**.
4. Klik **Run workflow** → **Run workflow**.
5. Tunggu sampai job berstatus hijau **Success**. Build juga otomatis berjalan setiap kali ada push ke branch `main`.

### D. Unduh APK
1. Buka hasil run yang berstatus Success.
2. Gulir ke bagian **Artifacts**.
3. Unduh `RemoteLaptopBluetooth-debug`.
4. Ekstrak ZIP artifact untuk memperoleh `app-debug.apk`.
5. Kirim APK ke HP dan instal. Jika Android meminta izin, izinkan instalasi aplikasi dari sumber tersebut.

### Catatan
- Tidak perlu memasang JDK, Android SDK, atau Gradle di laptop untuk workflow cloud ini. VS Code tetap dipakai untuk mengedit kode; Git diperlukan untuk mengunggahnya.
- GitHub Actions menjalankan build di runner cloud GitHub. Ketersediaan menit build dan ketentuan untuk repository private mengikuti paket/kuota GitHub akun.
- APK yang dihasilkan adalah **debug APK**, cocok untuk pengujian pribadi. Untuk distribusi luas, buat signed release APK dengan keystore yang dijaga rahasia.
- Workflow ini menginstal toolchain di runner cloud setiap build. Koneksi internet diperlukan oleh runner.
