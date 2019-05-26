package grondag.jmx.json.model;

public class BakedQuadFactoryHelper {
    public static final int UV_LEN = 4;
    private static final ThreadLocal<BakedQuadFactoryHelper> HELPERS = ThreadLocal.withInitial(BakedQuadFactoryHelper::new);
    
    public static BakedQuadFactoryHelper get() {
        return HELPERS.get();
    }
    
    public final int data[] = new int[28];
    public final float uv[] = new float[UV_LEN];
    public final float pos[] = new float[6];
}