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
package org.graylog2.radio;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Parameters(commandDescription = "Graylog2 Radio")
public class CommandLineArguments {

    private static final String FILE_SEPARATOR = System.getProperty("file.separator");
    private static final String TMPDIR = System.getProperty("java.io.tmpdir", "/tmp");

    @Parameter(names = {"-f", "--configfile"}, description = "Configuration file for graylog2-radio")
    private String configFile = "/etc/graylog2-radio.conf";

    @Parameter(names = {"-p", "--pidfile"}, description = "File containing the PID of graylog2-radio")
    private String pidFile = TMPDIR + FILE_SEPARATOR + "graylog2-radio.pid";

    @Parameter(names = {"-np", "--no-pid-file"}, description = "Do not write a PID file (overrides -p/--pidfile)")
    private boolean noPidFile = false;

    @Parameter(names = {"-d", "--debug"}, description = "Run graylog2-radio in debug mode")
    private boolean debug = false;

    @Parameter(names = "--version", description = "Print version of graylog2-radio and exit")
    private boolean showVersion = false;

    @Parameter(names = {"-h", "--help"}, description = "Show usage information and exit")
    private boolean showHelp = false;

    @Parameter(names = "--dump-config", description = "Show the effective graylog2-radio configuration and exit")
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

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isShowVersion() {
        return showVersion;
    }

    public boolean isShowHelp() {
        return showHelp;
    }

    public boolean isDumpConfig() {
        return dumpConfig;
    }

    public void setDumpConfig(boolean dumpConfig) {
        this.dumpConfig = dumpConfig;
    }

    public boolean isDumpDefaultConfig() {
        return dumpDefaultConfig;
    }

    public void setDumpDefaultConfig(boolean dumpDefaultConfig) {
        this.dumpDefaultConfig = dumpDefaultConfig;
    }
}
