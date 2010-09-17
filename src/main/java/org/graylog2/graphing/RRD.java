/**
 * Copyright 2010 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.graphing;

import java.io.File;
import org.graylog2.Main;

/**
 * RRD.java: Aug 18, 2010 6:59:04 PM
 *
 * Write to RRD files using the rrdtool binary defined in graylog2.conf
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class RRD {
    /**
     * The interval the RRD file is written to.
     */
    public static final int INTERVAL = 60;

    /**
     * How many days of data the RRD should be holding.
     */
    public static final int HOLD_DAYS = 7;

    /**
     * "Total messages" graph.
     */
    public static final int GRAPH_TYPE_TOTAL = 1;

    int type = 0;
    File rrdFile = null;
    String rrdFilePath = null;

    /**
     * @return the path of the RRD storage dir as defined in graylog2.conf
     */
    public static String getStorageFolderPath() {
        return Main.masterConfig.getProperty("rrd_storage_dir");
    }

    /**
     * @param type Type of the graph. Use constant like RRD.GRAPH_TYPE_TOTAL
     * @return The filename of the specified type
     * @throws Exception
     */
    public static String getFilename(int type) throws Exception {
        switch (type) {
            case RRD.GRAPH_TYPE_TOTAL:
                return "totalmessages.rrd";
            default:
                throw new Exception("Invalid graph type supplied.");
        }
    }

    /**
     * Get the RRD storage folder
     * @return RRD storage folder
     * @throws RRDInvalidStorageException
     */
    public static File getStorageFolder() throws RRDInvalidStorageException {
        String directory = RRD.getStorageFolderPath();
        File folder = new File(directory);

        if (!folder.exists()) {
            throw new RRDInvalidStorageException("Directory does not exist. (" + directory + ")");
        }

        if (!folder.isDirectory()) {
            throw new RRDInvalidStorageException("Is not a directory. (" + directory + ")");
        }

        return folder;
    }

    /**
     * Get the RRD file
     * @param type Type of the graph. Use constant like RRD.GRAPH_TYPE_TOTAL
     * @return RRD file
     * @throws RRDInvalidFileException
     */
    public static File getFile(int type) throws RRDInvalidFileException {
        String directory = RRD.getStorageFolderPath();
        String path = null;

        try {
            String filename = RRD.getFilename(type);
            path = directory + "/" + filename;
        } catch (Exception e) {
            throw new RRDInvalidFileException(e.toString());
        }

        return new File(path);
    }

    /**
     * Write to RRD files. Files are created lazy if not found.
     * @param type Type of the graph. Use constant like RRD.GRAPH_TYPE_TOTAL
     * @throws Exception
     */
    public RRD(int type) throws Exception {
        this.type = type;

        // Find the RRD.
        try {
            this.rrdFile = RRD.getFile(type);
            this.rrdFilePath = this.rrdFile.getAbsolutePath();
        } catch (RRDInvalidFileException e) {
            throw new Exception("Error: " + e.toString());
        }

        // Check if RRD exists and create lazy if it doesnt.
        if (!this.rrdFile.exists()) {
            if (!this.create()) {
                throw new Exception("Error: Not able to create RRD. Possibly not writable.");
            }
        }

        // Is the RRD writable?
        if (!this.rrdFile.canWrite()) {
            throw new Exception("Error: RRD is not writable.");
        }

    }

    /**
     * Writes a value with current timestamp to RRD
     * @param value Value to write
     * @return boolean
     * @throws Exception
     */
    public boolean write(int value) throws Exception {
        Runtime rt = Runtime.getRuntime();
        Process p;

        try {
            p = rt.exec("rrdtool update " + this.rrdFilePath + " " + System.currentTimeMillis()/1000 + ":" + value);
            p.waitFor();
            
            if (p.exitValue() == 1) {
                return false;
            }
        } catch (InterruptedException e) {}

        return true;
    }

    private String getCreationString() throws Exception {
        switch (this.type) {
            case RRD.GRAPH_TYPE_TOTAL:
                return "rrdtool create " + this.rrdFilePath + " --start NOW --step " + RRD.INTERVAL + " DS:messages:GAUGE:" + RRD.INTERVAL*2 + ":0:9000000 RRA:AVERAGE:0.5:5:" + (RRD.HOLD_DAYS*24*60)/5;
            default:
                throw new Exception("Invalid graph type supplied.");
        }
    }

    private boolean create() throws Exception {
        Runtime rt = Runtime.getRuntime();
        Process p;

        try {
            p = rt.exec(this.getCreationString());
            p.waitFor();

            // Did the call succeed?
            if (p.exitValue() == 1) {
                return false;
            }
        } catch (InterruptedException e) {}
        
        return true;
    }

}
