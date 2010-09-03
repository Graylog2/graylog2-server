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

class RRDInvalidFileException extends Exception {
    public RRDInvalidFileException(){
    }

    public RRDInvalidFileException(String msg){
        super(msg);
    }
}

public class RRD {
    public static final int INTERVAL = 60;
    public static final int HOLD_DAYS = 7;

    public static final int GRAPH_TYPE_TOTAL = 1;

    int type = 0;
    File folder = null;
    File rrdFile = null;
    String rrdFilePath = null;

    public static String getStorageFolderPath() {
        return Main.masterConfig.getProperty("rrd_storage_dir");
    }

    public static String getFilename(int type) throws Exception {
        switch (type) {
            case RRD.GRAPH_TYPE_TOTAL:
                return "totalmessages.rrd";
            default:
                throw new Exception("Invalid graph type supplied.");
        }
    }

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

    public static File getFile(int type) throws RRDInvalidFileException {
        String directory = RRD.getStorageFolderPath();
        String path = null;

        try {
            String filename = RRD.getFilename(type);
            path = directory + "/" + filename;
        } catch (Exception e) {
            throw new RRDInvalidFileException(e.toString());
        }

        File rrd = new File(path);

        return rrd;
    }

    public RRD(int type) throws Exception {
        this.type = type;

        try {
            this.folder = RRD.getStorageFolder();
        } catch (RRDInvalidStorageException e) {
            throw new Exception("Error: " + e.toString());
        }

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
