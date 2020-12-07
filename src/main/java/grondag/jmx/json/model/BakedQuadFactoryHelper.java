/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
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
