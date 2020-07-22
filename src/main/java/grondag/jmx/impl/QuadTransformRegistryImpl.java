package grondag.jmx.impl;

import grondag.jmx.api.QuadTransformRegistry;
import net.minecraft.util.Identifier;

import javax.annotation.Nullable;
import java.util.HashMap;

public class QuadTransformRegistryImpl implements QuadTransformRegistry {
    private final HashMap<Identifier, QuadTransformSource> registeredQuadTransforms = new HashMap<>();

    @Override
    public void register(Identifier id, QuadTransformSource quadTransformSource) {
        if (registeredQuadTransforms.containsKey(id)) {
            throw new IllegalStateException("There is already a quad transform registered with the ID " + id);
        }

        registeredQuadTransforms.put(id, quadTransformSource);
    }

    @Nullable
    @Override
    public QuadTransformSource getQuadTransform(Identifier id) {
        return registeredQuadTransforms.get(id);
    }
}
