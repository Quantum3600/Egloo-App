# 🐧 Egloo — Your Second Brain, powered by **Pingo**

<!--
Tip: Replace the placeholder "cover" image below with a real screenshot/GIF.
You can drop one into ./.artifacts and update the link.
-->

<p align="center">
  <strong>Collect → Cluster → Chat.</strong><br/>
  Turn Gmail, Slack & Drive into a daily digest — and ask questions like you’re talking to your own memory.
</p>

<p align="center">
  <a href="https://github.com/Quantum3600/Egloo-App/stargazers"><img alt="Stars" src="https://img.shields.io/github/stars/Quantum3600/Egloo-App?style=for-the-badge"/></a>
  <a href="https://github.com/Quantum3600/Egloo-App/network/members"><img alt="Forks" src="https://img.shields.io/github/forks/Quantum3600/Egloo-App?style=for-the-badge"/></a>
  <a href="https://github.com/Quantum3600/Egloo-App/issues"><img alt="Issues" src="https://img.shields.io/github/issues/Quantum3600/Egloo-App?style=for-the-badge"/></a>
  <a href="https://github.com/Quantum3600/Egloo-App/blob/master/LICENSE"><img alt="License" src="https://img.shields.io/github/license/Quantum3600/Egloo-App?style=for-the-badge"/></a>
</p>

<p align="center">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin&logoColor=white"/>
  <img alt="Compose Multiplatform" src="https://img.shields.io/badge/Compose%20Multiplatform-1.7.3-4285F4?logo=jetbrains&logoColor=white"/>
  <img alt="KMP" src="https://img.shields.io/badge/Kotlin%20Multiplatform-Android%20%7C%20iOS%20%7C%20Desktop%20%7C%20Web-000000"/>
  <img alt="Navigation" src="https://img.shields.io/badge/Navigation-Decompose-00BFA5"/>
  <img alt="DI" src="https://img.shields.io/badge/DI-Koin-FF5E00"/>
</p>

<p align="center">
  <em>“Pingo stores your knowledge in an igloo.”</em>
</p>

---

## ✨ What is Egloo?

**Egloo** is a Kotlin Multiplatform personal knowledge management app (a.k.a. a “second brain”) that:

- **Ingests** your work + life signals (Gmail, Slack, Google Drive)
- **Clusters** information into topics (LLM-powered concept)
- **Summarizes** into a daily digest
- Lets you **chat with Pingo** to ask questions and surface context

> Current stage: **prototype with dummy data** — UI, navigation, theming, and app structure are in place; backend wiring is next.

---

## 🧊 The “Igloo Loop” (how it’s meant to work)

1. **Connect sources** (Gmail / Slack / Drive)
2. **Ingest & index** (background jobs)
3. **Cluster** items into topics
4. **Digest** what matters today
5. **Ask Pingo** anything — with source citations

---

## 🧭 Targets (One codebase, four frontends)

Egloo runs on:

- **Android** (minSdk 26, targetSdk 35)
- **iOS** (iOS 15+)
- **Desktop** (Windows/macOS/Linux via JVM)
- **Web** (Kotlin/Wasm — modern browsers with WasmGC)

Shared-first philosophy: most UI + logic lives in `:shared`.

---

## 🧩 Tech Stack

- **Kotlin 2.1.0**
- **Compose Multiplatform 1.7.3**
- **AGP 9.0.0-rc01**
- **Decompose 3.2.0** (navigation)
- **Koin 4.0.0** (dependency injection)
- **Ktor 3.0.3** (HTTP client, planned for backend integration)

---

## 🗺️ Project Structure

```text
androidApp/   → Android entry point
iosApp/       → Xcode project (links shared framework)
desktopApp/   → Desktop entry point
webApp/       → Web entry point (Kotlin/Wasm)
shared/       → The app (UI, state, domain, data)
```

If you want the **full architectural tour**, see **AGENTS.md**.

---

## 🚀 Getting Started

### Requirements

- Android Studio (recommended for KMP)
- JDK 17+ (typical for modern Android/Gradle)
- Xcode (for iOS)

### Build & Run — Android

```bash
./gradlew :androidApp:assembleDebug
```

### Build & Run — Desktop (JVM)

```bash
./gradlew :desktopApp:run
```

Hot reload mode (desktop):

```bash
./gradlew :desktopApp:hotRun --auto
```

### Build & Run — Web

Wasm target (faster):

```bash
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

JS target (more compatible, slower):

```bash
./gradlew :webApp:jsBrowserDevelopmentRun
```

### Run — iOS

Open `iosApp/` in Xcode and run.

---

## 🖥️ What’s already built (prototype checklist)

- ✅ Navigation across Android / iOS / Desktop / Web
- ✅ Onboarding flow (4 pages)
- ✅ Home “Daily Digest” screen
- ✅ Chat screen (Pingo bubbles + streaming simulation)
- ✅ Topics grid + topic detail sheet
- ✅ Sources connect/disconnect simulation
- ✅ Settings (theme toggle, sync frequency UI)
- ✅ Dark/light theme (dark default)

---

## 🧠 Backend & API (planned / in progress)

This repo includes an OpenAPI spec: **API_DOCS.json**.

Highlights:

- Auth: `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`
- Sources: connect/disconnect (Gmail, Slack, Drive)
- Ingest: trigger ingest jobs + upload PDF
- Query: `POST /api/v1/query/ask` + `POST /api/v1/query/ask/stream` (SSE streaming)

> If you’re working on the integration, check `shared/src/commonMain/.../data/api/ApiGuidelines.kt` (referenced in AGENTS.md).

---

## 🎨 Design & Mascot

- **Mascot**: *Pingo the Penguin* 🐧
- **Vibe**: cold storage, warm answers
- **Brand palette**: teal + arctic blues + “beak amber” accents

If you add screenshots, consider dropping them into `.artifacts/` and showing a simple gallery here.

---

## 🧰 Contributing

Ideas and PRs are welcome.

A nice starting flow:

1. Pick an issue (or create one)
2. Skim **AGENTS.md** for architecture + conventions
3. Keep changes shared-first when possible (`shared/commonMain`)

---

## 🧊 The Pingo Promise (tiny project motto)

> **Your knowledge should feel searchable, calm, and yours.**

---

## 📄 License

See [LICENSE](./LICENSE).
