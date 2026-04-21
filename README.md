# 👁️ Smart Eye-Tracker: AI-Based Blink Detector

[![AI-Azhar Project](https://img.shields.io/badge/Project-AI--Azhar-blue.svg)]()
[![Mediapipe](https://img.shields.io/badge/Powered%20By-MediaPipe-green.svg)](https://mediapipe.dev)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-orange.svg)](https://kotlinlang.org)

**Smart Eye-Tracker** adalah aplikasi Android berbasis Kecerdasan Buatan (AI) yang dirancang untuk mencegah *Digital Eye Strain* (mata lelah akibat gadget). Aplikasi ini memantau aktivitas mata secara *real-time* dan mengingatkan pengguna untuk berkedip secara rutin.

## ✨ Fitur Unggulan
- **AI Real-time Tracking**: Menggunakan **Google MediaPipe Face Landmarker** untuk mendeteksi 478 titik wajah secara presisi.
- **EAR Algorithm (Eye Aspect Ratio)**: Logika deteksi kedipan menggunakan rasio perbandingan tinggi dan lebar mata, sehingga tetap akurat meski jarak wajah ke kamera berubah.
- **Picture-in-Picture (PiP) Support**: Aplikasi tetap berjalan di jendela kecil (floating window) sehingga pengguna terlindungi meski sedang membuka YouTube, Game, atau belajar di aplikasi lain.
- **Smart Reminder System**: 
  - **Visual**: Teks berubah menjadi merah menyala jika tidak berkedip selama 10 detik.
  - **Audio**: Voice Assistant (TTS) akan bersuara *"Ayo berkedip sekarang"* setiap 5 detik jika pengguna melotot terlalu lama.
- **Auto-Rotation & Mirroring**: Optimalisasi kamera depan untuk deteksi posisi wajah yang stabil.

## 🛠️ Tech Stack
- **Language**: Kotlin
- **Framework**: Android SDK (Min SDK 24)
- **AI Engine**: Google MediaPipe Solutions
- **Camera API**: CameraX
- **Logic**: EAR (Eye Aspect Ratio) Calculation

## 📐 Logika Deteksi (EAR)
Aplikasi menggunakan rumus matematika **EAR** untuk menentukan kondisi mata:

$$EAR = \frac{||p2 - p6|| + ||p3 - p5||}{2 ||p1 - p4||}$$

Nilai EAR akan dihitung pada setiap frame. Jika nilai turun di bawah ambang batas (Threshold: 0.22), sistem akan mencatatnya sebagai satu kedipan dan mereset timer peringatan 10 detik.

## 🚀 Cara Menjalankan
1. **Clone** repository ini: `git clone https://github.com/username/eyestracker.git`
2. Buka proyek di **Android Studio**.
3. Masukkan file model `face_landmarker.task` ke dalam folder `app/src/main/assets/`.
4. Hubungkan smartphone Android asli (disarankan menggunakan perangkat fisik, bukan emulator).
5. Klik **Run 'app'**.

## 📝 Metodologi Pengembangan
Proyek ini dikembangkan dalam 10 tahapan (Pertemuan), mulai dari analisis masalah kesehatan mata, perancangan UI menggunakan XML, implementasi CameraX, hingga integrasi model AI MediaPipe dan optimasi mode Picture-in-Picture.

---

**Developed with ❤️ for AI-Azhar Project**
*Created by [Rendi Abiem Pamungkas]*
