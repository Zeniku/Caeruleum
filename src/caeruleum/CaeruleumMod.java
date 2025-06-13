package caeruleum;

import arc.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;
import mindustry.ui.dialogs.*;
import caeruleum.content.*;
import caeruleum.gen.*;

public class CaeruleumMod extends Mod{

    public CaeruleumMod(){
        Log.info("Loaded ExampleJavaMod constructor.");

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
    public void loadContent(){
        EntityRegistry.register();

        CaeItems.load();
        CaeBullets.load();
        CaeUnits.load();
        CaeBlocks.load();
        CaePlanets.load();
    }

}
