package caeruleum.maps.planet;

import static mindustry.Vars.spawner;
import static mindustry.Vars.state;

import arc.math.Mathf;
import arc.math.Rand;
import caeruleum.content.CaeBlocks;
import mindustry.content.Blocks;
import mindustry.content.Liquids;
import mindustry.game.Schematics;
import mindustry.game.Waves;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;

public class TestGeneration extends RoomHandlers{
   @Override
   protected void generate(){
       
        cells(4);
        distort(10f, 12f);
        
        RoomHandler roomHandler = new RoomHandler();
        roomHandler.init();
        
        roomHandler.makeRooms(rand.random(5, 7));
        roomHandler.makeSpawn();

        cells(1);

        int tlen = tiles.width * tiles.height;
        int total = 0, waters = 0, grass = 0;

        for (int i = 0; i < tlen; i++) {
            Tile tile = tiles.geti(i);
            if (tile.block() == Blocks.air) {
                total++;
                if (tile.floor().liquidDrop == Liquids.water) {
                    waters++;
                }
                if(tile.floor() == (Floor) CaeBlocks.lazurigrass){
                    grass++;
                }
            }
        }

        roomHandler.naval = (float) waters / total >= 0.19f;

        roomHandler.ocean = (float) waters / total >= 0.5f;

        roomHandler.forest = (float) grass / total >= 0.5f;
        //render rooms
        for(Room room : roomHandler.roomseq){
          roomHandler.renderRoom(room);
        }

        
        roomHandler.connectEnemies(roomHandler.spawn);
        roomHandler.makeConnections();
        if(roomHandler.forest){

        }

        if(roomHandler.ocean){
            /*int randIslands = rand.random(6, 8);
            for (int i = 0; i < randIslands; i++) {
                Tmp.v1.trns(rand.random(360f), Math.max(30 , rand.random(roomHandler.radius * 1.5f)));

                float rx = (width/2f + Tmp.v1.x), ry = (height/2f + Tmp.v1.y);
                float maxrad = (roomHandler.radius * 1.5f) - Tmp.v1.len();
                float rrad = Math.min(rand.random(9f, maxrad / 2f), 35f);
                if(Structs.inBounds((int) rx, (int) ry, width, height)){
                    if(!(detectSolid((int)rx,(int) ry,(int) rrad) > 0)){
                        makeIsland((int) rx, (int) ry, (int) rrad);
                    }
                }
            }*/

            Room randR = roomHandler.randomRoom();
            if(randR != null) {
                for(Room roomEn : roomHandler.enemies){
                    randR.connectIslandsWater(roomEn);
                }
                roomHandler.spawn.connectIslandsWater(randR);
            }
        }

        // connect to the rooms
        roomHandler.connectRooms(roomHandler.spawn);
        for (Room room : roomHandler.roomseq) {
            if(roomHandler.ocean && (float) rand.random(0, 1) > 0.5f) roomHandler.spawn.connectIslandsWater(room);
        }

        Room fspawn = roomHandler.spawn;

        
        distort(10f, 6f);

        if(roomHandler.naval){
          shallowShores(3, (Floor) CaeBlocks.bluonixiteWater, (Floor) CaeBlocks.aquafluent, (Floor) Blocks.water);
          deepShores(3, (Floor) CaeBlocks.aquafluent, (Floor) CaeBlocks.deepAquafluent, (Floor) Blocks.deepwater);
        }

        float difficulty = sector.threat;
        Schematics.placeLaunchLoadout(roomHandler.spawn.x, roomHandler.spawn.y); //place core

        //place spawner block
        for (Room espawn : roomHandler.enemies) {
            tiles.getn(espawn.x, espawn.y).setOverlay(Blocks.spawn);
        }

        if (sector.hasEnemyBase()) {
            basegen.generate(tiles, 
                roomHandler.enemies.map(r -> tiles.getn(r.x, r.y)),
                tiles.get(roomHandler.spawn.x, roomHandler.spawn.y), 
                state.rules.waveTeam, sector, difficulty);

            state.rules.attackMode = sector.info.attack = true;
        } else {
            state.rules.winWave = sector.info.winWave = 10 + 5 * (int) Math.max(difficulty * 10, 1);
        }

        float waveTimeDec = 0.4f;

        state.rules.waveSpacing = Mathf.lerp(60 * 65 * 2, 60f * 60f * 1f, Math.max(difficulty - waveTimeDec, 0f));
        state.rules.waves = true;
        state.rules.env = sector.planet.defaultEnv;
        state.rules.enemyCoreBuildRadius = 600f;

        //spawn air only when spawn is blocked
        state.rules.spawns = Waves.generate(difficulty, new Rand(sector.id), state.rules.attackMode, state.rules.attackMode && spawner.countGroundSpawns() == 0, false);

    }
}
