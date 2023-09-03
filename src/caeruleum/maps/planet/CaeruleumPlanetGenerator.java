package caeruleum.maps.planet;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Vec3;
import arc.struct.ObjectMap;
import arc.util.Tmp;
import arc.util.noise.Simplex;
import caeruleum.content.CaeBlocks;
import caeruleum.utils.math.CaeMath;
import caeruleum.utils.noise.RidgeNoise;
import mindustry.content.Blocks;
import mindustry.maps.generators.BaseGenerator;
import mindustry.maps.generators.PlanetGenerator;
import mindustry.world.Block;

public class CaeruleumPlanetGenerator extends PlanetGenerator{
    public int seed = 0;
    public static boolean alt = false;

    BaseGenerator basegen = new BaseGenerator();
    float scl = 5.04f;
    float waterOffset = 0.3f;
    boolean genLakes = false;

    Block[] terrain = {CaeBlocks.deepAquafluent, CaeBlocks.aquafluent, CaeBlocks.bluonixiteWater, CaeBlocks.bluonixite, Blocks.darksand, CaeBlocks.lazurigrass, Blocks.iceSnow, Blocks.snow, Blocks.ice};

    ObjectMap<Block, Block> dec = ObjectMap.of(
        CaeBlocks.lazurigrass, CaeBlocks.lazurigrass,
        CaeBlocks.lazurigrass, CaeBlocks.lazurigrass,
        Blocks.water, Blocks.water,
        CaeBlocks.bluonixiteWater, Blocks.darksandWater
    );

    ObjectMap<Block, Block> tars = ObjectMap.of(
        CaeBlocks.lazurigrass, Blocks.shale,
        CaeBlocks.lazurigrass, Blocks.shale
    );

    float water = 1.5f / terrain.length;

    float rawHeight(Vec3 position){
        position = Tmp.v33.set(position).scl(scl);
        float noise1 = RidgeNoise.noise3d(seed, 7, 0.7f, 5f, 0f, 0.7f, 1f/4f, position.x, position.y, position.z);
        float noise2 = Simplex.noise3d(seed, 7, 0.2f, 1f/3f, position.x, position.y, position.z);
        //TODO ocean
        float fault3 = Mathf.clamp(Simplex.noise3d(seed + 3, 7, 0.5f, 1f/3f, position.x, position.y, position.z));
        float tempHeight = CaeMath.smoothMax(noise1 * 1.1f, CaeMath.smoothMin(noise2, (fault3 * -1f) - 0.3f, 1.3f) * 0.67f, 1.3f);
        float height = (float)((Math.pow(tempHeight, 2.33f) + waterOffset) / (1f + waterOffset)) - 0.06f;

        height *= (height < 0.25f)? -0.25f : 1f;
        return (float) height /*- Mathf.pow(fault3, 2f)*/;
    }

    Block getBlock(Vec3 position){
        float height = rawHeight(position);
        Tmp.v31.set(position);
        position = Tmp.v33.set(position).scl(scl);
        float rad = scl;
        float temp = Mathf.clamp(Math.abs(position.y * 2f) / (rad));
        float tnoise = Simplex.noise3d(seed, 7, 0.56, 1f/3f, position.x, position.y + 999f, position.z);

        temp = Mathf.lerp(temp, tnoise, 0.5f);
        height *= 1.2f;
        height = Mathf.clamp(height);

        float tar = Simplex.noise3d(seed, 4, 0.55f, 1f/2f, position.x, position.y + 999f, position.z) * 0.3f + Tmp.v31.dst(0, 0, 1f) * 0.2f;
        Block res = terrain[Mathf.clamp((int)(height * terrain.length), 0, terrain.length - 1)];

        //Block res = arr[Mathf.clamp((int)(temp * arr.length), 0, arr[0].length - 1)][Mathf.clamp((int)(height * arr[0].length), 0, arr[0].length - 1)];
        if(tar > 0.5f){
            return tars.get(res, res);
        }else{
            return res;
        }
    }

    @Override
    public Color getColor(Vec3 position){
        Block block = getBlock(position);
        //replace salt with sand color
        if(block == Blocks.salt) return Blocks.darksand.mapColor;
        return Tmp.c1.set(block.mapColor).a(1f - block.albedo);
    }

    @Override
    protected float noise(float x, float y, double octaves, double falloff, double scl, double mag){
        Vec3 v = sector.rect.project(x, y).scl(5f);
        return Simplex.noise3d(seed, octaves, falloff, 1f / scl, v.x, v.y, v.z) * (float)mag;
    }

    @Override
    public float getHeight(Vec3 position){
        float height = rawHeight(position);
        return Math.max(height, water - 1f);
    }
}
