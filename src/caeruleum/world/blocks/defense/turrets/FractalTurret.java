package caeruleum.world.blocks.defense.turrets;

import arc.math.*;
import arc.math.geom.Vec2;
import arc.util.Tmp;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.Mover;
import mindustry.entities.bullet.*;
import mindustry.gen.Bullet;

public class FractalTurret extends DisabledPredictTurret{
  
  public FractalTurret(String name){
    super(name);
    shootEffect = Fx.none;
    smokeEffect = Fx.none;
  }
  
  public class FractalTurretBuild extends DisabledPredictTurretBuild{
   
    protected void bullet(BulletType type, float xOffset, float yOffset, float angleOffset, Mover mover){
      queuedBullets --;

      if(dead || (!consumeAmmoOnce && !hasAmmo())) return;

      Vec2 tp = targetPos;

      Tmp.v1.set(tp);
      if(Mathf.dst(x, y, tp.x, tp.y) > range) Tmp.v1.set(tp).sub(x, y).clamp(-range, range).add(x, y);
      
      float hx = Tmp.v1.x + Angles.trnsx(Mathf.random(360), Mathf.random(range * 0.5f));
      float hy = Tmp.v1.y + Angles.trnsy(Mathf.random(360), Mathf.random(range * 0.5f));
      
      float ang = Angles.angle(hx, hy, tp.x, tp.y);
      float lifeScl = type.scaleLife ? Mathf.clamp(Mathf.dst(hx, hy, tp.x, tp.y) / type.range, minRange / type.range, range / type.range) : 1f;
      
      Bullet h = type.create(this, team, hx, hy, ang, lifeScl);
      handleBullet(h, xOffset, yOffset, rotation);

      (shootEffect == null ? type.shootEffect : shootEffect).at(hx, hy, rotation + angleOffset, type.hitColor);
      (smokeEffect == null ? type.smokeEffect : smokeEffect).at(hx, hy, rotation + angleOffset, type.hitColor);
      shootSound.at(hy, hx, Mathf.random(soundPitchMin, soundPitchMax));

      ammoUseEffect.at(
          x - Angles.trnsx(rotation, ammoEjectBack),
          y - Angles.trnsy(rotation, ammoEjectBack),
          rotation * Mathf.sign(xOffset)
      );

      if(shake > 0){
          Effect.shake(shake, shake, this);
      }

      curRecoil = 1f;
      if(recoils > 0){
          curRecoils[barrelCounter % recoils] = 1f;
      }
      heat = 1f;
      totalShots++;

      if(!consumeAmmoOnce){
          useAmmo();
      }
    };
  }
}
