/*
 * Copyright © Original Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
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
		final File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "jmx.properties");
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
