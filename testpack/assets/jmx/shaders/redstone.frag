#version 120

varying vec2 v_noise_uv;

void main()
{
    vec4 color = texture2D(u_textures, v_texcoord_0);
    float noise = tnoise(v_noise_uv * 64, u_time);
    if(color.a == 0.0) {
        color = vec4(color.r * (0.5 + 0.5 * noise), 0.0, 0.0, 1.0);
    } else {
        color = diffuseColor();
        if(noise > 0.95) {
            color = vec4(min(1.0, color.r + (noise - 0.95) * 10.0), color.gba);
        }
    }
    gl_FragColor = fog(color);
}
