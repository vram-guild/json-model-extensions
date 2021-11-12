/*
 * Copyright © Contributing Authors
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

package io.vram.jmx.api;

/**
 * Use to make JMX an optional dependency. To do so, implement this interface
 * in a stand-alone class and declare a "jmx" end point in the mod's
 * fabric.mod.json that points to the implementation.
 *
 * <p>Every mod that implements this interface and declares and end point will receive
 * exactly one call to {@link #onInitalizeJmx()}.
 *
 * <p>To maintain an optional dependency, all calls to JMX methods must be isolated to
 * the JmxEntryPoint instance or to classes that are only loaded if {@link #onInitalizeJmx()}
 * is called.
 *
 * <p>Note that it is NOT necessary to implement this interface and register a
 * "jmx" end point for mods that nest the JMX library or have a hard dependency on JMX.
 * Such mods can safely handle JMX registration in their client initialize instance.
 */
public interface JmxInitializer {
	/**
	 * Signals mods that maintain an optional dependency on JMX that JMX is
	 * loaded. Such mod should handle initialization activities that reference
	 * JMX classes during this call.
	 *
	 * <p>Will be called after mod initialization but before mode baking begins.
	 * It will be called exactly once per game start - subsequent resource or renderer
	 * reloads will not cause it to be called again.
	 */
	void onInitalizeJmx();
}
