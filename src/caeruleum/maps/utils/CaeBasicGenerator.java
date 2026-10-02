package caeruleum.maps.utils;

import arc.math.Rand;
import arc.math.geom.Vec3;
import mindustry.maps.generators.PlanetGenerator;
import mindustry.type.Sector;
import mindustry.world.Block;
import mindustry.world.Tiles;
import mindustry.world.blocks.environment.Floor;

public class CaeBasicGenerator extends PlanetGenerator {
    public Rand getRand(){ return this.rand; };
    public Sector getSector() {return this.sector;};
    public int getMapWidth(){ return this.width; };
    public int getMapHeight(){ return this.height; };
    public Tiles getTiles(){ return this.tiles; };

    public float getNoisef(float x, float y, double scl, double mag){
        return noise(x, y, scl, mag);
    }
    public float getNoisef(float x, float y, double octaves, double falloff, double scl){
        return noise(x, y, octaves, falloff, scl);
    }
    public float getNoisef(float x, float y, double octaves, double falloff, double scl, double mag){
        return noise(x, y, octaves, falloff, scl, mag);
    }
    public void getNoise(Floor floor, Block block, int octaves, float falloff, float scl, float threshold){
         noise(floor, block, octaves, falloff, scl, threshold);
    }
    public float rawHeight(Vec3 v) {
        //placeholder for rawHeight logic, which should be implemented in subclasses
        return 0f;
    }
    public Block getBlock(Vec3 position, boolean visualOnly) {
        // placeholder for getBlock logic, which should be implemented in subclasses
        return null;
    }
    public boolean withinCrater(Vec3 position, float buffer) {
        //placeholder for withinCrater logic, which should be implemented in subclasses
        return false;
    }
}
