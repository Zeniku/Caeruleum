package caeruleum.maps.utils.managers;

import java.util.Arrays;

import arc.math.Rand;
import arc.math.geom.Vec3;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.noise.Simplex;
import caeruleum.content.CaeBlocks;
import caeruleum.maps.utils.CaeBasicGenerator;
import caeruleum.maps.utils.CaeChunk;
import caeruleum.maps.utils.CaeMapUtilities;
import mindustry.world.Tile;
import mindustry.world.Tiles;
import arc.struct.IntQueue;


public class ForestManager{
    private CaeMapUtilities utils;
    private CaeBasicGenerator gen;

    public ForestManager(CaeBasicGenerator gen, CaeMapUtilities utils) {
        this.gen = gen;
        this.utils = utils;
    }

    public void generateHybridForest(CaeChunkHandler handler) {
        Seq<CaeChunk> forestChunks = handler.getChunks(c -> c.biome == CaeChunk.Biome.FOREST);
        if (forestChunks.isEmpty()) return;

        Tiles tiles = gen.getTiles();
        int width = tiles.width;
        int height = tiles.height;
        
        // 1. Setup Distance Field
        int[][] dist = new int[width][height];
        for (int x = 0; x < width; x++) {
            Arrays.fill(dist[x], 999); // Fill with a high number initially
        }

        IntQueue queue = new IntQueue();

        // 2. Seed the walls ONLY inside the forest chunks
        for (CaeChunk chunk : forestChunks) {
            for (int x = chunk.x; x < chunk.x + chunk.width; x++) {
                for (int y = chunk.y; y < chunk.y + chunk.height; y++) {
                    Tile tile = tiles.get(x, y);
                    // If it's a solid block, it's the "core" of the forest density
                    if (tile != null && tile.solid()) {
                        dist[x][y] = 0;
                        queue.addLast(x + y * width); // Pack x/y into a single int for speed
                    }
                }
            }
        }

        // 3. Fast Distance Transform (BFS)
        int maxFadeDistance = 10; // How many blocks outward the forest extends from walls
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        while (!queue.isEmpty()) {
            int pos = queue.removeFirst();
            int cx = pos % width;
            int cy = pos / width;
            int currentDist = dist[cx][cy];

            if (currentDist >= maxFadeDistance) continue; // Stop spreading once we reach the edge of the forest

            for (int i = 0; i < 4; i++) {
                int nx = cx + dx[i];
                int ny = cy + dy[i];

                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    if (dist[nx][ny] > currentDist + 1) {
                        dist[nx][ny] = currentDist + 1;
                        queue.addLast(nx + ny * width);
                    }
                }
            }
        }

        // 4. Tile Generation based on Wall Distance
        Rand rand = gen.getRand();

        for (CaeChunk chunk : forestChunks) {
            for (int x = chunk.x; x < chunk.x + chunk.width; x++) {
                for (int y = chunk.y; y < chunk.y + chunk.height; y++) {

                    Tile tile = tiles.get(x, y);

                    if (tile == null || tile.floor() != CaeBlocks.lazurigrass || !tile.block().isAir()) {
                        continue;
                    }

                    int distanceToWall = dist[x][y];
                    // Convert distance to wall density: 1.0 directly against walls, 0.0 at maxFadeDistance (e.g. 14 blocks away)
                    float wallDensity = (distanceToWall < maxFadeDistance) 
                        ? (1f - ((float) distanceToWall / maxFadeDistance)) 
                        : 0f;

                    Vec3 projectedPos = gen.getSector().rect.project(x, y);
                    // Large-scale Simplex noise for middle-of-nowhere forest groves (large scale = 60f to 80f)
                    float groveNoise = (Simplex.noise3d(gen.getSector().id, 2, 0.5f, 70f, projectedPos.x, projectedPos.y, projectedPos.z) + 1f) / 2f;

                    // Threshold the grove noise so only the highest peaks (>0.65) form standalone groves in open space
                    float groveDensity = (groveNoise > 0.65f) ? ((groveNoise - 0.65f) / 0.35f) : 0f;

                    // Combine both: A tile gets high density if it's NEAR A WALL OR inside an OPEN GROVE
                    float combinedDensity = Math.max(wallDensity, groveDensity);

                    // Skip tiles that have no density from either source
                    if (combinedDensity <= 0.05f) continue;

                    // Local detail noise (20f scale) for natural edges and breaks
                    float detailNoise = (Simplex.noise3d(gen.getSector().id + 10, 3, 0.5f, 20f, projectedPos.x, projectedPos.y, projectedPos.z) + 1f) / 2f;

                    float threshold = 1f - combinedDensity;

                    // Core trees (dense center of groves + dense wall hugs)
                    if (detailNoise > threshold) {
                        if (x % 2 == 0 && y % 2 == 0) { // Keep walkable
                            tile.setBlock(CaeBlocks.blueTree);
                        }
                    } 
                    // Scattered flora on outer boundaries
                    else if (detailNoise > threshold - 0.25f && rand.chance(0.20f)) {
                        float roll = rand.random(1f);
                        if (roll < 0.3f) tile.setBlock(CaeBlocks.blueTree);
                        else if (roll < 0.6f) tile.setBlock(CaeBlocks.blueFlower);
                        else tile.setBlock(CaeBlocks.blueTendrils);
                    }
                }
            }
        }
    }
}