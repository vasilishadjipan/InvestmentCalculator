# Release Notes - Investment Calculator v1.0.0

## 🚀 What's New & Improvements

### 📊 Advanced Charts & Visualizations
- **Vico Chart Integration**: Fully migrated chart components to Jetpack Compose Vico 3.3.1 (`columnModel` and `rememberPieChart`).
- **Investment Breakdown Doughnut Chart**: Displays proportional breakdowns with percentage slice labels and clean legend items.
- **Accumulation Growth Bar Chart**: Stacked column visualization for Principal, Contributions, and Interest growth over time.
- **Annual Schedule Table**: Fully expanded amortization and growth schedule table.

### 🧮 User-Controlled Calculation & Forms
- **Manual Calculation Flow**: Added **Calculate** and **Reset** buttons so calculations execute on demand.
- **Smooth Auto-Scrolling**: Automatically scrolls down by `2/3` of the screen height when calculating to reveal results.
- **Smart Number Inputs**: `NumberInputField` supports empty states (without resetting to `0`) with real-time error validation (`"Field cannot be empty"`).
- **Form Validation**: Calculate button is safely disabled until all required parameters are valid.
- **Initial State**: Graphs and schedules are hidden on startup and upon clicking Reset.

### 🎨 Theming & Material 3 Design
- **Custom 10-Color Palette**: Integrated a custom ColorKit palette across light and dark modes.
- **Edge-to-Edge UI**: Enabled modern edge-to-edge support with full system bar inset handling and a centered top app bar (`CenterAlignedTopAppBar`).
- **Custom Chips & Buttons**: Styled filter chips and primary/outlined buttons using Material 3 theme schemes (`FinancePrimary`, `FinanceSecondary`, `primaryContainer`, `onPrimary`).

### 💰 Monetization & AdMob Integration
- **Google AdMob Banners**: Pinned bottom banner ads configured with debug (test) and release (live) ad unit IDs.
- **App Open Ads**: Automatic full-screen App Open ads on app launch and background-to-foreground resume (with 4-hour expiration caching).
- **Debug Ad Preferences**: Added a debug-only ad control pre-screen to test the app with or without ads.

### 🌍 Localization & Robustness
- **Locale-Aware Currency**: Currency symbols (`solvedUnit`) dynamically adapt to the user's device locale.
- **Package Refactoring**: Refactored package namespace to `com.vasilis.investmentcalculator`.
- **Thoroughly Tested**: 100% passing unit tests and successful Gradle builds.
