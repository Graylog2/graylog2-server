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
import React from 'react';
import styled from 'styled-components';

import CreateFilterDropdown from 'components/common/EntityFilters/CreateFilterDropdown';
import type { Attributes } from 'stores/PaginationTypes';
import type { Filters, Filter } from 'components/common/EntityFilters/types';
import ActiveFilters from 'components/common/EntityFilters/ActiveFilters';

const Container = styled.div`
  display: inline-flex;
  height: 34px;
  align-items: center;
  margin-left: 5px;
`;

type Props = {
  attributes: Attributes,
  onChangeFilters: (filters: Filters) => void,
  activeFilters: Filters | undefined,
  filterValueRenderers?: { [attributeId: string]: (value: Filter['value'], title: string) => React.ReactNode };
}

const EntityFilters = ({ attributes = [], activeFilters = {}, filterValueRenderers, onChangeFilters }: Props) => {
  const filterableAttributes = attributes.filter(({ filterable, type }) => filterable && type === 'BOOLEAN');

  if (!filterableAttributes.length) {
    return null;
  }

  const onCreateFilter = (attributeId, filter: Filter) => {
    onChangeFilters({
      ...activeFilters,
      [attributeId]: [
        ...(activeFilters[attributeId] ?? []),
        filter,
      ],
    });
  };

  const onDeleteFilter = (attributeId: string, filterId: string) => {
    const filterGroup = activeFilters[attributeId];
    const updatedFilterGroup = filterGroup.filter(({ id }) => id !== filterId);
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

  const onChangeFilter = (attributeId: string, newFilter: Filter) => {
    const filterGroup = activeFilters[attributeId];
    const targetFilterIndex = filterGroup.findIndex(({ id }) => id === newFilter.id);

    const updatedFilterGroup = [...filterGroup];
    updatedFilterGroup[targetFilterIndex] = newFilter;

    onChangeFilters({
      ...activeFilters,
      [attributeId]: updatedFilterGroup,
    });
  };

  return (
    <Container>
      Filters
      <CreateFilterDropdown filterableAttributes={filterableAttributes}
                            onCreateFilter={onCreateFilter}
                            activeFilters={activeFilters}
                            filterValueRenderers={filterValueRenderers} />
      {activeFilters && (
        <ActiveFilters filters={activeFilters}
                       attributes={attributes}
                       onChangeFilter={onChangeFilter}
                       onDeleteFilter={onDeleteFilter}
                       filterValueRenderers={filterValueRenderers} />
      )}
    </Container>
  );
};

EntityFilters.defaultProps = {
  filterValueRenderers: undefined,
};

export default EntityFilters;
