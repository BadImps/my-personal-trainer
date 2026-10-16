# Gemini Food Coach gateway

The Android app deliberately does **not** contain a Gemini API key. Google advises that production mobile clients use a backend proxy so a key cannot be extracted from the APK.

Configure a user-owned HTTPS gateway in Nutrition → **Ask Coach online**. The app posts:

```json
{
  "meal": "chicken, rice, and vegetables",
  "goal": "Build muscle",
  "workoutFocus": "Full Body",
  "workoutMinutes": 45,
  "responseFormat": "Return JSON with summary, estimatedCalories, healthNote, and planFit. ...",
  "bodyContext": {
    "age": 31,
    "gender": "Female",
    "heightCm": 170,
    "weight": 70,
    "weightUnit": "kg"
  }
}
```

`bodyContext` is omitted unless the person explicitly enables it in the app.

The gateway should call Gemini with its server-side secret and return only structured, non-medical guidance:

```json
{
  "summary": "A balanced meal with protein, carbohydrate, and vegetables.",
  "estimatedCalories": "500–650 kcal, depending on portions and oil",
  "healthNote": "Check sauces and sodium if those are concerns for you.",
  "planFit": "Can fit a build-muscle plan when portions suit your overall needs."
}
```

Never use the response for diagnosis, emergency care, allergy management, or treatment. The app labels returned calories as estimates and asks the user before saving a meal to the on-device plan.
