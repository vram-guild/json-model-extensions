package grondag.jmx.json;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import grondag.jmx.json.v0.JmxTexturesExtV0;
import grondag.jmx.json.v1.JmxTexturesExtV1;
import net.minecraft.client.util.SpriteIdentifier;

import java.util.Map;

public abstract class JmxTexturesExt {
    public static void handleJmxTextures(JsonObject obj, Map<String, Either<SpriteIdentifier, String>> map) {
        switch (JmxModelExt.VERSION.get()) {
        default:
        case 0:
            JmxTexturesExtV0.handleTexturesV0(obj, map);
            break;
        case 1:
            JmxTexturesExtV1.handleTexturesV1(obj, map);
        }
    }
}
