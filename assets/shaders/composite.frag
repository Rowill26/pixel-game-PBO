// Final "camera" pass: tilt-shift depth of field + bloom + colour grading + vignette.
#ifdef GL_ES
precision mediump float;
#endif
varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;   // unit 0: sharp scene
uniform sampler2D u_blur;      // unit 1: blurred scene (for depth of field)
uniform sampler2D u_bloom;     // unit 2: blurred bright parts
uniform vec2 u_screen;
uniform float u_dof;           // 0..1
uniform vec4 u_focus;          // top start, top full, bottom start, bottom full (0 = bottom of screen)
uniform float u_bloomAmt;
uniform float u_sat;
uniform vec3 u_tint;
uniform vec3 u_lift;
uniform float u_vignette;
uniform float u_grade;         // 0 = no grading/vignette, 1 = on

void main() {
    vec3 sharp = texture2D(u_texture, v_texCoords).rgb;
    vec3 blur = texture2D(u_blur, v_texCoords).rgb;
    float y = gl_FragCoord.y / u_screen.y;
    float top = smoothstep(u_focus.x, u_focus.y, y);
    float bottom = 1.0 - smoothstep(u_focus.w, u_focus.z, y);
    vec3 c = mix(sharp, blur, clamp(max(top, bottom), 0.0, 1.0) * u_dof);
    c += texture2D(u_bloom, v_texCoords).rgb * u_bloomAmt;

    float lum = dot(c, vec3(0.299, 0.587, 0.114));
    vec3 graded = mix(vec3(lum), c, u_sat) * u_tint + u_lift;
    vec2 p = gl_FragCoord.xy / u_screen - 0.5;
    float vig = 1.0 - u_vignette * clamp(dot(p, p) * 2.2 - 0.12, 0.0, 1.0);
    c = mix(c, graded * vig, u_grade);
    gl_FragColor = vec4(c, 1.0);
}
