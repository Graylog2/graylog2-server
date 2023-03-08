package org.graylog2.rest.resources.system.debug.bundle;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.List;

@JsonAutoDetect
public record BundleEntries(List<LogFile> logfiles) {}
