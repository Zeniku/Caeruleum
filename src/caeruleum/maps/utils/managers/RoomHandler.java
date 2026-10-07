package caeruleum.maps.utils.managers;

import static mindustry.Vars.loadLocales;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.Structs;
import caeruleum.maps.utils.CaeBasicGenerator;
import caeruleum.maps.utils.CaeChunk;
import caeruleum.maps.utils.CaeMapUtilities;
import caeruleum.maps.utils.CaeRoom;
import mindustry.type.Sector;

public class RoomHandler {
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

        public <T> void shuffleSeq(Seq<T> seq, Rand rand) {
            for (int i = seq.size - 1; i > 0; i--) {
                int target = rand.random(i); // Generates an integer between 0 and i inclusive
                seq.swap(i, target);
            }
        }
        
        public void makeRooms(int amount, CaeChunkHandler chunkHandler) {
            Seq<CaeChunk> oceanChunks = chunkHandler.getChunks(c -> c.biome == CaeChunk.Biome.OCEAN);
            Seq<CaeChunk> landChunks = chunkHandler.getChunks(c -> 
                c.biome == CaeChunk.Biome.MOUNTAIN || 
                c.biome == CaeChunk.Biome.FOREST || 
                c.biome == CaeChunk.Biome.PLAINS
            );

            // 1. Fix integer division by casting to float
            float totalChunks = (float) (chunkHandler.chunks.length * chunkHandler.chunks[0].length);
            if (totalChunks == 0) return;

            float landRatio = landChunks.size / totalChunks;
            float waterRatio = oceanChunks.size / totalChunks;

            int targetLandRooms = Math.round(amount * landRatio);
            int targetOceanRooms = Math.round(amount * waterRatio);

            // Padding between room edges (in tiles) so walls/shores don't merge awkwardly
            int padding = 6; 

            // 2. Generate Land (Subtractive) Rooms
            shuffleSeq(landChunks, gen.getRand());
            for (CaeChunk c : landChunks) {
                if (roomseq.size >= targetLandRooms) break;

                int radius = gen.getRand().random(12, 22);
                int midX = c.getMidX();
                int midY = c.getMidY();

                // Validate distance against already spawned rooms
                if (isPositionValid(midX, midY, radius, padding)) {
                    CaeRoom room = new CaeRoom(midX, midY, radius, gen, utils);
                    gen.erase(room.x, room.y, room.radius); // Subtractive carve
                    roomseq.add(room);
                }
            }

            // 3. Generate Ocean (Additive) Rooms
            shuffleSeq(oceanChunks, gen.getRand());
            for (CaeChunk c : oceanChunks) {
                if (roomseq.size >= targetLandRooms + targetOceanRooms) break;

                int radius = gen.getRand().random(10, 18);
                int midX = c.getMidX();
                int midY = c.getMidY();

                if (isPositionValid(midX, midY, radius, padding)) {
                    CaeRoom room = new CaeRoom(midX, midY, radius, gen, utils);
                    // renderOceanRoom(room, c); // Additive stamp
                    roomseq.add(room);
                }
            }
        }

        /**
         * Checks if a proposed room circle overlaps with any already placed room in roomseq.
         */
        private boolean isPositionValid(int x, int y, int radius, int padding) {
            for (CaeRoom existing : roomseq) {
                // Distance check between centers: distSq < (r1 + r2 + padding)^2
                float minDistance = existing.radius + radius + padding;
                if (Mathf.dst2(x, y, existing.x, existing.y) < minDistance * minDistance) {
                    return false; // Collision detected
                }
            }
            return true;
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
        public void renderRoom(CaeRoom room, CaeChunkHandler chunkHandler){
            CaeChunk roomChunk = chunkHandler.getChunkAt(room.x, room.y, 32);
            if(roomChunk.biome == CaeChunk.Biome.PLAINS || roomChunk.biome == CaeChunk.Biome.MOUNTAIN || roomChunk.biome == CaeChunk.Biome.FOREST) gen.erase(room.x, room.y, room.radius);
            if(roomChunk.biome == CaeChunk.Biome.OCEAN) utils.makeIsland(room.x, room.y, room.radius);
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