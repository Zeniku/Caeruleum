package caeruleum.entities.bullet;

import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.util.Tmp;
import mindustry.content.Fx;
import mindustry.entities.bullet.ShrapnelBulletType;
import mindustry.gen.Bullet;
import mindustry.graphics.Drawf;

public class SpikeBulletType extends ShrapnelBulletType{

public SpikeBulletType(){
	lifetime = 120f;
		absorbable = false;
		reflectable = false;
		hittable = false;
		collides = false;
		collidesGround = true;
		collidesAir = false;
		collidesTiles = true;
		hitEffect = Fx.none;
		despawnEffect = Fx.none;
		shootEffect = Fx.none;
		smokeEffect = Fx.none;
	}
	
  @Override
	public void drawLight(Bullet b){
		//nothing
	};
	public void draw(Bullet b){
		float realLength = b.fdata, rot = b.rotation();
		
        Draw.color(fromColor, toColor, b.fin());
        for(int i = 0; i < (int)(serrations * realLength / length); i++){
            Tmp.v1.trns(rot, i * serrationSpacing);
            float sl = Mathf.clamp(b.fout() - serrationFadeOffset) * (serrationSpaceOffset - i * serrationLenScl);
            Drawf.tri(b.x + Tmp.v1.x, b.y + Tmp.v1.y, serrationWidth * b.finpow(), sl, b.rotation() + 45);
            Drawf.tri(b.x + Tmp.v1.x, b.y + Tmp.v1.y, serrationWidth * b.finpow(), sl, b.rotation() - 45);
        }
        Drawf.tri(b.x, b.y, width * b.finpow(), (realLength + 50) * b.finpow(), b.rotation());
        Drawf.tri(b.x, b.y, width * b.finpow(), 10f * b.finpow(), b.rotation() + 180f);
        Draw.reset();
	};
};
