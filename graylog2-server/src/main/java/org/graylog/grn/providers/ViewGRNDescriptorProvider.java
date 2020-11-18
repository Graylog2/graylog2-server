/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.grn.providers;

import org.graylog.grn.GRN;
import org.graylog.grn.GRNDescriptor;
import org.graylog.grn.GRNDescriptorProvider;
import org.graylog.plugins.views.search.views.ViewService;

import javax.inject.Inject;

public class ViewGRNDescriptorProvider implements GRNDescriptorProvider {
    private final ViewService viewService;

    @Inject
    public ViewGRNDescriptorProvider(ViewService viewService) {
        this.viewService = viewService;
    }

    @Override
    public GRNDescriptor get(GRN grn) {
        return viewService.get(grn.entity())
                .map(viewDTO -> GRNDescriptor.create(grn, viewDTO.title()))
                .orElse(GRNDescriptor.create(grn, "ERROR: View for <" + grn.toString() + "> not found!"));
    }
}
