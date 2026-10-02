package caeruleum.maps.utils.managers;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.Structs;
import caeruleum.maps.utils.CaeBasicGenerator;
import caeruleum.maps.utils.CaeMapUtilities;
import caeruleum.maps.utils.CaeRoom;
import mindustry.type.Sector;
public class CaeRoomManager {

    public static class RoomHandler {
        private Vec2 tmp = new Vec2();
        private CaeBasicGenerator gen;
        float constraint, radius, length;
        int rooms, enemySpawns, offset, angleStep, waterCheckRad, connections;
        public Seq<CaeRoom> roomseq = new Seq<>();
        public CaeRoom spawn = null; //the spawn room
        public Seq<CaeRoom> enemies = new Seq<>(); // enemies room
        public boolean naval = false, ocean = false;
        public boolean forest = false, cave = false;
        private CaeMapUtilities utils;

        public RoomHandler(CaeBasicGenerator gen, CaeMapUtilities utils){
            this.gen = gen;
            this.utils = utils;
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
                    CaeRoom ro = new CaeRoom((int) rx, (int) ry, (int) rrad, gen, utils);
                    roomseq.add(ro);
                }
            }
        }

        public void makeConnections(){

            Rand rand = gen.getRand();
            connections = rand.random(Math.max(rooms - 1, 1), rooms + 3);
            for (int i = 0; i < connections; i++) {
                CaeRoom room1 = randomRoom();
                CaeRoom room2 = randomRoom(); 

                room1.connect(room2, false); 
                room1.connectIslandsWater(room2);
            }
        }

        public CaeRoom randomRoom(){

            Rand rand = gen.getRand();
            return roomseq.random(rand);
        }

        public void connectRooms(CaeRoom to){
            for(CaeRoom room : roomseq){
                room.connect(to, false);
            }
        }

        public void connectEnemies(CaeRoom to){
            for(CaeRoom room : enemies){
                room.connect(to, false);
                if(naval) room.connectLiquid(to);
            }
        }

        public void renderRoom(CaeRoom room){
            gen.erase(room.x, room.y, room.radius);
            if(ocean) utils.makeIsland(room.x, room.y, room.radius);
        }

        public void renderPaths(){
            for(CaeRoom room : roomseq){
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

                int waterTiles = utils.countWater(cx, cy, waterCheckRad);

                if (waterTiles <= 4 || (i + angleStep >= 360)) {
                    roomseq.add(spawn = new CaeRoom(cx, cy, rand.random(10, 15), gen, utils));

                    for (int j = 0; j < enemySpawns; j++) {
                        float enemyOffset = rand.range(60f);
                        tmp.set(cx - width / 2, cy - height / 2).rotate(180f + enemyOffset).add(width / 2, height / 2);
                        CaeRoom espawn = new CaeRoom((int) tmp.x, (int) tmp.y, rand.random(8, 16), gen, utils);
                        roomseq.add(espawn);
                        enemies.add(espawn);
                    }

                    break;
                }
            }
        }
    }
}
