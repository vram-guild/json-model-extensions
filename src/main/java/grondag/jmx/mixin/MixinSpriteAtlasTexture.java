package grondag.jmx.mixin;

import grondag.jmx.json.v1.JmxTexturesExtV1;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ConcurrentLinkedQueue;

@Mixin(SpriteAtlasTexture.class)
public class MixinSpriteAtlasTexture {
    @Inject(
        method = "method_18160",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/SpriteAtlasTexture;getTexturePath(Lnet/minecraft/util/Identifier;)Lnet/minecraft/util/Identifier;"),
        cancellable = true
    )
    void blockDummySpriteLoad(Identifier id, ResourceManager resourceManager, ConcurrentLinkedQueue<Sprite.Info> queue, CallbackInfo ci) {
        if (id == JmxTexturesExtV1.DUMMY_ID) {
            ci.cancel();
        }
    }
}
