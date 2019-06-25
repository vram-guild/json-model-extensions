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

package grondag.jmx;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import blue.endless.jankson.Comment;
import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;

@Environment(EnvType.CLIENT)
public class Configurator {

    @SuppressWarnings("hiding")
    static class ConfigData {
        @Comment("Load all model as meshes. Ignored (and true) if any model transformers are in use.")
        boolean loadVanillaModels = false;
    }
    
    static final ConfigData DEFAULTS = new ConfigData();
    private static final Gson GSON = new GsonBuilder().create();
    private static final Jankson JANKSON = Jankson.builder().build();
    
    public static boolean loadVanillaModels = DEFAULTS.loadVanillaModels;
    
    /** use to stash parent screen during display */
    private static Screen screenIn;
    
    private static File configFile;
    
    public static void init() {
        configFile = new File(FabricLoader.getInstance().getConfigDirectory(), "jmx.json5");
        if(configFile.exists()) {
            loadConfig();
        } else {
            saveConfig();
        }
    }
    
    private static void loadConfig() {
        ConfigData config = new ConfigData();
        try {
            JsonObject configJson = JANKSON.load(configFile);
            String regularized = configJson.toJson(false, false, 0);
            config = GSON.fromJson(regularized, ConfigData.class);
        } catch (Exception e) {
            e.printStackTrace();
            JsonModelExtensions.LOG.error("Unable to load config. Using default values.");
        }
        loadVanillaModels = config.loadVanillaModels;
    }

    private static void saveConfig() {
        ConfigData config = new ConfigData();
        config.loadVanillaModels = loadVanillaModels;
        
        
        try {
            String result = JANKSON.toJson(config).toJson(true, true, 0);
            if (!configFile.exists())
                configFile.createNewFile();
            
            try(
                    FileOutputStream out = new FileOutputStream(configFile, false);
            ) {
                out.write(result.getBytes());
                out.flush();
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            JsonModelExtensions.LOG.error("Unable to save config.");
            return;
        }
    }
    
    private static Screen display() {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(screenIn).setTitle("config.jmx.title").setSavingRunnable(Configurator::saveUserInput);
        
        // FEATURES
        ConfigCategory features = builder.getOrCreateCategory("config.jmx.category.features");
        
        features.addEntry(new BooleanListEntry("config.jmx.value.load_vanilla", loadVanillaModels, "config.jmx.reset", 
                () -> DEFAULTS.loadVanillaModels, b -> loadVanillaModels = b, 
                () -> Optional.of(I18n.translate("config.jmx.help.load_vanilla").split(";"))));
        
        builder.setDoesConfirmSave(false);
        
        return builder.build();
    }
    
    public static Optional<Supplier<Screen>> getConfigScreen(Screen screen) {
        screenIn = screen;
        return Optional.of(Configurator::display);
    }
    
    public static Screen getRawConfigScreen(Screen screen) {
        screenIn = screen;
        return display();
    }
    
    private static void saveUserInput() {
        saveConfig();
    }
}
