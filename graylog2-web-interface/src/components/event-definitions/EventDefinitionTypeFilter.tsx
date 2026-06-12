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
import { useState } from 'react';

import type { FilterComponentProps } from 'stores/PaginationTypes';
import SuggestionsList from 'components/common/EntityFilters/FilterConfiguration/SuggestionsList';
import usePluginEntities from 'hooks/usePluginEntities';

const DEFAULT_SEARCH_PARAMS = {
  query: '',
  pageSize: 10,
  page: 1,
};

// Options are sourced from the `eventDefinitionTypes` plugin registry rather than a backend
// distinct-values call, so the dropdown lists every registered (and licensed) definition type
// with its display name. The backend `type` attribute (config.type) does the actual filtering.
const EventDefinitionTypeFilter = ({
  attribute,
  allActiveFilters,
  filter,
  filterValueRenderer,
  onSubmit,
}: FilterComponentProps) => {
  const [filterQuery, setFilterQuery] = useState(DEFAULT_SEARCH_PARAMS);
  const eventDefinitionTypes = usePluginEntities('eventDefinitionTypes');

  const suggestions = eventDefinitionTypes
    // Match the create form (EventConditionForm): only offer types whose license/feature
    // gate is satisfied, so unlicensed enterprise types aren't listed.
    .filter((edt) => edt.useCondition())
    .map((edt) => ({ id: edt.type, value: edt.displayName }))
    .filter((suggestion) => suggestion.value.toLowerCase().includes(filterQuery.query.toLowerCase()))
    .sort((a, b) => a.value.localeCompare(b.value));

  return (
    <SuggestionsList
      allActiveFilters={allActiveFilters}
      attribute={attribute}
      multiSelect={!filter}
      filterValueRenderer={filterValueRenderer}
      onSubmit={onSubmit}
      suggestions={suggestions}
      isLoading={false}
      total={suggestions.length}
      page={1}
      pageSize={suggestions.length}
      setSearchParams={setFilterQuery}
    />
  );
};

export default EventDefinitionTypeFilter;
