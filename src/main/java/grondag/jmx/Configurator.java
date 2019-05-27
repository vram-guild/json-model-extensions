//package grondag.jmx;
//
//import blue.endless.jankson.Comment;
//import net.fabricmc.api.EnvType;
//import net.fabricmc.api.Environment;
//
//@Environment(EnvType.CLIENT)
//public class Configurator {
//
//    @SuppressWarnings("hiding")
//    static class ConfigData {
//        @Comment("Applies material properties and shaders to items. (WIP)")
//        boolean itemShaderRender = false;
//        
//        @Comment("Reduces terrain lighting to full darkness in absence of moon/torch light.")
//        boolean hardcoreDarkness = false;
//        
//        @Comment("Makes terrain fog a little less foggy.")
//        boolean subtleFog = false;
//        
////        @Comment("TODO")
////        boolean enableCompactGPUFormats = false;
//        
//        @Comment("Truly smoothh lighting. Some impact to memory use, chunk loading and frame rate.")
//        boolean hdLightmaps = false;
//        
//        @Comment("Slight variation in light values - may prevent banding. Slight performance impact and not usually necessary.")
//        boolean lightmapNoise = false;
//        
//        @Comment("Mimics directional light.")
//        DiffuseMode diffuseShadingMode = DiffuseMode.NORMAL;
//        
//        @Comment("Makes light sources less cross-shaped. Chunk loading a little slower. Overall light levels remain similar.")
//        boolean lightSmoothing = false;
//        
//        @Comment("Mimics light blocked by nearby objects.")
//        AoMode aoShadingMode = AoMode.NORMAL;
//        
//        @Comment("Setting > 0 may give slightly better FPS at cost of potential flickering when lighting changes.")
//        int maxLightmapDelayFrames = 0;
//        
//        @Comment("Extra lightmap capacity. Ensure enabled if you are getting `unable to create HD lightmap(s) - out of space' messages.")
//        boolean moreLightmap = true;
//        
////        @Comment("TODO")
////        boolean enableSinglePassCutout = true;
//        
//        @Comment("Helps with chunk rebuild and also rendering when player is moving or many blocks update.")
//        boolean fastChunkOcclusion = true;
//        
//        @Comment("Draws multiple chunks with same view transformation. Much faster, but try without if you see visual defects.")
//        boolean batchedChunkRender = true;
//        
////        @Comment("TODO")
////        boolean disableVanillaChunkMatrix = true;
//        
//        @Comment("Adjusts quads on some vanilla models (like iron bars) to avoid z-fighting with neighbor blocks.")
//        boolean preventDepthFighting = true;
//        
//        @Comment("Forces game to allow up to this many nanoseconds for chunk loading each frame. May prevent chunk load delay at high FPS.")
//        long minChunkBudgetNanos = 100000;
//
//        // DEBUG
//        @Comment("Output runtime per-material shader source. For shader development debugging.")
//        boolean shaderDebug = false;
//        
//        @Comment("Shows HD lightmap pixels for debug purposes. Also looks cool.")        
//        boolean lightmapDebug = false;
//        
//        @Comment("Summarizes multiple errors and warnings to single-line entries in the log.")        
//        boolean conciseErrors = true;
//        
//        @Comment("Writes information useful for bug reports to the game log at startup.")        
//        boolean logMachineInfo = true;
//    }
//}
