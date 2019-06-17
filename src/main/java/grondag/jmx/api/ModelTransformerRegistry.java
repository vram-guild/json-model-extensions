package grondag.jmx.api;

import grondag.jmx.impl.ModelTransformersImpl;

public interface ModelTransformerRegistry {
    public static ModelTransformerRegistry INSTANCE = ModelTransformersImpl.INSTANCE;
    
    void addBlock(String targetModel, String sourceModel, ModelTransformer transform);
    
    void addItem(String targetModel, String sourceModel, ModelTransformer transform);
    
    void add(String targetModel, String sourceModel, ModelTransformer transform);
}
