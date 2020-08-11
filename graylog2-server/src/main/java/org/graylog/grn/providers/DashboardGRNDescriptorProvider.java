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
package org.graylog.grn.providers;

import org.graylog.grn.GRN;
import org.graylog.grn.GRNDescriptor;
import org.graylog.grn.GRNDescriptorProvider;
import org.graylog.plugins.views.search.views.ViewService;

import javax.inject.Inject;

public class DashboardGRNDescriptorProvider implements GRNDescriptorProvider {
    private final ViewService viewService;

    @Inject
    public DashboardGRNDescriptorProvider(ViewService viewService) {
        this.viewService = viewService;
    }

    @Override
    public GRNDescriptor get(GRN grn) {
        return viewService.get(grn.entity())
                .map(dashboard -> GRNDescriptor.create(grn, dashboard.title()))
                .orElse(GRNDescriptor.create(grn, "ERROR: Dashboard for <" + grn.toString() + "> not found!"));
    }
}
