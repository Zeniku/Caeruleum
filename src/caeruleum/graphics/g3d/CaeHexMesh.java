package caeruleum.graphics.g3d;

import arc.math.geom.Vec3;
import mindustry.graphics.Shaders;
import mindustry.graphics.g3d.HexMesh;
import mindustry.graphics.g3d.PlanetParams;
import mindustry.type.Planet;

public class CaeHexMesh extends HexMesh {
    public boolean ren = true;
    public CaeHexMesh(Planet planet, int divisions){
        super(planet, planet.generator, divisions, Shaders.planet);
    };
    
    public CaeHexMesh(HexData data){
        this.planet = data.planet; 
        this.shader = data.shader;
        this.ren = (data.shader == Shaders.clouds);
        this.mesh = data.buildHex();
    };
    
     @Override 
     public void preRender(PlanetParams params){
         if(ren){
             Shaders.clouds.planet = planet; 
             Shaders.clouds.lightDir.set(planet.solarSystem.position).sub(planet.position).rotate(Vec3.Y, planet.getRotation()).nor(); 
             Shaders.clouds.ambientColor.set(planet.solarSystem.lightColor); 
             Shaders.clouds.alpha = params.planet == planet ? 1f - params.uiAlpha : 1f;
         }else {
             super.preRender(params);
         };
     }; 
}
