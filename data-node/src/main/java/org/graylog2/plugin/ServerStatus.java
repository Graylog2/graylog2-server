package org.graylog2.plugin;

public class ServerStatus {
    public enum Capability {
        SERVER,
        /**
         * @deprecated Use {@link LeaderElectionService#isLeader()} to determine if the node currently acts as a leader,
         * if you absolutely must.
         */
        @Deprecated
        MASTER,
        LOCALMODE
    }

    public void start() {
    }

    public void fail() {
    }

    public void shutdown() {

    }
}
