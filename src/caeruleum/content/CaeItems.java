package caeruleum.content;

import mindustry.type.*;
import arc.graphics.Color;

public class CaeItems { 
    public static Item caeruleum, rubrarium, virideaurum, lonsdaleite;

    public static void load(){
        caeruleum = new Item("caeruleum-chalcitis", Color.valueOf("322947")){{
            hardness = 2;
            cost = 1f;
        }};
        rubrarium = new Item("rubrarium", Color.valueOf("964253")){{
            hardness = 3;
            cost = 1f;
        }};
        virideaurum = new Item("virideaurum", Color.valueOf("3CA370")){{
            hardness = 3;
            cost = 1f;
        }};
        lonsdaleite = new Item("lonsdaleite", Color.valueOf("473B78")){{
            hardness = 3;
            cost = 1.1f;
        }};
    }
}
