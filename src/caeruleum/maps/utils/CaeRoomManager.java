
package caeruleum.maps.utils;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.Structs;
import mindustry.ai.Astar;
import mindustry.content.Blocks;
import mindustry.type.Sector;
import mindustry.world.Tile;
import mindustry.world.Tiles;
import mindustry.world.blocks.environment.Floor;
import caeruleum.content.CaeBlocks;
import caeruleum.maps.planet.CaeMapUtils;
import arc.files.Fi;

import static mindustry.Vars.*;

public class CaeRoomManager {

    public static class RoomHandler {
        private Vec2 tmp = new Vec2();
        private CaeMapUtils gen;
        float constraint, radius, length;
        int rooms, enemySpawns, offset, angleStep, waterCheckRad, connections;
        public Seq<Room> roomseq = new Seq<>();
        public Room spawn = null; //the spawn room
        public Seq<Room> enemies = new Seq<>(); // enemies room
        public boolean naval = false, ocean = false;
        public boolean forest = false, cave = false;

        public RoomHandler(CaeMapUtils gen){
            this.gen = gen;
        }
        
        public void init(){
            Rand rand = gen.getRand();
            int width = gen.getMapWidth();
            Sector sector = gen.getSector();
            constraint = 1.4f;
            radius = width / 2f / Mathf.sqrt3;
            rooms = rand.random(2, 5); // default
            roomseq = new Seq<>();
            spawn = null; //the spawn room
            enemies = new Seq<>(); // enemies room
            enemySpawns = rand.random(1, Math.max((int) (sector.threat * 4), 1));
            offset = rand.nextInt(360);
            length = width / 2.55f - rand.random(13, 23);
            angleStep = 5;
            waterCheckRad = 5;
            naval = false;
            ocean = false;
            connections = 3;
        }

        public void makeRooms(int amount){
            rooms = amount;

            Rand rand = gen.getRand();
            int width = gen.getMapWidth();
            int height = gen.getMapHeight();

            for(int i = 0; i < amount; i++){
                tmp.trns(rand.random(360f), rand.random(radius / constraint));
                float rx = (width / 2f + tmp.x);
                float ry = (height / 2f + tmp.y);
                float maxrad = radius - tmp.len();
                float rrad = Math.min(rand.random(9f, maxrad / 2f), 30f);
                if(Structs.inBounds((int)rx, (int)ry, width, height)){
                    Room ro = new Room((int) rx, (int) ry, (int) rrad, gen);
                    roomseq.add(ro);
                }
            }
        }

        public void makeConnections(){

            Rand rand = gen.getRand();
            connections = rand.random(Math.max(rooms - 1, 1), rooms + 3);
            for (int i = 0; i < connections; i++) {
                Room room1 = randomRoom();
                Room room2 = randomRoom(); 

                room1.connect(room2, false); 
                room1.connectIslandsWater(room2);
            }
        }

        public Room randomRoom(){

            Rand rand = gen.getRand();
            return roomseq.random(rand);
        }

        public void connectRooms(Room to){
            for(Room room : roomseq){
                room.connect(to, false);
            }
        }

        public void connectEnemies(Room to){
            for(Room room : enemies){
                room.connect(to, false);
                if(naval) room.connectLiquid(to);
            }
        }

        public void renderRoom(Room room){
            gen.erase(room.x, room.y, room.radius);
            if(ocean) gen.makeIsland(room.x, room.y, room.radius);
        }

        public void renderPaths(){
            for(Room room : roomseq){
              room.connected.forEach(r -> {
                if(ocean) room.connectIslandsWater(r);
              });
            }
        }

        public void makeSpawn(){
            //spawn and enemySpawns 
            Rand rand = gen.getRand();
            int width = gen.getMapWidth();
            int height = gen.getMapHeight();
            for (int i = 0; i < 360; i += angleStep) {
                int angle = offset + i;
                int cx = (int) (width / 2 + Angles.trnsx(angle, length));
                int cy = (int) (height / 2 + Angles.trnsy(angle, length));

                int waterTiles = gen.countWater(cx, cy, waterCheckRad);

                if (waterTiles <= 4 || (i + angleStep >= 360)) {
                    roomseq.add(spawn = new Room(cx, cy, rand.random(10, 15), gen));

                    for (int j = 0; j < enemySpawns; j++) {
                        float enemyOffset = rand.range(60f);
                        tmp.set(cx - width / 2, cy - height / 2).rotate(180f + enemyOffset).add(width / 2, height / 2);
                        Room espawn = new Room((int) tmp.x, (int) tmp.y, rand.random(8, 16), gen);
                        roomseq.add(espawn);
                        enemies.add(espawn);
                    }

                    break;
                }
            }
        }
    }

    public static class Room {
        public int x, y, radius;
        Vec2 tmp1 = new Vec2();
        Vec2 tmp2 = new Vec2();
        CaeMapUtils gen;
        public boolean pathLogged = false;
        public boolean connectLogged = false;
        public ObjectSet<Room> connected = new ObjectSet<>();

        public Room(int x, int y, int radius, CaeMapUtils gen) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            connected.add(this);
            this.gen = gen;
        }
        void roomDist(Room to){
            Mathf.dst(x, y, to.x, to.y);
        }

        public void connect(Room to, boolean indirectPaths) {
            if(!connected.add(to) || to == this) return;

            Rand rand = gen.getRand();

            int width = gen.getMapWidth();
            int height = gen.getMapHeight();
            Vec2 midpoint = tmp1.set(to.x, to.y).add(x, y).scl(0.5f);
            rand.nextFloat();

            if(indirectPaths){
                midpoint.add(tmp2.set(1, 0f).setAngle(Angles.angle(to.x, to.y, x, y) + 90f * (rand.chance(0.5) ? 1f : -1f)).scl(tmp1.dst(x, y) * 2f));
            }else{
                //add randomized offset to avoid straight lines
                midpoint.add(tmp2.setToRandomDirection(rand).scl(tmp1.dst(x, y)));
            }

            midpoint.sub(width/2f, height/2f).limit(width / 2f / Mathf.sqrt3).add(width/2f, height/2f);

            int mx = (int)midpoint.x, my = (int)midpoint.y;

            join(x, y, mx, my);
            join(mx, my, to.x, to.y);       
        }

        void join(int x1, int y1, int x2, int y2) {
            Rand rand = gen.getRand();
            float nscl = rand.random(100f, 140f) * 6f;
            int stroke = rand.random(3, 9);

            int mapW = gen.getMapWidth();
            int mapH = gen.getMapHeight();

            // 1. The Crash Screen Logger
            // If ANY coordinate is negative or larger than the map, force a custom crash screen!
            if (x1 < 0 || y1 < 0 || x2 < 0 || y2 < 0 || x1 >= mapW || y1 >= mapH || x2 >= mapW || y2 >= mapH) {
                throw new RuntimeException(
                    "CUSTOM DEBUG CRASH!\n" +
                    "Out of bounds detected before pathfinding.\n" +
                    "x1: " + x1 + ", y1: " + y1 + "\n" +
                    "x2: " + x2 + ", y2: " + y2 + "\n" +
                    "Map Size: " + mapW + "x" + mapH
                );
            }

            // 2. The Bulletproof Clamp
            // (This acts as a safety net in case you remove the debug crash above later)
            int safeX1 = Mathf.clamp(x1, 0, mapW - 1);
            int safeY1 = Mathf.clamp(y1, 0, mapH - 1);
            int safeX2 = Mathf.clamp(x2, 0, mapW - 1);
            int safeY2 = Mathf.clamp(y2, 0, mapH - 1);

            // 3. The Pathfinder
            gen.brush(gen.pathfind(safeX1, safeY1, safeX2, safeY2, tile -> {
                // Extra safety: ensure the tile exists before checking if it's solid
                if (tile == null) return 500f; 
                
                float cost = tile.solid() ? 50f : 0f;
                float noiseVal = gen.getNoise(tile.x, tile.y, 2, 0.4f, 1f / nscl) * 500f;
                return cost + noiseVal;
            }, Astar.manhattan), stroke);        
        }

        public void connectLiquid(Room to) {
            if (to == this) return;
            // Simplified: Direct connection with noise, or add midpoint logic if preferred
            joinLiquid(x, y, to.x, to.y); 
        }

        void joinLiquid(int x1, int y1, int x2, int y2) {

            Rand rand = gen.getRand();
            Tiles tiles = gen.getTiles();
            float nscl = rand.random(100f, 140f) * 6f;
            int rad = rand.random(7, 11);
            int avoidSq = (2 + rad) * (2 + rad);
            int xx1 = Mathf.clamp(x1, 0, gen.getMapWidth() - 1);
            int yy1 = Mathf.clamp(y1, 0, gen.getMapHeight() - 1);
            int xx2 = Mathf.clamp(x2, 0, gen.getMapWidth() - 1);
            int yy2 = Mathf.clamp(y2, 0, gen.getMapHeight() - 1);
            var path = gen.pathfind(xx1, yy1, xx2, yy2, tile -> (tile.solid() || !tile.floor().isLiquid ? 70f : 0f) + gen.getNoise(tile.x, tile.y, 2, 0.4f, 1f / nscl) * 500, Astar.manhattan);
            
            path.each(t -> {
                if (Mathf.dst2(t.x, t.y, xx2, yy2) <= avoidSq) return; // Don't drill near target core

                gen.scanCircle(t.x, t.y, rad, (wx, wy, dst2) -> {
                     Tile other = tiles.getn(wx, wy);
                     other.setBlock(Blocks.air);
                     
                     // If inside inner radius and not liquid, make it liquid
                     int innerRad = rad - 1;
                     if (dst2 <= innerRad * innerRad && !other.floor().isLiquid) {
                         Floor f = other.floor();
                         other.setFloor((Floor) (f == Blocks.sand || f == Blocks.salt ? Blocks.sandWater : CaeBlocks.bluonixiteWater));
                     }
                });
            });
        }
        
        public void connectIslandsWater(Room to) {
             if (to == this) return;
             joinIslandsWater(x, y, to.x, to.y);
        }

        void joinIslandsWater(int x1, int y1, int x2, int y2) {
            Rand rand = gen.getRand();

            float nscl = rand.random(100f, 140f) * 6f;
            Floor[] floors = {(Floor) CaeBlocks.bluonixite, (Floor) CaeBlocks.bluonixiteWater};
            Floor[] floorsOuter = {(Floor) CaeBlocks.bluonixiteWater, (Floor) CaeBlocks.aquafluent};
            
            float totalDist = Mathf.dst(x1, y1, x2, y2);

            gen.pathfind(x1, y1, x2, y2, tile -> (!tile.solid() || tile.floor().isLiquid ? 70f : 0f) + gen.getNoise(tile.x, tile.y, 2, 0.4f, 1f / nscl) * 500, Astar.manhattan).each(t -> {
                float currentDist = Mathf.dst(x1, y1, t.x, t.y);
                float progress = 1f - (currentDist / totalDist); // 0.0 to 1.0
                
                int index = Mathf.clamp((int)(progress * floors.length), 0, floors.length - 1);
                
                // Varied radius based on noise
                float rad = (radius * progress) + gen.getNoise(t.x, t.y, 3, 1, 50, 1) * (radius / 3f);
                
                gen.makeIsland(t.x, t.y, (int)Math.max(rad, 4), (int)Math.max((rad * 0.7f), 8), floors[index], floorsOuter[index]);
            });
        }
    }
 
}
