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
package org.graylog2.shared;

import org.graylog2.plugin.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ServerVersion {
    private static final Logger LOG = LoggerFactory.getLogger(ServerVersion.class);

    public static final Version vDEV = new Version(0, 20, 0, "dev");
    public static final Version v0_20_0_PREVIEW_1 = new Version(0, 20, 0, "preview.1");
    public static final Version v0_20_0_PREVIEW_2 = new Version(0, 20, 0, "preview.2");
    public static final Version v0_20_0_PREVIEW_3 = new Version(0, 20, 0, "preview.3");
    public static final Version v0_20_0_PREVIEW_4 = new Version(0, 20, 0, "preview.4");
    public static final Version v0_20_0_PREVIEW_5 = new Version(0, 20, 0, "preview.5");
    public static final Version v0_20_0_PREVIEW_6 = new Version(0, 20, 0, "preview.6");
    public static final Version v0_20_0_PREVIEW_7 = new Version(0, 20, 0, "preview.7");
    public static final Version v0_20_0_PREVIEW_8 = new Version(0, 20, 0, "preview.8");

    public static final Version v0_20_0_RC_1 = new Version(0, 20, 0, "rc.1");
    public static final Version v0_20_0_RC_1_1 = new Version(0, 20, 0, "rc.1-1");
    public static final Version v0_20_0_RC_2 = new Version(0, 20, 0, "rc.2");
    public static final Version v0_20_0_RC_3 = new Version(0, 20, 0, "rc.3");

    public static final Version v0_20_0 = new Version(0, 20, 0);
    public static final Version v0_20_1 = new Version(0, 20, 1);

    public static final Version v0_20_1_SNAPSHOT = new Version(0, 20, 1, "SNAPSHOT");
    public static final Version v0_20_2_SNAPSHOT = new Version(0, 20, 2, "snapshot");
    public static final Version v0_20_2 = new Version(0, 20, 2);

    public static final Version v0_21_0_SNAPSHOT = new Version(0, 21, 0, "SNAPSHOT");
    public static final Version v0_21_0_BETA1 = new Version(0, 21, 0, "beta1");
    public static final Version VERSION = Version.CURRENT_CLASSPATH;
    public static final String CODENAME = "Rothaus";
}
