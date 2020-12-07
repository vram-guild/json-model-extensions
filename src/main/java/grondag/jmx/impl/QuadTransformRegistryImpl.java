/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.jmx.impl;

import java.util.HashMap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Identifier;

import grondag.jmx.api.QuadTransformRegistry;

public class QuadTransformRegistryImpl implements QuadTransformRegistry {
	private final HashMap<Identifier, QuadTransformSource> registeredQuadTransforms = new HashMap<>();

	@Override
	public void register(Identifier id, QuadTransformSource quadTransformSource) {
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
	public QuadTransformSource getQuadTransform(Identifier id) {
		return registeredQuadTransforms.get(id);
	}
}
