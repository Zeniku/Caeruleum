package caeruleum.utils.math;

import arc.math.Mathf;

public class CaeMath {
    public static float smoothMin(float a, float b, float k) {
        float h = Mathf.clamp((b - a + k) / (2f * k), -2f, 2f);
        return a * h + b * (1f - h) - k * h * (h - 1f);
    };
    public static float smoothMax(float a, float b, float k) {
        return smoothMin(a, b, -k);
    }
}