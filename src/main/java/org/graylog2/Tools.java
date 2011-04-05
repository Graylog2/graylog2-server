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

package org.graylog2;

import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import org.apache.log4j.Level;

/**
 * Tools.java: May 17, 2010 9:46:31 PM
 *
 * Utilty class for various tool/helper functions.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public final class Tools {

    private static final Logger LOG = Logger.getLogger(Tools.class);

    private Tools() { }

    /**
     * Get the own PID of this process.
     *
     * @return PID
     * @throws Exception
     */
    public static String getPID() {
        byte[] bo = new byte[100];
        String[] cmd = {"bash", "-c", "echo $PPID"};
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            p.getInputStream().read(bo);
        } catch (IOException e) {
            LOG.fatal("Could not determine own PID! " + e.getMessage(), e);
            return "unknown";
        }
        return new String(bo).trim();
    }

    /**
     * Converts integer syslog loglevel to human readable string
     *
     * @param level The level to convert
     * @return The human readable level
     */
    public static String syslogLevelToReadable(int level) {
        switch (level) {
            case 0:
                return "Emergency";
            case 1:
                return "Alert";
            case 2:
                return"Critical";
            case 3:
                return "Error";
            case 4:
                return "Warning";
            case 5:
                return "Notice";
            case 6:
                return "Informational";
            case 7:
                return "Debug";
        }

        return "Invalid";
    }

    /**
     * Converts integer syslog facility to human readable string
     *
     * @param facility The facility to convert
     * @return The human readable facility
     */
    public static String syslogFacilityToReadable(int facility) {
        switch (facility) {
            case 0:  return "kernel";
            case 1:  return "user-level";
            case 2:  return "mail";
            case 3:  return "system daemon";
            case 4: case 10: return "security/authorization";
            case 5:  return "syslogd";
            case 6:  return "line printer";
            case 7:  return "network news";
            case 8:  return "UUCP";
            case 9: case 15: return "clock";
            case 11: return "FTP";
            case 12: return "NTP";
            case 13: return "log audit";
            case 14: return "log alert";

            // TODO: Make user definable?
            case 16: return "local0";
            case 17: return "local1";
            case 18: return "local2";
            case 19: return "local3";
            case 20: return "local4";
            case 21: return "local5";
            case 22: return "local6";
            case 23: return "local7";
        }

        return "Unknown";
    }

    /**
     * Get a String containing version information of JRE, OS, ...
     * @return Descriptive string of JRE and OS
     */
    public static String getSystemInformation() {
        String ret = System.getProperty("java.vendor");
        ret += " " + System.getProperty("java.version");
        ret += " on " + System.getProperty("os.name");
        ret += " " + System.getProperty("os.version");
        return ret;
    }

    public static int log4jLevelToSyslog(Level level) {
        if (level.equals(Level.DEBUG)) {
            return 7;
        } else if (level.equals(Level.INFO)) {
            return 6;
        } else if (level.equals(Level.WARN)) {
            return 4;
        } else if (level.equals(Level.ERROR)) {
            return 3;
        } else if (level.equals(Level.FATAL)) {
            return 2;
        }

        return 4; // Warning.
    }


    /**
     * Decompress ZLIB (RFC 1950) compressed data
     *
     * @param compressedData
     * @return A string containing the decompressed data
     * @throws IOException
     */
    public static String decompressZlib(byte[] compressedData) throws IOException {
        byte[] buffer = new byte[compressedData.length];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InflaterInputStream in = new InflaterInputStream(new ByteArrayInputStream(compressedData));
        for (int bytesRead = 0; bytesRead != -1; bytesRead = in.read(buffer)) {
            out.write(buffer, 0, bytesRead);
        }
        return new String(out.toByteArray(), "UTF-8");
    }

    /**
     * Decompress GZIP (RFC 1952) compressed data
     * 
     * @param compressedData
     * @return A string containing the decompressed data
     * @throws IOException
     */
    public static String decompressGzip(byte[] compressedData) throws IOException {
        byte[] buffer = new byte[compressedData.length];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(compressedData));
        for (int bytesRead = 0; bytesRead != -1; bytesRead = in.read(buffer)) {
            out.write(buffer, 0, bytesRead);
        }
        return new String(out.toByteArray(), "UTF-8");
    }

    /**
     *
     * @return The current UTC UNIX timestamp.
     */
    public static int getUTCTimestamp() {
       return (int) (System.currentTimeMillis()/1000);
    }

    public static String getLocalHostname() {
        InetAddress addr = null;
        try {
            addr = InetAddress.getLocalHost();
        } catch (UnknownHostException ex) {
            return "Unknown";
        }

        return addr.getHostName();
    }
}
