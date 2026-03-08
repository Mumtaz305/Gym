# SADTAZ Workout App — Android Studio Setup Guide

## Kya Kya Hai Is App Mein
- Monday–Saturday workout split (tumhara exact plan)
- Plank → Sets + Minutes (weight nahi)
- Cardio → Sets + Minutes + Speed (km/h)
- Baaki sab → Reps + Weight (kg)
- Har roz fresh start — naya din, clean slate (history safe)
- Room Database → permanent storage (cache clear se nahi jaata)
- Progress Graph (MPAndroidChart)
- Personal Records

---

## Step 1 — Android Studio Install Karo
- https://developer.android.com/studio
- Free hai, ~1GB download

## Step 2 — Project Open Karo
- Android Studio open karo
- "Open" click karo
- Is ZIP ka extracted folder select karo (SadtazWorkout)
- Wait karo — Gradle sync hoga (5–10 min, internet chahiye)

## Step 3 — Ek Line Add Karo (MPAndroidChart ke liye)
- Project mein `settings.gradle` kholo
- `repositories` block mein yeh add karo:
  ```
  maven { url 'https://jitpack.io' }
  ```
  Final aisa dikhna chahiye:
  ```
  repositories {
      google()
      mavenCentral()
      maven { url 'https://jitpack.io' }
  }
  ```
- "Sync Now" dabao

## Step 4 — Build Karo
- Phone connect karo USB se (Developer Options + USB Debugging on)
- Ya: Build → Build APK(s)
- APK milega: app/build/outputs/apk/debug/app-debug.apk

## Step 5 — Install
- APK file phone mein transfer karo
- Tap karke install karo
- Done ✓

---

## Data Kahan Save Hota Hai
Room Database → Android internal storage
- App delete karo → data jaata hai
- Cache clear karo → DATA SAFE REHTA HAI ✅
- Phone restart → DATA SAFE ✅
- Kal ka naya din → aaj ka log clean, history mein saved ✅
