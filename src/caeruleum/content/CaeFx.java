package caeruleum.content;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import mindustry.entities.*;
import mindustry.graphics.*;
import caeruleum.utils.*;
import static arc.graphics.g2d.Draw.*;
import static arc.graphics.g2d.Lines.*;
import static arc.math.Angles.*;
import static mindustry.Vars.*;

public class CaeFx {
	private static final Rand rand = new Rand();
  private static final Vec2 v1 = new Vec2(), v2 = new Vec2();

  public static final Effect
	
  fakeLightning = new Effect(10f, 500f, e -> {
    if(!(e.data instanceof LightningData d)) return;
    float tx = d.pos.getX(), ty = d.pos.getY(), dst = Mathf.dst(e.x, e.y, tx, ty);
    v1.set(d.pos).sub(e.x, e.y).nor();

    float normx = v1.x, normy = v1.y;
    float range = 6f;
    int links = Mathf.ceil(dst / range);
    float spacing = dst / links;

    stroke(d.stroke * e.fout());
    color(Color.white, e.color, e.fin());

    beginLine();

    linePoint(e.x, e.y);

    rand.setSeed(e.id);

    for(int i = 0; i < links; i++){
      float nx, ny;
      if(i == links - 1){
        nx = tx;
        ny = ty;
      }else{
        float len = (i + 1) * spacing;
        v1.setToRandomDirection(rand).scl(range/2f);
        nx = e.x + normx * len + v1.x;
        ny = e.y + normy * len + v1.y;
      }

      linePoint(nx, ny);
    }

    endLine();
  }).followParent(false).layer(Layer.bullet + 0.01f),
    
    //[length, width, team]
  fakeLightningFast = new Effect(5f, 500f, e -> {
    Object[] data = (Object[])e.data;

    float length = (float)data[0];
    int tileLength = Mathf.round(length / tilesize);
        
    stroke((float)data[1] * e.fout());
    color(e.color, Color.white, e.fin());
        
    for(int i = 0; i < tileLength; i++){
      float offsetXA = i == 0 ? 0f : Mathf.randomSeed(e.id + (i * 6413), -4.5f, 4.5f);
      float offsetYA = (length / tileLength) * i;
            
      int f = i + 1;
            
      float offsetXB = f == tileLength ? 0f : Mathf.randomSeed(e.id + (f * 6413), -4.5f, 4.5f);
      float offsetYB = (length / tileLength) * f;
            
      v1.trns(e.rotation, offsetYA, offsetXA);
      v1.add(e.x, e.y);
            
      v2.trns(e.rotation, offsetYB, offsetXB);
      v2.add(e.x, e.y);
            
      line(v1.x, v1.y, v2.x, v2.y, false);
      Fill.circle(v1.x, v1.y, getStroke() / 2f);
      Drawf.light(v1.x, v1.y, v2.x, v2.y, (float)data[1] * 3f, e.color, 0.4f);
    }
    Fill.circle(v2.x, v2.y, getStroke() / 2);
  }).layer(Layer.bullet + 0.01f),
	
	flameBurst = new Effect(40, e -> {
	  CaeDraw.splashCircle(e.x, e.y, e.fout() * 2.5f, Pal.lightPyraFlame, Pal.darkFlame, e.fin(), e.id, 3, 2f + e.fin() * 9f, e.rotation, 360f);
	}),
	
	blockSplash = new Effect(40, e -> {
	  //e.rotation = block size soo h;
	  stroke((2 + e.rotation) * e.fout(), e.color);
    square(e.x, e.y, ((tilesize * e.rotation) / 2) * e.fin());
	}),

  spark = new Effect(40, e -> {
    color(Pal.lancerLaser);
    CaeDraw.splashLine(e.x, e.y, 4 * e.fout(), 3 * e.fin(), e.id, 4, e.finpow() * 16, e.rotation, 45);
  }),

  healWave = new Effect(22, e -> {
    //e.rotation is size
    CaeDraw.lineCircle(e.x, e.y, e.color, e.fout() * 3, 4 + e.finpow() * e.rotation);
  }),

  critTrail = new Effect(20, e -> {
    color(Pal.heal);
    randLenVectors(e.id, 3, 1 + e.fin() * 3, (x, y) -> {
      Fill.square(e.x + x, e.y + y, e.fout() * 3.3f + 0.5f);
    });
  }),

  swordSpawnFx = new Effect(20, e -> {
    color(Pal.heal);
    stroke(4 * (1 - e.finpow()));
    circle(e.x, e.y, 8 * e.finpow());
  }),
 
  orbExplode = new Effect(45, e -> {
    color(Pal.lancerLaser);
    CaeDraw.splashLine(e.x, e.y, 10f * e.fout(), 6f * e.fout(), e.id, 20, e.finpow() * (tilesize * 4f), e.rotation, 360f);
    CaeDraw.lineCircle(e.x, e.y, 4f * e.finpow(), (tilesize * 3f) * e.finpow());
  }),

  boom = new Effect(30, e -> {
    color(Pal.sapBullet, Pal.sapBulletBack, e.fin());
    CaeDraw.splashCircle(e.x, e.y, 5 * e.fslope(), e.id, 15, e.finpow() * (8 * 5), e.rotation, 360);
    CaeDraw.lineCircle(e.x, e.y, Pal.sapBullet, 4 * e.fout(), (4 * 8) * e.fin());
    CaeDraw.splashLine(e.x, e.y, Pal.sapBullet, Color.valueOf("b28768ff"), e.fin(), 4 * e.fout(), 6 * e.fout(), e.id, 15, e.finpow() * (8 * 5), e.rotation, 360);
  }),

  bigBoom = new Effect(30, e -> {
    color(Pal.sapBullet, Pal.sapBulletBack, e.fin());
    CaeDraw.splashCircle(e.x, e.y, 5 * e.fslope(), e.id, 20, e.finpow() * (8 * 10), e.rotation, 360);
    CaeDraw.lineCircle(e.x, e.y, 4 * e.fout(), (6 * 7) * e.finpow());
    CaeDraw.lineCircle(e.x, e.y, 6 * e.fout(), (6 * 11) * e.finpow());
    CaeDraw.splashLine(e.x, e.y, Pal.sapBullet, Color.valueOf("b28768ff"), e.fin(), 4 * e.fout(), 6 * e.fout(), e.id, 20, e.finpow() * (8 * 10), e.rotation, 360);
  }),

  laserCharge = new Effect(80, e -> {
    color(Pal.sapBullet, Pal.sapBulletBack, e.fin());
    CaeDraw.splashCircle(e.x, e.y, 5 * e.fslope(), e.id, 20, (1 - e.finpow()) * (8 * 6), e.rotation, 360);
    CaeDraw.lineCircle(e.x, e.y, 4 * e.fin(), (6 * 7) * (1 - e.finpow()));
    CaeDraw.lineCircle(e.x, e.y, 4 * e.fin(), (6 * 11) * (1 - e.finpow()));
    color(Pal.sapBullet);
    Fill.circle(e.x, e.y, 10 * e.fin());
    color(Color.white);
    Fill.circle(e.x, e.y, 8 * e.fin());
  }),

  earthDust = new Effect(20, e -> {
    color(e.color);
    CaeDraw.splashCircle(e.x, e.y, 2.5f * e.fslope(), e.id, 10, e.finpow() * 10, e.rotation, 360);
  }).layer(Layer.debris),

  earthDustII = new Effect(30, e -> {
    color(e.color);
    CaeDraw.splashCircle(e.x, e.y, 5 * e.fslope(), e.id, 20, e.finpow() * 20, e.rotation, 360);
  }).layer(Layer.debris);

	public static class LightningData{
    Position pos;
    float stroke;

    public LightningData(Position pos, float stroke){
      this.pos = pos;
      this.stroke = stroke;
    };
  };
};
