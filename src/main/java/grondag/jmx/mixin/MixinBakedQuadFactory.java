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

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import grondag.jmx.json.model.BakedQuadFactoryExt;
import grondag.jmx.json.model.BakedQuadFactoryHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.render.model.BakedQuadFactory;
import net.minecraft.client.render.model.CubeFace;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ModelRotation;
import net.minecraft.client.render.model.json.ModelElement;
import net.minecraft.client.render.model.json.ModelElementFace;
import net.minecraft.client.render.model.json.ModelElementTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
@Mixin(BakedQuadFactory.class)
public abstract class MixinBakedQuadFactory implements BakedQuadFactoryExt {
    @Shadow
    protected abstract ModelElementTexture uvLock(ModelElementTexture tex, Direction face, net.minecraft.client.render.model.ModelRotation rotation);

    @Shadow
    protected abstract void method_3463(Vector3f vector3f_1, @Nullable net.minecraft.client.render.model.json.ModelRotation modelRotation_1);

    @Shadow
    protected abstract void method_3462(int[] data, Direction face);
    
    @Shadow
    protected abstract void method_3460(int[] ints_1, int int_1, int int_2, Vector3f vector3f_1, int int_3, Sprite sprite_1, ModelElementTexture modelElementTexture_1);
   
    @Override
    public void bake(QuadEmitter q, int spriteIndex, ModelElement element, ModelElementFace elementFace,  ModelElementTexture tex, Sprite sprite, Direction face, ModelBakeSettings bakeProps) {
        final BakedQuadFactoryHelper help = BakedQuadFactoryHelper.get();
        final net.minecraft.client.render.model.json.ModelRotation modelRotation = element.rotation;

        if (bakeProps.isUvLocked()) {
            tex = uvLock(elementFace.textureData, face, bakeProps.getRotation());
        }

        // preserve tex data in case needed again (can have two passes)
        final float[] uvs = help.uv;
        System.arraycopy(tex.uvs, 0, uvs, 0, BakedQuadFactoryHelper.UV_LEN);

        float uCent = (float)sprite.getWidth() / (sprite.getMaxU() - sprite.getMinU());
        float vCent = (float)sprite.getHeight() / (sprite.getMaxV() - sprite.getMinV());
        float uvCent = 4.0F / Math.max(vCent, uCent);
        float uAdj = (tex.uvs[0] + tex.uvs[0] + tex.uvs[2] + tex.uvs[2]) / 4.0F;
        float vAdj = (tex.uvs[1] + tex.uvs[1] + tex.uvs[3] + tex.uvs[3]) / 4.0F;
        tex.uvs[0] = MathHelper.lerp(uvCent, tex.uvs[0], uAdj);
        tex.uvs[2] = MathHelper.lerp(uvCent, tex.uvs[2], uAdj);
        tex.uvs[1] = MathHelper.lerp(uvCent, tex.uvs[1], vAdj);
        tex.uvs[3] = MathHelper.lerp(uvCent, tex.uvs[3], vAdj);
        int[] vertexData = buildVertexData(help.data, tex, sprite, face, normalizePos(help.pos, element.from, element.to), bakeProps.getRotation(), modelRotation);
        Direction nominalFace = BakedQuadFactory.method_3467(vertexData);
        
        // restore tex data
        System.arraycopy(uvs, 0, tex.uvs, 0, BakedQuadFactoryHelper.UV_LEN);
        
        if (modelRotation == null) {
            method_3462(vertexData, nominalFace);
        }
        
        if(spriteIndex == 0) {
            q.fromVanilla(vertexData, 0, false);
            q.colorIndex( elementFace.tintIndex);
        } else {
            q.sprite(0, spriteIndex, Float.intBitsToFloat(vertexData[4]), Float.intBitsToFloat(vertexData[5]));
            q.sprite(1, spriteIndex, Float.intBitsToFloat(vertexData[11]), Float.intBitsToFloat(vertexData[12]));
            q.sprite(2, spriteIndex, Float.intBitsToFloat(vertexData[18]), Float.intBitsToFloat(vertexData[19]));
            q.sprite(3, spriteIndex, Float.intBitsToFloat(vertexData[25]), Float.intBitsToFloat(vertexData[26]));
            q.spriteColor(spriteIndex, vertexData[3], vertexData[10], vertexData[17], vertexData[24]);
        }
    }

    private int[] buildVertexData(int[] target, ModelElementTexture tex, Sprite sprite, Direction face, float[] pos, ModelRotation texRotation, @Nullable net.minecraft.client.render.model.json.ModelRotation modelRotation) {
        for(int i = 0; i < 4; ++i) {
            bakeVertex(target, i, face, tex, pos, sprite, texRotation, modelRotation);
        }
        return target;
    }
    
    /** like method_3461 but doesn't apply diffuse shading */
    private void bakeVertex(int[] data, int vertexIn, Direction face, ModelElementTexture tex, float[] uvs, Sprite sprite, ModelRotation modelRotation_1, @Nullable net.minecraft.client.render.model.json.ModelRotation modelRotation) {
        CubeFace.Corner cubeFace$Corner_1 = CubeFace.method_3163(face).getCorner(vertexIn);
        Vector3f pos = new Vector3f(uvs[cubeFace$Corner_1.xSide], uvs[cubeFace$Corner_1.ySide], uvs[cubeFace$Corner_1.zSide]);
        method_3463(pos, modelRotation);
        int vertexOut = ((BakedQuadFactory)(Object)this).method_3455(pos, face, vertexIn, modelRotation_1);
        method_3460(data, vertexOut, vertexIn, pos, -1, sprite, tex);
    }

    private static float[] normalizePos(float [] targets, Vector3f vector3f_1, Vector3f vector3f_2) {
        targets[CubeFace.DirectionIds.WEST] = vector3f_1.x() / 16.0F;
        targets[CubeFace.DirectionIds.DOWN] = vector3f_1.y() / 16.0F;
        targets[CubeFace.DirectionIds.NORTH] = vector3f_1.z() / 16.0F;
        targets[CubeFace.DirectionIds.EAST] = vector3f_2.x() / 16.0F;
        targets[CubeFace.DirectionIds.UP] = vector3f_2.y() / 16.0F;
        targets[CubeFace.DirectionIds.SOUTH] = vector3f_2.z() / 16.0F;
        return targets;
    }
}
