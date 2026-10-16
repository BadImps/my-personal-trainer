# Free local Coach

Version 3.6 makes the private, free branch the default in Nutrition:

1. **Free local Coach** is built into the app. It recognises common food words, creates a deliberately broad common-portion range, and connects the meal to the selected workout goal. It makes no network request and keeps the typed meal and body profile on-device.
2. **Optional on-device Gemma** can improve the wording of that answer. Coach Settings includes **Download Gemma model (official)**, which opens the official gated model page so the person can accept the model terms and download an Android-compatible `.litertlm` model. They then return to **Install downloaded Gemma model** to select that file from the device. The APK never bundles model credentials, accepts terms silently, or silently downloads a large model. The deterministic local range remains the source of the calorie estimate.
3. **Optional online Gemini** remains a separate button. It uses a person-owned HTTPS gateway and must never place an API key in the APK.

The local Coach is not a food label, medical service, allergy checker, calorie prescription, or diagnosis. Packaging, ingredients, portions, cultural context, and clinical guidance take precedence.

## Why the model is separate

The Android LiteRT-LM runtime loads a model from local storage, and Google documents that a production app should download a model at runtime instead of bundling it in the APK. Models may require substantial storage and work best on capable hardware. The app therefore remains useful on every supported phone without requiring an account, a large download, or cloud access.
