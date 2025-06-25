package caeruleum.world.blocks.storage;

import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.gen.Building;
import mindustry.ui.Bar;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.meta.Attribute;

import static mindustry.Vars.state;

import arc.Core;
import arc.func.Func;
import arc.math.Mathf;
import arc.util.Strings;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.graphics.Pal;
import mindustry.core.UI;

public class PowerCore extends CoreBlock {

  public float powerProduction = 30;
  public float effectChance =  0.65f;
  public Effect steamEffect = Fx.smoke;

  public PowerCore(String name){
    super(name);
        hasPower = true;
        hasLiquids = false;
        consumesPower = false;
        outputsPower = true;
        update = true;
        solid = true;
  };
    @Override
    public void setBars(){
    super.setBars();
    addBar("power", makePowerBalance());
    addBar("batteries", makeBatteryBalance());

        if(hasPower && outputsPower){
            addBar("power", (PowerCoreBuild entity) -> new Bar(() -> 
            Core.bundle.format("bar.poweroutput",
            Strings.fixed(entity.getPowerProduction() * 60 * entity.timeScale(), 1)),
            () -> Pal.powerBar,
            () -> entity.productionEfficiency));
        }
    }
    public static Func<Building, Bar> makePowerBalance(){
    return entity -> new Bar(() ->
        Core.bundle.format("bar.powerbalance",
            ((entity.power.graph.getPowerBalance() >= 0 ? "+" : "") + UI.formatAmount((long)(entity.power.graph.getPowerBalance() * 60)))),
            () -> Pal.powerBar,
            () -> Mathf.clamp(entity.power.graph.getLastPowerProduced() / entity.power.graph.getLastPowerNeeded())
        );
    }

    public static Func<Building, Bar> makeBatteryBalance(){
    return entity -> new Bar(() ->
        Core.bundle.format("bar.powerstored",
            (UI.formatAmount((long)entity.power.graph.getLastPowerStored())), UI.formatAmount((long)entity.power.graph.getLastCapacity())),
            () -> Pal.powerBar,
            () -> Mathf.clamp(entity.power.graph.getLastPowerStored() / entity.power.graph.getLastCapacity())
        );
    }

  public class PowerCoreBuild extends CoreBuild {
        public float productionEfficiency = 1, totalProgress = 0;
        
        @Override
        public void updateTile(){
            productionEfficiency = enabled ?
            state.rules.solarMultiplier * Mathf.maxZero(Attribute.light.env() +
                (state.rules.lighting ?
                    1f - state.rules.ambientLight.a :
                    1f
            )) : 0f;
        } 

        public float generateTime;
            /** The efficiency of the producer. An efficiency of 1.0 means 100% */
        
        @Override
        public float warmup(){
            return productionEfficiency;
        }

        @Override
        public void drawLight(){
            super.drawLight();
        }

        @Override
        public float ambientVolume(){
            return Mathf.clamp(productionEfficiency);
        }

        @Override
        public float getPowerProduction(){
            return powerProduction * productionEfficiency;
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(productionEfficiency);
            write.f(generateTime);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            productionEfficiency = read.f();
            if(revision >= 1){
                generateTime = read.f();
            }
        }
    }
};
