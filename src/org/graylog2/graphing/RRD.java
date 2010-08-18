/**
 * Copyright 2010 Lennart Koopmann <lennart@scopeport.org>
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

/**
 * RRD.java: lennart | Aug 18, 2010 6:59:04 PM
 */

package org.graylog2.graphing;

import java.io.File;
import org.graylog2.Main;

class RRDInvalidStorageException extends Exception {
    public RRDInvalidStorageException(){
    }

    public RRDInvalidStorageException(String msg){
        super(msg);
    }
}

public class RRD {

    String name = null;
    File folder = null;

    public static File getStorageFolder() throws RRDInvalidStorageException {
        String directory = Main.masterConfig.getProperty("rrd_storage_dir");
        File file = new File(directory);

        if (!file.exists()) {
            throw new RRDInvalidStorageException("Directory does not exist. (" + directory + ")");
        }

        if (!file.isDirectory()) {
            throw new RRDInvalidStorageException("Is not a directory. (" + directory + ")");
        }

        return file;
    }

    public RRD(String name) throws Exception {
        this.name = name;

        try {
            this.folder = RRD.getStorageFolder();
        } catch (RRDInvalidStorageException e) {
            throw new Exception("Error: " + e.toString());
        }
    }

}
