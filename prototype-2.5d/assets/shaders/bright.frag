// Bloom step 1: keep only the bright parts of the image.
#ifdef GL_ES
precision mediump float;
#endif
varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_threshold;

void main() {
    vec3 c = texture2D(u_texture, v_texCoords).rgb;
    float l = max(c.r, max(c.g, c.b));
    float k = clamp((l - u_threshold) / (1.0 - u_threshold), 0.0, 1.0);
    gl_FragColor = vec4(c * k, 1.0);
}
