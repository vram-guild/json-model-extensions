/*
 * Copyright © Contributing Authors
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

package grondag.jmx.api;

import net.minecraft.resources.ResourceLocation;

import grondag.jmx.impl.RexturedModelBuilderImpl;

/**
 * Use to create models by re-texturing existing JSON block and or item models.
 * Blocks and Items with re-textured models do NOT need JSON files.  They will
 * automatically be assigned a model that is a functional copy of the "template"
 * block/item with textured replaced as specified using this registry.
 *
 * <p>The "target block/item must have properties that match the template block/item.
 */
public interface RetexturedModelBuilder {
	static RetexturedModelBuilder builder(String sourceModel, String targetModel) {
		return builder(new ResourceLocation(sourceModel), new ResourceLocation(targetModel));
	}

	static RetexturedModelBuilder builder(ResourceLocation sourceModel, ResourceLocation targetModel) {
		return RexturedModelBuilderImpl.builder(sourceModel, targetModel);
	}

	RetexturedModelBuilder mapSprite(ResourceLocation from, ResourceLocation to);

	RetexturedModelBuilder mapSprite(String from, String to);

	void completeBlockWithItem();

	void completeBlock();

	void completeItem();
}
