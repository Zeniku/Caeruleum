package caeruleum.content;

import arc.graphics.Color;
import caeruleum.graphics.CaeShaders;
import caeruleum.graphics.g3d.CaeHexMesh;
import caeruleum.graphics.g3d.HexData;
import caeruleum.maps.planet.CaeruleumPlanetGenerator;
import mindustry.content.Items;
import mindustry.content.Planets;
import mindustry.game.Team;
import mindustry.graphics.Shaders;
import mindustry.graphics.g3d.HexMesh;
import mindustry.graphics.g3d.HexSkyMesh;
import mindustry.graphics.g3d.MultiMesh;
import mindustry.graphics.g3d.NoiseMesh;
import mindustry.type.Planet;


public class CaePlanets {
    public static Planet caeruleumPlanet;
    
    public static void load(){
        caeruleumPlanet = new Planet("caeruleum", Planets.sun, 1.1f, 2){{
            generator = new CaeruleumPlanetGenerator();
            meshLoader = () -> new MultiMesh(
                    new HexMesh(this, 6),
                    new CaeHexMesh(new HexData(this, null, Shaders.clouds, CaeBlocks.deepAquafluent.mapColor.cpy().mul(0.6f).a(0.8f)){{
                        divisions = 5;
                        radius = 1.008f;
                    }})
            );
            cloudMeshLoader = () -> new MultiMesh(
                    new HexSkyMesh(this, 11, 0.15f, 0.13f, 5, new Color().set(Color.valueOf("363f9a")).mul(0.9f).a(0.75f), 2, 0.45f, 0.9f, 0.38f),
                    new HexSkyMesh(this, 1, 0.6f, 0.16f, 5, Color.white.cpy().lerp(Color.valueOf("363f9a"), 0.55f).a(0.75f), 2, 0.45f, 1f, 0.41f)
            );
            launchCapacityMultiplier = 0.5f;
            sectorSeed = 2;
            allowWaves = true;
            allowWaveSimulation = true;
            allowSectorInvasion = true;
            allowLaunchSchematics = true;
            enemyCoreSpawnReplace = true;
            allowLaunchLoadout = true;
            //doesn't play well with configs
            prebuildBase = false;
            ruleSetter = r -> {
                r.waveTeam = Team.crux;
                r.placeRangeCheck = false;
                r.showSpawns = false;
            };
            iconColor = Color.valueOf("7d4dff");
            atmosphereColor = CaeBlocks.deepAquafluent.mapColor;
            atmosphereRadIn = 0.07f;
            atmosphereRadOut = 0.25f;
            startSector = 15;
            alwaysUnlocked = true;
            landCloudColor = Color.valueOf("363f9a").cpy().a(0.5f);
            hiddenItems.addAll(Items.erekirItems).removeAll(Items.serpuloItems);
        }};
    }
}
