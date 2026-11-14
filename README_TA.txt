KITTY AI — FULL SOURCE PROJECT (Prototype v2)

இதில் உள்ள செயல்படுத்தப்பட்டவை:
- Android app UI; Tamil/English input
- Android SpeechRecognizer voice input (device speech service சார்ந்தது; offline உறுதி இல்லை)
- Android TextToSpeech spoken response (Tamil voice device-ல் இருந்தால்)
- Offline basic commands: greeting, date, time, save/show/clear memory
- EncryptedSharedPreferences local memory (crypto init தோல்வியுற்றால் private app preferences fallback)
- Android biometric/device credential prompt
- Google/Web search intent
- Experimental foreground microphone wake phrase service
- GitHub Actions workflow மூலம் debug APK build

முக்கிய வரம்புகள் — நேர்மையாக:
- Wake listener Android SpeechRecognizer-ஐப் பயன்படுத்துகிறது; இது உண்மையான low-power always-on keyword spotting அல்ல. Battery/OS restrictions மற்றும் network speech provider காரணமாக offline/always-listening உறுதி இல்லை.
- On-device generative LLM இன்னும் சேர்க்கப்படவில்லை. Offline பதில்கள் கட்டளை அடிப்படையிலானவை; ChatGPT போன்ற சுதந்திரமான உரையாடல் அல்ல.
- Online AI API backend சேர்க்கப்படவில்லை; API key இல்லாமல் AI service இயங்காது. இந்த project எந்த paid API-யையும் கட்டாயப்படுத்தவில்லை.
- Device controls எல்லாம் universal ஆக இயலாது. Android permission / OS policy தேவை; torch, contacts, calls போன்றவை தனித்தனியாக integrate செய்யப்பட வேண்டும்.
- Security prompt உள்ளது; ஆனால் முழு app-ஐ திறக்கும் முன் lock gate, encrypted backup/export, threat review ஆகியவை production-ready அல்ல.
- App signing key இல்லை; இது debug APK மட்டும்.

GitHub build:
1) ZIP extract செய்.
2) உள்ளே உள்ள கோப்புகளை repository root-ல் upload செய் (.github folder உட்பட).
3) GitHub Actions > Build Kitty AI APK > Run workflow.
4) Build வெற்றி என்றால் Artifact KittyAI-debug-apk download செய்து extract செய்.

Build: JDK 17, Gradle 8.9, Android Gradle Plugin 8.7.3, compileSdk 35.


ANDROID PERMISSIONS / PHONE CONTROL (v2.1 patch):
- First launch asks RECORD_AUDIO, POST_NOTIFICATIONS and CAMERA runtime permissions. Camera permission is used for flashlight control.
- Hey Kitty button starts a microphone foreground service after microphone permission. Android 12+ / 14+ may restrict starting or continuing a microphone service in background; user should start it while app is visible and keep its persistent notification. Battery optimization / OEM restrictions can still stop it.
- Phone Control button opens Android Accessibility Settings. The user must explicitly enable Kitty AI there. The accessibility bridge implements only Back, Home, Notifications and Recents global navigation actions; it does not silently control other apps.
- Voice/text commands supported in this patch: “torch on/off” / “டார்ச் ஆன்/ஆஃப்”; “volume up/down” / “சத்தம் கூட்டு/குறை”; “home”, “back”, “notifications”. Flashlight requires camera permission; navigation requires manually enabled Accessibility service.
- SpeechRecognizer is Android's speech recognition provider; it is NOT a dedicated offline wake-word engine and may use the device's configured recognition service/network. This patch does not claim reliable always-on offline wake-word detection.
- Contacts and direct calling are not enabled as voice actions in this patch; permissions are not requested unnecessarily.
