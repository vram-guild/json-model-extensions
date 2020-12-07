package grondag.jmx.mixin;

import grondag.jmx.Configurator;
import grondag.jmx.JsonModelExtensions;
import grondag.jmx.json.v1.JmxModelExtV1;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.SpriteAtlasManager;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelLoader.class)
public class MixinModelLoader {
    @Inject(method = "upload", at = @At("TAIL"))
    void logErrorPresence(TextureManager textureManager, Profiler profiler, CallbackInfoReturnable<SpriteAtlasManager> cir) {
        if (!Configurator.logResolutionErrors && JmxModelExtV1.HAS_ERROR) {
            JsonModelExtensions.LOG.warn("One or more errors occurred in JMX model(s). Enable `log-resolution-errors` in config/jmx.properties to display all errors.");
        }
    }
}
