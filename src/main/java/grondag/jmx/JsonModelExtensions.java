package grondag.jmx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ClientModInitializer;

//TODO:  tag
//TODO:  uv per layer
//TODO:  rotation per layer
public class JsonModelExtensions implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // nothing yet
    }
    
    public static final Logger LOG = LogManager.getLogger("JMX");
}
