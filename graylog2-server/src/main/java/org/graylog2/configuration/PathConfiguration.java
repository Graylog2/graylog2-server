/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.configuration;

import com.github.joschi.jadconfig.Parameter;

import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class PathConfiguration {
    @Parameter(value = "bin_dir", required = true)
    private Path binDir = Paths.get("bin");

    @Parameter(value = "data_dir", required = true)
    private Path dataDir = Paths.get("data");

    @Parameter(value = "plugin_dir", required = true)
    private Path pluginDir = Paths.get("plugin");

    public Path getBinDir() {
        return binDir;
    }

    public Path getDataDir() {
        return dataDir;
    }

    public Path getPluginDir() {
        return pluginDir;
    }

}
