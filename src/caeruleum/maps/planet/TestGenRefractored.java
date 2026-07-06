
package caeruleum.maps.planet;

import arc.graphics.Color;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.noise.*;
import caeruleum.content.CaeBlocks;
import caeruleum.maps.utils.CaeRoomManager;
import caeruleum.maps.utils.CaeRoomManager.Room;
import caeruleum.maps.utils.CaeRoomManager.RoomHandler;
import caeruleum.utils.noise.CaeNoise;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.maps.generators.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.Floor;


public class TestGenRefractored extends CaeMapUtils {

    // Configuration
    public float oceanFloorDepth = 1.3f, oceanFloorSmoothing = 1.3f, oceanDepthMultiplier = 0.5f;
    public boolean altPathing = false; // Renamed from static 'alt' for thread/save safety
    public int seed = 0;
    
    // Terrain Settings
    private final float noiseScale = 5f;
    private final float waterOffset = 0.3f;
    private final boolean genLakes = false;

    // Craters
    private final Vec3 crater = new Vec3(0, 0f, 1f);
    private final float craterRadius = 0.39f;
    private final float craterDepth = 1f;

    // Generator State
    private final BaseGenerator basegen = new BaseGenerator();
    
    // Blocks
    private final Block[] terrain = {
        // Ocean
        CaeBlocks.deepAquafluent, CaeBlocks.aquafluent,
        // Shoreline
        CaeBlocks.bluonixiteWater, CaeBlocks.bluonixite, Blocks.darksand,
        // Middle
        CaeBlocks.lazurigrass, CaeBlocks.lazurigrass, CaeBlocks.lazurigrass, CaeBlocks.lazurigrass, CaeBlocks.lazurigrass,
        // Peaks
        Blocks.iceSnow, Blocks.iceSnow, Blocks.snow, Blocks.snow, Blocks.ice, Blocks.ice
    };

    private final Block[] craterTerrain = {
        Blocks.slag, Blocks.magmarock, Blocks.craters, Blocks.craters, Blocks.charr
    };

    private final ObjectMap<Block, Block> dec = ObjectMap.of(
        CaeBlocks.bluonixiteWater, Blocks.darksandWater
    );

    // Optimized: Use a Set for tars to avoid map lookup overhead if output is same as key
    private final ObjectSet<Block> tarBlocks = ObjectSet.with(CaeBlocks.lazurigrass);

    // Pre-allocated for ore generation to avoid recreating every gen
    private final Seq<Block> basicOres = Seq.with(Blocks.oreCopper, Blocks.oreLead);

    // --- Noise & Height Logic ---

    private boolean withinCrater(Vec3 position, float buffer) {
        return position.within(crater, craterRadius + buffer);
    }

    private float rawHeight(Vec3 position) {
        // Use Tmp.v33 to avoid allocation, apply scale
        Vec3 pos = Tmp.v33.set(position).scl(noiseScale);
        
        float noiseVal = CaeNoise.noise3d(seed, 5, 0.4f, 1f / 2.5f, pos);
        float mountainMask = Mathf.clamp(1f - (Mathf.pow(noiseVal - 0.16f, 2f) * 1.1f));
        float mountainShape = CaeNoise.ridgeNoise3d(seed, 2, 1.3f, 4f, 0f, 1.2f, 1f / 3.2f, pos);
        
        float res = (mountainShape * mountainMask);

        // Crater logic
        float dist = position.dst(crater);
        if (dist < craterRadius) {
            float n = CaeNoise.noise3d(0, 8.4d, 0.4d, 0.27d, position) * (craterRadius / 4f);
            float x = Interp.pow2Out.apply(1f - (dist / craterRadius));
            float craterShape = craterDepth * x + (1f - x) * n;
            return res - craterShape;
        }

        return res;
    }

    @Override
    public float getHeight(Vec3 position) {
        return rawHeight(position);
    }

    @Override
    public void getColor(Vec3 position, Color out) {
        Block block = getBlock(position, true);
        if (block == Blocks.salt) block = Blocks.darksand;
        out.set(block.mapColor).a(1f - block.albedo);
    }

    Block getBlock(Vec3 position, boolean visualOnly) {
        float height = rawHeight(position);
        
        // Crater Terrain
        if (withinCrater(position, 0f)) {
            float mask = CaeNoise.noise3d(seed, 5, 0.4f, 1f / 2.5f, position);
            if (mask > 0.5) return craterTerrain[2]; // Optimization: Early exit
            
            int index = Mathf.clamp((int) (height * craterTerrain.length), 0, craterTerrain.length - 1);
            return craterTerrain[index];
        }

        // Standard Terrain
        Vec3 pos = Tmp.v33.set(position).scl(noiseScale); // reuse Tmp.v33 safely as rawHeight is done
        height = Mathf.clamp(height * 1.2f);
        
        // Tar Logic
        Block res = terrain[Mathf.clamp((int) (height * terrain.length), 0, terrain.length - 1)];

        if(visualOnly) return res;
        float tarNoise = Simplex.noise3d(seed, 4, 0.55f, 1f / 2f, pos.x, pos.y + 999f, pos.z) * 0.3f 
                       + Tmp.v31.set(position).dst(0, 0, 1f) * 0.2f;
        

        if (tarNoise > 0.5f && tarBlocks.contains(res)) {
            return Blocks.shale;
        }

        //float falloff = 1f;
        return res;
    }

    @Override
    public void genTile(Vec3 position, TileGen tile) {
        tile.floor = getBlock(position, false);
        tile.block = tile.floor.asFloor().wall;

        if (Ridged.noise3d(seed + 1, position.x, position.y, position.z, 2, 22) > 0.31) {
            tile.block = Blocks.air;
        }
    }

    @Override
    protected float noise(float x, float y, double octaves, double falloff, double scl, double mag) {
        Vec3 v = sector.rect.project(x, y).scl(5f);
        return Simplex.noise3d(seed, octaves, falloff, 1f / scl, v.x, v.y, v.z) * (float) mag;
    }


    @Override
    protected void generate() {
        // 1. Setup Rooms & Layout
        cells(4);
        distort(10f, 12f);

        RoomHandler roomHandler = new RoomHandler(this);
        roomHandler.init();
        
        roomHandler.makeRooms(rand.random(5, 7));
        roomHandler.makeSpawn();

        cells(1);

        int tlen = tiles.width * tiles.height;
        int total = 0, waters = 0, grass = 0, rocks = 0;

        for (int i = 0; i < tlen; i++) {
            Tile tile = tiles.geti(i);
            if (tile.block() == Blocks.air) {
                total++;
                if (tile.floor().liquidDrop == Liquids.water) {
                    waters++;
                }
                if(tile.floor() == (Floor) CaeBlocks.lazurigrass){
                    grass++;
                }
                if(tile.floor() == (Floor) CaeBlocks.bluonixite){
                    rocks++;
                }
            }
        }

        roomHandler.naval = (float) waters / total >= 0.19f;

        roomHandler.ocean = (float) waters / total >= 0.65f;

        roomHandler.forest = (float) grass / total >= 0.19f;

        roomHandler.cave = (float) rocks / total >= 0.19f; 
        //render rooms
        for(Room room : roomHandler.roomseq){
          roomHandler.renderRoom(room);
        }

        
        roomHandler.connectEnemies(roomHandler.spawn);
        roomHandler.makeConnections();

        // connect to the rooms
        roomHandler.connectRooms(roomHandler.spawn);
        for (Room room : roomHandler.roomseq) {
            if(roomHandler.ocean && (float) rand.random(0, 1) > 0.5f) roomHandler.spawn.connectIslandsWater(room);
        }

        Room fspawn = roomHandler.spawn;

        
        distort(10f, 6f);

        // 4. Processing & Liquids
        cells(1);

        if (roomHandler.naval) {
            for (Room room : roomHandler.enemies) room.connectLiquid(roomHandler.spawn);
        }

        distort(10f, 6f);

        // Shoreline Smoothing
        smoothWater(3, false); // Normal shoreline
        if (roomHandler.naval) {
            smoothWater(2, true); // Deep water smoothing
        }

        // 5. Resources (Ores)
        generateOres(basicOres);
        trimDark();
        median(2);
        inverseFloodFill(tiles.getn(roomHandler.spawn.x, roomHandler.spawn.y));
        tech();

        if(roomHandler.forest){
          generateDensityForest(4, 3);
          generateRivers(roomHandler.spawn);
          //generateVegetation(roomHandler.roomseq, dec, genLakes);
        }
        if(roomHandler.cave){
            generateCaveDecorations(roomHandler.roomseq, dec, genLakes);
        }

        if(roomHandler.ocean){
            //plan to add island generation
        }

        generateRuins(roomHandler.spawn, new IntSeq(width * height / 4));
        
        Schematics.placeLaunchLoadout(roomHandler.spawn.x, roomHandler.spawn.y);
        for (Room espawn : roomHandler.enemies) tiles.getn(espawn.x, espawn.y).setOverlay(Blocks.spawn);

        setupRules(roomHandler.enemies, roomHandler.spawn, roomHandler.naval, basegen);
    }
}
