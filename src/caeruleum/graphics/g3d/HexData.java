package caeruleum.graphics.g3d;

import arc.graphics.Color;
import arc.graphics.Mesh;
import arc.graphics.gl.Shader;
import arc.math.geom.Vec3;
import arc.util.Nullable;
import mindustry.graphics.g3d.HexMesher;
import mindustry.graphics.g3d.MeshBuilder;
import mindustry.type.Planet;

public class HexData{
    public Planet planet;
    public @Nullable HexMesher mesher;
    public int divisions = 5;
    public boolean line = false;
    public float radius = 1f;
    public float intensity = 0.2f;
    public Shader shader;
    public Color color;

    public HexData(Planet planet, @Nullable HexMesher mesher, Shader shader, Color color){
        this.planet = planet;
        this.color = color;
        this.mesher = mesher != null? mesher : new HexMesher() {
            @Override
            public Color getColor(Vec3 position) {
                return color;
            };

            @Override
            public float getHeight(Vec3 position){
                return 0;
            };
        };
        this.shader = shader;
    };
    public HexData(Planet planet, Shader shader , Color color){
        this(planet, planet.generator, shader, color);
    };

    public Mesh buildHex(){
        return MeshBuilder.buildHex(this.mesher, this.divisions, this.line, this.radius, this.intensity);
    };
}
