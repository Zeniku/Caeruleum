package caeruleum.graphics.g3d;

import arc.graphics.Gl;
import arc.math.geom.Mat3D;
import arc.math.geom.Vec3;
import caeruleum.graphics.CaeShaders;
import mindustry.graphics.Shaders;
import mindustry.graphics.g3d.HexMesh;
import mindustry.graphics.g3d.PlanetParams;
import mindustry.type.Planet;

public class CaeHexMesh extends HexMesh {
    public boolean ren = true;
    public boolean depth = false;
    public HexData data;
    public CaeHexMesh(Planet planet, int divisions){
        super(planet, planet.generator, divisions, Shaders.planet);
    };
    
    public CaeHexMesh(HexData data){
        this.data = data;
        this.planet = data.planet; 
        this.shader = data.shader;
        this.mesh = data.buildHex();
    };
    @Override
    public void render(PlanetParams params, Mat3D projection, Mat3D transform){
        if (this.depth) Gl.depthMask(false);
        preRender(params);
        shader.bind();
        shader.setUniformMatrix4("u_proj", projection.val);
        shader.setUniformMatrix4("u_trans", transform.val);
        shader.apply();
        mesh.render(shader, Gl.triangles);
        if(this.depth) Gl.depthMask(true);
    }
     @Override 
     public void preRender(PlanetParams params){
         if (this.shader == Shaders.planet){ super.preRender(params); return;}
         CaeShaders.Tplanet.planet = planet;
         CaeShaders.Tplanet.lightDir.set(planet.solarSystem.position).sub(planet.position).rotate(Vec3.Y, planet.getRotation()).nor();
         CaeShaders.Tplanet.ambientColor.set(planet.solarSystem.lightColor);
         CaeShaders.Tplanet.alpha = params.planet == planet ? this.data.color.a : 1f;
     }; 
}
