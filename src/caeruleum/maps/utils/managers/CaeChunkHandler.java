package caeruleum.maps.utils.managers;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.EnumMap;
import java.util.zip.DataFormatException;

import arc.func.Boolf;
import arc.func.Cons;
import arc.func.Intc2;
import arc.math.Mathf;
import arc.math.geom.Vec3;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import caeruleum.content.CaeBlocks;
import caeruleum.maps.utils.CaeBasicGenerator;
import caeruleum.maps.utils.CaeChunk;
import caeruleum.maps.utils.CaeMapUtilities;
import mindustry.content.Blocks;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.Tiles;
import mindustry.world.blocks.environment.Floor;

public class CaeChunkHandler {
    public static CaeChunkHandler current;
    public CaeChunk[][] chunks;
    private @Nullable CaeMapUtilities utils;
    private @Nullable CaeBasicGenerator gen;
    private final EnumMap<CaeChunk.Biome, Seq<CaeChunk>> chunksByType = new EnumMap<>(CaeChunk.Biome.class);
    public int mapWidth, mapHeight, chunkSize;
    // Initialize a Seq for every enum value so they are never null
    {
        for (CaeChunk.Biome type : CaeChunk.Biome.values()) {
            chunksByType.put(type, new Seq<>());
        }
    }

    public CaeChunkHandler(CaeBasicGenerator gen, CaeMapUtilities utils){
        this.gen = gen;
        this.utils = utils;
    }

    public void initChunks(int width, int height, int chunkSize) {
        if(gen == null) throw new IllegalStateException("initChunks needs a generator; this handler was loaded from a save");
        CaeChunkHandler.current = this;
        this.chunkSize = chunkSize;
        this.mapWidth = width;
        this.mapHeight = height;
        for(Seq<CaeChunk> s : chunksByType.values()) s.clear();
        // 1. Calculate grid size using ceiling division to ensure remaining space gets a chunk
        int chunkCols = (width + chunkSize - 1) / chunkSize;
        int chunkRows = (height + chunkSize - 1) / chunkSize;
        
        chunks = new CaeChunk[chunkCols][chunkRows];

        for(int x = 0; x < chunkCols; x++){
            for(int y = 0; y < chunkRows; y++){
                int startX = x * chunkSize;
                int startY = y * chunkSize;
                
                // 2. Clamp the chunk's width and height so edge chunks don't overshoot
                int actualChunkWidth = Math.min(chunkSize, width - startX);
                int actualChunkHeight = Math.min(chunkSize, height - startY);

                
                CaeChunk chunk = new CaeChunk(startX, startY, actualChunkWidth, actualChunkHeight, gen);
                CaeChunk.Biome biome = sampleChunkBiome(gen.getTiles(), chunk);
                chunk.biome = biome;
                chunksByType.get(biome).add(chunk);
                
                chunks[x][y] = chunk;
            }
        }

        this.updateOceanInfluence();
    }
    /**
     * Retrieves the CaeChunk containing the specified tile world coordinates (x, y).
     * Works seamlessly across standard chunks and partial boundary/corner chunks.
     * 
     * @param worldX Tile X coordinate on the map.
     * @param worldY Tile Y coordinate on the map.
     * @return The CaeChunk containing the tile, or null if out of map bounds.
     */
    public CaeChunk getChunkAt(int worldX, int worldY) {
        if (chunks == null || chunks.length == 0 || chunks[0].length == 0) return null;

        // 1. Boundary check against map dimensions
        if (worldX < 0 || worldX >= mapWidth || worldY < 0 || worldY >= mapHeight) {
            return null;
        }

        // 2. Direct O(1) floor division translation
        int cX = worldX / chunkSize;
        int cY = worldY / chunkSize;

        // 3. Safety clamp against array bounds (handles edge case where worldX == mapW)
        cX = Mathf.clamp(cX, 0, chunks.length - 1);
        cY = Mathf.clamp(cY, 0, chunks[0].length - 1);

        return chunks[cX][cY];
    }
    public void setBiome(CaeChunk chunk, CaeChunk.Biome biome){
        if(chunk.biome == biome) return;
        chunksByType.get(chunk.biome).remove(chunk);
        chunk.biome = biome;
        chunksByType.get(biome).add(chunk);
        updateOceanInfluence();
    }
    public void save(DataOutput out) throws IOException {
        for(CaeChunk[] collum : chunks){
            for(CaeChunk chunk : collum){
                out.writeByte(chunk.biome.ordinal());
            }
        }
    }
    public static CaeChunkHandler load(int mapW, int mapH, int chunkSize, DataInput in) throws IOException {
        CaeChunkHandler h = new CaeChunkHandler(null, null);
        h.mapWidth = mapW; h.mapHeight = mapH; h.chunkSize = chunkSize;
        int cols = (mapW + chunkSize - 1) / chunkSize;
        int rows = (mapH + chunkSize - 1) / chunkSize;
        h.chunks = new CaeChunk[cols][rows];
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                int sx = x * chunkSize, sy = y * chunkSize;
                CaeChunk c = new CaeChunk(sx, sy,
                    Math.min(chunkSize, mapW - sx), Math.min(chunkSize, mapH - sy), null);
                c.biome = CaeChunk.Biome.values()[in.readByte()];
                h.chunksByType.get(c.biome).add(c);
                h.chunks[x][y] = c;
            }
        }
        h.updateOceanInfluence();
        return h;
        }
    // Centralized mapping helper
public CaeChunk.Biome getBlockBiome(Block floor) {
    if (floor == null) return CaeChunk.Biome.PLAINS;
    if (floor.asFloor().isLiquid || floor == Blocks.water || floor == CaeBlocks.aquafluent || 
        floor == CaeBlocks.deepAquafluent || floor == CaeBlocks.bluonixiteWater) {
        return CaeChunk.Biome.OCEAN;
    }
    if (floor == CaeBlocks.lazurigrass) {
        return CaeChunk.Biome.FOREST;
    }
    if (floor == Blocks.stone || floor == CaeBlocks.bluonixite || 
        floor == Blocks.iceSnow || floor == Blocks.ice) {
        return CaeChunk.Biome.MOUNTAIN;
    }
    // Add new biomes here in ONE place!
    // if (craterTerrainBlocks.contains(floor)) return CaeChunk.Biome.CRATER;

    return CaeChunk.Biome.PLAINS;
}

private CaeChunk.Biome sampleChunkBiome(Tiles tiles, CaeChunk chunk) {
    
    if(gen == null) throw new IllegalStateException("sampleChunkBiome needs a generator; this handler was loaded from a save");
    // 1. CRATER CHECK: Check if chunk center falls within a crater region
    Vec3 midPos = gen.getSector().rect.project(chunk.getMidX(), chunk.getMidY());
    if (gen.withinCrater(midPos, 0f)) {
        return CaeChunk.Biome.CRATER; // Add CRATER to CaeChunk.Biome enum
    }

    // 2. Extensible 5-Point Vote for Standard Floor Blocks
    int insetX = Math.max(2, chunk.width / 4);
    int insetY = Math.max(2, chunk.height / 4);

    int[][] samplePoints = {
        {chunk.getMidX(), chunk.getMidY()},
        {chunk.x + insetX, chunk.y + insetY},
        {chunk.x + chunk.width - insetX, chunk.y + insetY},
        {chunk.x + insetX, chunk.y + chunk.height - insetY},
        {chunk.x + chunk.width - insetX, chunk.y + chunk.height - insetY}
    };

    // Use an array to count votes per Biome enum index
    int[] votes = new int[CaeChunk.Biome.values().length];

    for (int[] pt : samplePoints) {
        Tile tile = tiles.get(pt[0], pt[1]);
        Block floor = (tile != null) ? tile.floor() : null;
        
        CaeChunk.Biome sampledBiome = getBlockBiome(floor);
        votes[sampledBiome.ordinal()]++;
    }

    // 3. Evaluate Majority Winner
    int maxVotes = -1;
    CaeChunk.Biome dominantBiome = CaeChunk.Biome.PLAINS;

    for (CaeChunk.Biome b : CaeChunk.Biome.values()) {
        if (votes[b.ordinal()] > maxVotes) {
            maxVotes = votes[b.ordinal()];
            dominantBiome = b;
        }
    }

    return dominantBiome;
}

    public void updateOceanInfluence() {
        int cols = chunks.length;
        int rows = chunks[0].length;

        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                if (chunks[x][y].biome == CaeChunk.Biome.OCEAN) {
                    chunks[x][y].oceanInfluence = 1f;
                    continue;
                }

                int oceanNeighbors = 0;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx >= 0 && nx < cols && ny >= 0 && ny < rows) {
                            if (chunks[nx][ny].biome == CaeChunk.Biome.OCEAN) oceanNeighbors++;
                        }
                    }
                }
                // 8 neighbors -> 0.0 to 1.0 influence
                chunks[x][y].oceanInfluence = oceanNeighbors / 8f; 
            }
        }
    }

    public boolean has(CaeChunk.Biome biome) {
        Seq<CaeChunk> chunks = chunksByType.get(biome);
        return chunks != null && !chunks.isEmpty();
    }


    public Seq<CaeChunk> getChunks(CaeChunk.Biome biome){
        return chunksByType.get(biome);
    }

    public Seq<CaeChunk> getChunks(CaeChunk.Biome... biomes) {
        Seq<CaeChunk> result = new Seq<>();
        for (CaeChunk.Biome biome : biomes) {
            Seq<CaeChunk> bucket = chunksByType.get(biome);
            if (bucket != null) {
                result.addAll(bucket);
            }
        }
        return result;
    }

    /** Directly iterates over filtered chunks without building an intermediary list */
    public void each(Boolf<CaeChunk> filter, Cons<CaeChunk> action) {
        for (int x = 0; x < chunks.length; x++) {
            for (int y = 0; y < chunks[0].length; y++) {
                CaeChunk chunk = chunks[x][y];
                if (filter.get(chunk)) {
                    action.get(chunk);
                }
            }
        }
    }
}