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

package grondag.jmx.api;

import net.minecraft.util.Identifier;

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
		return builder(new Identifier(sourceModel), new Identifier(targetModel));
	}

	static RetexturedModelBuilder builder(Identifier sourceModel, Identifier targetModel) {
		return RexturedModelBuilderImpl.builder(sourceModel, targetModel);
	}

	RetexturedModelBuilder mapSprite(Identifier from, Identifier to);

	RetexturedModelBuilder mapSprite(String from, String to);

	void completeBlockWithItem();

	void completeBlock();

	void completeItem();
}
