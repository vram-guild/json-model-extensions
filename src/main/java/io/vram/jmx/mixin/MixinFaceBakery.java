/*
 * Copyright © Contributing Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package io.vram.jmx.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.math.Transformation;
import com.mojang.math.Vector3f;

import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import io.vram.frex.api.buffer.QuadEmitter;
import io.vram.jmx.json.model.BakedQuadFactoryExt;
import io.vram.jmx.json.model.BakedQuadFactoryHelper;

@Mixin(FaceBakery.class)
public abstract class MixinFaceBakery implements BakedQuadFactoryExt {
	@Shadow
	protected abstract void applyElementRotation(Vector3f vector3f_1, @Nullable net.minecraft.client.renderer.block.model.BlockElementRotation modelRotation_1);

	@Shadow
	protected abstract void recalculateWinding(int[] data, Direction face);

	@Override
	public void jmx_bake(QuadEmitter q, int spriteIndex, BlockElement element, BlockElementFace elementFace, BlockFaceUV tex, TextureAtlasSprite sprite, Direction face, ModelState bakeProps, ResourceLocation modelId) {
		final BakedQuadFactoryHelper help = BakedQuadFactoryHelper.get();
		final net.minecraft.client.renderer.block.model.BlockElementRotation modelRotation = element.rotation;

		if (bakeProps.isUvLocked()) {
			tex = FaceBakery.recomputeUVs(elementFace.uv, face, bakeProps.getRotation(), modelId);
		}

		// preserve tex data in case needed again (can have two passes)
		final float[] uvs = help.uv;
		System.arraycopy(tex.uvs, 0, uvs, 0, BakedQuadFactoryHelper.UV_LEN);

		final float uCent = sprite.getWidth() / (sprite.getU1() - sprite.getU0());
		final float vCent = sprite.getHeight() / (sprite.getV1() - sprite.getV0());
		final float uvCent = 4.0F / Math.max(vCent, uCent);
		final float uAdj = (tex.uvs[0] + tex.uvs[0] + tex.uvs[2] + tex.uvs[2]) / 4.0F;
		final float vAdj = (tex.uvs[1] + tex.uvs[1] + tex.uvs[3] + tex.uvs[3]) / 4.0F;
		tex.uvs[0] = Mth.lerp(uvCent, tex.uvs[0], uAdj);
		tex.uvs[2] = Mth.lerp(uvCent, tex.uvs[2], uAdj);
		tex.uvs[1] = Mth.lerp(uvCent, tex.uvs[1], vAdj);
		tex.uvs[3] = Mth.lerp(uvCent, tex.uvs[3], vAdj);
		final int[] vertexData = jmx_buildVertexData(help.data, tex, sprite, face, jmx_normalizePos(help.pos, element.from, element.to), bakeProps.getRotation(), modelRotation);
		final Direction nominalFace = FaceBakery.calculateFacing(vertexData);

		// restore tex data
		System.arraycopy(uvs, 0, tex.uvs, 0, BakedQuadFactoryHelper.UV_LEN);

		if (modelRotation == null) {
			recalculateWinding(vertexData, nominalFace);
		}

		q.nominalFace(nominalFace);
		q.vertexColor(-1, -1, -1, -1);
		q.lightmap(0, 0, 0, 0);

		q.pos(0, Float.intBitsToFloat(vertexData[0]), Float.intBitsToFloat(vertexData[1]), Float.intBitsToFloat(vertexData[2]));
		q.uv(0, Float.intBitsToFloat(vertexData[4]), Float.intBitsToFloat(vertexData[5]));

		q.pos(1, Float.intBitsToFloat(vertexData[8]), Float.intBitsToFloat(vertexData[9]), Float.intBitsToFloat(vertexData[10]));
		q.uv(1, Float.intBitsToFloat(vertexData[12]), Float.intBitsToFloat(vertexData[13]));

		q.pos(2, Float.intBitsToFloat(vertexData[16]), Float.intBitsToFloat(vertexData[17]), Float.intBitsToFloat(vertexData[18]));
		q.uv(2, Float.intBitsToFloat(vertexData[20]), Float.intBitsToFloat(vertexData[21]));

		q.pos(3, Float.intBitsToFloat(vertexData[24]), Float.intBitsToFloat(vertexData[25]), Float.intBitsToFloat(vertexData[26]));
		q.uv(3, Float.intBitsToFloat(vertexData[28]), Float.intBitsToFloat(vertexData[29]));

		q.spriteBake(sprite, 0);
	}

	private int[] jmx_buildVertexData(int[] target, BlockFaceUV tex, TextureAtlasSprite sprite, Direction face, float[] pos, Transformation texRotation, @Nullable net.minecraft.client.renderer.block.model.BlockElementRotation modelRotation) {
		for (int i = 0; i < 4; ++i) {
			jmx_bakeVertex(target, i, face, tex, pos, sprite, texRotation, modelRotation);
		}

		return target;
	}

	private void jmx_bakeVertex(int[] data, int vertexIn, Direction face, BlockFaceUV tex, float[] uvs, TextureAtlasSprite sprite, Transformation modelRotation_1, @Nullable net.minecraft.client.renderer.block.model.BlockElementRotation modelRotation) {
		final FaceInfo.VertexInfo cubeFace$Corner_1 = FaceInfo.fromFacing(face).getVertexInfo(vertexIn);
		final Vector3f pos = new Vector3f(uvs[cubeFace$Corner_1.xFace], uvs[cubeFace$Corner_1.yFace], uvs[cubeFace$Corner_1.zFace]);
		applyElementRotation(pos, modelRotation);
		((FaceBakery) (Object) this).applyModelRotation(pos, modelRotation_1);
		jmx_packVertexData(data, vertexIn, pos, sprite, tex);
	}

	// NB: name must not conflict with vanilla names - somehow acts as an override if does, even though private
	private void jmx_packVertexData(int[] vertices, int cornerIndex, Vector3f position, TextureAtlasSprite sprite, BlockFaceUV modelElementTexture) {
		final int i = cornerIndex * 8;
		vertices[i] = Float.floatToRawIntBits(position.x());
		vertices[i + 1] = Float.floatToRawIntBits(position.y());
		vertices[i + 2] = Float.floatToRawIntBits(position.z());
		vertices[i + 3] = -1;
		vertices[i + 4] = Float.floatToRawIntBits(modelElementTexture.getU(cornerIndex));
		vertices[i + 4 + 1] = Float.floatToRawIntBits(modelElementTexture.getV(cornerIndex));
	}

	private static float[] jmx_normalizePos(float[] targets, Vector3f vector3f_1, Vector3f vector3f_2) {
		targets[FaceInfo.Constants.MIN_X] = vector3f_1.x() / 16.0F;
		targets[FaceInfo.Constants.MIN_Y] = vector3f_1.y() / 16.0F;
		targets[FaceInfo.Constants.MIN_Z] = vector3f_1.z() / 16.0F;
		targets[FaceInfo.Constants.MAX_X] = vector3f_2.x() / 16.0F;
		targets[FaceInfo.Constants.MAX_Y] = vector3f_2.y() / 16.0F;
		targets[FaceInfo.Constants.MAX_Z] = vector3f_2.z() / 16.0F;
		return targets;
	}
}
