package caeruleum.utils.math;

public class CaeMath {
    public static float smoothMax(float a, float b, float k) {
        return smoothMin(a, b, -k);
    };
    public static float smoothMin(float a, float b, float k) {
        float h = Math.max(0f, Math.min(1f, 0.5f + 0.5f * (b - a) / k));
        return a + h * h * k;
    }
}