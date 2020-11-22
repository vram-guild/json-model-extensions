#include frex:shaders/api/fragment.glsl

void frx_startFragment(inout frx_FragmentData fragData) {
    fragData.emissivity = 1.0 - fragData.spriteColor.a;
    fragData.spriteColor.a = 1.0;
}
