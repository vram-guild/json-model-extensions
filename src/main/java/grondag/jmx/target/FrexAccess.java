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

package grondag.jmx.target;

import grondag.jmx.json.ext.JmxMaterial;

import net.minecraft.client.render.model.json.ModelElement;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.util.TriState;

public interface FrexAccess {
	default boolean isFrexLoaded() {
		return false;
	}

	default boolean isFrexRendererAvailable() {
		return false;
	}

	default RenderMaterial loadMaterial(MaterialFinder finder, JmxMaterial jmxMat, ModelElement element, boolean usesAo, int spriteIndex) {
		finder.clear();

		final TriState diffuse = jmxMat.getDiffuse(spriteIndex);
		final boolean disableDiffuse = diffuse == TriState.DEFAULT ? !element.shade : !diffuse.get();
		finder.disableDiffuse(0, disableDiffuse);

		final TriState ao = jmxMat.getAo(spriteIndex);
		final boolean disableAo = ao == TriState.DEFAULT ? !usesAo : !ao.get();
		finder.disableAo(0, disableAo);

		finder.emissive(0, jmxMat.getEmissive(spriteIndex).get());

		if (jmxMat.getColorIndex(spriteIndex) == TriState.FALSE) {
			finder.disableColorIndex(0, true);
		}

		final BlendMode layer = jmxMat.getLayer(spriteIndex);
		if (layer != null) {
			finder.blendMode(0, layer);
		}

		return finder.find();
	}
}
