package br.com.fiap.embarquefacil.util;

public final class HeadingCalculator {
    private HeadingCalculator() {}

    /** Returns the shortest signed rotation from the device heading to the target, in [-180, 180). */
    public static float arrowRotation(float targetDegrees, float deviceHeadingDegrees) {
        float delta = (targetDegrees - deviceHeadingDegrees + 540f) % 360f - 180f;
        return delta == 180f ? -180f : delta;
    }
}
