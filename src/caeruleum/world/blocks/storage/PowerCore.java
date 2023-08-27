package caeruleum.world.blocks.storage;

import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.world.blocks.storage.CoreBlock;
import arc.math.Mathf;
import arc.util.Time;

public class PowerCore extends CoreBlock {

  public float powerProduction = 5;
  public float effectChance =  0.65f;
  public float steamgenWarmdown = 0.002f;
  public float heatLoss = 0.001f;
  public Effect steamEffect = Fx.smoke;

  public PowerCore(String name){
    super(name);
        hasPower = true;
        hasLiquids = true;
        consumesPower = false;
        outputsPower = true;
  };

  public class PowerCoreBuild extends CoreBuild {
    public float steamgenWarmup = 0, productionEfficiency = 0, totalProgress = 0;
    
    @Override
    public void updateTile(){
      super.updateTile();
      steamgenWarmup = Mathf.clamp(steamgenWarmup - steamgenWarmdown * Time.delta);
      productionEfficiency = Mathf.lerpDelta(Mathf.clamp(productionEfficiency - heatLoss * Time.delta), 1, steamgenWarmup);
      totalProgress += productionEfficiency;

      if(Mathf.randomBoolean(productionEfficiency * effectChance)) steamEffect.at(x, y);
    };

    @Override
    public float getPowerProduction() {
        return powerProduction * productionEfficiency;
    }
  }
};
