package caeruleum.content;

import arc.graphics.Color;
import mindustry.content.*;
import mindustry.world.Block;
import mindustry.world.blocks.environment.*;
import mindustry.graphics.CacheLayer;

public class CaeBlocks { 
    public static Block 
    bluonixite, bluonixiteWall, bluonixiteBoulder, bluonixiteWater, lazurigrass, lazurigrassWall, 
    
    aquafluent, deepAquafluent,
    bush, blueFlower, blueTendrils, 
    caeruleumOre, rubrariumOre, virideaurumOre;

    public static void load(){
        bluonixite = new Floor("bluonixite-stone"){{
            variants = 4;
        }};
        bluonixiteWall = new StaticWall("bluonixite-wall"){{
            bluonixite.asFloor().wall = this;
            variants = 3;
            mapColor = Color.valueOf("43434F");
        }};
        bluonixiteBoulder = new Prop("bluonixite-boulder"){{
            variants = 2;
            bluonixite.asFloor().decoration = this;
        }};
        aquafluent = new Floor("aquafluent-water"){{
            speedMultiplier = 0.5f;
            variants = 0;
            status = StatusEffects.wet;
            statusDuration = 90f;
            liquidDrop = Liquids.water;
            isLiquid = true;
            cacheLayer = CacheLayer.water;
            albedo = 0.9f;
        }};
        deepAquafluent = new Floor("aquafluent-deep-water"){{
            speedMultiplier = 0.2f;
            variants = 0;
            liquidDrop = Liquids.water;
            liquidMultiplier = 1.5f;
            isLiquid = true;
            status = StatusEffects.wet;
            statusDuration = 120f;
            drownTime = 200f;
            cacheLayer = CacheLayer.water;
            albedo = 0.9f;
            supportsOverlay = true;        
        }};
        bluonixiteWater = new ShallowLiquid("bluonixite-aquafluent-water"){{ 
            supportsOverlay = true;
            speedMultiplier = 0.8f;
            statusDuration = 50f;
            albedo = 0.9f;
            supportsOverlay = true;
            mapColor = Color.valueOf("2A2E43");
        }};

        ((ShallowLiquid)bluonixiteWater).set(aquafluent, bluonixite);

        lazurigrass = new Floor("lazurigrass"){{
            variants = 4;
        }};
        blueTendrils = new OverlayFloor("blue-tendrils"){{
            variants = 3;
        }};
        blueFlower = new Seaweed("blue-flower"){{
            variants = 3;
            lazurigrass.asFloor().decoration = this;
        }};
        bush = new SeaBush("bluBush"){{
            variants = 3;
        }};
        lazurigrassWall = new StaticWall("lazurigrass-wall"){{
            lazurigrass.asFloor().wall = this;
            variants = 3;
        }};
        caeruleumOre = new OreBlock("ore-caeruleum", CaeItems.caeruleum){{
            oreThreshold = 0.864f;
            oreScale = 24.904762f;
        }};
        rubrariumOre = new OreBlock("ore-rubrarium", CaeItems.rubrarium){{
            oreThreshold = 0.864f;
            oreScale = 24.004762f;
        }};
        virideaurumOre = new OreBlock("ore-virideaurum", CaeItems.virideaurum){{
            oreThreshold = 0.853f;
            oreScale = 24.004762f;
        }};
    }
}
