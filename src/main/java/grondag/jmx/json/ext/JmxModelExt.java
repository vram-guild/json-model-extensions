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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import grondag.frex.api.material.MaterialLoader;
import grondag.jmx.JsonModelExtensions;
import grondag.jmx.target.FrexHolder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class JmxModelExt {
    private static final Renderer RENDERER = RendererAccess.INSTANCE.getRenderer();
    private static final RenderMaterial STANDARD_MATERIAL = RENDERER.materialById(RenderMaterial.MATERIAL_STANDARD);

	public static final ThreadLocal<JmxModelExt> TRANSFER  = new ThreadLocal<>();

	private static final boolean FREX_RENDERER = FrexHolder.target().isFrexRendererAvailable();

	public JmxModelExt parent;

	private final Map<String, Object> materialMap;
	private final int[] tags;
	private final int[] colors;
	@Nullable
	private final Identifier quadTransformId;


	private JmxModelExt(Map<String, Object> materialMap, int[] tags, int[] colors, @Nullable Identifier quadTransformId) {
		this.materialMap = materialMap;
        this.tags = tags;
        this.colors = colors;
        this.quadTransformId = quadTransformId;
	}

	public boolean isEmpty() {
		return materialMap.isEmpty() && getQuadTransformId() == null;
	}

	public boolean hasTag(int i) {
	    return 0 <= i && i < tags.length;
    }

	public int getTag(int i) {
	    return tags[i];
    }

    public boolean hasColor(int i) {
	    return 0 <= i && i < colors.length;
    }

    public int getColor(int i) {
	    return colors[i];
    }

	@Nullable
	public Identifier getQuadTransformId() {
		return quadTransformId == null && parent != null ? parent.getQuadTransformId() : quadTransformId;
	}

	public RenderMaterial resolveMaterial(String matName) {
        if (matName == null || materialMap == null) throw new RuntimeException("matName = " + matName + ", materialMap? " + (materialMap == null));
        return resolveMaterialInner(matName);
    }

	private RenderMaterial resolveMaterialInner(String matName) {
        final Object result = resolveMaterial(matName, new MaterialResolutionContext(this));
        if (result instanceof RenderMaterial) return (RenderMaterial) result;
        throw new RuntimeException("didn't get RenderMaterial for " + matName + ": " + result);
	}

	private Object resolveMaterial(Object val, MaterialResolutionContext context) {
		if (val instanceof String) {
			if (this == context.current) {
				JsonModelExtensions.LOG.warn("Unable to resolve material due to upward reference: {}", val);
				throw new RuntimeException("Upward reference " + val);
			} else {
				Object result = materialMap.get(((String)val).substring(1));
				if(result instanceof RenderMaterial) {
					return result;
				}

				if (result == null && parent != null) {
					result = parent.resolveMaterial(val, context);
				}

				if (isMaterialId(result)) {
				    return MaterialLoader.loadMaterial(new Identifier((String)result));
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

	public static boolean isMaterialId(Object val) {
	    return val instanceof String && ((String) val).charAt(0) != '#';
    }

	public static boolean isMaterialReference(Object val) {
		return val instanceof String && ((String) val).charAt(0) == '#';
	}

	public static void deserialize(JsonObject jsonObjIn) {
		if(FREX_RENDERER && jsonObjIn.has("frex")) {
			deserializeInner(jsonObjIn.getAsJsonObject("frex"));
		} else if(jsonObjIn.has("jmx")) {
			deserializeInner(jsonObjIn.getAsJsonObject("jmx"));
		} else {
			TRANSFER.set(new JmxModelExt(Collections.emptyMap(), new int[0], new int[0], null));
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
		    final JsonArray arr = jsonObj.getAsJsonArray("materials");
		    for (int i = 0, size = arr.size(); i < size; i++) {
		        final JsonObject material = arr.get(i).getAsJsonObject();
                for (final Entry<String, JsonElement> e : material.entrySet()) {
                    if (e.getValue().isJsonNull()) {
                        map.put(e.getKey() + i, RenderMaterial.MATERIAL_STANDARD.toString());
                    } else {
                        String value = e.getValue().getAsString();
                        if (value.charAt(0) == '#') {
                            map.put(e.getKey() + i, value + i);
                        } else {
                            map.put(e.getKey() + i, value);
                        }
                    }
                }
            }
		}

		final int[] tags;
		if (jsonObj.has("tags")) {
		    JsonArray arr = jsonObj.getAsJsonArray("tags");
		    tags = new int[arr.size()];
		    for (int i = 0, size = arr.size(); i < size; i++) {
		        tags[i] = arr.get(i).getAsInt();
            }
        } else {
		    tags = new int[0];
        }

		final int[] colors;
		if (jsonObj.has("colors")) {
		    JsonArray arr = jsonObj.getAsJsonArray("colors");
		    colors = new int[arr.size()];
		    for (int i = 0, size = arr.size(); i < size; i++) {
		        String color = arr.get(i).getAsString();
                colors[i] = color.startsWith("0x") ? Integer.parseUnsignedInt(color.substring(2), 16) : Integer.parseInt(color);
            }
        } else {
		    colors = new int[0];
        }

		final String idString = JsonHelper.getString(jsonObj, "quad_transform", null);
		final Identifier quadTransformId;
		if (idString != null) {
			quadTransformId = Identifier.tryParse(idString);
		} else {
			quadTransformId = null;
		}

		TRANSFER.set(new JmxModelExt(map, tags, colors, quadTransformId));
	}
}
