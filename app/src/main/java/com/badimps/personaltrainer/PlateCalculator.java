package com.badimps.personaltrainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure metric barbell plate calculation. Plates are always returned per side. */
public final class PlateCalculator {
    private static final int[] DEFAULT_PLATES_KG = {25, 20, 15, 10, 5, 2};

    public static final class Result {
        public final int requestedKg;
        public final int barKg;
        public final int achievableKg;
        public final Map<Integer, Integer> platesPerSide;

        Result(int requestedKg, int barKg, int achievableKg, Map<Integer, Integer> platesPerSide) {
            this.requestedKg = requestedKg;
            this.barKg = barKg;
            this.achievableKg = achievableKg;
            this.platesPerSide = Collections.unmodifiableMap(new LinkedHashMap<>(platesPerSide));
        }

        public boolean exact() { return requestedKg == achievableKg; }

        public String perSideLabel() {
            if (platesPerSide.isEmpty()) return "Empty bar";
            ArrayList<String> labels = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : platesPerSide.entrySet()) {
                labels.add(entry.getValue() + " × " + entry.getKey() + " kg");
            }
            return String.join("  +  ", labels) + " per side";
        }
    }

    public static Result calculate(int requestedKg) {
        return calculate(requestedKg, 20, DEFAULT_PLATES_KG);
    }

    public static Result calculate(int requestedKg, int barKg, int[] availablePlatesKg) {
        int safeBar = Math.max(0, barKg);
        int requested = Math.max(safeBar, requestedKg);
        int sideTarget = Math.max(0, (requested - safeBar) / 2);
        LinkedHashMap<Integer, Integer> chosen = new LinkedHashMap<>();
        int remaining = sideTarget;
        if (availablePlatesKg != null) {
            for (int plate : availablePlatesKg) {
                if (plate <= 0 || plate > remaining) continue;
                int count = remaining / plate;
                if (count > 0) {
                    chosen.put(plate, count);
                    remaining -= count * plate;
                }
            }
        }
        int achieved = safeBar + 2 * (sideTarget - remaining);
        return new Result(requested, safeBar, achieved, chosen);
    }

    private PlateCalculator() { }
}
