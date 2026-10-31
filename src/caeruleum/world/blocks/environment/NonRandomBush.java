package caeruleum.world.blocks.environment;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Prop;

public class NonRandomBush extends Prop {

   public NonRandomBush(String name) {
      super(name);
      this.obstructsLight = false;
   }

   public void drawBase(Tile tile) {
      TextureRegion region = this.variants > 0 ? this.variantRegions[Mathf.randomSeed((long)tile.pos(), 0, Math.max(0, this.variantRegions.length - 1))] : this.region;
      float x = tile.worldx(), y = tile.worldy();

      float w = (float)region.width * region.scl();
      float h = (float)region.height * region.scl();
      float scl = 30.0F;
      float mag = 0.3F;
      Draw.rectv(region, x, y, w, h, (vec) -> vec.add(Mathf.sin(vec.y * 3.0F + Time.time, scl, mag) + Mathf.sin(vec.x * 3.0F - Time.time, 70.0F, 0.8F), Mathf.cos(vec.x * 3.0F + Time.time + 8.0F, scl + 6.0F, mag * 1.1F) + Mathf.sin(vec.y * 3.0F - Time.time, 50.0F, 0.2F)));
   }
}
