package grondag.jmx.impl;

import java.util.HashMap;

import grondag.jmx.api.QuadTransformRegistry;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Identifier;

public class QuadTransformRegistryImpl implements QuadTransformRegistry {
	private final HashMap<Identifier, QuadTransformSource> registeredQuadTransforms = new HashMap<>();

	@Override
	public void register(Identifier id, QuadTransformSource quadTransformSource) {
		if (id == null ){
			throw new IllegalStateException("Cannot register a quad transform with null ID.");
		}

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
