/*
 * Copyright © Original Authors
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

package grondag.jmx.json.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class BakedQuadFactoryHelper {
	public static final int UV_LEN = 4;
	private static final ThreadLocal<BakedQuadFactoryHelper> HELPERS = ThreadLocal.withInitial(BakedQuadFactoryHelper::new);

	public static BakedQuadFactoryHelper get() {
		return HELPERS.get();
	}

	public final int[] data = new int[32];
	public final float[] uv = new float[UV_LEN];
	public final float[] pos = new float[6];
}
