# 💰 Investment Calculator

A modern, feature-rich Android financial calculator app built with **Jetpack Compose**, **Material 3**, **Vico Charts**, **Google AdMob**, and **Firebase (Analytics & Crashlytics)**.

---

## ✨ Features

### 🧮 Multi-Mode Financial Calculations
- **End Amount**: Calculate final accumulated balance over time.
- **Starting Amount**: Calculate initial deposit required to reach a target.
- **Contribution Amount**: Calculate periodic savings needed to hit a goal.
- **Return Rate**: Calculate required annual return rate.
- **Investment Length**: Calculate investment duration in years.
- **Flexible Frequencies**: Monthly or annual contribution frequencies, beginning/end contribution timing, and various compounding intervals (Annually, Semiannually, Quarterly, Monthly, Daily, Continuously).

### 📊 Interactive Charts & Tables
- **Accumulation Growth Chart**: Stacked bar chart built with **Vico 3** showing Principal, Contributions, and Interest growth trajectory.
- **Investment Breakdown Doughnut Chart**: Proportional doughnut chart with percentage slice labels and custom Material 3 legend.
- **Annual Schedule Table**: Full amortization schedule breaking down year-by-year starting balances, contributions, interest, and ending balances.

### 🎨 Material 3 Design & Theming
- **Custom Color Palette**: Styled using a curated 10-color ColorKit palette matching the app icon.
- **Dynamic Theming**: Fully optimized Light and Dark mode color schemes with explicit M3 container and outline roles.
- **Edge-to-Edge Support**: Modern edge-to-edge UI layout with transparent system bar handling.

### 📱 Monetization & Analytics
- **Google AdMob Integration**: 
  - Banner ads pinned at the bottom of the screen.
  - Full-screen App Open ads on app launch and background-to-foreground resume.
  - Debug-only ad control settings screen to test with or without ads.
- **Firebase Analytics & Crashlytics**:
  - Tracks user screen views per tab, calculate button taps, and reset actions.
  - Comprehensive crash reporting and non-fatal exception recording.

### 🌍 Localization
- **Locale-Aware Currency**: Automatically adapts currency symbols (`$`, `€`, `£`, `¥`, etc.) based on the device locale.

---

## 🛠️ Tech Stack & Libraries
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose & Material 3
- **Architecture**: MVVM (Model-View-ViewModel) with StateFlow
- **Charting**: Vico 3 (`com.patrykandpatrick.vico`)
- **Ads & Analytics**: Google Mobile Ads (AdMob) & Firebase (BOM, Analytics, Crashlytics)
- **Build System**: Gradle with Version Catalogs (`libs.versions.toml`)

---

## 🔒 Privacy Policy
Read the full app [Privacy Policy](PRIVACY_POLICY.md).

---