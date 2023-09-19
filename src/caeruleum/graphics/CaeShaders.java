package caeruleum.graphics;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.gl.Shader;
import arc.math.geom.Vec3;
import arc.util.Log;
import mindustry.Vars;
import mindustry.type.Planet;

import static mindustry.Vars.*;
// TODO wip
public class CaeShaders {
 public static TransparencyPlanetShader Tplanet = new TransparencyPlanetShader();

  //literally mindus copy
  public static class TransparencyPlanetShader extends CaeLoadShader{
      public Vec3 lightDir = new Vec3(1, 1, 1).nor();
      public Color ambientColor = Color.white.cpy();
      public Vec3 camDir = new Vec3();
      public Vec3 camPos = new Vec3();
      public Planet planet;
      public float alpha = 1f;
      public TransparencyPlanetShader(){
          super("caeplanet", "caeplanet");
      }
      @Override
      public void apply(){
        camDir.set(renderer.planets.cam.direction).rotate(Vec3.Y, planet.getRotation());

        setUniformf("u_lightdir", lightDir);
        setUniformf("u_ambientColor", ambientColor.r, ambientColor.g, ambientColor.b);
        setUniformf("u_camdir", camDir);
        setUniformf("u_campos", renderer.planets.cam.position);
        setUniformf("u_alpha", alpha);
      }
  };
    public static class CaeLoadShader extends Shader {
        public CaeLoadShader(String frag, String vert){
            super(Vars.tree.get("shaders/" + vert + ".vert"), Vars.tree.get("shaders/" + frag + ".frag"));
        }
    }
}
