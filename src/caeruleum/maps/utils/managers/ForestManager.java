package caeruleum.maps.utils.managers;

import arc.math.Rand;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.noise.Simplex;
import mindustry.world.Tile;
import mindustry.world.Tiles;
import caeruleum.content.CaeBlocks;
import caeruleum.maps.utils.*;

public class ForestManager extends BasicManager {

    public ForestManager(CaeBasicGenerator gen, CaeMapUtilities utils) {
        super(gen, utils);
    }

    public void generateHybridForest(CaeChunkHandler handler, int blurIterations) {
        // 1. FILTER: Only retrieve chunks marked as FOREST
        Seq<CaeChunk> forestChunks = handler.getChunks(c -> c.biome == CaeChunk.Biome.FOREST);
        
        Log.info("[ForestManager] Total forest chunks found: @", forestChunks.size);
        if (forestChunks.isEmpty()) return;

        int cols = handler.chunks.length;
        int rows = handler.chunks[0].length;
        int baseChunkSize = handler.chunks[0][0].width;

        // 2. MACRO GRID
        float[][] chunkDensity = new float[cols][rows];
        float[][] write = new float[cols][rows];

        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                chunkDensity[x][y] = (handler.chunks[x][y].biome == CaeChunk.Biome.FOREST) ? 1.0f : 0.0f;
            }
        }

        // Blur the macro grid
        for (int iter = 0; iter < blurIterations; iter++) {
            for (int x = 0; x < cols; x++) {
                for (int y = 0; y < rows; y++) {
                    float sum = 0f;
                    int count = 0;
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            int nx = x + dx;
                            int ny = y + dy;
                            if (nx >= 0 && nx < cols && ny >= 0 && ny < rows) {
                                sum += chunkDensity[nx][ny];
                                count++;
                            }
                        }
                    }
                    write[x][y] = sum / count;
                }
            }
            for (int x = 0; x < cols; x++) {
                System.arraycopy(write[x], 0, chunkDensity[x], 0, rows);
            }
        }

        // 3. TILE GENERATION & DEBUG COUNTERS
        Tiles tiles = gen.getTiles();
        Rand rand = gen.getRand();

        int totalTilesScanned = 0;
        int validFloorTiles = 0;
        int highDensityTreesPlaced = 0;
        int lowDensityDecorPlaced = 0;

        for (CaeChunk chunk : forestChunks) {
            int cx = Math.min(chunk.x / baseChunkSize, cols - 1);
            int cy = Math.min(chunk.y / baseChunkSize, rows - 1);
            float macroDensity = chunkDensity[cx][cy];

            for (int x = chunk.x; x < chunk.x + chunk.width; x++) {
                for (int y = chunk.y; y < chunk.y + chunk.height; y++) {
                    totalTilesScanned++;
                    Tile tile = tiles.get(x, y);

                    // Debug check for tile filtering
                    if (tile == null || tile.floor() != CaeBlocks.lazurigrass || !tile.block().isAir()) {
                        continue;
                    }

                    validFloorTiles++;

                    // Normalize Simplex noise from [-1, 1] to [0, 1]
                    float rawNoise = Simplex.noise2d(gen.getSector().id, 3, 0.5f, 1f / 12f, x, y);
                    float detailNoise = (rawNoise + 1f) / 2f; 
                    float finalDensity = macroDensity * detailNoise;

                    if (finalDensity > 0.30f) {
                        if (x % 2 == 0 && y % 2 == 0 && rand.chance(0.85f)) {
                            tile.setBlock(CaeBlocks.blueTree);
                            highDensityTreesPlaced++;
                        }
                    } else if (finalDensity > 0.12f && rand.chance(0.20f)) {
                        float roll = rand.random(1f);
                        if (roll < 0.4f) tile.setBlock(CaeBlocks.blueTree);
                        else if (roll < 0.7f) tile.setBlock(CaeBlocks.blueFlower);
                        else tile.setBlock(CaeBlocks.blueTendrils);
                        
                        lowDensityDecorPlaced++;
                    }
                }
            }
        }

        // Summary Debug Log
        Log.info("[ForestManager] --- Generation Summary ---");
        Log.info("[ForestManager] Total Forest Chunk Tiles Scanned: @", totalTilesScanned);
        Log.info("[ForestManager] Matching Floor Tiles (lazurigrass + air): @", validFloorTiles);
        Log.info("[ForestManager] Dense Trees Placed (>0.30): @", highDensityTreesPlaced);
        Log.info("[ForestManager] Low Density Decor Placed (>0.12): @", lowDensityDecorPlaced);
        Log.info("[ForestManager] Total Foliage Placed: @", (highDensityTreesPlaced + lowDensityDecorPlaced));
    }
}