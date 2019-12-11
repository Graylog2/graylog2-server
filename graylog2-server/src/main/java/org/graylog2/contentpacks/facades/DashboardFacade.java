/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.contentpacks.facades;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.inject.Inject;
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
