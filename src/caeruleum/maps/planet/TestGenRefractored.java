package caeruleum.maps.planet;

import arc.graphics.Color;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.noise.*;
import caeruleum.content.CaeBlocks;
import caeruleum.maps.utils.CaeBasicGenerator;
import caeruleum.maps.utils.CaeMapUtilities;
import caeruleum.maps.utils.CaeRoom;
import caeruleum.maps.utils.CaeChunk;
import caeruleum.maps.utils.managers.CaeChunkHandler;
import caeruleum.maps.utils.managers.RoomHandler;
import caeruleum.maps.utils.managers.ForestManager;
import caeruleum.utils.math.CaeMath;
import caeruleum.utils.noise.CaeNoise;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.maps.generators.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.Floor;


public class TestGenRefractored extends CaeBasicGenerator {

    // Configuration
    public float oceanFloorDepth = 1.3f, oceanFloorSmoothing = 1.3f, oceanDepthMultiplier = 0.5f;
    public boolean altPathing = false; // Renamed from static 'alt' for thread/save safety
    public int seed = 0;
    private Vec3 tempVec3 = new Vec3();
    
    // Terrain Settings
    private final float noiseScale = 1f;
    private final float waterOffset = 0.3f;
    private final boolean genLakes = false;

    // Craters
    private final Vec3 crater = new Vec3(0, 0f, 1f);
    private final float craterRadius = 0.39f;
    private final float craterDepth = 1f;

    // Generator State
    private final BaseGenerator basegen = new BaseGenerator();
    private CaeMapUtilities mapUtils = new CaeMapUtilities(this);
    private RoomHandler roomHandler = new RoomHandler(this, mapUtils);
    private CaeChunkHandler chunkHandler = new CaeChunkHandler(this, mapUtils);
    private ForestManager forestManager = new ForestManager(this, mapUtils);
    
    // Blocks
    private final Block[][] terrain = {
        // Deep Ocean
        {CaeBlocks.deepAquafluent},
        // Shallow Ocean
        {CaeBlocks.deepAquafluent, CaeBlocks.deepAquafluent, CaeBlocks.aquafluent},
        // Shore
        {CaeBlocks.aquafluent, CaeBlocks.aquafluent, CaeBlocks.aquafluent, CaeBlocks.aquafluent, CaeBlocks.aquafluent, CaeBlocks.bluonixiteWater},
        {CaeBlocks.bluonixite, CaeBlocks.lazurigrass},
        {CaeBlocks.lazurigrass, Blocks.iceSnow},
        // Peaks
        {Blocks.iceSnow, Blocks.snow, Blocks.ice}
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
    @Override 
    public boolean withinCrater(Vec3 position, float buffer) {
        return position.within(crater, craterRadius + buffer);
    }
    
    @Override 
    public float rawHeight(Vec3 position) {
        // Use Tmp.v33 to avoid allocation, apply scale
        Vec3 pos = Tmp.v33.set(position).add(0,10f,0).scl(noiseScale);

        // cant be bothered to change it any longer
        float continentalShape = CaeNoise.noise3d(seed, 4, 0.4f, 0.8f, pos);
        float mountainShape = CaeNoise.ridgeNoise3d(seed, 3, 0.4f, 4f, 0f, 1.5f, 0.5f, pos);
        float mountainMask = Mathf.clamp(CaeMath.smoothMax(0, continentalShape * continentalShape, 0.5f) * 4);
        float finalHeight = ((mountainMask * mountainShape) + continentalShape) / 2f;
        // Crater logic
        float dist = position.dst(crater);
        if (dist < craterRadius) {
            float n = CaeNoise.noise3d(0, 8.4d, 0.4d, 0.27d, position) * (craterRadius / 4f);
            float x = Interp.pow2Out.apply(1f - (dist / craterRadius));
            float craterShape = craterDepth * x + (1f - x) * n;
            return finalHeight - craterShape;
        }

        return finalHeight;
    }

    @Override
    public float getHeight(Vec3 position) {
        float height = rawHeight(position);//CaeMath.smoothMax(0, rawHeight(position), 0.5f);
        if (withinCrater(position, 0f)) {
           return height - 0.5f; // Crater depth adjustment
        }
        return CaeMath.smoothMax(0, height - 0.5f, 0.5f);
        //why subtract 0.5? because the height on the planet intersect the sector selected, so we need to lower the height to make it more realistic
    }

    @Override
    public void getColor(Vec3 position, Color out) {
        Block block = getBlock(position, true);
        if (block == Blocks.salt) block = Blocks.darksand;
        out.set(block.mapColor).a(1f - block.albedo);
    }

    @Override 
    public Block getBlock(Vec3 position, boolean visualOnly) {
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
        
        // Height Logic
        Block[][] localTerrain = terrain;
        float scaledHeight = (height) * localTerrain.length;
        int index = Mathf.clamp((int) (scaledHeight), 0, localTerrain.length - 1);
        int subIndex = Mathf.clamp((int) ((scaledHeight - index) * localTerrain[index].length), 0, localTerrain[index].length - 1);
        Block res = localTerrain[index][subIndex];


        if(visualOnly) return res;
        float tarNoise = Simplex.noise3d(seed, 4, 0.55f, 1f / 2f, pos.x, pos.y + 999f, pos.z) * 0.3f 
                       + Tmp.v31.set(position).dst(0, 0, 1f) * 0.2f;
        

        if (tarNoise > 0.5f && tarBlocks.contains(res)) {
            return CaeBlocks.lazurigrass;
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
        
        // I HATE the rand not being different everytime  
        if (sector != null) {
            rand.setSeed(sector.id);
        } else {
            rand.setSeed(seed);
        }
        // 1. Setup Rooms & Layout
        cells(4);
        distort(10f, 12f);

        chunkHandler.initChunks(width, height, 32);
        roomHandler.init();

        roomHandler.makeRooms(rand.random(5, 7), chunkHandler);
        roomHandler.makeSpawn();

        cells(1);


        int oceanChunksCount = chunkHandler.getChunks(c -> c.biome == CaeChunk.Biome.OCEAN).size;
        int landChunksCount = chunkHandler.getChunks(c -> (c.biome == CaeChunk.Biome.PLAINS || c.biome == CaeChunk.Biome.MOUNTAIN || c.biome == CaeChunk.Biome.FOREST)).size;
        if (landChunksCount == 0) landChunksCount = 1; // Avoid division by zero
        if (oceanChunksCount == 0) oceanChunksCount = 1; // Avoid division by zero
        roomHandler.naval = ((float)(oceanChunksCount / landChunksCount) >= 0.19f);
        roomHandler.ocean = chunkHandler.has(CaeChunk.Biome.OCEAN);
        roomHandler.cave = chunkHandler.has(CaeChunk.Biome.MOUNTAIN); 
        

        for(CaeRoom room : roomHandler.roomseq){
          roomHandler.renderRoom(room, chunkHandler);
        }

        roomHandler.connectEnemies(roomHandler.spawn);

        roomHandler.makeConnections();

        // connect to the rooms
        roomHandler.connectRooms(roomHandler.spawn);

        for (CaeRoom room : roomHandler.roomseq) {
            if(roomHandler.ocean && rand.chance(0.5)) roomHandler.spawn.connectIslandsWater(room);
        }

        distort(10f, 6f);

        // 4. Processing & Liquids
        cells(1);

        if (roomHandler.naval) {
            for (CaeRoom room : roomHandler.enemies) room.connectLiquid(roomHandler.spawn);
        }

        distort(10f, 6f);

        // Shoreline Smoothing
        mapUtils.smoothWater(3, false); // Normal shoreline
        if (roomHandler.naval) {
            mapUtils.smoothWater(2, true); // Deep water smoothing
        }

        // 5. Resources (Ores)
        mapUtils.generateOres(basicOres);
        trimDark();
        median(2);
        inverseFloodFill(tiles.getn(roomHandler.spawn.x, roomHandler.spawn.y));
        tech();

        if(chunkHandler.has(CaeChunk.Biome.FOREST)){
            mapUtils.generateRivers(roomHandler.spawn);
            forestManager.generateHybridForest(chunkHandler);
        }
        
        if(roomHandler.cave){
            mapUtils.generateCaveDecorations(roomHandler.roomseq, dec, genLakes);
        }

        if(roomHandler.ocean){
            //plan to add island generation
        }

        mapUtils.generateRuins(roomHandler.spawn, new IntSeq(width * height / 4));
        
        Schematics.placeLaunchLoadout(roomHandler.spawn.x, roomHandler.spawn.y);
        for (CaeRoom espawn : roomHandler.enemies) tiles.getn(espawn.x, espawn.y).setOverlay(Blocks.spawn);

        mapUtils.setupRules(roomHandler.enemies, roomHandler.spawn, roomHandler.naval, basegen);
    }
}
