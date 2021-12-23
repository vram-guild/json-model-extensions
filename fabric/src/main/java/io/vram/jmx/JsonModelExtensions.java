/*
 * This file is part of JSON Model Extensions and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
 */

package io.vram.jmx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import io.vram.jmx.api.JmxInitializer;

public class JsonModelExtensions implements ClientModInitializer {
	public static final Logger LOG = LogManager.getLogger("JMX");

	public static final String MODID = "json-model-extensions";

	private static boolean isEndpointInitializationNeeded = true;

	@Override
	public void onInitializeClient() {
		// NOOP
	}

	public static void initializeEndpointsOnce() {
		if (isEndpointInitializationNeeded) {
			FabricLoader.getInstance().getEntrypoints("jmx", JmxInitializer.class).forEach(api -> api.onInitalizeJmx());
			isEndpointInitializationNeeded = false;
		}
	}
}
