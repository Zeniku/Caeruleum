package caeruleum.world.blocks.defense;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.Time;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import caeruleum.utils.*;

import static mindustry.Vars.*;

public class StatusEffectProjector extends Block{
	
	public Color starColor = Pal.lightPyraFlame;
	public float reloadTime = 60f * 3.5f;
  public float range = 160f;
  public float healPercent = 5f;
  public float damage = 20f;
  public StatusEffect allyStatus = StatusEffects.none;
  public StatusEffect enemiesStatus = StatusEffects.burning;
  public Effect statusFxAlly = Fx.none;
  public Effect statusFxEnemies = Fx.none;
  public Effect healEffect = Fx.none;
  public boolean enableAFxAura = false;
  public boolean enableEFxAura = true;
  public boolean enableHealing = true;
  
  public StatusEffectProjector(String name){
    super(name);
    solid = true;
    update = true;
    group = BlockGroup.projectors;
    hasPower = true;
  };
	
  @Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		super.drawPlace(x, y, rotation, valid);
	  Drawf.dashCircle(x * tilesize, y * tilesize, range, Pal.accent);
	};
	
	@Override
  public void setStats(){
    super.setStats();
    stats.add(Stat.range, range);
    stats.add(Stat.reload, reloadTime / 60f, StatUnit.perSecond);
  };
	
  public class StatusEffectProjectorBuild extends Building implements Ranged{
    protected float aTime = 0f;
    protected boolean wasHealed, appliedEnemies, appliedAlly = false;
		
    @Override
    public float range(){
      return range;
    }
    
    @Override
    public void updateTile(){

			aTime = Math.min(aTime + edelta(), reloadTime);

			  if(aTime < reloadTime) return;
					wasHealed = false; 
					appliedAlly = false;

			    Units.nearby(team, x, y, range(), a -> {
			      if(enableHealing && a.damaged()){
			          a.heal(healPercent);
			          Fx.heal.at(a);
			          wasHealed = true;
			      };

			      if(allyStatus != StatusEffects.none){
			        if(enableAFxAura && statusFxAlly != Fx.none) statusFxAlly.at(a);
			        a.apply(allyStatus, 60f);
			        appliedAlly = true;
			      };
			    });

					if(wasHealed && healEffect != Fx.none){
							healEffect.at(x, y, range, Pal.heal);
					};

					appliedEnemies = false;

			    CaeFunc.radiusEnemies(team, x, y, range(), e -> {
			      e.apply(enemiesStatus, 60f);
			      if(statusFxEnemies != Fx.none && enableEFxAura) statusFxEnemies.at(e);
			      e.damage(damage);
			      appliedEnemies = true;
			    });

					if(appliedEnemies || appliedAlly) consume();
					aTime = 0f;

    };
		
    @Override
		public void draw(){
			super.draw();
			if(efficiency() < 0) return; 
			  Draw.z(Layer.effect - 0.01f);
				CaeDraw.spike(x, y, starColor, 2f * 2.9f + Mathf.absin(Time.time, 5f, 1f) + Mathf.random(0.1f), Time.time *  2f);
				CaeDraw.spike(x, y, Color.white, 2f * 1.9f + Mathf.absin(Time.time, 5f, 1f) + Mathf.random(0.1f),  Time.time * 2f);
		}
		
		@Override
		public void drawSelect(){
			Drawf.dashCircle(x, y, range(), Pal.accent);
		}
  }
}
