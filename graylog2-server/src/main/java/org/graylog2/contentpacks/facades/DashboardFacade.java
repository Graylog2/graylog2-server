package org.graylog2.contentpacks.facades;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;

import java.util.stream.Stream;

public class DashboardFacade extends ViewFacade {
    public static final ModelType TYPE_V2 = ModelTypes.DASHBOARD_V2;
    private ViewService viewService;

    @Inject
    public DashboardFacade(ObjectMapper objectMapper, SearchDbService searchDbService, ViewService viewService) {
        super(objectMapper, searchDbService, viewService);
        this.viewService = viewService;
    }

    @Override
    public ModelType getModelType() {
        return TYPE_V2;
    }

    @Override
    public Stream<ViewDTO> getNativeViews() {
        return viewService.streamAll().filter(v -> v.type().equals(ViewDTO.Type.DASHBOARD));
    }
}
