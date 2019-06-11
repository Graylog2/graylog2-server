package org.graylog.plugins.enterprise.search.views;

import com.google.inject.assistedinject.Assisted;
import org.graylog.plugins.enterprise.Requirement;
import org.graylog.plugins.enterprise.Requirements;

import javax.inject.Inject;
import java.util.Set;

public class ViewRequirements extends Requirements<ViewDTO> {
    @Inject
    public ViewRequirements(Set<Requirement<ViewDTO>> requirements, @Assisted ViewDTO dto) {
        super(requirements, dto);
    }

    public interface Factory extends Requirements.Factory<ViewDTO> {
        ViewRequirements create(ViewDTO view);
    }
}
