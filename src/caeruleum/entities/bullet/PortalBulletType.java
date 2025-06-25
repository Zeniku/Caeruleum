package caeruleum.entities.bullet;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.math.*;
import arc.math.geom.Vec2;
import arc.util.*;
import mindustry.content.Bullets;
import mindustry.entities.*;
import mindustry.entities.Units.Sortf;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.world.blocks.defense.turrets.Turret;


/* could just check if a unit or a turret and 
 * check where theyre aiming but will not cause im dumb
 * wip made a bullet shoot a bullet aaaa  
*/
public class PortalBulletType extends BasicBulletType{
	public BulletType bulletT = Bullets.placeholder;
	public float growTime = 10f, fadeTime = 10f;
	public float shootTime = 30f;
	public float radius = 16f;
	public float rotateSpeed = 4f; //why you ask?, i dont want it instant
	public float range = 8f * 20f;
	public float inaccuracy = 3f, spread = 1f;
	public float offsetX = 0f, offsetY = 0f;
	public float warmupTime = 0f;
	public float falloutTime = 170f;
	public int shots = 1;
	public boolean continuous = false;
	public boolean followParentAim = false;
	public Sortf unitSort = UnitSorts.closest;

	
  public PortalBulletType(float speed, float damage, String sprite){
    super(speed, damage, sprite);
    spin = 5f;
  };

	public PortalBulletType(float speed, float damage){
		this(speed, damage, "bullet");
	}

	public PortalBulletType(){
		this(0f, 1f);
	}

	@Override
	public void init(Bullet b){
	  b.data = new PortalBulletData();
		if(b.data instanceof PortalBulletData data) data.rotation = b.rotation();
		super.init(b);
	}

	public void targetPosition(Bullet b, Posc pos){
    if(pos == null) return;
		var offset = Tmp.v1.setZero();

		if(b.data instanceof PortalBulletData data){
		  data.targetPos.set(Predict.intercept(b, pos, offset.x, offset.y, bulletT.speed <= 0.01f ? 99999999f : bulletT.speed));
 
			if(data.targetPos.isZero()){
			  data.targetPos.set(pos);
			}
		}
	}

	public void turnToTarget(Bullet b, float targetRot){
	  if(b.data instanceof PortalBulletData data){
			//dont want to rotate the bullet it will be h
      data.rotation = Angles.moveToward(data.rotation, targetRot, rotateSpeed * Time.delta);
		}
	}

  protected void findTarget(Bullet b){
	  if(!(b.data instanceof PortalBulletData data)) return;
		if(collidesAir && !collidesGround){
			data.target = Units.bestEnemy(b.team, b.x, b.y, range, e -> !e.dead() && !e.isGrounded(), unitSort);
		}else{
			data.target = Units.bestTarget(b.team, b.x, b.y, range, e -> !e.dead() && (e.isGrounded() || collidesAir) && (!e.isGrounded() || collidesGround), g -> collidesGround, unitSort);

			if(data.target == null && healPercent != 0){
					data.target = Units.findAllyTile(b.team, b.x, b.y, range, t -> t.damaged());
			}
		}
	}

	public void parentAim(Bullet b){
		if(!(b.data instanceof PortalBulletData data)) return;
		if(b.owner == null) return;

    float tx = 0, ty = 0;
    if(b.owner instanceof Unit u){
      tx = u.aimX;
      ty = u.aimY;
    };
    if(b.owner instanceof Turret.TurretBuild t){
      tx = t.targetPos.x;
      ty = t.targetPos.y;
    };
		if(b.owner instanceof Bullet bb && bb.type instanceof PortalBulletType){
			if(bb.data instanceof PortalBulletData bbdata){

        tx = bbdata.targetPos.x;
        ty = bbdata.targetPos.y;
			}
		}

		data.targetPos.set(tx, ty);
	}

	@Override
	public void update(Bullet b){
		if(!(b.data instanceof PortalBulletData data)) return;
		if(followParentAim){
			parentAim(b);
		}else{
			findTarget(b);
			targetPosition(b, data.target);
		};

		data.warmup = (warmupTime == 0f)? 0f : Math.min(data.warmup + Time.delta, warmupTime);

    if(Float.isNaN(data.rotation)) data.rotation = b.rotation();

		turnToTarget(b, b.angleTo(data.targetPos));
		if (continuous){
			handleContinuous(b, data); //cannot handle multiple bullet for now 
		}else{
			if(b.timer(0, shootTime) && data.warmup >= warmupTime){
				for (int i = 0; i < shots; i++) {
					bulletT.create(b, b.team, b.x, b.y, data.rotation + Mathf.range(inaccuracy + bulletT.inaccuracy) + (i - (int)(shots / 2f)) * spread , 1f);
				}
			}
		}
	}
	public void handleContinuous(Bullet b, PortalBulletData data){

		if(data.bullet != null){
			data.fallout = Math.min(data.fallout + Time.delta, falloutTime);

			float bx = b.x + Angles.trnsx(data.rotation, offsetX);
			float by = b.y + Angles.trnsy(data.rotation, offsetY);
			data.bullet.set(bx, by);
			data.bullet.rotation(data.rotation);
			data.bullet.time = 0f;
			
			if(data.fallout >= falloutTime){
				data.bullet = null;
				data.fallout = 0;
				data.warmup = 0; // rest warmup cause it updates outside continuous
			}
		}
		if(data.bullet == null && data.warmup >= warmupTime){
			data.bullet = bulletT.create(b, b.x, b.y, data.rotation);
			data.warmup = 0f; // dont need this but for good measure
		}
	} 
	@Override
	public void draw(Bullet b){
		float fin = Mathf.curve(b.fin(), 0, growTime / b.lifetime);
		float fout = 1 - Mathf.curve(b.fin(), (b.lifetime - fadeTime) / b.lifetime, 1);
		float size = fin * fout * radius;
		float offset = -90 + (spin != 0 ? Mathf.randomSeed(b.id, 360) + b.time * spin : 0);
		
		Color mix = Tmp.c1.set(mixColorFrom).lerp(mixColorTo, b.fin());

		Draw.mixcol(mix, mix.a);

		Draw.color(backColor);
		Draw.rect(backRegion, b.x, b.y, size, size, b.rotation() + offset);
		Draw.color(frontColor);
		Draw.rect(frontRegion, b.x, b.y, size, size, b.rotation() + offset);

		Draw.reset();
	}

	public class PortalBulletData {
		public Vec2 targetPos = new Vec2();
		public float rotation = 0f;
		public float offsetX = 0f, offsetY = 0f;
		public float warmup = 0f, fallout = 0f;
		public @Nullable Posc target;
		public @Nullable Bullet bullet = null;
		
		public PortalBulletData(){}

	}
}
