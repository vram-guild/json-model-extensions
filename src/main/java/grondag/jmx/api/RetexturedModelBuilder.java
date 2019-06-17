package grondag.jmx.api;

import grondag.jmx.impl.RexturedModelBuilderImpl;
import net.minecraft.util.Identifier;

public interface RetexturedModelBuilder {
    
    public static RetexturedModelBuilder create(String sourceModel, String targetModel) {
        return RexturedModelBuilderImpl.create(sourceModel, targetModel);
    }
    
    public static RetexturedModelBuilder builder(Identifier sourceModel, Identifier targetModel) {
        return RexturedModelBuilderImpl.create(sourceModel, targetModel);
    }

    RetexturedModelBuilder mapSprite(Identifier from, Identifier to);

    RetexturedModelBuilder mapSprite(String from, String to);

    ModelTransformer build();
    
    public void complete();

    void completeBlock();

    void completeItem();

}
