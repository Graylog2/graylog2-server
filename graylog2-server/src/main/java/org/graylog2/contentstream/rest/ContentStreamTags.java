package org.graylog2.contentstream.rest;

public enum ContentStreamTags {
    OPEN("open-feed"),              //anyone on opensource
    ENTERPRISE("enterprise-feed"),  //anyone with Enterprise or Security License
    SMB("smb-feed");                //anyone with Small business free enterprise license (OPS only not security)

    public static final long SMB_TRAFFIC_LIMIT = 2L * 1024 * 1024 * 1024;

    private String tag;

    ContentStreamTags(String tag) {
        this.tag = tag;
    }

    @Override
    public String toString() {
        return tag;
    }
}
