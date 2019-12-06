package org.graylog2.contentpacks.facades;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.inject.Inject;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;

import java.util.stream.Stream;

public class SearchFacade extends ViewFacade {
    public static final ModelType TYPE_V1 = ModelTypes.SEARCH_V1;
    private ViewService viewService;

    @Inject
    public SearchFacade(ObjectMapper objectMapper, SearchDbService searchDbService, ViewService viewService) {
        super(objectMapper, searchDbService, viewService);
        this.viewService = viewService;
    }

    @Override
    public ModelType getModelType() {
        return TYPE_V1;
    }

    @Override
    public Stream<ViewDTO> getNativeViews() {
        return viewService.streamAll().filter(v -> v.type().equals(ViewDTO.Type.SEARCH));
    }
}
