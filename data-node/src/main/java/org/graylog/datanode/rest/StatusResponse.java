package org.graylog.datanode.rest;

public record StatusResponse(long processId, boolean alive) {}
