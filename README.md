# SciLog

An Android app for tracking GLP-1 injections (Semaglutide / Tirzepatide) with a two-compartment pharmacokinetic model that estimates how much medication is active in your body at any moment.

**Android only · No account · No cloud · Everything stays on-device**

---

## Features

### Dashboard
- Combined summary card: Medication Status + Dosage Outlook + Weight Progress in one view
- Green / yellow / red stoplight showing where you are in your cycle
- Current level as % of steady-state peak with next-dose countdown
- Mini serum chart with projected doses and target dose reference lines
- Compact weight progress chart
- Catch-up dose calculator for missed injections
- Five-tab navigation — Home, PK Chart, Weight, History, More

### Weight Tracker
- Full-screen premium chart with daily average line, gradient fill, and bookend dots
- Min/Max error bars showing daily range when multiple logs exist per day
- Linear-regression trend line over the selected date range
- Projection line extending to your goal weight (or 90 days forward)
- Goal weight reference line labeled with target value
- Injection-date markers with dose labels on the chart
- Date range filter chips: 30d / 60d / 90d / All
- Toggle chips to show/hide Trend, Projection, and Min/Max overlays
- Share chart as PNG via the Android share sheet
- Track weight over time with full history, edit, and delete

### Serum Level Forecast (PK Chart)
- Two-compartment PK model (RK4 ODE) fitted to Semaglutide and Tirzepatide pharmacokinetics
- Shows drug accumulation over the first ~4 weeks to steady state
- Projects 3 upcoming doses forward
- Allometric weight scaling for personalized clearance estimates
- Inline target dose input — set your goal dose directly on the chart page
- Target dose reference lines (T.Peak / T.Trough) labeled with mg and absolute mg/L value
- Shaded therapeutic band between target trough and peak
- Symptom overlay — log nausea, fatigue, appetite changes and see if they correlate with your level
- Home mini-chart matches full chart layout (4-level grid, unit labels, two-line Cmax/Cmin labels)

### Injection Tracking
- Log each shot with medication type, dose (mg), and injection site
- Backdate entries if you forgot to log on the day
- Full scrollable history grouped by month — edit or delete any entry

### Vial Inventory
- Add vials with concentration and starting volume (mL)
- Each logged shot automatically calculates mL used and subtracts from the vial balance

### Import / Export
- Export all data as CSV
- Import from CSV (AI prompt generator included — generates a structured prompt you can share to ChatGPT or similar with a screenshot)

### Configuration
- Injection cycle interval (days)
- Starting date and weight
- Target weight and target dose (drives the blue reference lines on the PK chart)
- Biometric lock

---

## Tech Stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Hilt DI |
| Database | Room |
| Navigation | Navigation Compose |
| Async | Kotlin Coroutines + Flow |
| Chart capture | GraphicsLayer (Compose 1.7+) |
| Auth | AndroidX Biometric |
| Min SDK | API 26 (Android 8.0) |

---

## Building

Requires Android Studio with the Android SDK installed.

```bash
./gradlew installDebug
```

The debug APK lands at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## PK Model Notes

The serum level estimate uses a two-compartment model solved with a 4th-order Runge-Kutta integrator:

- **ka** = 0.5 /day (absorption from SC depot)
- **CL** = 0.8 L/day (elimination clearance)
- **V₂** = 10 L (central compartment)
- **V₃** = 12 L (peripheral compartment)
- **Q** = 1.5 L/day (intercompartmental clearance)

Parameters are scaled allometrically by body weight. Tirzepatide and Semaglutide use different default parameter sets to reflect their different half-lives (~5 days vs ~7 days).

This is a pharmacokinetic model for informational purposes only — not medical advice.

---

## License

MIT
