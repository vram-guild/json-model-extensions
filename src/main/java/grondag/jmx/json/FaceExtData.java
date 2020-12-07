package grondag.jmx.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import grondag.jmx.json.v0.FaceExtDataV0;
import grondag.jmx.json.v1.FaceExtDataV1;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.util.SpriteIdentifier;

import java.util.HashSet;
import java.util.function.Supplier;

public abstract class FaceExtData {
    public static FaceExtData empty() {
        switch (JmxModelExt.VERSION.get()) {
            default:
            case 0:
                return FaceExtDataV0.EMPTY;
            case 1:
                return FaceExtDataV1.EMPTY;
        }
    }

    public static final ThreadLocal<FaceExtData> TRANSFER  = new ThreadLocal<>();

    public static void deserialize(JsonObject jsonObj, JsonDeserializationContext context) {
        final FaceExtData faceExt;
        switch (JmxModelExt.VERSION.get()) {
        default:
        case 0:
            faceExt = FaceExtDataV0.deserializeV0(jsonObj, context);
            break;
        case 1:
            faceExt = FaceExtDataV1.deserializeV1(jsonObj, context);
            break;
        }
        TRANSFER.set(faceExt);
    }

    /**
     * If all <code>FaceExtData</code>s in a model are empty, the model will be formed by vanilla.
     */
    public abstract boolean isEmpty();

    public abstract void getTextureDependencies(JsonUnbakedModel model, Supplier<HashSet<Pair<String, String>>> errors, Supplier<HashSet<SpriteIdentifier>> deps);
}
