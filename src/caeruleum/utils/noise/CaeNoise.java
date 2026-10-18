package caeruleum.utils.noise;

import arc.math.geom.Vec3;
import arc.util.noise.Simplex;

public class CaeNoise {

    private CaeNoise(){};

    public static float ridgeNoise3d(int seed, double octaves, double persistence, double roughness, double minValue, float strength, double scale, double x, double y, double z){
    double sum = 0;
    double frequency = scale;
    double amplitude = 1;
    double weight = 1;
    double maxAmplitude = 0; // Track what actually gets added

    for(int i = 0; i < octaves; i++){
        double raw = Simplex.raw3d(seed, x * frequency, y * frequency, z * frequency);
        double v = 1f - Math.abs(raw);
        v *= v;
        v *= weight;
        weight = Math.min(1.0, Math.max(0,v * 2f)); // Optional: clamp weight so it doesn't instantly die to 0

        sum += v * amplitude;
        maxAmplitude += amplitude;
        
        frequency *= roughness;
        amplitude *= persistence;
    }
    double n = sum / maxAmplitude; // Normalize to [0,1]
    n = Math.max(0, n - minValue ) / (1 - minValue);
    
    // Use a safer scaling factor or scale directly by strength
    return (float)(n * strength);
}
    public static float ridgeNoise3d(int seed, double octaves, double persistence, double roughness, double minValue, float strength, double scale, Vec3 vec) {
        return ridgeNoise3d(seed, octaves, persistence, roughness, minValue, strength, scale, vec.x, vec.y, vec.z);
    };
    public static float noise3d(int seed, double octaves, double persistance, double scale, Vec3 vec){
        return Simplex.noise3d(seed, octaves, persistance, scale, vec.x, vec.y, vec.z);
    };

};
