package grondag.jmx.json;

import com.google.gson.JsonObject;
import grondag.jmx.json.model.BakedQuadFactoryExt;
import grondag.jmx.json.v0.JmxModelExtV0;
import grondag.jmx.json.v1.JmxModelExtV1;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuadFactory;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.util.function.Function;

public abstract class JmxModelExt<Self extends JmxModelExt<Self>> {
    public static final ThreadLocal<JmxModelExt<?>> TRANSFER  = new ThreadLocal<>();
    public static final ThreadLocal<Integer> VERSION = new ThreadLocal<>();

    public Self parent;

    public static void deserialize(JsonObject obj) {
        final JmxModelExt<?> modelExt;
        final int version = JsonHelper.getInt(obj, "jmx_version", 0);
        VERSION.set(version);
        switch (version) {
        default:
        case 0:
            modelExt = JmxModelExtV0.deserializeV0(obj);
            break;
        case 1:
            modelExt = JmxModelExtV1.deserializeV1(obj);
            break;
        }
        TRANSFER.set(modelExt);
    }

    public abstract int version();

    /**
     * Checks whether any model in the hierarchy has any JMX data.
     * If a ModelExt is empty, its associated model will be formed by vanilla.
     */
    public boolean hierarchyIsEmpty() {
        return selfIsEmpty() && (parent == null || parent.hierarchyIsEmpty());
    }

    /**
     * Checks whether only the current model has any JMX data.
     */
    public abstract boolean selfIsEmpty();

    public abstract BakedModel buildModel(ModelOverrideList modelOverrideList, boolean hasDepth, Sprite particleSprite, ModelBakeSettings bakeProps, Identifier modelId, JsonUnbakedModel me, Function<SpriteIdentifier, Sprite> textureGetter);

    protected static final BakedQuadFactory QUADFACTORY = new BakedQuadFactory();
    protected static final BakedQuadFactoryExt QUADFACTORY_EXT = (BakedQuadFactoryExt)QUADFACTORY;
}
