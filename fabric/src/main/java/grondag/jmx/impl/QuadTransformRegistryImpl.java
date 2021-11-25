/*
 * Copyright © Original Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.jmx.impl;

import java.util.HashMap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;

import grondag.jmx.api.QuadTransformRegistry;

public class QuadTransformRegistryImpl implements QuadTransformRegistry {
	private final HashMap<ResourceLocation, QuadTransformSource> registeredQuadTransforms = new HashMap<>();

	@Override
	public void register(ResourceLocation id, QuadTransformSource quadTransformSource) {
		if (id == null) {
			throw new IllegalStateException("Cannot register a quad transform with null ID.");
		}

		if (registeredQuadTransforms.containsKey(id)) {
			throw new IllegalStateException("There is already a quad transform registered with the ID " + id);
		}

		registeredQuadTransforms.put(id, quadTransformSource);
	}

	@Nullable
	@Override
	public QuadTransformSource getQuadTransform(ResourceLocation id) {
		return registeredQuadTransforms.get(id);
	}
}
