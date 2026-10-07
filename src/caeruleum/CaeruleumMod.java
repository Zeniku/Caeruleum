package caeruleum;

import arc.*;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Lines;
import arc.util.*;
import mindustry.Vars;
import mindustry.game.EventType.*;
import mindustry.graphics.Layer;
import mindustry.mod.*;
import mindustry.ui.Fonts;
import mindustry.ui.dialogs.*;
import caeruleum.content.*;
import caeruleum.gen.*;
import caeruleum.maps.utils.CaeChunk;
import caeruleum.maps.utils.managers.CaeChunkHandler;

public class CaeruleumMod extends Mod{

    public CaeruleumMod(){
        Log.info("Loaded CaeruleumMod constructor.");

        //listen for game load event
        Events.on(ClientLoadEvent.class, e -> {
            //show dialog upon startup
            Time.runTask(10f, () -> {
                BaseDialog dialog = new BaseDialog("frog");
                dialog.cont.add("This is a wip mod \nEverything in this mod is experimental\nand constantly changing").center().row();
                //mod sprites are prefixed with the mod name (this mod is called 'example-java-mod' in its config)
                dialog.cont.image(Core.atlas.find("caeruleum-icon")).pad(20f).row();
                dialog.cont.button("you suck", dialog::hide).size(100f, 50f);
                dialog.show();
            });
        });
    }
    @Override 
    public void init(){
        Log.info("Loaded CaeruleumMod init.");
        setupChunkDebugRenderer();
    }
    private void setupChunkDebugRenderer() {
        Events.run(Trigger.draw, () -> {
            // Only draw if world is loaded and chunk handler has chunks
            if (Vars.state.isMenu() || CaeChunkHandler.current == null || CaeChunkHandler.current.chunks == null) {
                return;
            }

            CaeChunk[][] chunks = CaeChunkHandler.current.chunks;

            Draw.draw(Layer.overlayUI, () -> {
                Lines.stroke(1.5f);

                for (int x = 0; x < chunks.length; x++) {
                    for (int y = 0; y < chunks[0].length; y++) {
                        CaeChunk chunk = chunks[x][y];
                        if (chunk == null) continue;

                        // World pixel coordinates (1 tile = 8 pixels)
                        float worldX = chunk.x * Vars.tilesize;
                        float worldY = chunk.y * Vars.tilesize;
                        float worldW = chunk.width * Vars.tilesize;
                        float worldH = chunk.height * Vars.tilesize;

                        // Color coding per biome
                        Color color = switch (chunk.biome) {
                            case FOREST -> Color.blue;
                            case OCEAN -> Color.sky;
                            case MOUNTAIN -> Color.slate;
                            case PLAINS -> Color.gold;
                            default -> throw new IllegalArgumentException("Unexpected value: " + chunk.biome);
                        };

                        // 1. Draw Chunk Outline Box
                        Draw.color(color, 0.75f);
                        Lines.rect(worldX, worldY, worldW, worldH);

                        // 2. Draw Text at Chunk Center
                        float centerX = worldX + (worldW / 2f);
                        float centerY = worldY + (worldH / 2f);

                        Font font = Fonts.outline;
                        font.getData().setScale(0.8f);
                        font.setColor(color);
                        
                        String infoText = chunk.biome.name() + "\nInf: " + String.format("%.2f", chunk.oceanInfluence);
                        font.draw(infoText, centerX, centerY, 1);
                        
                        font.getData().setScale(1f); // Reset font scale
                    }
                }
                Draw.reset();
            });
        });
    }

    @Override
    public void loadContent(){
        EntityRegistry.register();

        CaeItems.load();
        CaeStatusEffects.load();
        CaeBullets.load();
        CaeUnits.load();
        CaeBlocks.load();
        CaePlanets.load();
    }

}
