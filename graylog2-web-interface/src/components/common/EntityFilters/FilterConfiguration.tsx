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

import type { Attribute } from 'stores/PaginationTypes';
import type { Filters, Filter } from 'components/common/EntityFilters/types';
import MenuItem from 'components/bootstrap/MenuItem';
import StaticOptionsList from 'components/common/EntityFilters/configuration/StaticOptionsList';
import SuggestionsList from 'components/common/EntityFilters/configuration/SuggestionsList';
import DateRangeForm from 'components/common/EntityFilters/configuration/DateRangeForm';

type Props = {
  attribute: Attribute,
  filter?: Filter,
  filterValueRenderer: (value: Filter['value'], title: string) => React.ReactNode | undefined,
  onSubmit: (filter: { title: string, value: string }, closeDropdown?: boolean) => void,
  allActiveFilters: Filters | undefined,
}

export const FilterConfiguration = ({
  allActiveFilters,
  attribute,
  filter,
  filterValueRenderer,
  onSubmit,
}: Props) => (
  <>
    <MenuItem header>{filter ? 'Edit' : 'Create'} {attribute.title.toLowerCase()} filter</MenuItem>
    {attribute.filter_options && (
      <StaticOptionsList attribute={attribute}
                         filterValueRenderer={filterValueRenderer}
                         onSubmit={onSubmit} />
    )}
    {(!attribute.filter_options?.length && attribute.type !== 'DATE') && (
      <SuggestionsList attribute={attribute}
                       filterValueRenderer={filterValueRenderer}
                       onSubmit={onSubmit}
                       allActiveFilters={allActiveFilters}
                       filter={filter} />
    )}
    {attribute.type === 'DATE' && (
      <DateRangeForm onSubmit={onSubmit}
                     filter={filter} />
    )}
  </>
);

FilterConfiguration.defaultProps = {
  filter: undefined,
};

export default FilterConfiguration;
