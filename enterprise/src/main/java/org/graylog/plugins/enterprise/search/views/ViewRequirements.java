package org.graylog.plugins.enterprise.search.views;

import com.google.common.collect.ForwardingMap;
import com.google.inject.assistedinject.Assisted;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ViewRequirements extends ForwardingMap<String, PluginMetadataSummary> {
    private final Map<String, PluginMetadataSummary> requirements;

    @Inject
    public ViewRequirements(Set<ViewRequirement> viewRequirements, @Assisted ViewDTO view) {
        this.requirements = viewRequirements.stream()
                .map(requirement -> requirement.test(view))
                .flatMap(s -> s.entrySet().stream())
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (entry1, entry2) -> entry1));
    }

    @Override
    protected Map<String, PluginMetadataSummary> delegate() {
        return this.requirements;
    }

    public interface Factory {
        ViewRequirements create(ViewDTO view);
    }
}
