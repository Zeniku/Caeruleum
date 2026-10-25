package caeruleum.graphics.g3d;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Mesh;
import arc.graphics.VertexAttribute;
import arc.math.geom.Icosphere;
import arc.math.geom.MeshResult;
import arc.math.geom.Vec3;
import arc.struct.Seq;
import mindustry.graphics.g3d.HexMesher;
import mindustry.graphics.g3d.MeshBuilder;
import mindustry.graphics.g3d.PlanetGrid;
import mindustry.maps.generators.PlanetGenerator;

public class CaeMeshBuilder extends MeshBuilder {
   private static final boolean packNormals;
   private static volatile float[] tmpHeights;

    static {
      packNormals = Core.gl30 != null && (Core.app.isMobile() || Core.graphics.getGLVersion().atLeast(3, 3));
      tmpHeights = new float[14580];
    }

   public static Mesh buildIcosphere(int divisions, float radius) {
      MeshResult result = Icosphere.create(divisions);
      Mesh mesh = begin(result.vertices.size / 3, result.indices.size, false, false, false);
      if (result.vertices.size >= 65535) {
         throw new RuntimeException("Due to index size limits, only meshes with a maximum of 65535 vertices are supported. If you want more than that, make your own non-indexed mesh builder.");
      } else {
         float[] items = result.vertices.items;

         for(int i = 0; i < result.vertices.size; ++i) {
            items[i] *= radius;
         }

         mesh.getVerticesBuffer().put(items, 0, result.vertices.size);
         short[] indices = new short[result.indices.size];

         for(int i = 0; i < result.indices.size; ++i) {
            indices[i] = (short)result.indices.items[i];
         }

         mesh.getIndicesBuffer().put(indices);
         return end(mesh);
      }
   }

    public static Mesh buildPlanetGrid(PlanetGrid grid, Color color, float scale) {
      Mesh mesh = begin(grid.tiles.length * 12, 0, false, false);
      float col = color.toFloatBits();
      float[] floats = new float[8];

      for(PlanetGrid.Ptile tile : grid.tiles) {
         PlanetGrid.Corner[] c = tile.corners;

         for(int i = 0; i < c.length; ++i) {
            Vec3 v1 = c[i].v;
            Vec3 v2 = c[(i + 1) % c.length].v;
            floats[0] = v1.x * scale;
            floats[1] = v1.y * scale;
            floats[2] = v1.z * scale;
            floats[3] = col;
            floats[4] = v2.x * scale;
            floats[5] = v2.y * scale;
            floats[6] = v2.z * scale;
            floats[7] = col;
            mesh.getVerticesBuffer().put(floats);
         }
      }

      return end(mesh);
   }

   public static Mesh buildHex(Color color, int divisions, float radius){
        return buildHex(new HexMesher(){
            @Override
            public float getHeight(Vec3 position){
                return 0;
            }

            @Override
            public void getColor(Vec3 position, Color out){
                out.set(color);
            }
        }, divisions, radius, 0);
    }

   public static synchronized Mesh buildHex(HexMesher mesher, int divisions, float radius, float intensity) {
      PlanetGrid grid = PlanetGrid.create(divisions);

      if (mesher instanceof PlanetGenerator generator) {
         generator.seed = generator.baseSeed;
      }

      boolean emit = mesher.isEmissive();
      boolean indexed = grid.tiles.length * 6 < 65535; // 6 verts per tile check if it fits short limit
      // The hex mesh uses the same attribute order as the float array below:
      // position, normal, color, optional emissive color, then UV.
        Mesh mesh = begin(
            indexed ? grid.tiles.length * 6 : grid.tiles.length * 12, //if indexed 6 verts per tile, if not 12 verts
            indexed ? grid.tiles.length * 4 * 3 : 0, //if indexed 12 indecies, else 0 indices
            true, emit, true, true
        );

      float[] heights;

      boolean tmpHeight_large_enough = tmpHeights.length >= grid.corners.length;
      if (tmpHeights != null && tmpHeight_large_enough) {
         heights = tmpHeights; // reuse
      } else {
         heights = tmpHeights = new float[grid.corners.length]; //make new one
      }

      //every hex corner set distance from the center
      for(int i = 0; i < grid.corners.length; ++i) {
         heights[i] = (1.0F + mesher.getHeight(grid.corners[i].v) * intensity) * radius;
      }

      int position = 0;
      short[] shorts = indexed ? new short[12] : null;

      float[] floats = new float[
        3 // position
        + (packNormals ? 1 : 3) // normal (packed into 1 float or stored as xyz)
        + 1 // vertex color
        + (emit ? 1 : 0) // optional emissive color
        + 2 // UV (u, v)
      ];

      Vec3 nor = new Vec3(); //normals
      Color tmpCol = new Color(); // temporary Colors

      for(PlanetGrid.Ptile tile : grid.tiles) {
         if (!mesher.skip(tile.v)) {

            PlanetGrid.Corner[] c = tile.corners; // current Tile corners
            float h1 = heights[c[0].id]; 
            float h2 = heights[c[2].id];
            float h3 = heights[c[4].id];
            Vec3 v1 = c[0].v; 
            Vec3 v2 = c[2].v;
            Vec3 v3 = c[4].v;

            // i think it gets all the normals with those points
            // normals
            normal(v1.x * h1, v1.y * h1, v1.z * h1, v2.x * h2, v2.y * h2, v2.z * h2, v3.x * h3, v3.y * h3, v3.z * h3, nor);
            tmpCol.set(1.0F, 1.0F, 1.0F, 1.0F);

            mesher.getColor(tile.v, tmpCol); // get planet color from hexmesher generated by PlanetGenerator
            float color = tmpCol.toFloatBits();// covert to float
            float emissive = 0.0F;

            // If the mesher is emissive, get the emissive color for the tile
            if (emit) {
               tmpCol.set(0.0F, 0.0F, 0.0F, 0.0F);
               mesher.getEmissiveColor(tile.v, tmpCol);
               emissive = tmpCol.toFloatBits();
            }

            // Non-indexed meshes write each triangle's vertices directly.
            if (!indexed) {
               // 4 triangles for a hexagon
               verts(mesh, floats, 
                c[0].v, heights[c[0].id], 
                c[1].v, heights[c[1].id], 
                c[2].v, heights[c[2].id], 
                tile.v, nor, color, emissive, emit);

               verts(mesh, floats, 
                c[0].v, heights[c[0].id], 
                c[2].v, heights[c[2].id], 
                c[3].v, heights[c[3].id], 
                tile.v, nor, color, emissive, emit);

               verts(mesh, floats, 
                c[0].v, heights[c[0].id], 
                c[3].v, heights[c[3].id], 
                c[4].v, heights[c[4].id], 
                tile.v, nor, color, emissive, emit);

                //some are pentagons so skip the 4th one
               if (c.length > 5) {
                  verts(mesh, floats, 
                    c[0].v, heights[c[0].id], 
                    c[4].v, heights[c[4].id], 
                    c[5].v, heights[c[5].id], 
                    tile.v, nor, color, emissive, emit);
               }
            } else {
               // Indexed meshes write each tile corner once, then use indices
               // below to form the tile's triangles.
               //for every corner write the vert, it only writes 6 as indicies do the rest for the triangle
               for(PlanetGrid.Corner corner : c) {
                  float height = heights[corner.id];
                  vert(mesh, floats, corner.v, height, tile.v, nor, color, emissive, emit);
               }

               //set triangle indicies
               shorts[0] = (short)position;
               shorts[1] = (short)(position + 1);
               shorts[2] = (short)(position + 2);
               shorts[3] = (short)position;
               shorts[4] = (short)(position + 2);
               shorts[5] = (short)(position + 3);
               shorts[6] = (short)position;
               shorts[7] = (short)(position + 3);
               shorts[8] = (short)(position + 4);

               //skip the 4th for pentagon
               if (c.length > 5) {
                  shorts[9] = (short)position;
                  shorts[10] = (short)(position + 4);
                  shorts[11] = (short)(position + 5);
               }

               mesh.getIndicesBuffer().put(shorts, 0, c.length > 5 ? 12 : 9);
               position += c.length;
            }
         }
      }

      return end(mesh);
   }

   private static Mesh begin(int vertices, int indices, boolean normal, boolean emissive) {
      return begin(vertices, indices, normal, emissive, true);
   }

   private static Mesh begin(int vertices, int indices, boolean normal, boolean emissive, boolean color) {
      return begin(vertices, indices, normal, emissive, color, false);
   }

   private static Mesh begin(int vertices, int indices, boolean normal, boolean emissive, boolean color, boolean texCoords) {
      Seq<VertexAttribute> attributes = Seq.with(new VertexAttribute[]{VertexAttribute.position3});
      if (normal) {
         attributes.add(packNormals ? VertexAttribute.packedNormal : VertexAttribute.normal);
      }

      if (color) {
         attributes.add(VertexAttribute.color);
      }

      if (emissive) {
         attributes.add(new VertexAttribute(4, 5121, true, "a_emissive"));
      }

      // Keep this order in sync with how vert() fills the float array.
      if (texCoords) {
         attributes.add(VertexAttribute.texCoords);
      }

      Mesh mesh = new Mesh(true, vertices, indices, (VertexAttribute[])attributes.toArray(VertexAttribute.class));
      mesh.getVerticesBuffer().limit(mesh.getVerticesBuffer().capacity());
      mesh.getVerticesBuffer().position(0);

      if (indices > 0) {
         mesh.getIndicesBuffer().limit(mesh.getIndicesBuffer().capacity());
         mesh.getIndicesBuffer().position(0);
      }

      return mesh;
   }

   private static Mesh end(Mesh mesh) {
      mesh.getVerticesBuffer().limit(mesh.getVerticesBuffer().position());
      if (mesh.getNumIndices() > 0) {
         mesh.getIndicesBuffer().limit(mesh.getIndicesBuffer().position());
      }

      return mesh;
   }

   private static Vec3 normal(Vec3 v1, Vec3 v2, Vec3 v3, Vec3 out) {
      return out.set(v2).sub(v1).crs(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z).nor();
   }

   private static void normal(float v1x, float v1y, float v1z, float v2x, float v2y, float v2z, float v3x, float v3y, float v3z, Vec3 out) {
      float x = v2x - v1x;
      float y = v2y - v1y;
      float z = v2z - v1z;
      float vx = v3x - v1x;
      float vy = v3y - v1y;
      float vz = v3z - v1z;
      float cx = y * vz - z * vy;
      float cy = z * vx - x * vz;
      float cz = x * vy - y * vx;
      out.set(cx, cy, cz).nor();
   }

   private static void verts(Mesh mesh, float[] floats, Vec3 a, float h1, Vec3 b, float h2, Vec3 c, float h3, Vec3 tileCenter, Vec3 normal, float color, float emissive, boolean emit) {
      // A tile triangle has three corners; write one complete vertex per corner.
      vert(mesh, floats, a, h1, tileCenter, normal, color, emissive, emit);
      vert(mesh, floats, b, h2, tileCenter, normal, color, emissive, emit);
      vert(mesh, floats, c, h3, tileCenter, normal, color, emissive, emit);
   }

   private static void vert(Mesh mesh, float[] floats, Vec3 direction, float height, Vec3 tileCenter, Vec3 normal, float color, float emissive, boolean emit) {
      // Start with the position, then append attributes in begin()'s order.
      floats[0] = direction.x * height;
      floats[1] = direction.y * height;
      floats[2] = direction.z * height;

      int offset = 3;//position
      if (packNormals) {
         floats[offset++] = packNormals(normal.x, normal.y, normal.z);
      } else {
         floats[offset++] = normal.x;
         floats[offset++] = normal.y;
         floats[offset++] = normal.z;
      }

      floats[offset++] = color;
      if (emit) {
         floats[offset++] = emissive;
      }

      
      float u = (float)(Math.atan2(direction.z, direction.x) / (Math.PI * 2.0) + 0.5);
      float centerU = (float)(Math.atan2(tileCenter.z, tileCenter.x) / (Math.PI * 2.0) + 0.5);
      // Keep a tile crossing the longitude seam locally continuous (e.g. 0.99 and 1.01).
      if (u - centerU > 0.5f) u -= 1f;
      if (centerU - u > 0.5f) u += 1f;
      floats[offset++] = u;
      floats[offset] = (float)(Math.asin(Math.max(-1f, Math.min(1f, direction.y))) / Math.PI + 0.5);

      mesh.getVerticesBuffer().put(floats);
   }
   private static float packNormals(float x, float y, float z) {
      int xs = x < -0.001953125F ? 1 : 0;
      int ys = y < -0.001953125F ? 1 : 0;
      int zs = z < -0.001953125F ? 1 : 0;
      int vi = zs << 29 | ((int)(z * 511.0F + (float)(zs << 9)) & 511) << 20 | ys << 19 | ((int)(y * 511.0F + (float)(ys << 9)) & 511) << 10 | xs << 9 | (int)(x * 511.0F + (float)(xs << 9)) & 511;
      return Float.intBitsToFloat(vi);
   }
}
