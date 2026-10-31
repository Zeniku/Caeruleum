package caeruleum.world.blocks.environment;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.Vars;
import mindustry.graphics.Layer;
import mindustry.world.Block;
import mindustry.world.Tile;
import arc.Core;
import arc.math.geom.Vec2;

public class CaeTreeBlock extends Block {
   public float shadowOffset = -4.0F;
   public TextureRegion shadow;

   public CaeTreeBlock(String name) {
      super(name);
      this.solid = true;
      this.clipSize = 90.0F;
      this.customShadow = true;
   }

    public float parallax = 0.05f;
    public float shadowParallax = 0.2f;   // multiplier of parallax
    public float canopyParallax = 2f;     // multiplier of parallax
    public float depthScale = 0.5f;       // perspective zoom strength
    public TextureRegion canopyRegion;    // optional upper layer

    
    // fade settings
    public boolean fadeEnabled = true;
    public boolean fadeToMouse = false;  // fade near cursor instead of player
    public float fadeStart = 55f;        // world units: fully visible beyond this
    public float fadeEnd = 15f;          // world units: min opacity inside this
    public float fadeOpacity = 0.2f;     // minimum alpha (0-1)
    public float fadeDistMulti = 2f;     // scale both distances (use for big sprites)
    public TextureRegion[] canopyRegions;

    @Override 
    public void load(){
        super.load();
        this.canopyRegion = Core.atlas.find(this.name + "-canopy");
        
      if (this.variants != 0) {
        this.canopyRegions = new TextureRegion[this.variants];
        for(int i = 0; i < this.variants; ++i) {
            this.canopyRegions[i] = Core.atlas.find(this.name + "canopy" + (i + 1));
         }
        this.canopyRegion = this.canopyRegions[0];
      }

    }

    @Override
    public void drawBase(Tile tile) {
        float x = tile.worldx();
        float y = tile.worldy();

        // Copy MineDusty
        float fade = 1f;
        if (fadeEnabled && Vars.player != null && Vars.player.unit() != null && !Vars.player.unit().dead()) {
            float dst;
            if (fadeToMouse) {
                Vec2 m = Core.input.mouseWorld(Core.input.mouseX(), Core.input.mouseY());
                dst = Mathf.dst(m.x, m.y, x, y);
            } else {
                dst = Mathf.dst(Vars.player.unit().x, Vars.player.unit().y, x, y);
            }
            float end = fadeEnd * fadeDistMulti;
            float start = fadeStart * fadeDistMulti;
            fade = Mathf.clamp((dst - end) / (start - end), fadeOpacity, 1f);
        }

        // --- parallax ---
        // will probably make a global layer for this
        float zoomFactor = Mathf.clamp((Vars.tilesize * 40f) / Core.camera.width, 0.5f, 1.5f);
        float p = parallax * zoomFactor;
        float dx = (x - Core.camera.position.x) * p;
        float dy = (y - Core.camera.position.y) * p;

        float w = region.width * region.scl();
        float h = region.height * region.scl();
        float scl = 30f, mag = 0.2f;
        float baseScale = 1f + p * depthScale;
        int regionIndex = Mathf.randomSeed((long)tile.pos(), 0, Math.max(0, variantRegions.length - 1));
        // shadow: never faded
        TextureRegion shad = variants == 0 ? customShadowRegion
            : variantShadowRegions[regionIndex];
        if (shad.found()) {
            Draw.z(69f);
            Draw.alpha(1f);
            Draw.rect(shad,
                x + shadowOffset + dx * shadowParallax,
                y + shadowOffset + dy * shadowParallax
            );
        }

        // main sprite: faded
        TextureRegion reg = variants == 0 ? region
            : variantRegions[regionIndex];
        Draw.z(Layer.darkness + 0.01f);
        Draw.alpha(fade);
        Draw.rectv(reg, x + dx, y + dy, w * baseScale, h * baseScale, vec -> vec.add(
            Mathf.sin(vec.y * 3f + Time.time, scl, mag) + Mathf.sin(vec.x * 3f - Time.time, 70f, 0.8f),
            Mathf.cos(vec.x * 3f + Time.time + 8f, scl + 6f, mag * 1.1f) + Mathf.sin(vec.y * 3f - Time.time, 50f, 0.2f)));

        // canopy: faded
        TextureRegion canopyReg = variants == 0 ? region 
            : canopyRegions[regionIndex];

        if (canopyReg != null && canopyReg.found()) {
            float canopyScale = 1f + p * depthScale * canopyParallax;
            Draw.z(72f);
            Draw.alpha(fade);
            Draw.rectv(canopyRegion,
                x + dx * canopyParallax, y + dy * canopyParallax,
                canopyRegion.width * canopyRegion.scl() * canopyScale,
                canopyRegion.height * canopyRegion.scl() * canopyScale, vec -> vec.add(
                    Mathf.sin(vec.y * 3f + Time.time, scl, mag * 1.5f),
                    Mathf.cos(vec.x * 3f + Time.time + 8f, scl + 6f, mag * 1.5f)));
        }

        // reset so other draws aren't affected
        Draw.color();
        Draw.alpha(1f);
    }

    public void drawShadow(Tile tile) {
    }
}
