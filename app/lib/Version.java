/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package lib;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Version {

    /*
     * Following semantic versioning.
     *
     * http://semver.org/
     */

    public static final Version vDEV = new Version(0, 20, 0, "dev");
    public static final Version v0_20_0_PREVIEW_1 = new Version(0, 20, 0, "preview.1");
    public static final Version v0_20_0_PREVIEW_2 = new Version(0, 20, 0, "preview.2");

    public static final Version VERSION = v0_20_0_PREVIEW_2;

    public final int major;
    public final int minor;
    public final int patch;
    public final String additional;

    public Version(int major, int minor, int patch) {
        this(major, minor, patch, null);
    }

    public Version(int major, int minor, int patch, String additional) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.additional = additional;
    }

    public String getName() {
        StringBuilder sb = new StringBuilder();

        sb.append(major).append(".").append(minor).append(".").append(patch);

        if (additional != null && !additional.isEmpty()) {
            sb.append("-").append(additional);
        }

        return sb.toString();
    }

    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        Version version = (Version) obj;

        return getName().equals(version.getName());
    }

}
