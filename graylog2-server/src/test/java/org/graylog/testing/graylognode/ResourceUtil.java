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
package org.graylog.testing.graylognode;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.UUID;

public class ResourceUtil {
    static File resourceToTmpFile(@SuppressWarnings("SameParameterValue") String resourceName) {

        InputStream resource = ResourceUtil.class.getClassLoader().getResourceAsStream(resourceName);

        if (resource == null)
            throw new RuntimeException("Couldn't load resource " + resourceName);

        File f = new File("/tmp/" + UUID.randomUUID().toString() + "-" + Paths.get(resourceName).getFileName());

        try {
            FileUtils.copyInputStreamToFile(resource, f);
        } catch (IOException e) {
            throw new RuntimeException("Error copying resource to file: " + resourceName);
        }

        return f;
    }
}
