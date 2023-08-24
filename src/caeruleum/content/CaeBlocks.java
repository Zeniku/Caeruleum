package caeruleum.content;

import mindustry.world.Block;
import mindustry.world.blocks.environment.*;

public class CaeBlocks { 
    public static Block 
    bluonixite, bluonixiteWall, caeruleumOre, rubrariumOre, virideaurumOre;

    public static void load(){
        bluonixite = new Floor("bluonixite-stone"){{
            variants = 4;
        }};
        bluonixiteWall = new StaticWall("bluonixite-wall"){{
            bluonixite.asFloor().wall = this;
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
