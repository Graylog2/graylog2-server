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
import * as React from 'react';

import type { FilterComponentProps } from 'stores/PaginationTypes';
import StaticOptionsList from 'components/common/EntityFilters/FilterConfiguration/StaticOptionsList';
import usePluginEntities from 'hooks/usePluginEntities';

// Renders the same searchless option list as the Status/Source filters, but builds its options
// from the `eventDefinitionTypes` plugin registry (gated by useCondition() like the create form)
// instead of a backend filter_options enum. The backend `type` attribute (config.type) filters.
const EventDefinitionTypeFilter = ({ attribute, allActiveFilters, filterValueRenderer, onSubmit }: FilterComponentProps) => {
  const eventDefinitionTypes = usePluginEntities('eventDefinitionTypes');

  const filterOptions = eventDefinitionTypes
    .filter((edt) => edt.useCondition())
    .map((edt) => ({ value: edt.type, title: edt.displayName }));

  return (
    <StaticOptionsList
      attribute={{ ...attribute, filter_options: filterOptions }}
      allActiveFilters={allActiveFilters}
      filterValueRenderer={filterValueRenderer}
      onSubmit={onSubmit}
    />
  );
};

export default EventDefinitionTypeFilter;
