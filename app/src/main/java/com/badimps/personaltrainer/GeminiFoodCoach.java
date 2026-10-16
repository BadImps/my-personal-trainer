package com.badimps.personaltrainer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Calls a user-owned HTTPS gateway which invokes Gemini server-side. This keeps a Gemini API key
 * out of the APK, where it could be extracted and abused.
 */
public final class GeminiFoodCoach {
    public static final class Request {
        public final String meal, goal, workoutFocus;
        public final int workoutMinutes, age;
        public final float heightCm, weight;
        public final String weightUnit, gender;
        public final boolean includeBodyContext;

        public Request(String meal, String goal, String workoutFocus, int workoutMinutes, int age,
                       float heightCm, float weight, String weightUnit, String gender, boolean includeBodyContext) {
            this.meal = meal;
            this.goal = goal;
            this.workoutFocus = workoutFocus;
            this.workoutMinutes = workoutMinutes;
            this.age = age;
            this.heightCm = heightCm;
            this.weight = weight;
            this.weightUnit = weightUnit;
            this.gender = gender;
            this.includeBodyContext = includeBodyContext;
        }
    }

    public static final class Result {
        public final String summary, calories, healthNote, planFit;
        Result(String summary, String calories, String healthNote, String planFit) {
            this.summary = summary;
            this.calories = calories;
            this.healthNote = healthNote;
            this.planFit = planFit;
        }
    }

    public static Result ask(String endpoint, Request request) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("meal", request.meal);
        payload.put("goal", request.goal);
        payload.put("workoutFocus", request.workoutFocus);
        payload.put("workoutMinutes", request.workoutMinutes);
        payload.put("responseFormat", "Return JSON with summary, estimatedCalories, healthNote, and planFit. Use calorie ranges when uncertain. Do not diagnose or prescribe treatment.");
        if (request.includeBodyContext) {
            JSONObject body = new JSONObject();
            body.put("age", request.age);
            body.put("gender", request.gender);
            body.put("heightCm", request.heightCm);
            body.put("weight", request.weight);
            body.put("weightUnit", request.weightUnit);
            payload.put("bodyContext", body);
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setConnectTimeout(12_000);
        connection.setReadTimeout(20_000);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Accept", "application/json");
        byte[] bytes = payload.toString().getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(bytes);
        }
        int status = connection.getResponseCode();
        String body = read(status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream());
        if (status < 200 || status >= 300) throw new IllegalStateException("The Gemini service returned " + status + (body.isEmpty() ? "" : ". " + body));
        return parse(body);
    }

    static Result parse(String response) throws Exception {
        JSONObject root = new JSONObject(response);
        String embeddedText = geminiText(root);
        if (!embeddedText.isEmpty() && !embeddedText.trim().startsWith("{")) {
            return new Result(embeddedText, "Not provided", "Review ingredients and your personal needs.", "Ask your coach or clinician if you need personalised guidance.");
        }
        JSONObject value = embeddedText.trim().startsWith("{") ? new JSONObject(embeddedText) : root;
        return new Result(nonEmpty(value.optString("summary"), "No summary was returned."),
                nonEmpty(value.optString("estimatedCalories", value.optString("calories")), "Not provided"),
                nonEmpty(value.optString("healthNote"), "No health note was returned."),
                nonEmpty(value.optString("planFit"), "No plan-fit note was returned."));
    }

    private static String geminiText(JSONObject root) {
        try {
            JSONArray candidates = root.optJSONArray("candidates");
            if (candidates == null || candidates.length() == 0) return "";
            JSONArray parts = candidates.getJSONObject(0).getJSONObject("content").optJSONArray("parts");
            return parts == null || parts.length() == 0 ? "" : parts.getJSONObject(0).optString("text", "");
        } catch (Exception ignored) { return ""; }
    }

    private static String nonEmpty(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static String read(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) output.append(line);
        }
        return output.toString();
    }

    private GeminiFoodCoach() { }
}
