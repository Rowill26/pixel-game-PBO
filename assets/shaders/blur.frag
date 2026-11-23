// Separable 9-tap gaussian blur. u_dir = one texel step along x or y (times a spread factor).
#ifdef GL_ES
precision mediump float;
#endif
varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform vec2 u_dir;

void main() {
    vec3 c = texture2D(u_texture, v_texCoords).rgb * 0.2270270;
    c += (texture2D(u_texture, v_texCoords + u_dir * 1.0).rgb + texture2D(u_texture, v_texCoords - u_dir * 1.0).rgb) * 0.1945946;
    c += (texture2D(u_texture, v_texCoords + u_dir * 2.0).rgb + texture2D(u_texture, v_texCoords - u_dir * 2.0).rgb) * 0.1216216;
    c += (texture2D(u_texture, v_texCoords + u_dir * 3.0).rgb + texture2D(u_texture, v_texCoords - u_dir * 3.0).rgb) * 0.0540540;
    c += (texture2D(u_texture, v_texCoords + u_dir * 4.0).rgb + texture2D(u_texture, v_texCoords - u_dir * 4.0).rgb) * 0.0162162;
    gl_FragColor = vec4(c, 1.0);
}
