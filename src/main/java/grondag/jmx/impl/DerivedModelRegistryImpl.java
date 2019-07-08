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

package grondag.jmx.impl;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import grondag.jmx.JsonModelExtensions;
import grondag.jmx.json.ext.JsonUnbakedModelExt;
import grondag.jmx.json.model.JsonUnbakedModelHelper;
import grondag.jmx.json.model.LazyModelDelegate;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.model.ModelProviderContext;
import net.fabricmc.fabric.api.client.model.ModelProviderException;
import net.fabricmc.fabric.api.client.model.ModelVariantProvider;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public class DerivedModelRegistryImpl implements DerivedModelRegistry, ModelVariantProvider, Function<ResourceManager, ModelVariantProvider> {
    private DerivedModelRegistryImpl() {}
    
    public static final DerivedModelRegistryImpl INSTANCE = new DerivedModelRegistryImpl();
    
    private final Object2ObjectOpenHashMap<String, Pair<String, ModelTransformer>> blockModels = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<String, Pair<String, ModelTransformer>> itemModels = new Object2ObjectOpenHashMap<>();
    private boolean isEmpty = true;
    
    public boolean isEmpty() {
        return isEmpty;
    }
    
    @Override
    public void addBlock(String targetModel, String sourceModel, ModelTransformer transform) {
        isEmpty = false;
        blockModels.put(targetModel, Pair.of(sourceModel, transform));
    }
    
    @Override
    public void addItem(String targetModel, String sourceModel, ModelTransformer transform) {
        isEmpty = false;
        itemModels.put(targetModel, Pair.of(sourceModel, transform));
    }
    
    @Override
    public void addBlockWithItem(String targetModel, String sourceModel, ModelTransformer transform) {
        addBlock(targetModel, sourceModel, transform);
        addItem(targetModel, sourceModel, transform);
    }
    
    @Override
    public UnbakedModel loadModelVariant(ModelIdentifier modelId, ModelProviderContext context) throws ModelProviderException {
        final String fromString = modelId.getNamespace() + ":" + modelId.getPath();
        final Pair<String, ModelTransformer> match  = modelId.getVariant().equals("inventory")
                ? itemModels.get(fromString) :  blockModels.get(fromString);
                
        if(match != null) {
            final ModelIdentifier templateId = new ModelIdentifier(match.getLeft(), modelId.getVariant());
            final UnbakedModel template = context.loadModel(templateId);
            if(template instanceof JsonUnbakedModel) {
            	final JsonUnbakedModel jsonTemplate = (JsonUnbakedModel)template;
            	final Identifier parentId = ((JsonUnbakedModelExt)template).jmx_parentId();
                if (parentId.getNamespace().equals("minecraft") && parentId.getPath().equals("item/generated") && match.getRight() instanceof RetexturedModelTransformer) {
                	return JsonUnbakedModelHelper.remap(jsonTemplate, ((RetexturedModelTransformer)match.getRight()).textureMap);
                }
            } 
            return new LazyModelDelegate(templateId, match.getRight());
        }
        return null;
    }

    @Override
    public ModelVariantProvider apply(ResourceManager resourceManager) {
        JsonModelExtensions.initializeEndpointsOnce();
        return this;
    }
}
