package org.graylog2.datanode;

public record RemoteReindexAllowlistEvent(String host, ACTION action) {
    public enum ACTION {
        ADD, REMOVE
    }
}
