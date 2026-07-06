package caeruleum.type;

import mindustry.entities.units.StatusEntry;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.meta.*;
import caeruleum.world.meta.*;

public class ShieldStatusEffect extends StatusEffect{
  public float shieldAmount = 20f;
  public float maxShield = 250f;
  public boolean remove = false;

  public ShieldStatusEffect(String name){
    super(name); 
  }

  @Override
  public void setStats(){
    super.setStats();
    stats.add(Stat.shieldHealth, CaeStatValues.shieldListVal(this));
  }

  @Override
  public boolean isHidden(){
    return false;
  }

  @Override
  public void update(Unit unit, StatusEntry entry){
    super.update(unit, entry);
    if(unit.shield < maxShield){
      if(!remove){
        unit.shield = Math.min(unit.shield + (shieldAmount / 60), maxShield);
      }else{
        unit.shield = Math.max(unit.shield - (shieldAmount / 60), 0);
      }
    }
  }
}
