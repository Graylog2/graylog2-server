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

    @Parameter(names = {"-np", "--no-pid-file"}, description = "Do not write a PID file (overrides -p/--pidfile)")
    private boolean noPidFile = false;

    @Parameter(names = {"-t", "--configtest"}, description = "Validate graylog2 configuration and exit")
    private boolean configTest = false;

    @Parameter(names = {"-d", "--debug"}, description = "Run graylog2 in debug mode")
    private boolean debug = false;
    
    @Parameter(names = {"-l", "--local"}, description = "Run graylog2 in local mode. Only interesting for Graylog2 developers.")
    private boolean local = false;
    
    @Parameter(names = {"-s", "--statistics"}, description = "Print utilization statistics to STDOUT")
    private boolean stats = false;

    @Parameter(names = {"-r", "--no-retention"}, description = "Do not automatically remove messages from index that are older than the retention time")
    private boolean noRetention = false;

    @Parameter(names = {"-x", "--install-plugin"}, description = "Install plugin with provided short name from graylog2.org")
    private String pluginShortname;
    
    @Parameter(names = {"-v", "--plugin-version"}, description = "Install plugin with this version")
    private String pluginVersion = Core.GRAYLOG2_VERSION.toString();

    @Parameter(names = {"-m", "--force-plugin"}, description = "Force plugin installation even if this version of graylog2-server is not officially supported.")
    private boolean forcePlugin = false;
    
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
    
    public boolean isInstallPlugin() {
        return pluginShortname != null && !pluginShortname.isEmpty();
    }
    
    public String getPluginShortname() {
        return pluginShortname;
    }
    
    public String getPluginVersion() {
        return pluginVersion;
    }
    
    public boolean isForcePlugin() {
        return forcePlugin;
    }
    
}
