package caeruleum.utils.math;

public class CaeMath {
    public static float smoothMax(float a, float b, float k){
        return smoothMax(a, b, k, 0f, 1f);
    }
    public static float smoothMin(float a, float b, float k){
        return smoothMin(a, b, k, 0f, 1f);
    }
    public static float smoothMax(float a, float b, float k, float min, float max) {
        float h = Math.max(min, Math.min(max, (a - b) / k + 0.5f));
        return a * (1 - h) + b * h + k * h * (1 - h);
    };
    public static float smoothMin(float a, float b, float k, float min, float max) {
        float h = Math.max(min, Math.min(max, 0.5f + 0.5f * (b - a) / k));
        return a + h * h * k;
    }
}
