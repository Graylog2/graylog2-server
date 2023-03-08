package org.graylog2.rest.resources.system.debug.bundle;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect
public record SupportBundleNodeManifest(BundleEntries entries) {}
