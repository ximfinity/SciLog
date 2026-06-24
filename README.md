# SciLog

An Android app for tracking GLP-1 injections (Semaglutide / Tirzepatide) with a two-compartment pharmacokinetic model that estimates how much medication is active in your body at any moment.

**Android only · No account · No cloud · Everything stays on-device**

---

## Features

### Injection Tracking
- Log each shot with medication type, dose (mg), and injection site
- Backdate entries if you forgot to log on the day
- Full scrollable history grouped by month — edit or delete any entry

### Vial Inventory
- Add vials with concentration and starting volume (mL)
- Each logged shot automatically calculates mL used and subtracts from the vial balance

### Weight Log
- Track weight over time in lbs
- Chart overlays injection dates so you can spot patterns

### Serum Level Forecast (PK Chart)
- Two-compartment PK model (RK4 ODE) fitted to Semaglutide and Tirzepatide pharmacokinetics
- Shows drug accumulation over the first ~4 weeks to steady state
- Projects 3 upcoming doses forward
- Allometric weight scaling for personalized clearance estimates
- Optional target dose reference lines (T.Peak / T.Trough in blue) for goal-setting
- Symptom overlay — log nausea, fatigue, appetite changes and see if they correlate with your level

### Dashboard
- Green / yellow / red stoplight showing where you are in your cycle
- Current level as % of steady-state peak
- Mini serum chart with projected doses
- Weight progress chart
- Catch-up dose calculator for missed injections

### Import / Export
- Export all data as CSV
- Import from CSV (AI prompt generator included — generates a structured prompt you can share to ChatGPT or similar with a screenshot)

### Configuration
- Injection cycle interval (days)
- Starting date and weight
- Target weight
- Target dose (drives the blue reference lines on the PK chart)
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
