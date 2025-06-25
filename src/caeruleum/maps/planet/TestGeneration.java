package caeruleum.maps.planet;

import static mindustry.Vars.spawner;
import static mindustry.Vars.state;

import arc.math.Angles;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Vec2;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Structs;
import arc.util.Tmp;
import caeruleum.content.CaeBlocks;
import mindustry.ai.Astar;
import mindustry.content.Blocks;
import mindustry.content.Liquids;
import mindustry.game.Schematics;
import mindustry.game.Waves;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;

public class TestGeneration extends CaeruleumPlanetGenerator{
   @Override
   protected void generate(){
        class Room {
            int x, y, radius;
            ObjectSet<Room> connected = new ObjectSet<>();

            Room(int x, int y, int radius) {
                this.x = x;
                this.y = y;
                this.radius = radius;
                connected.add(this);
            }

            void join(int x1, int y1, int x2, int y2) {
                float nscl = rand.random(100f, 140f) * 6f;
                int stroke = rand.random(3, 9);
                brush(pathfind(x1, y1, x2, y2, tile -> (tile.solid() ? 50f : 0f) + noise(tile.x, tile.y, 2, 0.4f, 1f / nscl) * 500, Astar.manhattan), stroke);
            }

            void connect(Room to) {
                if (!connected.add(to) || to == this) return;

                Vec2 midpoint = Tmp.v1.set(to.x, to.y).add(x, y).scl(0.5f);
                rand.nextFloat();

                if (alt) {
                    midpoint.add(Tmp.v2.set(1, 0f).setAngle(Angles.angle(to.x, to.y, x, y) + 90f * (rand.chance(0.5) ? 1f : -1f)).scl(Tmp.v1.dst(x, y) * 2f));
                } else {
                    //add randomized offset to avoid straight lines
                    midpoint.add(Tmp.v2.setToRandomDirection(rand).scl(Tmp.v1.dst(x, y)));
                }

                midpoint.sub(width / 2f, height / 2f).limit(width / 2f / Mathf.sqrt3).add(width / 2f, height / 2f);

                int mx = (int) midpoint.x, my = (int) midpoint.y;

                join(x, y, mx, my);
                join(mx, my, to.x, to.y);
            }

            void joinLiquid(int x1, int y1, int x2, int y2) {
                float nscl = rand.random(100f, 140f) * 6f;
                int rad = rand.random(7, 11);
                int avoid = 2 + rad;
                var path = pathfind(x1, y1, x2, y2, tile -> (tile.solid() || !tile.floor().isLiquid ? 70f : 0f) + noise(tile.x, tile.y, 2, 0.4f, 1f / nscl) * 500, Astar.manhattan);
                path.each(t -> {
                    //don't place liquid paths near the core
                    if (Mathf.dst2(t.x, t.y, x2, y2) <= avoid * avoid) {
                        return;
                    }

                    for (int x = -rad; x <= rad; x++) {
                        for (int y = -rad; y <= rad; y++) {
                            int wx = t.x + x, wy = t.y + y;
                            if (Structs.inBounds(wx, wy, width, height) && Mathf.within(x, y, rad)) {
                                Tile other = tiles.getn(wx, wy);
                                other.setBlock(Blocks.air);
                                if (Mathf.within(x, y, rad - 1) && !other.floor().isLiquid) {
                                    Floor floor = other.floor();
                                    other.setFloor((Floor) (floor == Blocks.sand || floor == Blocks.salt ? Blocks.sandWater : CaeBlocks.bluonixiteWater));
                                }
                            }
                        }
                    }
                });
            }

            void connectLiquid(Room to) {
                if (to == this) return;

                Vec2 midpoint = Tmp.v1.set(to.x, to.y).add(x, y).scl(0.5f);
                rand.nextFloat();

                //add randomized offset to avoid straight lines
                midpoint.add(Tmp.v2.setToRandomDirection(rand).scl(Tmp.v1.dst(x, y)));
                midpoint.sub(width / 2f, height / 2f).limit(width / 2f / Mathf.sqrt3).add(width / 2f, height / 2f);

                int mx = (int) midpoint.x, my = (int) midpoint.y;

                joinLiquid(x, y, mx, my);
                joinLiquid(mx, my, to.x, to.y);
            }
        }

        cells(4);
        distort(10f, 12f);

        float constraint = 1.3f;
        float radius = width / 2f / Mathf.sqrt3;
        int rooms = rand.random(2, 5);
        Seq<Room> roomseq = new Seq<>();

        //room maker
        for (int i = 0; i < rooms; i++) {
            Tmp.v1.trns(rand.random(360f), rand.random(radius / constraint));
            float rx = (width / 2f + Tmp.v1.x);
            float ry = (height / 2f + Tmp.v1.y);
            float maxrad = radius - Tmp.v1.len();
            float rrad = Math.min(rand.random(9f, maxrad / 2f), 30f);
            roomseq.add(new Room((int) rx, (int) ry, (int) rrad));
        }

        //check positions on the map to place the player spawn. this needs to be in the corner of the map
        Room spawn = null; //the spawn room
        Seq<Room> enemies = new Seq<>(); // enemies room
        int enemySpawns = rand.random(1, Math.max((int) (sector.threat * 4), 1));
        int offset = rand.nextInt(360);
        float length = width / 2.55f - rand.random(13, 23);
        int angleStep = 5;
        int waterCheckRad = 5;

        //spawn and enemySpawns 
        for (int i = 0; i < 360; i += angleStep) {
            int angle = offset + i;
            int cx = (int) (width / 2 + Angles.trnsx(angle, length));
            int cy = (int) (height / 2 + Angles.trnsy(angle, length));

            int waterTiles = 0;

            //check for water presence
            for (int rx = -waterCheckRad; rx <= waterCheckRad; rx++) {
                for (int ry = -waterCheckRad; ry <= waterCheckRad; ry++) {
                    Tile tile = tiles.get(cx + rx, cy + ry);
                    if (tile == null || tile.floor().liquidDrop != null) {
                        waterTiles++;
                    }
                }
            }

            if (waterTiles <= 4 || (i + angleStep >= 360)) {
                roomseq.add(spawn = new Room(cx, cy, rand.random(8, 15)));

                for (int j = 0; j < enemySpawns; j++) {
                    float enemyOffset = rand.range(60f);
                    Tmp.v1.set(cx - width / 2, cy - height / 2).rotate(180f + enemyOffset).add(width / 2, height / 2);
                    Room espawn = new Room((int) Tmp.v1.x, (int) Tmp.v1.y, rand.random(8, 16));
                    roomseq.add(espawn);
                    enemies.add(espawn);
                }

                break;
            }
        }

        //clear radius around each room
        for (Room room : roomseq) {
            erase(room.x, room.y, room.radius);
        }

        //randomly connect rooms together
        int connections = rand.random(Math.max(rooms - 1, 1), rooms + 3);
        for (int i = 0; i < connections; i++) {
            roomseq.random(rand).connect(roomseq.random(rand));
        }

        // connect to the rooms
        for (Room room : roomseq) {
            spawn.connect(room);
        }

        Room fspawn = spawn;

        cells(1);

        int tlen = tiles.width * tiles.height;
        int total = 0, waters = 0;

        for (int i = 0; i < tlen; i++) {
            Tile tile = tiles.geti(i);
            if (tile.block() == Blocks.air) {
                total++;
                if (tile.floor().liquidDrop == Liquids.water) {
                    waters++;
                }
            }
        }

        float difficulty = sector.threat;
        Schematics.placeLaunchLoadout(spawn.x, spawn.y); //place core

        // should in theory make a circle iland on the core
        for (int x = -spawn.radius; x <= spawn.radius; x++) {
            for (int y = -spawn.radius; y <= spawn.radius; y++) {
                int wx = spawn.x + x, wy = spawn.y + y;
                if (Structs.inBounds(wx, wy, width, height) && Mathf.within(x, y, spawn.radius)) {
                    Tile other = tiles.getn(wx, wy);
                    if (Mathf.within(x, y, spawn.radius - 1) && other.floor().isLiquid) {
                        other.setFloor((Floor) CaeBlocks.bluonixite);
                    };
                }
            }
        }

        //place spawner block
        for (Room espawn : enemies) {
            tiles.getn(espawn.x, espawn.y).setOverlay(Blocks.spawn);
        }

        if (sector.hasEnemyBase()) {
            basegen.generate(tiles, enemies.map(r -> tiles.getn(r.x, r.y)), tiles.get(spawn.x, spawn.y), state.rules.waveTeam, sector, difficulty);

            state.rules.attackMode = sector.info.attack = true;
        } else {
            state.rules.winWave = sector.info.winWave = 10 + 5 * (int) Math.max(difficulty * 10, 1);
        }

        float waveTimeDec = 0.4f;

        state.rules.waveSpacing = Mathf.lerp(60 * 65 * 2, 60f * 60f * 1f, Math.max(difficulty - waveTimeDec, 0f));
        state.rules.waves = true;
        state.rules.env = sector.planet.defaultEnv;
        state.rules.enemyCoreBuildRadius = 600f;

        //spawn air only when spawn is blocked
        state.rules.spawns = Waves.generate(difficulty, new Rand(sector.id), state.rules.attackMode, state.rules.attackMode && spawner.countGroundSpawns() == 0, false);

    }
}
