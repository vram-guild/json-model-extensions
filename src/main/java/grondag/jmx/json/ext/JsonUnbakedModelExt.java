package grondag.jmx.json.ext;

import net.minecraft.util.Identifier;

public interface JsonUnbakedModelExt {
    public JmxModelExt jmx_modelExt();

    JsonUnbakedModelExt jmx_parent();

    Identifier jmx_parentId();

    void jmx_parent(JsonUnbakedModelExt parent);
}
