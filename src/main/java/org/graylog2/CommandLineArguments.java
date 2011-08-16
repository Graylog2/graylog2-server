package org.graylog2;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Java bean to hold command line argument values
 *
 * @author Jochen Schalanda <jochen@schalanda.name>
 */
@Parameters(commandDescription = "Graylog2 server")
public class CommandLineArguments {

    private static final String FILE_SEPARATOR = System.getProperty("file.separator");
    private static final String TMPDIR = System.getProperty("java.io.tmpdir", "/tmp");

    @Parameter(names = {"-f", "--configfile"}, description = "Configuration file for graylog2")
    private String configFile = "/etc/graylog2.conf";

    @Parameter(names = {"-p", "--pidfile"}, description = "File containing the PID of graylog2")
    private String pidFile = TMPDIR + FILE_SEPARATOR + "graylog2.pid";

    @Parameter(names = {"-t", "--configtest"}, description = "Validate graylog2 configuration and exit")
    private boolean configTest = false;

    @Parameter(names = {"-d", "--debug"}, description = "Run graylog2 in debug mode")
    private boolean debug = false;

    @Parameter(names = "--version", description = "Show version of graylog2 and exit")
    private boolean showVersion = false;

    @Parameter(names = {"-h", "--help"}, description = "Show usage information and exit")
    private boolean showHelp = false;

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

    public boolean isConfigTest() {
        return configTest;
    }

    public void setConfigTest(boolean configTest) {
        this.configTest = configTest;
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

    public void setShowVersion(boolean showVersion) {
        this.showVersion = showVersion;
    }

    public boolean isShowHelp() {
        return showHelp;
    }

    public void setShowHelp(boolean showHelp) {
        this.showHelp = showHelp;
    }
}
