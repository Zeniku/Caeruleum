package caeruleum.maps.utils.managers;

import arc.func.Boolf;
import arc.func.Cons;
import arc.math.Mathf;
import arc.math.geom.Vec3;
import arc.struct.Seq;
import arc.util.Log;
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
    private CaeMapUtilities utils;
    private CaeBasicGenerator gen;

    public CaeChunkHandler(CaeBasicGenerator gen, CaeMapUtilities utils){
        this.gen = gen;
        this.utils = utils;
    }

    public void initChunks(int width, int height, int chunkSize) {
        CaeChunkHandler.current = this;
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

                
                CaeChunk chunk = new CaeChunk(startX, startY, actualChunkWidth, actualChunkHeight);
                chunk.biome = sampleChunkBiome(gen.getTiles(), chunk);
                
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
    public CaeChunk getChunkAt(int worldX, int worldY, int chunkSize) {
        if (chunks == null || chunks.length == 0 || chunks[0].length == 0) return null;

        int mapW = gen.getMapWidth();
        int mapH = gen.getMapHeight();

        // 1. Boundary check against map dimensions
        if (worldX < 0 || worldX >= mapW || worldY < 0 || worldY >= mapH) {
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
        for (int x = 0; x < chunks.length; x++) {
            for (int y = 0; y < chunks[0].length; y++) {
                if (chunks[x][y] != null && chunks[x][y].biome == biome) {
                    return true; // Return early as soon as one match is found
                }
            }
        }
        return false;
    }

    public Seq<CaeChunk> getChunks(Boolf<CaeChunk> predicate) {
        Seq<CaeChunk> result = new Seq<>();
        for (int x = 0; x < chunks.length; x++) {
            for (int y = 0; y < chunks[0].length; y++) {
                if (predicate.get(chunks[x][y])) {
                    result.add(chunks[x][y]);
                }
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