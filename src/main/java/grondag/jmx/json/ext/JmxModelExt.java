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

package grondag.jmx.json.ext;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import grondag.jmx.JsonModelExtensions;
import grondag.jmx.target.FrexHolder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class JmxModelExt {
	public static final ThreadLocal<JmxModelExt> TRANSFER  = new ThreadLocal<>();

	private static final boolean FREX_RENDERER = FrexHolder.target().isFrexRendererAvailable();

	public JmxModelExt parent;

	private final Map<String, Object> materialMap;


	private JmxModelExt(Map<String, Object> materialMap) {
		this.materialMap = materialMap;
	}

	public boolean isEmpty() {
		return materialMap.isEmpty();
	}

	public JmxMaterial resolveMaterial(String matName) {
		return matName == null || materialMap == null ? JmxMaterial.DEFAULT : resolveMaterialInner(matName);
	}

	private JmxMaterial resolveMaterialInner(String matName) {
		if (!isMaterialReference(matName)) {
			matName = '#' + matName;
		}

		final Object result = resolveMaterial(matName, new MaterialResolutionContext(this));
		return result != null && result instanceof JmxMaterial ? (JmxMaterial) result : JmxMaterial.DEFAULT;
	}

	private Object resolveMaterial(Object val, MaterialResolutionContext context) {
		if (isMaterialReference(val)) {
			if (this == context.current) {
				JsonModelExtensions.LOG.warn("Unable to resolve material due to upward reference: {}", val);
				return JmxMaterial.DEFAULT;
			} else {
				Object result = materialMap.get(((String)val).substring(1));
				if(result != null && result instanceof JmxMaterial) {
					return result;
				}

				if (result == null && parent != null) {
					result = parent.resolveMaterial(val, context);
				}

				if (isMaterialReference(result)) {
					context.current = this;
					result = context.root.resolveMaterial(result, context);
				}

				return result;
			}
		} else {
			return val;
		}
	}

	public static boolean isMaterialReference(Object val) {
		return val instanceof String && ((String)val).charAt(0) == '#';
	}

	public static void deserialize(JsonObject jsonObjIn) {
		if(FREX_RENDERER && jsonObjIn.has("frex")) {
			deserializeInner(jsonObjIn.getAsJsonObject("frex"));
		} else if(jsonObjIn.has("jmx")) {
			deserializeInner(jsonObjIn.getAsJsonObject("jmx"));
		} else {
			TRANSFER.set(new JmxModelExt(Collections.emptyMap()));
		}
	}

	public static final class MaterialResolutionContext {
		public final JmxModelExt root;
		public JmxModelExt current;

		private MaterialResolutionContext(JmxModelExt root) {
			this.root = root;
		}
	}

	private static void deserializeInner(JsonObject jsonObj) {
		final Object2ObjectOpenHashMap<String, Object> map = new Object2ObjectOpenHashMap<>();
		if (jsonObj.has("materials")) {
			final JsonObject job = jsonObj.getAsJsonObject("materials");
			for (final Entry<String, JsonElement> e : job.entrySet()) {
				if (e.getValue().isJsonObject()) {
					map.put(e.getKey(), new JmxMaterial(e.getKey(), e.getValue().getAsJsonObject()));
				} else {
					map.put(e.getKey(), e.getValue().getAsString());
				}
			}
		}
		TRANSFER.set(new JmxModelExt(map));
	}
}
