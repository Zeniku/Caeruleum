varying vec4 v_col;
varying vec2 v_texCoords;
varying vec3 v_dir;

uniform sampler2D u_texture;
uniform vec4 u_textureRegion;
uniform float u_time;
uniform float u_triplanar;   // 0 = spherical, 1 = triplanar

// hash2() and pnoise() unchanged
vec2 hash2(vec2 p) {
    p = vec2(dot(p, vec2(127.1, 311.7)), dot(p, vec2(269.5, 183.3)));
    return -1.0 + 2.0 * fract(sin(p) * 43758.5453);
}

float pnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0); // quintic fade

    return mix(
        mix(dot(hash2(i),                  f),
            dot(hash2(i + vec2(1.0, 0.0)), f - vec2(1.0, 0.0)), u.x),
        mix(dot(hash2(i + vec2(0.0, 1.0)), f - vec2(0.0, 1.0)),
            dot(hash2(i + vec2(1.0, 1.0)), f - vec2(1.0, 1.0)), u.x),
        u.y);
}

vec4 tex(vec2 p){
    float t = u_time;
    vec2 warp = vec2(
        pnoise(p * 2.0 + vec2(t, 0.0)) + t,
        pnoise(p * 2.0 + vec2(0.0, t) + 17.3)
    );
    vec2 uv = fract(p + warp * 0.1);
    return texture2D(u_texture, mix(u_textureRegion.xy, u_textureRegion.zw, uv));
}

void main(){
    vec4 c;

    if(u_triplanar < 0.5){
        // spherical: your existing path
        c = tex(v_texCoords * 4.0);
    }else{
        vec3 n = normalize(v_dir);
        vec3 w = pow(abs(n), vec3(2.0));   // blend sharpness
        w /= (w.x + w.y + w.z);

        const float scale = 8.0;           // repeats per unit, tune this
        const float cut = 0.05;            // skip planes weaker than this

        float sum = 0.0;
        c = vec4(0.0);
        if(w.x > cut){ c += tex(n.zy * scale) * w.x; sum += w.x; }
        if(w.y > cut){ c += tex(n.xz * scale) * w.y; sum += w.y; }
        if(w.z > cut){ c += tex(n.xy * scale) * w.z; sum += w.z; }
        c /= sum;
    }

    gl_FragColor = c * v_col;
}