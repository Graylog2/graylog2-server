package org.graylog2.inputs;

public class ReservedInputIds {

    // ID of the ephemeral collector ingest input that, on cloud setups, is system-managed and not visible to users.
    public static String EPHEMERAL_COLLECTOR_INGEST = "000000000000000000000001";

    private ReservedInputIds() {
    }
}
