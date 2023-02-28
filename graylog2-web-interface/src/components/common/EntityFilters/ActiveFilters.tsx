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

import type { Filters, Filter } from 'components/common/EntityFilters/types';
import type { Attributes } from 'stores/PaginationTypes';
import ActiveFilter from 'components/common/EntityFilters/ActiveFilter';

const Container = styled.div`
  display: flex;
  margin-left: 10px;
  align-items: center;
`;

const FilterGroup = styled.div`
  display: flex;
`;

const FilterGroupTitle = styled.div`
  margin-right: 3px;
`;

type Props = {
  attributes: Attributes,
  filterValueRenderers: { [attributeId: string]: (value: unknown, title: string) => React.ReactNode } | undefined,
  filters: Filters,
  onChangeFilter: (attributeId: string, newFilter: Filter) => void,
  onDeleteFilter: (attributeId: string, filterId: string) => void,
}

const ActiveFilters = ({ attributes = [], filters, filterValueRenderers, onDeleteFilter, onChangeFilter }: Props) => (
  <Container>
    {Object.entries(filters).map(([attributeId, filterValues]) => {
      const attribute = attributes.find(({ id }) => id === attributeId);

      return (
        <FilterGroup key={attributeId}>
          <FilterGroupTitle>
            {attribute.title}:
          </FilterGroupTitle>
          {filterValues.map((filter) => (
            <ActiveFilter filter={filter}
                          key={filter.id}
                          attribute={attribute}
                          filterValueRenderer={filterValueRenderers?.[attributeId]}
                          onChangeFilter={onChangeFilter}
                          onDeleteFilter={onDeleteFilter} />
          ))}
        </FilterGroup>
      );
    })}

  </Container>
);

export default ActiveFilters;
