package caeruleum.graphics.g3d;

import arc.graphics.Color;
import arc.graphics.gl.Shader;
import arc.math.geom.Vec3;
import arc.util.Nullable;
import arc.util.Tmp;
import arc.util.noise.Simplex;
import mindustry.graphics.Shaders;
import mindustry.graphics.g3d.HexMesh;
import mindustry.graphics.g3d.HexMesher;
import mindustry.graphics.g3d.MeshBuilder;
import mindustry.graphics.g3d.PlanetParams;
import mindustry.type.Planet;

public class CaeHexMesh extends HexMesh {
    public CaeHexMesh(Planet planet, int divisions){
        super(planet, planet.generator, divisions, Shaders.clouds);
    };
    public CaeHexMesh(Planet planet, @Nullable HexMesher mesher, int divisions, float radius, Color color){
        this(planet, mesher, divisions, radius, color, Shaders.clouds);
    };
    public CaeHexMesh(Planet planet, @Nullable HexMesher mesher, int divisions, float radius, Color color, Shader shader){
        this.planet = planet; 
        this.shader = shader;

        HexMesher temp = mesher != null? mesher: new HexMesher() {
            @Override
            public Color getColor(Vec3 position) {
                return color;
            };

            @Override
            public float getHeight(Vec3 position){
                return 0;
            };
        };

        this.mesh = MeshBuilder.buildHex(temp, divisions, false, radius, 0.2f);
    };
    

    @Override 
     public void preRender(PlanetParams params){
         Shaders.clouds.planet = planet; 
         Shaders.clouds.lightDir.set(planet.solarSystem.position).sub(planet.position).rotate(Vec3.Y, planet.getRotation()).nor(); 
         Shaders.clouds.ambientColor.set(planet.solarSystem.lightColor); 
         Shaders.clouds.alpha = params.planet == planet ? 1f - params.uiAlpha : 1f;
     };

}
