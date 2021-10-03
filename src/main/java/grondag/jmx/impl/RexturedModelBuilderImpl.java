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

package grondag.jmx.impl;

import net.minecraft.resources.ResourceLocation;

import grondag.jmx.api.RetexturedModelBuilder;

public class RexturedModelBuilderImpl implements RetexturedModelBuilder {
	public static RetexturedModelBuilder builder(ResourceLocation sourceModel, ResourceLocation targetModel) {
		return new RexturedModelBuilderImpl(sourceModel, targetModel);
	}

	RetexturedModelTransformer.Builder builder;

	private void checkNotComplete() {
		if (builder == null) {
			throw new IllegalStateException("Attempt to modify RetextureModelBuilder after complete().");
		}
	}

	RexturedModelBuilderImpl(ResourceLocation sourceModel, ResourceLocation targetModel) {
		builder = RetexturedModelTransformer.builder(sourceModel, targetModel);
	}

	@Override
	public RetexturedModelBuilder mapSprite(ResourceLocation from, ResourceLocation to) {
		checkNotComplete();
		builder.mapSprite(from, to);
		return this;
	}

	@Override
	public RetexturedModelBuilder mapSprite(String from, String to) {
		checkNotComplete();
		builder.mapSprite(from, to);
		return this;
	}

	public RetexturedModelTransformer build() {
		checkNotComplete();
		final RetexturedModelTransformer result = builder.build();
		builder = null;
		return result;
	}

	@Override
	public void completeBlock() {
		final RetexturedModelTransformer transform = build();
		DerivedModelRegistryImpl.INSTANCE.addBlock(transform.targetModel.toString(), transform.sourceModel.toString(), transform);
	}

	@Override
	public void completeItem() {
		final RetexturedModelTransformer transform = build();
		DerivedModelRegistryImpl.INSTANCE.addItem(transform.targetModel.toString(), transform.sourceModel.toString(), transform);
	}

	@Override
	public void completeBlockWithItem() {
		final RetexturedModelTransformer transform = build();
		DerivedModelRegistryImpl.INSTANCE.addBlockWithItem(transform.targetModel.toString(), transform.sourceModel.toString(), transform);
	}
}
