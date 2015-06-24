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
package org.graylog2.bootstrap.commands;

import io.airlift.airline.Command;
import org.graylog2.bootstrap.CliCommand;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.Version;

@Command(name = "version", description = "Show the Graylog and JVM versions")
public class ShowVersion implements CliCommand {
    private final Version version = Version.CURRENT_CLASSPATH;

    @Override
    public void run() {
        System.out.println("Graylog " + version);
        System.out.println("JRE: " + Tools.getSystemInformation());
    }
}
