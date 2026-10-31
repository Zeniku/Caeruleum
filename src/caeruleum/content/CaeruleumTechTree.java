package caeruleum.content;

import static mindustry.content.TechTree.*;

public class CaeruleumTechTree {
    public static void load(){
        CaePlanets.caeruleumPlanet.techTree = CaePlanets.die.techTree = nodeRoot("caeruleum", CaeBlocks.miniCore, () -> {
            node(CaeBlocks.lonsdaleiteDuct, () -> {
                
                node(CaeBlocks.drill1, () -> {
                    node(CaeBlocks.drill2, () -> {
                        node(CaeBlocks.drill3, () -> {
                            node(CaeBlocks.drill4, () -> {

                            });
                        });
                    });
                });


                node(CaeBlocks.lonsdaleiteCompressor, () -> {
                    node(CaeBlocks.lonsdaleitePress, () -> {

                    });
                    node(CaeBlocks.lonsdaleiteWall, () -> {
                        node(CaeBlocks.lonsdaleiteWallLarge, () -> {
                        
                        });
                    });

                    node(CaeBlocks.t1frac, () -> {
                        node(CaeBlocks.t2frac, () -> {
                            node(CaeBlocks.t3frac, () -> {
                                node(CaeBlocks.t4frac, () -> {
                                    
                                }); 
                            }); 
                        });
                    });
                });
            });
        });
    }
}
