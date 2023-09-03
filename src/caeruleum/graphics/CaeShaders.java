package caeruleum.graphics;

import arc.graphics.Color;
import arc.math.geom.Vec3;
import mindustry.graphics.Shaders;
import mindustry.graphics.Shaders.PlanetShader;
import mindustry.type.Planet;

import static mindustry.Vars.renderer;
// TODO wip
public class CaeShaders {
 /* public static TransparencyPlanetShader Tplanet;

  public static void init(){
    Tplanet = new TransparencyPlanetShader();
  };
  public static class TransparencyPlanetShader extends PlanetShader{
      public Vec3 lightDir = new Vec3(1, 1, 1).nor();
      public Color ambientColor = Color.white.cpy();
      public Vec3 camDir = new Vec3();
      public Vec3 camPos = new Vec3();
      public Planet planet;

      @Override
      public void apply(){
        camDir.set(renderer.planets.cam.direction).rotate(Vec3.Y, planet.getRotation());

        setUniformf("u_lightdir", lightDir);
        setUniformf("u_ambientColor", ambientColor.r, ambientColor.g, ambientColor.b);
        setUniformf("u_camdir", camDir);
        setUniformf("u_campos", renderer.planets.cam.position);
      }
  };
  */
}
