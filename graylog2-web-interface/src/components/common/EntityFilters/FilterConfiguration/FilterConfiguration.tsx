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
import { MenuItem } from 'components/bootstrap';
import {
  isAttributeWithFilterOptions,
  isAttributeWithRelatedCollection, isDateAttribute, isCustomComponentFilter,
} from 'components/common/EntityFilters/helpers/AttributeIdentification';

import SuggestionsListFilter from './SuggestionsListFilter';
import GenericFilterInput from './GenericFilterInput';
import StaticOptionsList from './StaticOptionsList';
import DateRangeForm from './DateRangeForm';

const FilterComponent = ({ allActiveFilters, attribute, filter = undefined, filterValueRenderer, onSubmit }: FilterComponentProps) => {
  if (isCustomComponentFilter(attribute)) {
    const CustomFilterComponent = attribute.filter_component;

    return (
      <CustomFilterComponent attribute={attribute}
                             filterValueRenderer={filterValueRenderer}
                             onSubmit={onSubmit}
                             allActiveFilters={allActiveFilters}
                             filter={filter} />
    );
  }

  if (isAttributeWithFilterOptions(attribute)) {
    return (
      <StaticOptionsList attribute={attribute}
                         filterValueRenderer={filterValueRenderer}
                         onSubmit={onSubmit}
                         allActiveFilters={allActiveFilters} />
    );
  }

  if (isAttributeWithRelatedCollection(attribute)) {
    return (
      <SuggestionsListFilter attribute={attribute}
                             filterValueRenderer={filterValueRenderer}
                             onSubmit={onSubmit}
                             allActiveFilters={allActiveFilters}
                             filter={filter} />
    );
  }

  if (isDateAttribute(attribute)) {
    return (
      <DateRangeForm onSubmit={onSubmit}
                     filter={filter} />
    );
  }

  return <GenericFilterInput filter={filter} onSubmit={onSubmit} />;
};

export const FilterConfiguration = ({
  allActiveFilters,
  attribute,
  filter = undefined,
  filterValueRenderer,
  onSubmit,
}: FilterComponentProps) => (
  <>
    <MenuItem header>{filter ? 'Edit' : 'Create'} {attribute.title.toLowerCase()} filter</MenuItem>
    <FilterComponent attribute={attribute}
                     filterValueRenderer={filterValueRenderer}
                     onSubmit={onSubmit}
                     allActiveFilters={allActiveFilters}
                     filter={filter} />
  </>
);

export default FilterConfiguration;
