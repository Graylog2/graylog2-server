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
import React, { useCallback } from 'react';
import styled from 'styled-components';

import CreateFilterDropdown from 'components/common/EntityFilters/CreateFilterDropdown';
import type { Attributes } from 'stores/PaginationTypes';
import type { Filters, Filter, UrlQueryFilters } from 'components/common/EntityFilters/types';
import ActiveFilters from 'components/common/EntityFilters/ActiveFilters';
import useFiltersWithTitle from 'components/common/EntityFilters/hooks/useFiltersWithTitle';

import { ROW_MIN_HEIGHT } from './Constants';

const SUPPORTED_ATTRIBUTE_TYPES = ['STRING', 'BOOLEAN', 'DATE'];

const FilterCreation = styled.div`
  display: inline-flex;
  height: ${ROW_MIN_HEIGHT}px;
  align-items: center;
  margin-left: 5px;
  
  && {
    margin-right: 10px;
  }
`;

type Props = {
  attributes: Attributes,

  urlQueryFilters: UrlQueryFilters | undefined,
  setUrlQueryFilters: (urlQueryFilters: UrlQueryFilters) => void,
  filterValueRenderers?: { [attributeId: string]: (value: Filter['value'], title: string) => React.ReactNode };
}

const EntityFilters = ({ attributes = [], filterValueRenderers, urlQueryFilters, setUrlQueryFilters }: Props) => {
  const {
    data: activeFilters,
    onChange: onChangeFiltersWithTitle,
  } = useFiltersWithTitle(
    urlQueryFilters,
    attributes,
    !!attributes,
  );

  const onChangeFilters = useCallback((newFilters: Filters) => {
    const newUrlQueryFilters = Object.entries(newFilters).reduce((col, [attributeId, filterCol]) => ({
      ...col,
      [attributeId]: [...col[attributeId] ?? [], ...filterCol.map(({ value }) => value)],
    }), {});

    onChangeFiltersWithTitle(newFilters, newUrlQueryFilters);

    setUrlQueryFilters(newUrlQueryFilters);
  }, [onChangeFiltersWithTitle, setUrlQueryFilters]);

  const filterableAttributes = attributes.filter(({ filterable, type }) => filterable && SUPPORTED_ATTRIBUTE_TYPES.includes(type));

  if (!filterableAttributes.length) {
    return null;
  }

  const onCreateFilter = (attributeId, filter: Filter) => {
    onChangeFilters({
      ...(activeFilters ?? {}),
      [attributeId]: [
        ...(activeFilters?.[attributeId] ?? []),
        filter,
      ],
    });
  };

  const onDeleteFilter = (attributeId: string, filterId: string) => {
    const filterGroup = activeFilters[attributeId];
    const updatedFilterGroup = filterGroup.filter(({ value }) => value !== filterId);
    let updatedFilters = { ...activeFilters };

    if (updatedFilterGroup.length) {
      updatedFilters = {
        ...activeFilters,
        [attributeId]: updatedFilterGroup,
      };
    } else {
      delete updatedFilters[attributeId];
    }

    onChangeFilters(updatedFilters);
  };

  const onChangeFilter = (attributeId: string, prevValue: string, newFilter: Filter) => {
    const filterGroup = activeFilters[attributeId];
    const targetFilterIndex = filterGroup.findIndex(({ value }) => value === prevValue);

    const updatedFilterGroup = [...filterGroup];
    updatedFilterGroup[targetFilterIndex] = newFilter;

    onChangeFilters({
      ...activeFilters,
      [attributeId]: updatedFilterGroup,
    });
  };

  return (
    <>
      <FilterCreation>
        Filters
        <CreateFilterDropdown filterableAttributes={filterableAttributes}
                              onCreateFilter={onCreateFilter}
                              activeFilters={activeFilters}
                              filterValueRenderers={filterValueRenderers} />

      </FilterCreation>
      {activeFilters && (
        <ActiveFilters filters={activeFilters}
                       attributes={attributes}
                       onChangeFilter={onChangeFilter}
                       onDeleteFilter={onDeleteFilter}
                       filterValueRenderers={filterValueRenderers} />
      )}
    </>
  );
};

EntityFilters.defaultProps = {
  filterValueRenderers: undefined,
};

export default EntityFilters;
