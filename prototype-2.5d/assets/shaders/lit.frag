// Pixel-art lighting: diffuse sprite x (ambient + sun + point lights), shaped by a normal map.
#ifdef GL_ES
precision mediump float;
#endif
varying vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture;   // unit 0: diffuse
uniform sampler2D u_normals;   // unit 1: normal map (RGB = n*0.5+0.5, +Y up)
uniform vec3 u_ambient;
uniform vec3 u_sunColor;
uniform vec3 u_sunDir;
uniform float u_useNormals;    // 0 = flat, 1 = use normal map
uniform float u_lit;           // 0 = unlit (sky), 1 = fully lit
uniform float u_flash;         // 0..1 white hit flash

const int MAX_LIGHTS = 16;
uniform int u_lightCount;
uniform vec3 u_lightPos[MAX_LIGHTS];     // x, y in framebuffer pixels, z = height
uniform vec3 u_lightColor[MAX_LIGHTS];   // already multiplied by intensity
uniform float u_lightRadius[MAX_LIGHTS];

void main() {
    vec4 diff = texture2D(u_texture, v_texCoords) * v_color;
    if (diff.a < 0.01) discard;
    vec4 nt = texture2D(u_normals, v_texCoords);
    vec3 n = normalize(mix(vec3(0.0, 0.0, 1.0), nt.rgb * 2.0 - 1.0, u_useNormals * nt.a));

    vec3 light = u_ambient + u_sunColor * max(dot(n, normalize(u_sunDir)), 0.0);
    for (int i = 0; i < MAX_LIGHTS; i++) {
        if (i >= u_lightCount) break;
        vec3 d = vec3(u_lightPos[i].xy - gl_FragCoord.xy, u_lightPos[i].z);
        float att = clamp(1.0 - length(d.xy) / u_lightRadius[i], 0.0, 1.0);
        float ndl = max(dot(n, normalize(d)), 0.0) * 0.8 + 0.2;
        light += u_lightColor[i] * att * att * ndl;
    }
    light = mix(vec3(1.0), light, u_lit);
    vec3 c = mix(diff.rgb * light, vec3(1.0), u_flash);
    gl_FragColor = vec4(c, diff.a);
}
