/*******************************************************************************
 * Copyright 2019 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.jmx.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import grondag.jmx.json.ext.ModelEntryAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.WeightedPicker;

@Environment(EnvType.CLIENT)
@Mixin(targets = {"net/minecraft/client/render/model/WeightedBakedModel$Entry"})
public abstract class MixinWeightedBakedModelEntry extends WeightedPicker.Entry implements ModelEntryAccess {
	public MixinWeightedBakedModelEntry(int weight) {
		super(weight);
	}

	@Shadow
	protected BakedModel model;

	@Override
	public BakedModel jmx_getModel() {
		return model;
	}

	@Override
	public int jmx_getWeight() {
		return weight;
	}
}
