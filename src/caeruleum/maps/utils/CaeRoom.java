package caeruleum.maps.utils;
import mindustry.ai.Astar;
import mindustry.content.Blocks;
import mindustry.maps.generators.PlanetGenerator;
import mindustry.world.Tile;
import mindustry.world.Tiles;
import mindustry.world.blocks.environment.Floor;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Vec2;
import arc.struct.ObjectSet;
import caeruleum.content.CaeBlocks;

public class CaeRoom {
        public int x, y, radius;
        Vec2 tmp1 = new Vec2();
        Vec2 tmp2 = new Vec2();
        private CaeBasicGenerator gen;
        public boolean pathLogged = false;
        public boolean connectLogged = false;
        public ObjectSet<CaeRoom> connected = new ObjectSet<>();
        private CaeMapUtilities utils;

        public CaeRoom(int x, int y, int radius, CaeBasicGenerator gen, CaeMapUtilities utils) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            connected.add(this);
            this.gen = gen;
            this.utils = utils;
        }
        void roomDist(CaeRoom to){
            Mathf.dst(x, y, to.x, to.y);
        }

        public void connect(CaeRoom to, boolean indirectPaths) {
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
                float noiseVal = gen.getNoisef(tile.x, tile.y, 2, 0.4f, 1f / nscl) * 500f;
                return cost + noiseVal;
            }, Astar.manhattan), stroke);        
        }

        public void connectLiquid(CaeRoom to) {
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
            var path = gen.pathfind(xx1, yy1, xx2, yy2, tile -> (tile.solid() || !tile.floor().isLiquid ? 70f : 0f) + gen.getNoisef(tile.x, tile.y, 2, 0.4f, 1f / nscl) * 500, Astar.manhattan);
            
            path.each(t -> {
                if (Mathf.dst2(t.x, t.y, xx2, yy2) <= avoidSq) return; // Don't drill near target core

                utils.scanCircle(t.x, t.y, rad, (wx, wy, dst2) -> {
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
        
        public void connectIslandsWater(CaeRoom to) {
             if (to == this) return;
             joinIslandsWater(x, y, to.x, to.y);
        }

        void joinIslandsWater(int x1, int y1, int x2, int y2) {
            Rand rand = gen.getRand();

            float nscl = rand.random(100f, 140f) * 6f;
            Floor[] floors = {(Floor) CaeBlocks.bluonixite, (Floor) CaeBlocks.bluonixiteWater};
            Floor[] floorsOuter = {(Floor) CaeBlocks.bluonixiteWater, (Floor) CaeBlocks.aquafluent};
            
            float totalDist = Mathf.dst(x1, y1, x2, y2);

            gen.pathfind(x1, y1, x2, y2, tile -> (!tile.solid() || tile.floor().isLiquid ? 70f : 0f) + gen.getNoisef(tile.x, tile.y, 2, 0.4f, 1f / nscl) * 500, Astar.manhattan).each(t -> {
                float currentDist = Mathf.dst(x1, y1, t.x, t.y);
                float progress = 1f - (currentDist / totalDist); // 0.0 to 1.0
                
                int index = Mathf.clamp((int)(progress * floors.length), 0, floors.length - 1);
                
                // Varied radius based on noise
                float rad = (radius * progress) + gen.getNoisef(t.x, t.y, 3, 1, 50, 1) * (radius / 3f);
                
                utils.makeIsland(t.x, t.y, (int)Math.max(rad, 4), (int)Math.max((rad * 0.7f), 8), floors[index], floorsOuter[index]);
            });
        }
    }