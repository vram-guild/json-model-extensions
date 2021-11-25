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

import net.fabricmc.fabric.api.renderer.v1.render.RenderContext.QuadTransform;

/**
 * Functions used by transformable models to transform their internal state.
 */
public interface TransformableModelContext {
	/** Used to transform particle sprite or other sprites not part of a mesh. */
	SpriteMap spriteTransform();

	/** Apply to all meshes in the model. */
	QuadTransform quadTransform();

	/**
	 * For multi-part models, use to map new model states back to original state
	 * so that predicate structure can be reused.
	 * */
	InverseStateMap inverseStateMap();
}
