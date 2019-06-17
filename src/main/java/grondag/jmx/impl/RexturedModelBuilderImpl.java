package grondag.jmx.impl;

import grondag.jmx.api.RetexturedModelBuilder;
import net.minecraft.util.Identifier;

public class RexturedModelBuilderImpl implements RetexturedModelBuilder {
    public static RetexturedModelBuilder create(String sourceModel, String targetModel) {
        return new RexturedModelBuilderImpl(sourceModel, targetModel);
    }
    
    public static RetexturedModelBuilder create(Identifier sourceModel, Identifier targetModel) {
        return new RexturedModelBuilderImpl(sourceModel, targetModel);
    }
        
    RetexturedModelTransformer.Builder builder;
    
    private void checkNotComplete() {
        if(builder == null) throw new IllegalStateException("Attempt to modify RetextureModelBuilder after complete().");
    }
    
    RexturedModelBuilderImpl(String sourceModel, String targetModel) {
        builder =RetexturedModelTransformer.builder(sourceModel, targetModel);
    }
    
    RexturedModelBuilderImpl(Identifier sourceModel, Identifier targetModel) {
        builder =RetexturedModelTransformer.builder(sourceModel, targetModel);
    }

    @Override
    public RetexturedModelBuilder mapSprite(Identifier from, Identifier to) {
        checkNotComplete();
        builder.mapSprite(from, to);
        return this;
    }

    @Override
    public RetexturedModelBuilder mapSprite(String from, String to) {
        checkNotComplete();
        builder.mapSprite(from, to);
        return this;
    }
    
    @Override
    public RetexturedModelTransformer build() {
        checkNotComplete();
        RetexturedModelTransformer result = builder.build();
        builder = null;
        return result;
    }

    @Override
    public void completeBlock() {
        RetexturedModelTransformer transform = build();
        ModelTransformersImpl.INSTANCE.addBlock(transform.targetModel.toString(), transform.sourceModel.toString(), transform);
    }
    
    @Override
    public void completeItem() {
        RetexturedModelTransformer transform = build();
        ModelTransformersImpl.INSTANCE.addBlock(transform.targetModel.toString(), transform.sourceModel.toString(), transform);
    }

    @Override
    public void complete() {
        RetexturedModelTransformer transform = build();
        ModelTransformersImpl.INSTANCE.add(transform.targetModel.toString(), transform.sourceModel.toString(), transform);
    }
}
