
package caeruleum.maps.planet;

import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.noise.Ridged;
import arc.util.noise.Simplex;
import caeruleum.content.CaeBlocks;
import caeruleum.maps.utils.CaeRoomManager.Room;
import mindustry.ai.BaseRegistry;
import mindustry.content.*;
import mindustry.game.Team;
import mindustry.game.Waves;
import mindustry.type.Sector;
import mindustry.maps.generators.BaseGenerator;
import mindustry.maps.generators.PlanetGenerator;
import mindustry.world.*;
import mindustry.world.blocks.environment.Floor;

import static mindustry.Vars.*;

public abstract class CaeMapUtils extends PlanetGenerator {
    
    // --- Island Generation ---

    public Rand getRand(){ return this.rand; };
    public Sector getSector() {return this.sector;};
    public int getMapWidth(){ return this.width; };
    public int getMapHeight(){ return this.height; };
    public Tiles getTiles(){ return this.tiles; };

    public float getNoise(float x, float y, float scl, float mag){
        return noise(x, y, scl, mag);
    }
    public float getNoise(float x, float y, float octaves, float falloff, float scl){
        return noise(x, y, octaves, falloff, scl);
    }
    public float getNoise(float x, float y, float octaves, float falloff, float scl, float mag){
        return noise(x, y, octaves, falloff, scl, mag);
    }
    public void getNoise(Floor floor, Block block, int octaves, float falloff, float scl, float threshold){
         noise(floor, block, octaves, falloff, scl, threshold);
    }
    
    public void makeOrganicIsland(int cx, int cy, int baseRadius, Floor inner, Floor outer) {
        int padding = 15; // Extra space for the noise to push outward
        int maxRadius = baseRadius + padding;
        
        scanCircle(cx, cy, maxRadius, (wx, wy, dst2) -> {
            Tile tile = tiles.getn(wx, wy);
            if (!tile.floor().isLiquid) return; // Don't overwrite existing land

            // 1. Calculate linear distance from center (0.0 at center, 1.0 at edge of baseRadius)
            float dist = Mathf.dst(cx, cy, wx, wy) / baseRadius;
            
            // 2. Sample low-frequency noise to warp the coastline
            // The noise returns roughly -1.0 to 1.0
            float edgeNoise = noise(wx, wy, 3, 0.5, 20f) * 0.45f; 
            
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

    public GridBits applyCells(GridBits input, int width, int height, int iterations, int birthLimit, int deathLimit, int cradius) {
        GridBits read = new GridBits(width, height);
        GridBits write = new GridBits(width, height);
        
        // Copy input into our read buffer
        read.set(input);

        for (int i = 0; i < iterations; i++) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int alive = 0;

                    for (int cx = -cradius; cx <= cradius; cx++) {
                        for (int cy = -cradius; cy <= cradius; cy++) {
                            if ((cx == 0 && cy == 0) || !Mathf.within(cx, cy, cradius)) continue;
                            
                            if (!Structs.inBounds(x + cx, y + cy, width, height) || read.get(x + cx, y + cy)) {
                                alive++;
                            }
                        }
                    }

                    if (read.get(x, y)) {
                        write.set(x, y, alive >= deathLimit);
                    } else {
                        write.set(x, y, alive > birthLimit);
                    }
                }
            }
            // Flush results for the next iteration
            read.set(write);
        }
        
        return read;
    }

    public void generateDensityForest(int blurIterations, int cradius) {
        float[][] density = new float[tiles.width][tiles.height];
        float[][] write = new float[tiles.width][tiles.height];

        //initial density
        tiles.each((x, y) -> {
            density[x][y] = rand.chance(0.1f)? 0.7f : 0.1f;
            if(!tiles.get(x, y).block().isAir()){
                density[x][y] = 1.0f;
            }
            density[x][y] = rand.chance(0.1f)? 0.7f : 0.1f;
        });

        for (int i = 0; i < blurIterations; i++) {
            tiles.each((x, y) -> {
                float totalDensity = 0f;
                int count = 0;

                for (int cx = -cradius; cx <= cradius; cx++) {
                    for (int cy = -cradius; cy <= cradius; cy++) {
                        if (Structs.inBounds(x + cx, y + cy, tiles.width, tiles.height)) {
                            totalDensity += density[x + cx][y + cy];
                            count++;
                        }
                    }
                }
                write[x][y] = totalDensity / count;
            });

            // Copy the smoothed results back to the main density grid
            for (int x = 0; x < tiles.width; x++) {
                System.arraycopy(write[x], 0, density[x], 0, tiles.height);
            }
        }

        //Apply the density to the world using thresholds
        pass((x, y) -> {
            float d = density[x][y];
            // Only plant on valid soil
            boolean isAir = tiles.get(x,y).block().isAir();
            if (floor == CaeBlocks.lazurigrass && isAir){
                if ((d > 0.57f) ) {
                    if(x % 2 == 0 && y % 2 == 0){
                      if(rand.chance(0.9f)) block = CaeBlocks.blueTree; 
                    }
                } else if (d > 0.25f && rand.chance(0.1)) {
                    block = CaeBlocks.blueTendrils;
                    if(rand.chance(0.5)){
                        block = CaeBlocks.blueTree;
                    }
                    if(rand.chance(0.5)){
                        block = CaeBlocks.blueFlower;
                    }
                    if(rand.chance(0.1)){
                        block = CaeBlocks.blueTendrils;
                    }
                }
            }
        });
    }

    protected void generateRivers(Room spawn) {
        pass((x, y) -> {
            if (block.solid) return;

            Vec3 v = sector.rect.project(x, y);
            float rr = Simplex.noise2d(sector.id, 2, 0.6f, 1f / 7f, x, y) * 0.1f;
            float value = Ridged.noise3d(2, v.x, v.y, v.z, 1, 1f / 55f) + rr; // Removed - rawHeight * 0f
            float rrscl = rr * 44 - 2;

            if (value > 0.17f && !Mathf.within(x, y, spawn.x, spawn.y, 12 + rrscl)) {
                boolean deep = value > 0.27f && !Mathf.within(x, y, spawn.x, spawn.y, 15 + rrscl);
                boolean spore = floor != Blocks.sand && floor != Blocks.salt;
                
                if (floor != Blocks.ice && floor != Blocks.iceSnow && floor != Blocks.snow && !floor.asFloor().isLiquid) {
                    if (spore) {
                        floor = deep ? CaeBlocks.bluonixiteWater : Blocks.darksandWater;
                    } else {
                        floor = deep ? Blocks.water : 
                               (floor == Blocks.sand || floor == Blocks.salt ? Blocks.sandWater : Blocks.darksandWater);
                    }
                }
            }
        });
    }

    protected void smoothWater(int deepRadius, boolean navalMode) {
        pass((x, y) -> {
            // Logic selection based on mode
            boolean condition = navalMode 
                ? (floor.asFloor().isLiquid && !floor.asFloor().isDeep() && !floor.asFloor().shallow)
                : (floor.asFloor().isLiquid && floor.asFloor().shallow);

            if (condition) {
                for (int cx = -deepRadius; cx <= deepRadius; cx++) {
                    for (int cy = -deepRadius; cy <= deepRadius; cy++) {
                        if (cx*cx + cy*cy <= deepRadius*deepRadius) {
                            Tile tile = tiles.get(cx + x, cy + y);
                            
                            boolean breakCondition = navalMode
                                ? (tile != null && (tile.floor().shallow || !tile.floor().isLiquid))
                                : (tile != null && (!tile.floor().isLiquid || tile.block() != Blocks.air));

                            if (breakCondition) return;
                        }
                    }
                }
                
                if (navalMode) {
                    floor = (floor == Blocks.water) ? Blocks.deepwater : CaeBlocks.bluonixiteWater;
                } else {
                    floor = (floor == Blocks.darksandWater) ? CaeBlocks.bluonixiteWater : Blocks.water;
                }
            }
        });
    }

    protected void generateOres(Seq<Block> basicOres) {
        Seq<Block> ores = new Seq<>(basicOres);
        float poles = Math.abs(sector.tile.v.y);
        float scl = 1f;

        // Dynamic Ore Addition
        if (Simplex.noise3d(seed, 2, 0.5, scl, sector.tile.v.x, sector.tile.v.y, sector.tile.v.z) * 0.5f + poles > 0.325f)
            ores.add(Blocks.oreCoal);
        if (Simplex.noise3d(seed, 2, 0.5, scl, sector.tile.v.x + 1, sector.tile.v.y, sector.tile.v.z) * 0.5f + poles > 0.65f)
            ores.add(Blocks.oreTitanium);
        if (Simplex.noise3d(seed, 2, 0.5, scl, sector.tile.v.x + 2, sector.tile.v.y, sector.tile.v.z) * 0.5f + poles > 0.91f)
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
                if (Math.abs(0.5f - noise(offsetX, offsetY + i * 999, 2, 0.7, (40 + i * 2))) > 0.22f + i * 0.01 &&
                    Math.abs(0.5f - noise(offsetX, offsetY - i * 999, 1, 1, (30 + i * 4))) > 0.37f + freq) {
                    ore = entry;
                    break;
                }
            }

            if (ore == Blocks.oreScrap && rand.chance(0.33)) {
                floor = Blocks.metalFloorDamaged;
            }
        });
    }
    protected void generateVegitation(){
       pass((x, y) -> {
            // Trees
            if (rand.chance(0.0075)) {
                boolean hasSpace = false;
                boolean surrounded = true;
                for (Point2 p : Geometry.d4) {
                    Tile other = tiles.get(x + p.x, y + p.y);
                    if (other != null && other.block() == Blocks.air) hasSpace = true;
                    else surrounded = false;
                }

                if (hasSpace && ((block == Blocks.snowWall || block == Blocks.iceWall) || 
                    (surrounded && block == Blocks.air && floor == Blocks.snow && rand.chance(0.03)))) {
                    block = rand.chance(0.5) ? Blocks.whiteTree : Blocks.whiteTreeDead;
                }
            }
        }); 
    }

    protected void generateCaveDecorations(Seq<Room> rooms, ObjectMap<Block, Block> dec ,boolean genLakes) {
        pass((x, y) -> {
            //Tar
            if (floor == Blocks.darksand) {
                if (Math.abs(0.5f - noise(x - 40, y, 2, 0.7, 80)) > 0.25f &&
                    Math.abs(0.5f - noise(x, y + sector.id * 10, 1, 1, 60)) > 0.41f && 
                    !rooms.contains(r -> Mathf.within(x, y, r.x, r.y, 30))) {
                    floor = Blocks.tar;
                }
            }
            // Hotrock & Lakes
           if (floor == Blocks.hotrock) {
                    if (Math.abs(0.5f - noise(x - 90, y, 4, 0.8, 80)) > 0.035) {
                        floor = Blocks.basalt;
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
                            floor = Blocks.magmarock;
                        }
                    }
                } else if (genLakes && floor != Blocks.basalt && floor != Blocks.ice && floor.asFloor().hasSurface()) {
                    float noise = noise(x + 782, y, 5, 0.75f, 260f, 1f);
                    if (noise > 0.67f && !rooms.contains(e -> Mathf.within(x, y, e.x, e.y, 14))) {
                        if (noise > 0.72f) {
                            floor = noise > 0.78f ? CaeBlocks.bluonixiteWater : (floor == Blocks.sand ? Blocks.sandWater : CaeBlocks.bluonixiteWater);
                        } else {
                            floor = (floor == Blocks.sand ? floor : Blocks.darksand);
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

    protected void generateRuins(Room spawn, IntSeq ints) {
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

    protected void setupRules(Seq<Room> enemies, Room spawn, boolean naval, BaseGenerator basegen) {
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
