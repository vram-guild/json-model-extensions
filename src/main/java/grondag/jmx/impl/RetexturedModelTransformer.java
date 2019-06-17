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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableMap;

import grondag.jmx.api.InverseStateMap;
import grondag.jmx.api.ModelTransformer;
import grondag.jmx.api.SpriteTransform;
import grondag.jmx.api.TransformableModel;
import grondag.jmx.api.TransformableModelContext;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext.QuadTransform;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.state.StateFactory;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class RetexturedModelTransformer implements ModelTransformer, TransformableModelContext {

    public final Identifier targetModel; 
    public final Identifier sourceModel;
    
    private final ImmutableMap<Identifier, Identifier> textureMap;
    
    private Object2ObjectOpenHashMap<BlockState, BlockState> inverseStateMap = null;
    
    private RetexturedModelTransformer(Identifier sourceModel, Identifier targetModel, ImmutableMap<Identifier, Identifier> textureMap) {
        this.sourceModel = sourceModel;
        this.targetModel = targetModel;
        this.textureMap = textureMap;
    }
    
    @Override
    public Collection<Identifier> textures() {
        return textureMap.values();
    }

    @Override
    public BakedModel transform(BakedModel model) {
        return model instanceof TransformableModel 
                ? ((TransformableModel)model).transform(this)
                : MinecraftClient.getInstance().getBakedModelManager().getMissingModel();
    }
    
    public static Builder builder(String sourceModel, String targetModel) {
        return new Builder(new Identifier(sourceModel), new Identifier(targetModel));
    }
    
    public static Builder builder(Identifier sourceModel, Identifier targetModel) {
        return new Builder(sourceModel, targetModel);
    }
    
    public static class Builder {
        private final ImmutableMap.Builder<Identifier, Identifier> builder = ImmutableMap.builder();
        
        final Identifier sourceModel;
        final Identifier targetModel; 
        
        private Builder(Identifier sourceModel, Identifier targetModel) {
            this.sourceModel = sourceModel;
            this.targetModel = targetModel;
        }
        
        public Builder mapSprite(Identifier from, Identifier to) {
            builder.put(from, to);
            return this;
        }
        
        public Builder mapSprite(String from, String to) {
            builder.put(new Identifier(from), new Identifier(to));
            return this;
        }
        
        public RetexturedModelTransformer build() {
            return new RetexturedModelTransformer(sourceModel, targetModel, builder.build());
        }
    }

    private static void remapSprite(MutableQuadView q, Sprite oldSprite, Sprite newSprite, int spriteIndex) {
        for(int i = 0; i < 4; i++) {
            float u = q.spriteU(i, spriteIndex);
            float v = q.spriteV(i, spriteIndex);
            u = newSprite.getU((double)oldSprite.getXFromU(u));
            v = newSprite.getV((double)oldSprite.getYFromV(v));
            q.sprite(i, spriteIndex, u, v);
        }
    }
    
    public boolean transform(MutableQuadView quad) {
        SpriteAtlasTexture atlas = MinecraftClient.getInstance().getSpriteAtlas();
        SpriteFinder sf = SpriteFinder.get(atlas);
        Sprite oldSprite = sf.find(quad, 0);
        Sprite newSprite = spriteTransform().mapSprite(oldSprite, atlas);
        remapSprite(quad, oldSprite, newSprite, 0);
        final int depth = quad.material().spriteDepth();
        if(depth == 2) {
            oldSprite = sf.find(quad, 1);
            newSprite = spriteTransform().mapSprite(oldSprite, atlas);
            remapSprite(quad, oldSprite, newSprite, 1);
        }
        return true;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object2ObjectOpenHashMap<BlockState, BlockState> inverseStateMapImpl() {
        Object2ObjectOpenHashMap<BlockState, BlockState> result = inverseStateMap;
        if(result == null) {
            final Object2ObjectOpenHashMap<BlockState, BlockState> newMap = new Object2ObjectOpenHashMap<>();
            final BlockState defaultState = Registry.BLOCK.get(targetModel).getDefaultState();
            
            StateFactory<Block, BlockState> factory = Registry.BLOCK.get(sourceModel).getStateFactory();
            factory.getStates().forEach(s -> {
                BlockState targetState = defaultState;
                final ImmutableMap<Property<?>, Comparable<?>> props = s.getEntries();
                if(!props.isEmpty()) {
                    Iterator<Entry<Property<?>, Comparable<?>>> it = props.entrySet().iterator();
                    while(it.hasNext()) {
                        Entry<Property<?>, Comparable<?>> prop = it.next();
                        Property p = prop.getKey();
                        Comparable v = prop.getValue();
                        targetState = targetState.with(p, v);
                    }
                }
                newMap.put(targetState, s);
            });
            result = newMap;
            inverseStateMap = newMap;
        }
        return result;
    }

    @Override
    public QuadTransform quadTransform() {
        return this::transform;
    }

    @Override
    public InverseStateMap inverseStateMap() {
        return inverseStateMapImpl()::get;
    }

    @Override
    public SpriteTransform spriteTransform() {
        return textureMap::get;
    }
}
