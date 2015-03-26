package org.graylog2.plugin;

import com.google.common.collect.Sets;

import java.util.Set;

public class Capabilities {
    public static Set<String> toStringSet(Set<ServerStatus.Capability> capabilities) {
        final Set<String> stringSet = Sets.newHashSetWithExpectedSize(capabilities.size());
        for (ServerStatus.Capability capability : capabilities) {
            stringSet.add(capability.toString());
        }

        return stringSet;
    }
}
