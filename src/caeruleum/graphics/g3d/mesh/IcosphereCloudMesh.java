package caeruleum.graphics.g3d.mesh;

import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.noise.*;
import caeruleum.graphics.g3d.CaeMeshBuilder;
import mindustry.graphics.*;
import mindustry.graphics.g3d.HexMesher;
import mindustry.graphics.g3d.PlanetMesh;
import mindustry.graphics.g3d.PlanetParams;
import mindustry.type.*;

/** A smooth cloud sphere that uses noise to cut holes, without hexagonal tiles. */
public class IcosphereCloudMesh extends PlanetMesh{
    static Mat3D mat = new Mat3D();

    public float speed = 0f;

    public IcosphereCloudMesh(Planet planet, int seed, float speed, float radius, int divisions, Color color, int octaves, float persistence, float scl, float thresh){
        super(planet, CaeMeshBuilder.buildSphere(new HexMesher(){
            @Override
            public float getHeight(Vec3 position){
                return 1f;
            }

            @Override
            public void getColor(Vec3 position, Color out){
                out.set(color);
            }

            @Override
            public boolean skip(Vec3 position){
                return Simplex.noise3d(7 + seed, octaves, persistence, scl, position.x, position.y * 3f, position.z) >= thresh;
            }
        }, divisions, planet.radius, radius), Shaders.clouds);

        this.speed = speed;
    }

    public IcosphereCloudMesh(){
    }

    public float relRot(){
        return Time.globalTime * speed / 40f;
    }

    @Override
    public void render(PlanetParams params, Mat3D projection, Mat3D transform){
        //don't waste performance rendering 0-alpha clouds
        if(params.planet == planet && Mathf.zero(1f - params.uiAlpha, 0.01f)) return;

        preRender(params);
        shader.bind();
        shader.setUniformMatrix4("u_proj", projection.val);
        shader.setUniformMatrix4("u_trans", mat.setToTranslation(planet.position).rotate(Vec3.Y, planet.getRotation() + relRot()).val);
        shader.apply();
        mesh.render(shader, Gl.triangles);
    }

    @Override
    public void preRender(PlanetParams params){
        Shaders.clouds.planet = planet;
        Shaders.clouds.lightDir.set(planet.solarSystem.position).sub(planet.position).rotate(Vec3.Y, planet.getRotation() + relRot()).nor();
        Shaders.clouds.ambientColor.set(planet.solarSystem.lightColor);
        Shaders.clouds.alpha = params.planet == planet ? 1f - params.uiAlpha : 1f;
    }
}