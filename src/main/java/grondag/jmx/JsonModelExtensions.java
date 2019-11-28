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

package grondag.jmx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import grondag.jmx.api.JmxInitializer;
import grondag.jmx.impl.DerivedModelRegistryImpl;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.loader.api.FabricLoader;

@Environment(EnvType.CLIENT)
public class JsonModelExtensions implements ClientModInitializer {
	public static final Logger LOG = LogManager.getLogger("JMX");

	public static final String MODID = "json-model-extensions";

	private static boolean isEndpointInitializationNeeded = true;
	private static boolean isClientInitialized = false;

	@Override
	public void onInitializeClient() {
		ModelLoadingRegistry.INSTANCE.registerVariantProvider(DerivedModelRegistryImpl.INSTANCE);
		isClientInitialized = true;
	}

	public static void initializeEndpointsOnce() {
		if(isClientInitialized && isEndpointInitializationNeeded) {
			FabricLoader.getInstance().getEntrypoints("jmx", JmxInitializer.class).forEach(
				api -> api.onInitalizeJmx());

			isEndpointInitializationNeeded = false;
		}
	}
}
