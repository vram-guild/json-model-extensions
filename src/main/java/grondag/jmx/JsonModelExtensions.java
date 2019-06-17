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

import grondag.jmx.impl.ModelTransformersImpl;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;

@Environment(EnvType.CLIENT)
public class JsonModelExtensions implements ClientModInitializer {
    public static final Logger LOG = LogManager.getLogger("JMX");
    
    public static final String MODID = "json-model-extensions";

    @Override
    public void onInitializeClient() {
        Configurator.init();
        ModelLoadingRegistry.INSTANCE.registerVariantProvider(ModelTransformersImpl.INSTANCE);
    }
}
