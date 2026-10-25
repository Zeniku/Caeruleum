package caeruleum.maps.utils;

import arc.func.Intc2;
import mindustry.world.Tile;
import mindustry.world.Tiles;

public class CaeChunk {
    private CaeBasicGenerator gen;
    public int width;
    public int height;
    public int y;
    public int x;
    public float oceanInfluence = 0f;
    public CaeRoom room = null;

    public enum Biome{
        FOREST, DESERT, OCEAN, MOUNTAIN, PLAINS, CRATER
    }

    public Biome biome;

    public CaeChunk (int x, int y, int width, int height, CaeBasicGenerator gen){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.gen = gen;
    }
    // just to get every coords inside the chunk
    public void pass(Intc2 r){
        for (int cx = x; cx < x + width; cx++) {
            for (int cy = y; cy < y + height; cy++) {
                r.get(cx, cy);
            }
        }
    }

    public int getMidX() {
        return x + (width / 2);
    }

    public int getMidY() {
        return y + (height / 2);
    }
}
