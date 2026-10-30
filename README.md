# PhoneAI • AETHER Physics Community & On-Device AI Mentor

[![License: MIT](https://img.shields.io/badge/License-MIT-gold.svg)](https://opensource.org/licenses/MIT)
[![Platform: Android](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-blue.svg)](https://developer.android.com)
[![Engine: llama.cpp](https://img.shields.io/badge/Engine-llama.cpp%20GGUF-orange.svg)](https://github.com/ggerganov/llama.cpp)
[![Offline: 100%](https://img.shields.io/badge/Offline-Zero%20Internet%20Required-success.svg)](#)
[![RAM: <350MB](https://img.shields.io/badge/RAM%20Footprint-%3C350MB-informational.svg)](#)

**PhoneAI (AETHER • Physics)** is a high-performance, 100% private, on-device Android application designed to run compact language models (**200MB – 1.7GB**) directly on budget smartphones (such as the **MediaTek Helio G85 / Redmi 13C** and Qualcomm Snapdragon devices). 

It couples an ultra-fast **native `llama.cpp` GGUF engine** with a **local physics social network**, an interactive **Physics Buddy AI mentor**, and an **offline physics knowledge base** (with support for Wikipedia `.zim` archives).

---

## 🌟 Key Features

### ⚛️ 1. Local Physics Social Media (The Feed)
- **Zero-Cloud, Fully Offline Community:** Powered by an embedded SQLite database (`phoneai_physics.db`).
- **Rich Discussions & Topics:** Pre-seeded with real discussions spanning Quantum Mechanics, Relativistic Astrophysics, Thermodynamics, and Solid State Physics.
- **Peer-to-Peer Interactivity:** Upvoting system, post saving/bookmarking, and local multi-threaded comment trees.
- **✨ One-Tap AI Hypotheses:** Tap *"Ask Buddy"* on any community post to trigger on-device AI analysis, generating a peer-review theoretical critique and formula citations directly onto the post card.
- **Discussion Creator:** Publish new hypotheses and queries directly into your local feed with category tags (`#Quantum`, `#Relativity`, `#Thermodynamics`, `#Astrophysics`, `#SolidState`).

### 💬 2. Physics Buddy (Interactive AI Mentor)
- **Deep Explanations Without Truncation:** Tuned with an expanded **1024 token limit** and model-native chat templates (`<start_of_turn>` for Gemma-2, `<|im_start|>` ChatML for SmolLM2) to prevent mid-sentence cut-offs during code and formula derivations.
- **Instant Quick Prompts:** Pre-configured one-tap chips for *Bell's Theorem*, *Step-by-Step Derivation of $E=mc^2$*, *Entropy & the Arrow of Time*, and *Hawking Radiation*.
- **Real-Time Telemetry:** Live feedback displaying tokens generated, generation time in milliseconds, and tokens per second (`tok/s`).
- **Persistent Chat History:** Seamlessly stored in SQLite across sessions.

### 📚 3. Offline Physics Encyclopedia & ZIM Integration
- **Formulas & Core Principles:** Instant access to foundational physics formulas (Schrödinger Equation, Einstein Field Equations, Maxwell's Equations, Bekenstein-Hawking Entropy).
- **"Discuss with Buddy":** One-click button to immediately pipe any formula or principle into the AI mentor for in-depth Socratic study.
- **openZIM / Kiwix Ready:** Auto-detects offline Wikipedia Physics dumps (`wikipedia_en_physics_nopic_2026-07.zim`) in device storage.

### 🏛️ 4. Minimalist Royal Boxy Design
- **Color Palette:** Deep Royal Obsidian (`#090D16`), Imperial Navy cards (`#151E32`), Crisp Borders (`#233252`), and Royal Gold accents (`#D4AF37`).
- **Boxy Geometry:** Sharp, functional 4dp cards designed for legibility, scientific clarity, and clean navigation.

---

## ⚡ Supported Models & Engine Architecture

PhoneAI utilizes a dynamic dispatch architecture that chooses the best engine for your model file:

| Model | File Format | Size | RAM Used | Inference Speed (Helio G85) | Recommended Use |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **SmolLM2-360M-Instruct** *(Top Pick)* | `.gguf` (Q4_K_M) | **~258 MB** | **~320 MB** | **30 – 45 tok/s** | Blazing-fast chats, battery efficiency, instant replies |
| **Gemma-2-2B-Instruct** | `.gguf` (Q4_K_M) | **~1.71 GB** | **~1.6 GB** | **4 – 8 tok/s** | Complex theoretical derivations, deep reasoning |
| **Qwen2.5-0.5B-Instruct** | `.gguf` (Q4_K_M) | **~320 MB** | **~380 MB** | **22 – 35 tok/s** | Python math scripts, physics equations, logic |
| **SmolLM2-135M-Instruct** | `.gguf` (Q4_K_M) | **~90 MB** | **~180 MB** | **45 – 60 tok/s** | Ultra-lightweight footprint |
| **Gemma-2B-Instruct (GPU)** | `.bin` / `.task` | ~1.35 GB | ~1.8 GB | 5 – 10 tok/s | MediaPipe Tasks GenAI (Qualcomm Adreno OpenCL only) |

> [!NOTE]
> For universal compatibility across all Android chipsets (including **MediaTek Mali**, **Samsung Exynos**, and **Google Tensor**), **GGUF format** via the integrated CPU engine is recommended. MediaPipe GPU `.bin` files rely on Qualcomm-specific OpenCL symbols (`clSetPerfHintQCOM`) that are not available on Mali GPUs.

---

## 🛠️ Architecture & Codebase Tour

```
PhoneAI/
├── app/src/main/java/com/phoneai/localmodel/
│   ├── MainActivity.kt           # Main coordinator: Tab navigation (Feed/Buddy/Wiki/Saved) & UI
│   ├── LocalAIEngine.kt          # Dual-inference engine: llama.cpp multi-threading & MediaPipe
│   ├── PhysicsDatabaseHelper.kt  # SQLite database helper: posts, comments, wiki topics & chat
│   ├── ModelFileManager.kt       # Model discovery, SAF storage importer, and ZIM scanner
│   ├── PhysicsPostAdapter.kt     # RecyclerView adapter for social feed cards & AI hypotheses
│   ├── PhysicsCommentAdapter.kt  # Adapter for nested community discussion threads
│   ├── PhysicsWikiAdapter.kt     # Adapter for physics theory formula cards
│   ├── ChatAdapter.kt            # Chat bubble adapter with syntax formatting & copy actions
│   └── ChatMessage.kt            # Data model representing user & assistant turns with telemetry
└── app/src/main/res/
    ├── layout/                   # Activity & dialog XML layouts with Royal Boxy styling
    ├── values/colors.xml         # Royal Obsidian, Imperial Navy & Royal Gold palette
    └── values/themes.xml         # Material3 Dark borderless theme
```

---

## 🚀 Quick Start Guide

### Step 1: Download GGUF Model Weights
Download any compatible `.gguf` model from Hugging Face:
- **SmolLM2 360M Instruct (Recommended):** [smollm2-360m-instruct-q4_k_m.gguf](https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q4_k_m.gguf) (~258 MB)
- **Gemma 2 2B Instruct:** [gemma-2-2b-it-q4_k_m.gguf](https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf) (~1.71 GB)
- **Qwen 2.5 0.5B Instruct:** [qwen2.5-0.5b-instruct-q4_k_m.gguf](https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf) (~320 MB)

Place the `.gguf` file in the root project folder.

---

### Step 2: Deploy to Device

#### Method A: Automated Deployment Script (via ADB)
Connect your Android phone with **USB Debugging** enabled:

**PowerShell:**
```powershell
.\push_model.ps1
```
*(To target a specific model: `.\push_model.ps1 -ModelFile "gemma-2-2b-it-q4_k_m.gguf"`)*

**Command Prompt:**
```cmd
push_model.bat
```

#### Method B: Direct On-Device Import (No PC Required)
1. Download the `.gguf` file directly in your mobile browser or transfer it to your phone's `Downloads` folder via USB.
2. Launch the **PhoneAI** app.
3. Tap **"Select Model File"** and choose the downloaded file using Android's Storage Access Framework.
4. The file will be imported safely into private app storage and initialized.

---

### Step 3: Build & Install the APK

Ensure you have **Android Studio** and **JDK 17** configured:

```bash
# Build the debug APK
./gradlew assembleDebug

# Install on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📊 Performance Benchmarks (Redmi 13C / MediaTek Helio G85)

| Benchmark Metric | SmolLM2-360M (Q4_K_M) | Gemma-2-2B (Q4_K_M) | Qwen2.5-0.5B (Q4_K_M) |
| :--- | :--- | :--- | :--- |
| **Model Size** | **258 MB** | 1.71 GB | 320 MB |
| **Active RAM Usage** | **~310 MB** | ~1.65 GB | ~370 MB |
| **Generation Speed** | **32.4 tok/s** | 5.8 tok/s | 26.1 tok/s |
| **Latency to First Token** | **< 200 ms** | ~1.4 s | ~250 ms |
| **Battery Consumption** | Minimal (~2% / hr) | Moderate (~8% / hr) | Low (~3% / hr) |
| **Internet Required** | **No (0 KB)** | **No (0 KB)** | **No (0 KB)** |

---

## 🔒 Privacy & Permissions
- **100% Offline:** The app declares no network access permissions. All inference, prompts, and database queries execute strictly on your device.
- **Zero Telemetry / Analytics:** Your thoughts, queries, and community discussions never leave your phone.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE). You are free to use, modify, distribute, and build upon this software for personal and commercial projects.
