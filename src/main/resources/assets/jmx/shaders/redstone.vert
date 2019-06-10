#version 120

varying vec2 v_noise_uv;

void main()
{
    v_noise_uv = uv(gl_Vertex.xyz, in_normal_ao.xyz);
    setupVertex();
}
