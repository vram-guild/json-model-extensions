package grondag.jmx.json.ext;

public interface JmxExtension<T> {
    T jmx_ext();
    
    void jmx_ext(T val);
}
