/**
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
 */
package org.graylog2;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Java bean to hold command line argument values
 */
@Parameters(commandDescription = "Graylog2 server")
public class CommandLineArguments {

    private static final String FILE_SEPARATOR = System.getProperty("file.separator");
    private static final String TMPDIR = System.getProperty("java.io.tmpdir", "/tmp");

    @Parameter(names = {"-f", "--configfile"}, description = "Configuration file for Graylog2")
    private String configFile = "/etc/graylog2.conf";

    @Parameter(names = {"-p", "--pidfile"}, description = "File containing the PID of Graylog2")
    private String pidFile = TMPDIR + FILE_SEPARATOR + "graylog2.pid";

    @Parameter(names = {"-np", "--no-pid-file"}, description = "Do not write a PID file (overrides -p/--pidfile)")
    private boolean noPidFile = false;

    @Parameter(names = {"-t", "--configtest"}, description = "Validate Graylog2 configuration and exit")
    private boolean configTest = false;

    @Parameter(names = {"-d", "--debug"}, description = "Run Graylog2 in debug mode")
    private boolean debug = false;
    
    @Parameter(names = {"-l", "--local"}, description = "Run Graylog2 in local mode. Only interesting for Graylog2 developers.")
    private boolean local = false;
    
    @Parameter(names = {"-s", "--statistics"}, description = "Print utilization statistics to STDOUT")
    private boolean stats = false;

    @Parameter(names = {"-r", "--no-retention"}, description = "Do not automatically remove messages from index that are older than the retention time")
    private boolean noRetention = false;

    @Parameter(names = "--version", description = "Show version of graylog2 and exit")
    private boolean showVersion = false;

    @Parameter(names = {"-h", "--help"}, description = "Show usage information and exit")
    private boolean showHelp = false;

    @Parameter(names = "--dump-config", description = "Show the effective Graylog2 configuration and exit")
    private boolean dumpConfig = false;

    @Parameter(names = "--dump-default-config", description = "Show the default configuration and exit")
    private boolean dumpDefaultConfig = false;

    public String getConfigFile() {
        return configFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    public String getPidFile() {
        return pidFile;
    }

    public void setPidFile(String pidFile) {
        this.pidFile = pidFile;
    }

    public boolean isNoPidFile() {
        return noPidFile;
    }

    public void setNoPidFile(final boolean noPidFile) {
        this.noPidFile = noPidFile;
    }

    public boolean isConfigTest() {
        return configTest;
    }

    public void setConfigTest(boolean configTest) {
        this.configTest = configTest;
    }

    public boolean isDebug() {
        return debug;
    }
    
    public boolean isLocal() {
        return local;
    }
    
    public boolean isStats() {
        return stats;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean performRetention() {
        return !noRetention;
    }

    public boolean isShowVersion() {
        return showVersion;
    }

    public void setShowVersion(boolean showVersion) {
        this.showVersion = showVersion;
    }

    public boolean isShowHelp() {
        return showHelp;
    }

    public void setShowHelp(boolean showHelp) {
        this.showHelp = showHelp;
    }
    
    public boolean isDumpDefaultConfig() {
        return dumpDefaultConfig;
    }

    public void setDumpDefaultConfig(boolean dumpDefaultConfig) {
        this.dumpDefaultConfig = dumpDefaultConfig;
    }

    public boolean isDumpConfig() {
        return dumpConfig;
    }

    public void setDumpConfig(boolean dumpConfig) {
        this.dumpConfig = dumpConfig;
    }
}
