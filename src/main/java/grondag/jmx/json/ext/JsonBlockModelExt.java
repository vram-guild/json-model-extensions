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

package grondag.jmx.json.ext;

import java.util.Map;

import com.mojang.datafixers.util.Either;

import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.jmx.json.JmxModelExt;

@Environment(EnvType.CLIENT)
public interface JsonBlockModelExt {
	JmxModelExt<?> jmx_modelExt();

	JsonBlockModelExt jmx_parent();

	ResourceLocation jmx_parentId();

	void jmx_parent(JsonBlockModelExt parent);

	Map<String, Either<Material, String>> jmx_textureMap();
}
