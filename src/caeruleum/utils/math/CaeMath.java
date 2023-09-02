package caeruleum.utils.math;

public class CaeMath {
    public static float smoothMax(float a, float b, float k) {
        float h = Math.max(0f, Math.min(1f, (a - b) / k + 0.5f));
        return a * (1 - h) + b * h + k * h * (1 - h);
    };
    public static float smoothMin(float a, float b, float k) {
        float h = Math.max(0f, Math.min(1f, 0.5f + 0.5f * (b - a) / k));
        return a + h * h * k;
    }
}