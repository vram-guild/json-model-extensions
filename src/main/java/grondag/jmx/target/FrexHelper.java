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

import grondag.frex.Frex;
import grondag.frex.FrexInitializer;
import grondag.frex.api.material.MaterialLoader;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.minecraft.util.Identifier;

public class FrexHelper implements FrexInitializer, FrexAccess {
	@Override
	public void onInitalizeFrex() {
		FrexHolder.setTarget(this);
	}

	@Override
	public boolean isFrexLoaded() {
		return true;
	}

	@Override
	public boolean isFrexRendererAvailable() {
		return Frex.isAvailable();
	}

	@Override
	public RenderMaterial loadFrexMaterial(Identifier id) {
		return MaterialLoader.loadMaterial(id);
	}
}
