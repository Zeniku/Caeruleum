package caeruleum.utils.noise;

import arc.math.geom.Vec3;
import arc.util.Log;
import arc.util.noise.Ridged;
import arc.util.noise.Simplex;
import caeruleum.utils.math.CaeMath;
import arc.math.Interp;
import arc.math.Mathf;

public class CaeNoise {

    private CaeNoise(){};

    public static float ridgeNoise3d(int seed, double octaves, double persistence, double roughness, double minValue, float strength,double scale, double x, double y, double z){
        double noiseValue = 0;
        double frequency = scale;
        double amplitude = 1;
        double weight = 1;
        // We have to keep track of the largest possible amplitude,
        // because each octave adds more, and we need a value in `[-1, 1].
        double maxAmplitude = 0;

        for(int i = 0; i < octaves; i++){
            double v = 1f - Math.abs((Simplex.raw3d(seed, x * frequency, y * frequency, z * frequency) + 1f) / 2f);
            v *= v;
            v *= weight;
            weight = v;

            noiseValue += v * amplitude;
            frequency *= roughness;
            maxAmplitude += amplitude;
            amplitude *= persistence;
        }
        noiseValue = Math.max(0, noiseValue - minValue);
        return (float)(noiseValue / maxAmplitude) * strength;
    };
    public static float ridgeNoise3d(int seed, double octaves, double persistence, double roughness, double minValue, float strength, double scale, Vec3 vec) {
        return ridgeNoise3d(seed, octaves, persistence, roughness, minValue, strength, scale, vec.x, vec.y, vec.z);
    };
    public static float noise3d(int seed, double octaves, double persistance, double scale, Vec3 vec){
        return Simplex.noise3d(seed, octaves, persistance, scale, vec.x, vec.y, vec.z);
    };

};
