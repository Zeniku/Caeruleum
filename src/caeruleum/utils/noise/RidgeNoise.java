package caeruleum.utils.noise;

import arc.util.Log;
import arc.util.noise.Simplex;

public class RidgeNoise {

    private RidgeNoise(){};

    public static float noise3d(int seed, double octaves, double persistence, double roughness, double minValue, float strength,double scale, double x, double y, double z){
        double total = 0;
        double frequency = scale;
        double amplitude = 1;
        double weight = 1;
        // We have to keep track of the largest possible amplitude,
        // because each octave adds more, and we need a value in [-1, 1].
        double maxAmplitude = 0;

        for(int i = 0; i < octaves; i++){
            double v = 1-Math.abs((Simplex.raw3d(seed, x * frequency, y * frequency, z * frequency) + 1f) / 2f);
            v *= weight;
            weight = v;

            total += v * amplitude;
            frequency *= roughness;
            maxAmplitude += amplitude;
            amplitude *= persistence;
        }
        total = Math.max(0, total - minValue);
        return (float)total * strength;
    }
};
