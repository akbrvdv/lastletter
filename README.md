# 🔠 LastLetter - Permainan Sambung Kata KBBI Multiplayer

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)
![Room Database](https://img.shields.io/badge/Room_DB-00599C?style=for-the-badge&logo=sqlite&logoColor=white)

**LastLetter** adalah aplikasi permainan edukatif interaktif berbasis Android di mana pemain harus menyambung kata berdasarkan huruf terakhir dari kata sebelumnya. Seluruh input divalidasi secara ketat menggunakan **Kamus Besar Bahasa Indonesia (KBBI)**. Aplikasi ini mendukung mode bermain *Offline* melawan AI Bot maupun mode Multiplayer (PvP) *Online* secara *real-time*.

---

## ✨ Fitur Utama

* ⚔️ **PvP Online Multiplayer (Real-Time)**
  Sistem *matchmaking* menggunakan Room ID (6 digit) yang disinkronisasi dalam hitungan milidetik menggunakan **Firebase Realtime Database**. Memanfaatkan *ValueEventListener* untuk pembaruan *timer*, nyawa, dan giliran secara presisi antar dua perangkat yang berbeda.

* 🤖 **Offline Mode vs AI Bot**
  Bermain tanpa koneksi internet melawan bot cerdas yang mengambil kosakata langsung dari ribuan data KBBI di **Local Room Database**.

* 🔐 **Persistent Smart Authentication**
  Sistem *Auto-Login* menggunakan *Firebase AuthStateListener*. Aplikasi mampu mengingat sesi login terakhir pengguna dengan mulus meskipun aplikasi dihapus dari latar belakang (*recent apps*).

* 📜 **Isolated Match History**
  Riwayat pertandingan (skor, status menang/kalah, rentetan kata) disimpan menggunakan **Room Database**. Skema database dirancang dengan kolom `userId` agar histori terisolasi per akun, didukung oleh sistem `fallbackToDestructiveMigration` untuk stabilitas pembaruan.

* ✨ **Dynamic UI/UX**
  Dibangun sepenuhnya dengan **Jetpack Compose**. Menggunakan algoritma *Dynamic Font Sizing* yang secara otomatis menyesuaikan ukuran teks jika kata dari KBBI terlalu panjang agar tidak terpotong (overflow) di layar.

---

## 🛠️ Teknologi & Arsitektur

Aplikasi ini dibangun dengan mengimplementasikan standar industri Android modern:

* **Bahasa:** Kotlin
* **Arsitektur:** MVVM (Model-View-ViewModel) + Clean Architecture
* **UI Toolkit:** Jetpack Compose (Material Design 3)
* **Dependency Injection:** Dagger Hilt
* **Asynchronous Programming:** Kotlin Coroutines & Flow (StateFlow)
* **Backend & Cloud:** Firebase Authentication, Firebase Realtime Database
* **Local Database:** Room Database (SQLite)

---

## 📱 Cuplikan Layar


| Tampilan Login | Beranda (Home) | Mode AI Bot | Mode PvP Online | Histori |
| :---: | :---: | :---: | :---: | :---: |
| <img src="https://github.com/user-attachments/assets/534388fc-56dc-4eca-8e9e-146a9ec28ac5" />"| <img src="https://github.com/user-attachments/assets/24a4dc9e-b4d0-4d7d-88a0-e1587fe4fac4" /> | <img src="https://github.com/user-attachments/assets/e79a8013-85a2-4a40-b500-bc7a72ec46ea" /> | <img src="https://github.com/user-attachments/assets/83ba8a34-1208-4f87-95af-037e85164fab" /> | <img src="https://github.com/user-attachments/assets/f3d86c80-5a97-45d1-a60e-61ffe769f5c5" /> |

---

## 🚀 Panduan Instalasi

Untuk menjalankan aplikasi ini secara lokal di Android Studio, ikuti langkah-langkah berikut:

1. **Clone Repositori:**
   ```bash
   git clone https://github.com/akbrvdv/lastletter.git
2. **Buka di Android Studio:**
   
   Buka direktori yang baru saja di-clone menggunakan Android Studio versi terbaru.
3. **Konfigurasi Firebase:**
   
   *Pastikan Anda memiliki proyek aktif di Firebase Console.
   *Aktifkan layanan Authentication (Email/Password) dan Realtime Database.
   *Unduh file google-services.json dari Firebase Console Anda.
   *Letakkan file tersebut di dalam folder app/ pada proyek ini.
3. **Build & Run:**
   
   Klik Run (Shift + F10) untuk menjalankan aplikasi di Emulator atau perangkat Android fisik.

---

## 📂 Struktur Proyek Terpenting
Proyek ini dipisahkan berdasarkan ranah tanggung jawab (Separation of Concerns):

* data/ : Memuat lapisan akses data (Room Entity, DAO, Firebase Repo, DTO).

* domain/ : Memuat aturan bisnis inti aplikasi, seperti logika validasi kata KBBI (WordValidator).

* di/ : Memuat modul Dependency Injection (Hilt) untuk penyediaan Database, Firebase, dan Repository.

* ui/ : Memuat seluruh komponen visual (Compose) yang dipecah per layar fungsional (Auth, Game, Home, History).

---

## 🎓 Tentang Proyek
Proyek ini dikembangkan sebagai pemenuhan Tugas Ujian Akhir Semester (UAS) Pemrograman Mobile II di program studi Teknologi Rekayasa Perangkat Lunak, Politeknik Negeri Madiun.

Dikembangkan oleh Kelompok 6:

* **Dava Febri Wardana** * *Backend Ecosystem & Firebase Sync:* Integrasi Firebase Auth dan Realtime Database untuk sinkronisasi mode PvP.
  * *Database Architecture:* Perancangan Room Database untuk riwayat permainan terisolasi (*fallback migration*) dan implementasi logika *Auto-Login*.

* **Muhammad Akbar Fadilah** * *UI/UX Design:* Perancangan antarmuka visual modern dan responsif menggunakan Jetpack Compose.
  * *Navigation & Core Features:* Mengatur alur perpindahan layar (NavHost) dan merangkai komponen visual permainan agar interaktif.

* **Riyan Zakaria Zulkarnain** * *Application Logic:* Mengembangkan aturan dan alur dasar dari permainan sambung kata.
  * *Data Validation & Testing:* Membangun sistem `WordValidator` untuk memvalidasi input kata pemain secara ketat menggunakan database KBBI lokal.




