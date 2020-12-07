/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.jmx;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

@Environment(EnvType.CLIENT)
public class Configurator {
	public static boolean loadVanillaModels;
	public static boolean logResolutionErrors;

	static {
		final File configFile = new File(FabricLoader.getInstance().getConfigDirectory(), "jmx.properties");
		final Properties properties = new Properties();

		if (configFile.exists()) {
			try (FileInputStream stream = new FileInputStream(configFile)) {
				properties.load(stream);
			} catch (final IOException e) {
				JsonModelExtensions.LOG.warn("[JMX] Could not read property file '" + configFile.getAbsolutePath() + "'", e);
			}
		}

		loadVanillaModels = ((String) properties.computeIfAbsent("load-vanilla-models", (a) -> "false")).toLowerCase(Locale.ROOT).equals("true");
		logResolutionErrors = ((String) properties.computeIfAbsent("log-resolution-errors", (a) -> "false")).toLowerCase(Locale.ROOT).equals("true");

		try (FileOutputStream stream = new FileOutputStream(configFile)) {
			properties.store(stream, "JMX properties file");
		} catch (final IOException e) {
			JsonModelExtensions.LOG.warn("[JMX] Could not store property file '" + configFile.getAbsolutePath() + "'", e);
		}
	}
}
