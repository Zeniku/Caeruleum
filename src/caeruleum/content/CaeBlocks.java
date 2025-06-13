package caeruleum.content;

import arc.graphics.Color;
import caeruleum.world.blocks.defense.*;
import caeruleum.world.blocks.storage.*;
import caeruleum.world.blocks.defense.turrets.*;
import mindustry.content.*;
import mindustry.entities.pattern.ShootAlternate;
import mindustry.entities.pattern.ShootPattern;
import mindustry.entities.pattern.ShootSummon;
import mindustry.game.Team;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.draw.DrawFrames;
import mindustry.graphics.CacheLayer;
import mindustry.type.Category;
import mindustry.type.ItemStack;

import static mindustry.type.ItemStack.*;

public class CaeBlocks { 
    public static Block 
    bluonixite, bluonixiteWall, bluonixiteBoulder, bluonixiteWater, lazurigrass, lazurigrassWall, 
    
    aquafluent, deepAquafluent,
    bush, blueFlower, blueTendrils, 
    caeruleumOre, rubrariumOre, virideaurumOre,
    //walls
    lonsdaleiteWall, lonsdaleiteWallLarge,
    //turrets
    heavenlyStrike,	praefector,
    //defense
    statusEffectProjector, tesla,
    //Production
    lonsdaleiteCompressor,

    miniCore;

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
        bush = new Seaweed("bluBush"){{
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
        
        statusEffectProjector = new StatusEffectProjector("statusEffectProjector"){{
            statusFxEnemies = CaeFx.flameBurst;
            healEffect = CaeFx.healWave;
            size = 3;
            health = 900;
            requirements(Category.effect, with(Items.titanium, 200, Items.plastanium, 100, Items.silicon, 200, Items.graphite, 300));
            consumePower(300f/60f);
            consumeItems(with(CaeItems.rubrarium, 5f));
        }};

        tesla = new Tesla("tesla"){{
            hitEffect = CaeFx.spark;
            size = 3;
            health = 1500;
            requirements(Category.effect, with(Items.titanium, 150, Items.plastanium, 150, CaeItems.lonsdaleite, 100, Items.silicon, 200, Items.graphite, 300));
            consumePower(300f/60f); 
            consumeItems(with(CaeItems.caeruleum, 5f));
        }};

        lonsdaleiteCompressor = new GenericCrafter("lonsdaleite-compressor"){{
            size = 3;
            hasPower = true;
            hasItems = true;
            hasLiquids = false;
            craftTime = 45;
            craftEffect = Fx.producesmoke;
            update = true;
            itemCapacity = 30;
            consumePower(0.7f);
            consumeItem(Items.graphite, 20);

            drawer = new DrawFrames(){{
            frames = 4;
        }};

            outputItem = new ItemStack(CaeItems.lonsdaleite, 1);
            requirements(Category.crafting, with(Items.copper, 100, Items.lead, 150, Items.silicon, 250, Items.titanium, 120, Items.graphite, 80));
        }};

        heavenlyStrike = new FractalTurret("heavenlyStrike"){{
            health = 1540;
            recoil = 0;
            shootType = CaeBullets.mediumSword;
            range = (shootType.lifetime * shootType.speed) / 1.5f;
            shoot = new ShootSummon(0f, 0f, range, 10f){{
                shots = 3;
            }};
            size = 4;
            reload = 20f;
            requirements(Category.turret, with(CaeItems.lonsdaleite, 150, Items.titanium, 200, Items.lead, 280));
        }};

        praefector = new DisabledPredictTurret("praefector"){{
            health = 1280;
            recoil = 2;
            shoot = new ShootAlternate(){{
            shots = 2;
        }};
            size = 4;
            reload = 15f;
            shootType = CaeBullets.mediumOverseer;
            range = (shootType.lifetime * shootType.speed) / 1.5f;
            requirements(Category.turret, with(CaeItems.lonsdaleite, 100, Items.titanium, 150, Items.lead, 180));
        }};

        lonsdaleiteWall = new DRWall("lonsdaleiteWall"){{
            dRChance = 15f;
            dRPercentage = 20f;
            health = 520;
            insulated = true;
            requirements(Category.defense, with(CaeItems.lonsdaleite, 6, Items.phaseFabric, 6));
        }};

        lonsdaleiteWallLarge = new DRWall("lonsdaleiteWallLarge"){{
            size = 2;
            dRChance = 15f;
            dRPercentage = 45f;
            health = 2800;
            insulated = true;
            requirements(Category.defense, with(CaeItems.lonsdaleite, 24, Items.phaseFabric, 24));
        }};

        miniCore = new PowerCore("miniCore"){{

            size = 2;
            itemCapacity = 300;
            requirements(Category.effect, with(CaeItems.lonsdaleite, 20, Items.copper, 30, Items.titanium, 40));
        }
        @Override
            public boolean canPlaceOn(Tile tile, Team team, int rotation) {
                return true;
            }
        };
    }
}
