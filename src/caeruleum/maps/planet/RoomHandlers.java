package caeruleum.maps.planet;

import arc.func.Intc4;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Structs;
import arc.util.Tmp;
import caeruleum.content.CaeBlocks;
import mindustry.ai.Astar;
import mindustry.content.Blocks;
import mindustry.content.Liquids;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;


public class RoomHandlers extends CaeruleumPlanetGenerator{
   public void makeIsland(int x, int y, int radius, int miniAmount, int distFromBig){
       for(int i = 0; i > miniAmount; i++){
           int miniRadius = (radius / miniAmount);
           int miniDist = (radius + distFromBig) * rand.random(miniRadius / distFromBig);
           int posx = miniDist * (int)Mathf.cosDeg(i *(360/miniAmount)),
               posy = miniDist * (int)Mathf.cosDeg(i *(360/miniAmount));
           makeIsland(x + posx, y + posy, miniRadius);
       }
       makeIsland(x, y, radius);
   }
    public void makeIsland(int x, int y, int radius){
        makeIsland(x, y, radius, (Floor) CaeBlocks.bluonixite, (Floor) CaeBlocks.bluonixiteWater);
    }
    public void makeIsland(int x, int y, int radius, Floor inner, @Nullable Floor outer){
       makeIsland(x, y, radius, 8, inner, outer); // 8 to take account for shores
    }
    public void makeIsland(int x, int y, int radius, int outerOffset, Floor inner, @Nullable Floor outer){
    // should in theory make a circle islands
       circle(x, y, radius + outerOffset, (ix, iy, wx, wy) -> {
            Tile other = tiles.getn(wx, wy); 
            if (Mathf.within(ix, iy, radius + outerOffset) && other.floor().isLiquid && outer != null){
                other.setFloor(outer);
            }
            if (Mathf.within(ix, iy, radius) && other.floor().isLiquid){
                other.setFloor(inner);
            }
       });
   }

   public void circle(int x, int y, int radius, Intc4 cons){
    for (int ix = -radius; ix <= radius; ix++) {
        for (int iy = -radius; iy <= radius; iy++) {
            int wx = ix + x, wy = iy + y;
            if (Structs.inBounds(wx, wy, width, height) && Mathf.within(ix, iy, radius)) {
                cons.get(ix, iy, wx, wy);
            }
        }
      }
   }
 
    public void shallowShores(int radius, Floor check, Floor replace, Floor retain){
        pass((x, y) -> {
            if (floor.asFloor().isLiquid && floor.asFloor().shallow) {
                for (int ix = -radius; ix <= radius; ix++) {
                    for (int iy = -radius; iy <= radius; iy++) {
                        if ((ix) * (ix) + (iy) * (iy) <= radius * radius) {                    
                            int wx = ix + x, wy = iy + y;
                            Tile tile = tiles.get(wx, wy);
                            if (tile != null && (!tile.floor().isLiquid || tile.block() != Blocks.air)) {
                            //found something solid, skip replacing anything
                                return;
                            }
                        }
                    }
                }
                floor = floor == check ? replace : retain;
            }
        });
    }
    public int detectWater(int x, int y, int radius){
        int waterTiles = 0;
        for (int ix = -radius; ix <= radius; ix++) {
            for (int iy = -radius; iy <= radius; iy++) {
                if (Mathf.within(ix, iy, radius)) {                    
                    int wx = ix + x, wy = iy + y;
                    Tile tile = tiles.get(wx, wy);
                    if (tile != null && tile.floor().liquidDrop == Liquids.water) {
                      waterTiles++;
                    }
                }
            }
        }
        return waterTiles;
    }
    public int detectSolid(int x, int y, int radius){
        int solidTiles = 0;
        for (int ix = -radius; ix <= radius; ix++) {
            for (int iy = -radius; iy <= radius; iy++) {
                if (Mathf.within(ix, iy, radius)) {                    
                    int wx = ix + x, wy = iy + y;
                    Tile tile = tiles.get(wx, wy);
                    if (tile != null && (!tile.floor().isLiquid || tile.block() != Blocks.air)) {
                      solidTiles++;
                    }
                }
            }
        }
        return solidTiles;
    }
    public void deepShores(int radius, Floor check, Floor replace, Floor retain){
        pass((x, y) -> {
            if (floor.asFloor().isLiquid && !floor.asFloor().isDeep() && !floor.asFloor().shallow) { 
                for (int ix = -radius; ix <= radius; ix++) {
                    for (int iy = -radius; iy <= radius; iy++) {
                        if (Mathf.within(ix, iy, radius)) {                    
                            int wx = ix + x, wy = iy + y;
                            Tile tile = tiles.get(wx, wy);
                            if (tile != null && (tile.floor().shallow || !tile.floor().isLiquid)) {
                              //found something shallow, skip replacing anything
                                //i want to use circle but return do not work that way
                                return;
                            }
                        }
                    }
                }
                floor = floor == check ? replace : retain;
            }
        });
    }

    public class RoomHandler {
        float constraint, radius, length;
        int rooms, enemySpawns, offset, angleStep, waterCheckRad, connections;
        Seq<Room> roomseq = new Seq<>();
        Room spawn = null; //the spawn room
        Seq<Room> enemies = new Seq<>(); // enemies room
        boolean naval = false, ocean = false;
        boolean forest = false;

        void init(){
            constraint = 1.3f;
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

        void makeRooms(int amount){
            rooms = amount;
            for(int i = 0; i < amount; i++){
                Tmp.v1.trns(rand.random(360f), rand.random(radius / constraint));
                float rx = (width / 2f + Tmp.v1.x);
                float ry = (height / 2f + Tmp.v1.y);
                float maxrad = radius - Tmp.v1.len();
                float rrad = Math.min(rand.random(9f, maxrad / 2f), 30f);
                Room ro = new Room((int) rx, (int) ry, (int) rrad);
                roomseq.add(ro);
            }
        }

        void makeConnections(){
            connections = rand.random(Math.max(rooms - 1, 1), rooms + 3);
            for (int i = 0; i < connections; i++) {
                Room room1 = randomRoom();
                Room room2 = randomRoom(); 

                room1.connect(room2); 
                room1.connectIslandsWater(room2);
            }
        }

        Room randomRoom(){
            return roomseq.random(rand);
        }

        void connectRooms(Room to){
            for(Room room : roomseq){
                room.connect(to);
            }
        }

        void connectEnemies(Room to){
            for(Room room : enemies){
                room.connect(to);
                if(naval) room.connectLiquid(to);
            }
        }

        void renderRoom(Room room){
            erase(room.x, room.y, room.radius);
            if(ocean) makeIsland(room.x, room.y, room.radius);
        }

        void renderPaths(){
            for(Room room : roomseq){
              room.connected.forEach(r -> {
                if(ocean) room.connectIslandsWater(r);
              });
            }
        }

        void makeSpawn(){
            //spawn and enemySpawns 
            for (int i = 0; i < 360; i += angleStep) {
                int angle = offset + i;
                int cx = (int) (width / 2 + Angles.trnsx(angle, length));
                int cy = (int) (height / 2 + Angles.trnsy(angle, length));

                int waterTiles = detectWater(cx, cy, waterCheckRad);

                if (waterTiles <= 4 || (i + angleStep >= 360)) {
                    roomseq.add(spawn = new Room(cx, cy, rand.random(10, 15)));

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
        }
    }
    public class Room {
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
        void roomDist(Room to){
            Mathf.dst(x, y, to.x, to.y);
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

        void joinIslands(int x1, int y1, int x2, int y2){
            float nscl = rand.random(100f, 140f) * 6f;

            pathfind(x1, y1, x2, y2, tile -> (!tile.solid() || tile.floor().isLiquid ? 70f : 0f) + noise(tile.x, tile.y, 2, 0.4f, 1f / nscl) * 500, Astar.manhattan).each(t -> {
                makeIsland(t.x, t.y, radius);
            });
        }
        void connectIslands(Room to){
            if (to == this) return;

            Vec2 midpoint = Tmp.v1.set(to.x, to.y).add(x, y).scl(0.5f);
            rand.nextFloat();

            //add randomized offset to avoid straight lines
            midpoint.add(Tmp.v2.setToRandomDirection(rand).scl(Tmp.v1.dst(x, y)));
            midpoint.sub(width / 2f, height / 2f).limit(width / 2f / Mathf.sqrt3).add(width / 2f, height / 2f);

            int mx = (int) midpoint.x, my = (int) midpoint.y;

            joinIslands(x, y, mx, my);
            joinIslands(mx, my, to.x, to.y);
        }

        void joinIslandsWater(int x1, int y1, int x2, int y2){
            float nscl = rand.random(100f, 140f) * 6f;
            Floor [] floors = {
                (Floor) CaeBlocks.bluonixite,
                (Floor) CaeBlocks.bluonixiteWater
            };
            Floor [] floorsOuter = {
                (Floor) CaeBlocks.bluonixiteWater,
                (Floor) CaeBlocks.aquafluent
            };
            float distTo = Mathf.dst(x1, y1, x2, y2);
            
            pathfind(x1, y1, x2, y2, tile -> (!tile.solid() || tile.floor().isLiquid ? 70f : 0f) + noise(tile.x, tile.y, 2, 0.4f, 1f / nscl) * 500, Astar.manhattan).each(t -> {
                float currDist = Mathf.dst(x1, y1, t.x, t.y);
                float len = 1 - (currDist / distTo);
                int index = (int)(len * (floors.length - 1));
                float rad = (radius * len) + noise(t.x, t.y, 3, 1, 50, 1) * (radius / 3);

                makeIsland(t.x, t.y, (int) Math.max(rad, 4), (int) Math.max(((rad * 2.2f) / 3), 8), floors[index], floorsOuter[index]);
            });
        }

        void connectIslandsWater(Room to){
            if (to == this) return;

            Vec2 midpoint = Tmp.v1.set(to.x, to.y).add(x, y).scl(0.5f);
            rand.nextFloat();

            //add randomized offset to avoid straight lines
            midpoint.add(Tmp.v2.setToRandomDirection(rand).scl(Tmp.v1.dst(x, y)));
            midpoint.sub(width / 2f, height / 2f).limit(width / 2f / Mathf.sqrt3).add(width / 2f, height / 2f);

            int mx = (int) midpoint.x, my = (int) midpoint.y;

            joinIslandsWater(x, y, mx, my);
            joinIslandsWater(to.x, to.y, mx, my);
        }
    }
}
