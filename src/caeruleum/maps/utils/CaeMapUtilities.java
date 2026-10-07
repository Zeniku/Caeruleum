package caeruleum.maps.utils;

import arc.func.Boolf;
import arc.func.Intc2;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.math.geom.Vec3;
import arc.struct.FloatSeq;
import arc.struct.GridBits;
import arc.struct.IntSeq;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Structs;
import arc.util.noise.Ridged;
import arc.util.noise.Simplex;
import caeruleum.content.CaeBlocks;
import caeruleum.maps.utils.managers.CaeChunkHandler;
import mindustry.ai.BaseRegistry;
import mindustry.content.Blocks;
import mindustry.content.Liquids;
import mindustry.game.Team;
import mindustry.game.Waves;
import mindustry.maps.generators.BaseGenerator;
import mindustry.type.Sector;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.Tiles;
import mindustry.world.blocks.environment.Floor;

import static mindustry.Vars.*;

public class CaeMapUtilities {
    private CaeBasicGenerator gen;
    private Floor floor = null;
    private Block block = null;
    private Block ore = null;

    public CaeMapUtilities(CaeBasicGenerator gen) {
        this.gen = gen;
    }
    public void pass(Intc2 r){
        for (Tile tile : gen.getTiles()) {
            floor = tile.floor();
            block = tile.block();
            ore = tile.overlay();
            r.get(tile.x, tile.y);

            tile.setFloor(floor.asFloor());
            if(block != tile.block()) tile.setBlock(block);
            tile.setOverlay(ore);
        }
    }
    public void makeOrganicIsland(int cx, int cy, int baseRadius, Floor inner, Floor outer) {
        int padding = 15; // Extra space for the noise to push outward
        int maxRadius = baseRadius + padding;
        Tiles tiles = gen.getTiles();
        scanCircle(cx, cy, maxRadius, (wx, wy, dst2) -> {
            Tile tile = tiles.getn(wx, wy);
            if (!tile.floor().isLiquid) return; // Don't overwrite existing land

            // 1. Calculate linear distance from center (0.0 at center, 1.0 at edge of baseRadius)
            float dist = Mathf.dst(cx, cy, wx, wy) / baseRadius;
            
            // 2. Sample low-frequency noise to warp the coastline
            // The noise returns roughly -1.0 to 1.0
            float edgeNoise = gen.getNoisef(wx, wy, 3f, 0.5f, 20f) * 0.45f; 
            
            // 3. Combine distance and noise
            float finalDist = dist + edgeNoise;

            if (finalDist < 0.8f) {
                tile.setFloor(inner); // Solid land
            } else if (finalDist < 1.1f) {
                tile.setFloor(outer); // Shoreline/Shallows
            }
        });
    }

    public void makeIsland(int x, int y, int radius, int miniAmount, int distFromBig) {
        // Fix: Use sin for Y, fix loop condition
        Rand rand = gen.getRand();
        float slice = 360f / miniAmount;
        for (int i = 0; i < miniAmount; i++) {
            int miniRadius = (radius / miniAmount);
            // Add variance to distance
            int miniDist = (radius + distFromBig) + rand.random(miniRadius); 
            
            float angle = i * slice;
            int posx = x + (int) (miniDist * Mathf.cosDeg(angle));
            int posy = y + (int) (miniDist * Mathf.sinDeg(angle));
            
            makeIsland(posx, posy, miniRadius);
        }
        makeIsland(x, y, radius);
    }

    public void makeIsland(int x, int y, int radius) {
        makeIsland(x, y, radius, (Floor) CaeBlocks.bluonixite, (Floor) CaeBlocks.bluonixiteWater);
    }

    public void makeIsland(int x, int y, int radius, Floor inner, @Nullable Floor outer) {
        makeIsland(x, y, radius, 8, inner, outer);
    }

    public void makeIsland(int x, int y, int radius, int outerOffset, Floor inner, @Nullable Floor outer) {
        int r2 = radius * radius;
        int r2Outer = (radius + outerOffset) * (radius + outerOffset);
        Tiles tiles = gen.getTiles();
        // Optimized circle iteration
        scanCircle(x, y, radius + outerOffset, (wx, wy, dst2) -> {
            Tile other = tiles.getn(wx, wy);
            if (other.floor().isLiquid) {
                if (outer != null && dst2 <= r2Outer && dst2 > r2) {
                    other.setFloor(outer);
                } else if (dst2 <= r2) {
                    other.setFloor(inner);
                }
            }
        });
    }

    /** Optimized circle scanner that avoids checking bounds every pixel */
    public void scanCircle(int x, int y, int radius, CircleCons cons) {
        int width = gen.getMapWidth();
        int height = gen.getMapHeight();
        for (int cx = -radius; cx <= radius; cx++) {
            for (int cy = -radius; cy <= radius; cy++) {
                int dst2 = cx * cx + cy * cy;
                if (dst2 <= radius * radius) {
                    int wx = x + cx, wy = y + cy;
                    if (Structs.inBounds(wx, wy, width, height)) {
                        cons.get(wx, wy, dst2);
                    }
                }
            }
        }
    }

    public interface CircleCons {
        void get(int wx, int wy, int dst2);
    }

    // --- Shoreline & Detection ---

    public void refineWater(int radius, boolean deepMode, Floor check, Floor replace, Floor retain) {
        int r2 = radius * radius;
        Tiles tiles = gen.getTiles();
        pass((x, y) -> {
            boolean isTarget = deepMode 
                ? (floor.asFloor().isLiquid && !floor.asFloor().isDeep() && !floor.asFloor().shallow)
                : (floor.asFloor().isLiquid && floor.asFloor().shallow);

            if (isTarget) {
                // Check neighbors
                for (int cx = -radius; cx <= radius; cx++) {
                    for (int cy = -radius; cy <= radius; cy++) {
                        if (cx * cx + cy * cy <= r2) {
                            Tile tile = tiles.get(x + cx, y + cy);
                            
                            boolean failCondition = deepMode
                                ? (tile != null && (tile.floor().shallow || !tile.floor().isLiquid)) // Found shallow
                                : (tile != null && (!tile.floor().isLiquid || tile.block() != Blocks.air)); // Found solid

                            if (failCondition) return;
                        }
                    }
                }
                floor = (floor == check) ? replace : retain;
            }
        });
    }

    public int countTiles(int x, int y, int radius, Boolf<Tile> predicate) {
        int count = 0;
        int r2 = radius * radius;
        Tiles tiles = gen.getTiles();
        for (int cx = -radius; cx <= radius; cx++) {
            for (int cy = -radius; cy <= radius; cy++) {
                if (cx * cx + cy * cy <= r2) {
                    Tile tile = tiles.get(x + cx, y + cy);
                    if (tile != null && predicate.get(tile)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public int countWater(int cx, int cy, int range) {
        int waterTiles = 0;
        Tiles tiles = gen.getTiles();
        for (int rx = -range; rx <= range; rx++) {
            for (int ry = -range; ry <= range; ry++) {
                Tile tile = tiles.get(cx + rx, cy + ry);
                if (tile == null || tile.floor().liquidDrop != null) {
                    waterTiles++;
                }
            }
        }
        return waterTiles;
    }

    protected boolean checkNavalStatus() {
        Tiles tiles = gen.getTiles();
        int tlen = tiles.width * tiles.height;
        int waters = 0;
        int totalAir = 0;
        for (int i = 0; i < tlen; i++) {
            Tile tile = tiles.geti(i);
            if (tile.block() == Blocks.air) {
                totalAir++;
                if (tile.floor().liquidDrop == Liquids.water) waters++;
            }
        }
        return (float) waters / totalAir >= 0.19f;
    }
public void generateRivers(CaeRoom spawn) {
    Sector sector = gen.getSector();

    pass((x, y) -> {
        // Skip solid terrain walls
        if (block.solid) return;

        // 1. Noise calculations for river mask
        Vec3 projectedPos = sector.rect.project(x, y);
        float noiseRipple = Simplex.noise2d(sector.id, 2, 0.6f, 1f / 7f, x, y) * 0.1f;
        float riverNoise = Ridged.noise3d(2, projectedPos.x, projectedPos.y, projectedPos.z, 1, 1f / 55f) + noiseRipple;

        // Dynamic spawn clearance padding
        float spawnProtectionRadius = 12f + (noiseRipple * 44f - 2f);

        // 2. Validate river placement and spawn protection
        boolean isRiverPath = riverNoise > 0.17f;
        boolean isNearSpawn = Mathf.within(x, y, spawn.x, spawn.y, spawnProtectionRadius);

        if (!isRiverPath || isNearSpawn) return;

        // 3. Determine depth and water block type
        boolean isDeepWater = riverNoise > 0.27f && !Mathf.within(x, y, spawn.x, spawn.y, spawnProtectionRadius + 3f);
        
        floor = getRiverFloor(floor, isDeepWater);
    });
}

/**
 * Determines the river floor replacement block based on the existing terrain floor.
 */
private Floor getRiverFloor(Block currentFloor, boolean isDeep) {
    // Skip liquids and frozen biomes
    if (currentFloor.asFloor().isLiquid || 
        currentFloor == Blocks.ice || 
        currentFloor == Blocks.iceSnow || 
        currentFloor == Blocks.snow) {
        return currentFloor.asFloor();
    }

    // Deep water override
    if (isDeep) {
        return (Floor) CaeBlocks.deepAquafluent;
    }

    // Shallow water variants based on underlying sand/salt terrain
    if (currentFloor == Blocks.sand || currentFloor == Blocks.salt) {
        return (Floor) CaeBlocks.bluonixiteWater;
    }

    return (Floor) CaeBlocks.bluonixiteWater; // Default shallow water
}

    public void smoothWater(int deepRadius, boolean navalMode) {
        int radiusSq = deepRadius * deepRadius;

        pass((x, y) -> {
            // 1. Only process tiles eligible for upgrading
            if (!shouldProcessWater(floor.asFloor(), navalMode)) return;

            // 2. Check surrounding neighborhood for land or shallow water boundaries
            boolean connectsToBoundary = hasNearbyBoundary(x, y, deepRadius, radiusSq, navalMode);

            // 3. Upgrade water depth if far enough away from land/shallow borders
            if (!connectsToBoundary) {
                floor = getUpgradedWaterFloor(floor, navalMode);
            }
        });
    }

    /**
     * Determines whether a tile is a candidate for water smoothing/deepening.
     */
    private boolean shouldProcessWater(Floor floor, boolean navalMode) {
        if (!floor.isLiquid) return false;

        if (navalMode) {
            // Mid-depth liquid (not shallow, not already deep)
            return !floor.isDeep() && !floor.shallow;
        } else {
            // Shallow shoreline liquid
            return floor.shallow;
        }
    }

    /**
     * Checks if any tile within deepRadius violates the depth condition.
     */
    private boolean hasNearbyBoundary(int x, int y, int radius, int radiusSq, boolean navalMode) {
        for (int cx = -radius; cx <= radius; cx++) {
            for (int cy = -radius; cy <= radius; cy++) {
                // Circle distance check
                if (cx * cx + cy * cy > radiusSq) continue;

                Tile tile = gen.getTiles().get(x + cx, y + cy);
                if (tile == null) continue;

                Floor tileFloor = tile.floor();

                if (navalMode) {
                    // Border if touches shallow water or solid land
                    if (tileFloor.shallow || !tileFloor.isLiquid) return true;
                } else {
                    // Border if touches dry land or solid block structures
                    if (!tileFloor.isLiquid || tile.block() != Blocks.air) return true;
                }
            }
        }
        return false;
    }

    /**
     * Maps existing shallow/mid water floors to their upgraded deeper counterparts.
     * Modify or add custom floor conversions here easily!
     */
    private Floor getUpgradedWaterFloor(Block currentFloor, boolean navalMode) {
        if (navalMode) {
            // Upgrade to deep water variants
            if (currentFloor == CaeBlocks.aquafluent) return (Floor) CaeBlocks.deepAquafluent;
            
            return (Floor) CaeBlocks.bluonixiteWater; // Default deep ocean floor
        } else {
            // Upgrade shallow water to standard liquid floors
            if (currentFloor == Blocks.darksandWater) return (Floor) CaeBlocks.bluonixiteWater;
            
            return (Floor) CaeBlocks.aquafluent; // Default standard water floor
        }
    }

    public void generateOres(Seq<Block> basicOres) {
        Sector sector = gen.getSector();
        Rand rand = gen.getRand();
        Seq<Block> ores = new Seq<>(basicOres);
        float poles = Math.abs(sector.tile.v.y);
        float scl = 1f;

        // Dynamic Ore Addition
        if (Simplex.noise3d(gen.seed, 2, 0.5, scl, sector.tile.v.x, sector.tile.v.y, sector.tile.v.z) * 0.5f + poles > 0.325f)
            ores.add(Blocks.oreCoal);
        if (Simplex.noise3d(gen.seed, 2, 0.5, scl, sector.tile.v.x + 1, sector.tile.v.y, sector.tile.v.z) * 0.5f + poles > 0.65f)
            ores.add(Blocks.oreTitanium);
        if (Simplex.noise3d(gen.seed, 2, 0.5, scl, sector.tile.v.x + 2, sector.tile.v.y, sector.tile.v.z) * 0.5f + poles > 0.91f)
            ores.add(Blocks.oreThorium);
        if (rand.chance(0.25)) 
            ores.add(Blocks.oreScrap);

        FloatSeq frequencies = new FloatSeq();
        for (int i = 0; i < ores.size; i++) {
            frequencies.add(rand.random(-0.1f, 0.01f) - i * 0.01f + poles * 0.04f);
        }

        pass((x, y) -> {
            if (!floor.asFloor().hasSurface()) return;

            int offsetX = x - 4, offsetY = y + 23;
            for (int i = ores.size - 1; i >= 0; i--) {
                Block entry = ores.get(i);
                float freq = frequencies.get(i);
                if (Math.abs(0.5f - gen.getNoisef(offsetX, offsetY + i * 999, 2, 0.7, (40 + i * 2))) > 0.22f + i * 0.01 &&
                    Math.abs(0.5f - gen.getNoisef(offsetX, offsetY - i * 999, 1, 1, (30 + i * 4))) > 0.37f + freq) {
                    ore = entry;
                    break;
                }
            }

            if (ore == Blocks.oreScrap && rand.chance(0.33)) {
                floor = (Floor) Blocks.metalFloorDamaged;
            }
        });
    }


    public void generateCaveDecorations(Seq<CaeRoom> rooms, ObjectMap<Block, Block> dec ,boolean genLakes) {
        Rand rand = gen.getRand();
        Tiles tiles = gen.getTiles();
        Sector sector = gen.getSector();

        pass((x, y) -> {
            //Tar
            if (floor == Blocks.darksand) {
                if (Math.abs(0.5f - gen.getNoisef(x - 40, y, 2, 0.7, 80)) > 0.25f &&
                    Math.abs(0.5f - gen.getNoisef(x, y + sector.id * 10, 1, 1, 60)) > 0.41f && 
                    !rooms.contains(r -> Mathf.within(x, y, r.x, r.y, 30))) {
                    floor = (Floor) Blocks.tar;
                }
            }
            // Hotrock & Lakes
           if (floor == Blocks.hotrock) {
                    if (Math.abs(0.5f - gen.getNoisef(x - 90, y, 4, 0.8, 80)) > 0.035) {
                        floor = (Floor) Blocks.basalt;
                    } else {
                        ore = Blocks.air;
                        boolean all = true;
                        for (Point2 p : Geometry.d4) {
                            Tile other = tiles.get(x + p.x, y + p.y);
                            if (other == null || (other.floor() != Blocks.hotrock && other.floor() != Blocks.magmarock)) {
                                all = false;
                            }
                        }
                        if (all) {
                            floor = (Floor) Blocks.magmarock;
                        }
                    }
                } else if (genLakes && floor != Blocks.basalt && floor != Blocks.ice && floor.asFloor().hasSurface()) {
                    float noise = gen.getNoisef(x + 782, y, 5, 0.75f, 260f, 1f);
                    if (noise > 0.67f && !rooms.contains(e -> Mathf.within(x, y, e.x, e.y, 14))) {
                        if (noise > 0.72f) {
                            floor = noise > 0.78f ? (Floor) CaeBlocks.bluonixiteWater : (floor == Blocks.sand ? (Floor) Blocks.sandWater : (Floor) CaeBlocks.bluonixiteWater);
                        } else {
                            floor = (floor == Blocks.sand ? (Floor) floor : (Floor) Blocks.darksand);
                        }
                    }
                } 


            // Decorations
            if (rand.chance(0.01) && floor.asFloor().hasSurface() && block == Blocks.air) {
                boolean clear = true;
                for (Point2 p : Geometry.d4) {
                    if (tiles.get(x + p.x, y + p.y).block() != Blocks.air) {
                        clear = false; 
                        break; 
                    }
                }
                if(clear) block = dec.get(floor, floor.asFloor().decoration);
            }
        });
    }

    public void generateRuins(CaeRoom spawn, IntSeq ints) {
        Rand rand = gen.getRand();
        Sector sector = gen.getSector();
        int width = gen.getMapWidth(), height = gen.getMapHeight();
        Tiles tiles = gen.getTiles();
        int ruinCount = rand.random(-2, 4);
        if (ruinCount <= 0) return;

        float difficulty = sector.threat;
        ints.clear();
        ints.ensureCapacity(width * height / 4);
        int padding = 25;

        // Collect potential positions
        for (int x = padding; x < width - padding; x++) {
            for (int y = padding; y < height - padding; y++) {
                Tile tile = tiles.getn(x, y);
                if (!tile.solid() && (tile.drop() != null || tile.floor().liquidDrop != null)) {
                    ints.add(tile.pos());
                }
            }
        }
        ints.shuffle(rand);

        int placed = 0;
        for (int i = 0; i < ints.size && placed < ruinCount; i++) {
            int val = ints.items[i];
            int x = Point2.x(val), y = Point2.y(val);

            if (Mathf.within(x, y, spawn.x, spawn.y, 18f)) continue;

            float range = difficulty + rand.random(0.4f);
            Tile tile = tiles.getn(x, y);
            BaseRegistry.BasePart part = null;

            if (tile.overlay().itemDrop != null) {
                part = bases.forResource(tile.drop()).getFrac(range);
            } else if (tile.floor().liquidDrop != null && rand.chance(0.05)) {
                part = bases.forResource(tile.floor().liquidDrop).getFrac(range);
            } else if (rand.chance(0.05)) {
                part = bases.parts.getFrac(range);
            }

            if (part != null && BaseGenerator.tryPlace(part, x, y, Team.derelict, rand, (cx, cy) -> {
                Tile other = tiles.getn(cx, cy);
                if (other.floor().hasSurface()) {
                    other.setOverlay(Blocks.oreScrap);
                    // Add scattered scrap around ruins
                    for (int j = 1; j <= 2; j++) {
                        for (Point2 p : Geometry.d8) {
                            Tile t = tiles.get(cx + p.x * j, cy + p.y * j);
                            if (t != null && t.floor().hasSurface() && rand.chance(j == 1 ? 0.4 : 0.2)) {
                                t.setOverlay(Blocks.oreScrap);
                            }
                        }
                    }
                }
            })) {
                placed++;
                int debrisRadius = Math.max(part.schematic.width, part.schematic.height) / 2 + 3;
                Geometry.circle(x, y, tiles.width, tiles.height, debrisRadius, (cx, cy) -> {
                    float dst = Mathf.dst(cx, cy, x, y);
                    float removeChance = Mathf.lerp(0.05f, 0.5f, dst / debrisRadius);
                    Tile other = tiles.getn(cx, cy);
                    if (other.build != null && other.isCenter()) {
                        if (other.team() == Team.derelict && rand.chance(removeChance)) {
                            other.remove();
                        } else if (rand.chance(0.5)) {
                            other.build.health -= rand.random(other.build.health * 0.9f);
                        }
                    }
                });
            }
        }
        
        // Remove invalid ores (placed by ruins or generation)
        for (Tile tile : tiles) {
            if (tile.overlay().needsSurface && !tile.floor().hasSurface()) {
                tile.setOverlay(Blocks.air);
            }
        }
    }

    public void setupRules(Seq<CaeRoom> enemies, CaeRoom spawn, boolean naval, BaseGenerator basegen) {
        Tiles tiles = gen.getTiles();
        Sector sector = gen.getSector();
        float difficulty = sector.threat;
        
        if (sector.hasEnemyBase()) {
            basegen.generate(tiles, enemies.map(r -> tiles.getn(r.x, r.y)), tiles.get(spawn.x, spawn.y), state.rules.waveTeam, sector, difficulty);
            state.rules.attackMode = sector.info.attack = true;
        } else {
            state.rules.winWave = sector.info.winWave = 10 + 5 * (int) Math.max(difficulty * 10, 1);
        }

        state.rules.waveSpacing = Mathf.lerp(60 * 65 * 2, 60f * 60f * 1f, Math.max(difficulty - 0.4f, 0f));
        state.rules.waves = true;
        state.rules.env = sector.planet.defaultEnv;
        state.rules.enemyCoreBuildRadius = 600f;
        state.rules.spawns = Waves.generate(difficulty, new Rand(sector.id), state.rules.attackMode, state.rules.attackMode && spawner.countGroundSpawns() == 0, naval);
    }
}
