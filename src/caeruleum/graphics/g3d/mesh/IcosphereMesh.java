package caeruleum.graphics.g3d.mesh;

import arc.graphics.gl.*;
import arc.math.geom.*;
import caeruleum.graphics.g3d.CaeMeshBuilder;
import mindustry.graphics.*;
import mindustry.graphics.g3d.HexMesher;
import mindustry.graphics.g3d.PlanetMesh;
import mindustry.graphics.g3d.PlanetParams;
import mindustry.type.*;

/** A smooth sphere mesh that samples colors and heights from a {@link HexMesher}, without hexagonal tiles. */
public class IcosphereMesh extends PlanetMesh{

    public IcosphereMesh(Planet planet, int divisions){
        this(planet, planet.generator != null ? planet.generator : new HexMesher(){}, divisions, Shaders.planet);
    }

    public IcosphereMesh(Planet planet, HexMesher mesher, int divisions, Shader shader){
        super(planet, CaeMeshBuilder.buildSphere(mesher, divisions, planet.radius, 0.2f), shader);
    }

    public IcosphereMesh(){
    }

    @Override
    public void preRender(PlanetParams params){
        Shaders.planet.planet = planet;
        Shaders.planet.emissive = planet.generator != null && planet.generator.isEmissive();
        Shaders.planet.lightDir.set(planet.solarSystem.position).sub(planet.position).rotate(Vec3.Y, planet.getRotation()).nor();
        Shaders.planet.ambientColor.set(planet.solarSystem.lightColor);
    }
}