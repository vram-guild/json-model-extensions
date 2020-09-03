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

import net.minecraft.client.render.model.BakedQuadFactory;
import net.minecraft.client.render.model.CubeFace;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.json.ModelElement;
import net.minecraft.client.render.model.json.ModelElementFace;
import net.minecraft.client.render.model.json.ModelElementTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.AffineTransformation;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;

import grondag.jmx.json.model.BakedQuadFactoryExt;
import grondag.jmx.json.model.BakedQuadFactoryHelper;

@Environment(EnvType.CLIENT)
@Mixin(BakedQuadFactory.class)
public abstract class MixinBakedQuadFactory implements BakedQuadFactoryExt {
	@Shadow
	protected abstract void rotateVertex(Vector3f vector3f_1, @Nullable net.minecraft.client.render.model.json.ModelRotation modelRotation_1);

	@Shadow
	protected abstract void encodeDirection(int[] data, Direction face);

	@Override
	public void jmx_bake(QuadEmitter q, int spriteIndex, ModelElement element, ModelElementFace elementFace,  ModelElementTexture tex, Sprite sprite, Direction face, ModelBakeSettings bakeProps, Identifier modelId) {
		final BakedQuadFactoryHelper help = BakedQuadFactoryHelper.get();
		final net.minecraft.client.render.model.json.ModelRotation modelRotation = element.rotation;

		if (bakeProps.isShaded()) { // NB: yarn name is wrong
			tex = BakedQuadFactory.uvLock(elementFace.textureData, face, bakeProps.getRotation(), modelId);
		}

		// preserve tex data in case needed again (can have two passes)
		final float[] uvs = help.uv;
		System.arraycopy(tex.uvs, 0, uvs, 0, BakedQuadFactoryHelper.UV_LEN);

		final float uCent = sprite.getWidth() / (sprite.getMaxU() - sprite.getMinU());
		final float vCent = sprite.getHeight() / (sprite.getMaxV() - sprite.getMinV());
		final float uvCent = 4.0F / Math.max(vCent, uCent);
		final float uAdj = (tex.uvs[0] + tex.uvs[0] + tex.uvs[2] + tex.uvs[2]) / 4.0F;
		final float vAdj = (tex.uvs[1] + tex.uvs[1] + tex.uvs[3] + tex.uvs[3]) / 4.0F;
		tex.uvs[0] = MathHelper.lerp(uvCent, tex.uvs[0], uAdj);
		tex.uvs[2] = MathHelper.lerp(uvCent, tex.uvs[2], uAdj);
		tex.uvs[1] = MathHelper.lerp(uvCent, tex.uvs[1], vAdj);
		tex.uvs[3] = MathHelper.lerp(uvCent, tex.uvs[3], vAdj);
		final int[] vertexData = jmx_buildVertexData(help.data, tex, sprite, face, jmx_normalizePos(help.pos, element.from, element.to), bakeProps.getRotation(), modelRotation);
		final Direction nominalFace = BakedQuadFactory.decodeDirection(vertexData);

		// restore tex data
		System.arraycopy(uvs, 0, tex.uvs, 0, BakedQuadFactoryHelper.UV_LEN);

		if (modelRotation == null) {
			encodeDirection(vertexData, nominalFace);
		}

		q.nominalFace(nominalFace);
		q.spriteColor(spriteIndex, -1, -1, -1, -1);
		q.lightmap(0, 0, 0, 0);

		q.pos(0, Float.intBitsToFloat(vertexData[0]), Float.intBitsToFloat(vertexData[1]), Float.intBitsToFloat(vertexData[2]));
		q.sprite(0, spriteIndex, Float.intBitsToFloat(vertexData[4]), Float.intBitsToFloat(vertexData[5]));

		q.pos(1, Float.intBitsToFloat(vertexData[8]), Float.intBitsToFloat(vertexData[9]), Float.intBitsToFloat(vertexData[10]));
		q.sprite(1, spriteIndex, Float.intBitsToFloat(vertexData[12]), Float.intBitsToFloat(vertexData[13]));

		q.pos(2, Float.intBitsToFloat(vertexData[16]), Float.intBitsToFloat(vertexData[17]), Float.intBitsToFloat(vertexData[18]));
		q.sprite(2, spriteIndex, Float.intBitsToFloat(vertexData[20]), Float.intBitsToFloat(vertexData[21]));

		q.pos(3, Float.intBitsToFloat(vertexData[24]), Float.intBitsToFloat(vertexData[25]), Float.intBitsToFloat(vertexData[26]));
		q.sprite(3, spriteIndex, Float.intBitsToFloat(vertexData[28]), Float.intBitsToFloat(vertexData[29]));

		q.spriteBake(0, sprite, 0);
	}

	private int[] jmx_buildVertexData(int[] target, ModelElementTexture tex, Sprite sprite, Direction face, float[] pos, AffineTransformation texRotation, @Nullable net.minecraft.client.render.model.json.ModelRotation modelRotation) {
		for(int i = 0; i < 4; ++i) {
			jmx_bakeVertex(target, i, face, tex, pos, sprite, texRotation, modelRotation);
		}

		return target;
	}

	private void jmx_bakeVertex(int[] data, int vertexIn, Direction face, ModelElementTexture tex, float[] uvs, Sprite sprite, AffineTransformation modelRotation_1, @Nullable net.minecraft.client.render.model.json.ModelRotation modelRotation) {
		final CubeFace.Corner cubeFace$Corner_1 = CubeFace.getFace(face).getCorner(vertexIn);
		final Vector3f pos = new Vector3f(uvs[cubeFace$Corner_1.xSide], uvs[cubeFace$Corner_1.ySide], uvs[cubeFace$Corner_1.zSide]);
		rotateVertex(pos, modelRotation);
		((BakedQuadFactory)(Object)this).transformVertex(pos, modelRotation_1);
		jmx_packVertexData(data, vertexIn, pos, sprite, tex);
	}

	// NB: name must not conflict with vanilla names - somehow acts as an override if does, even though private
	private void jmx_packVertexData(int[] vertices, int cornerIndex, Vector3f position, Sprite sprite, ModelElementTexture modelElementTexture) {
		final int i = cornerIndex * 8;
		vertices[i] = Float.floatToRawIntBits(position.getX());
		vertices[i + 1] = Float.floatToRawIntBits(position.getY());
		vertices[i + 2] = Float.floatToRawIntBits(position.getZ());
		vertices[i + 3] = -1;
		vertices[i + 4] = Float.floatToRawIntBits(modelElementTexture.getU(cornerIndex));
		vertices[i + 4 + 1] = Float.floatToRawIntBits(modelElementTexture.getV(cornerIndex));
	}

	private static float[] jmx_normalizePos(float [] targets, Vector3f vector3f_1, Vector3f vector3f_2) {
		targets[CubeFace.DirectionIds.WEST] = vector3f_1.getX() / 16.0F;
		targets[CubeFace.DirectionIds.DOWN] = vector3f_1.getY() / 16.0F;
		targets[CubeFace.DirectionIds.NORTH] = vector3f_1.getZ() / 16.0F;
		targets[CubeFace.DirectionIds.EAST] = vector3f_2.getX() / 16.0F;
		targets[CubeFace.DirectionIds.UP] = vector3f_2.getY() / 16.0F;
		targets[CubeFace.DirectionIds.SOUTH] = vector3f_2.getZ() / 16.0F;
		return targets;
	}
}
