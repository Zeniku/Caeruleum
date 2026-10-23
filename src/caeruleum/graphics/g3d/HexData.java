package caeruleum.graphics.g3d;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Mesh;
import arc.graphics.g2d.TextureRegion;
import arc.graphics.gl.Shader;
import arc.math.geom.Vec3;
import arc.util.Nullable;
import mindustry.graphics.g3d.HexMesher;
import mindustry.type.Planet;
import mindustry.content.Planets;

public class HexData{
    public @Nullable Planet planet;
    public @Nullable HexMesher mesher;
    public int divisions = 5;
    public float radius = 1f;
    public float intensity = 0.2f;
    public Shader shader;
    public Color color;
    public TextureRegion texture;

    public HexData(@Nullable Planet planet, @Nullable HexMesher mesher, Shader shader, @Nullable Color color, @Nullable TextureRegion texture){
        this.planet = planet != null? planet : Planets.serpulo;
        this.color = color != null? color : new Color().set(Color.white);
        this.texture = texture != null? texture : Core.atlas.find("caeruleum-white");
        Color colorTemp = this.color;
        this.mesher = mesher != null? mesher : new HexMesher() {
            @Override
            public void getColor(Vec3 position , Color out) {
                out.set(colorTemp);
            };

            @Override
            public float getHeight(Vec3 position){
                return 0.01f;
            };
        };
        this.shader = shader;
    };

    public HexData(Planet planet, Shader shader, TextureRegion texture) {
        this(planet, null, shader, null, texture);
    }

    public Mesh buildHex(){
        return CaeMeshBuilder.buildHex(this.mesher, this.divisions, this.radius, this.intensity);
    };

    
}
