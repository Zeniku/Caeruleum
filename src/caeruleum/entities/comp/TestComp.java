package caeruleum.entities.comp;

import arc.util.*;
import ent.anno.Annotations.*;
import mindustry.gen.*;

// This will generate `Myc` component interface in `mymod.gen.entities`.
@EntityComponent
abstract class MyComp implements Unitc{
    // Do note that `@Override` does *not* mean "replace"
    @Override
    public void update(){
        Log.info("Hello, world!");
    }
}
