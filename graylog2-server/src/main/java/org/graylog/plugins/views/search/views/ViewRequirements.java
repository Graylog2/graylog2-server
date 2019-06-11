package org.graylog.plugins.views.search.views;

import com.google.inject.assistedinject.Assisted;
import org.graylog.plugins.views.Requirement;
import org.graylog.plugins.views.Requirements;
import org.graylog.plugins.views.Requirement;
import org.graylog.plugins.views.Requirements;

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
