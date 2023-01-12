package org.graylog.datanode.rest;

public record StatusResponse(String dataNodeVersion, String opensearchVersion, long processId, boolean alive) {}
