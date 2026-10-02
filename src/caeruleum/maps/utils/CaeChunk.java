package caeruleum.maps.utils;

public class CaeChunk {
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

    public CaeChunk (int x, int y, int width, int height){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getMidX() {
        return x + (width / 2);
    }

    public int getMidY() {
        return y + (height / 2);
    }
}
