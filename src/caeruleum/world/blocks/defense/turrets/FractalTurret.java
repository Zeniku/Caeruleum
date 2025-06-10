package caeruleum.world.blocks.defense.turrets;

import arc.util.*;
import arc.math.*;
import arc.math.geom.*;
import mindustry.entities.Effect;
import mindustry.entities.Mover;
import mindustry.entities.bullet.*;

public class FractalTurret extends DisabledPredictTurret{
  
  public FractalTurret(String name){
    super(name);
  }
  
  public class FractalTurretBuild extends DisabledPredictTurretBuild{
   
    protected void bullet(BulletType type, float xOffset, float yOffset, float angleOffset, Mover mover){
      queuedBullets --;

      if(dead || (!consumeAmmoOnce && !hasAmmo())) return;

      float
      xSpread = Mathf.range(xRand),
      bulletX = x + Angles.trnsx(rotation - 90, shootX + xOffset + xSpread, shootY + yOffset),
      bulletY = y + Angles.trnsy(rotation - 90, shootX + xOffset + xSpread, shootY + yOffset),
      shootAngle = rotation + angleOffset + Mathf.range(inaccuracy + type.inaccuracy);

      float lifeScl = type.scaleLife ? Mathf.clamp(Mathf.dst(bulletX, bulletY, targetPos.x, targetPos.y) / type.range, minRange / type.range, range() / type.range) : 1f;

      //TODO aimX / aimY for multi shot turrets?

      handleBullet(type.create(this, team, bulletX, bulletY, shootAngle, -1f, (1f - velocityRnd) + Mathf.random(velocityRnd), lifeScl, null, mover, targetPos.x, targetPos.y), xOffset, yOffset, shootAngle - rotation);

      (shootEffect == null ? type.shootEffect : shootEffect).at(bulletX, bulletY, rotation + angleOffset, type.hitColor);
      (smokeEffect == null ? type.smokeEffect : smokeEffect).at(bulletX, bulletY, rotation + angleOffset, type.hitColor);
      shootSound.at(bulletX, bulletY, Mathf.random(soundPitchMin, soundPitchMax));

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
