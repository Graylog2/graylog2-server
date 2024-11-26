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
import { ROW_MIN_HEIGHT } from 'components/common/EntityFilters/Constants';

const FilterGroup = styled.div`
  display: inline-flex;
  align-items: center;
  min-height: ${ROW_MIN_HEIGHT}px;
  gap: 3px;
  flex-wrap: wrap;
`;

const FilterGroupTitle = styled.div`
  margin-right: 3px;
`;

type Props = {
  attributes: Attributes,
  filterValueRenderers: { [attributeId: string]: (value: unknown, title: string) => React.ReactNode } | undefined,
  filters: Filters,
  onChangeFilter: (attributeId: string, prevValue: string, newFilter: Filter) => void,
  onDeleteFilter: (attributeId: string, filterValue: string) => void,
}

const ActiveFilters = ({ attributes, filters, filterValueRenderers, onDeleteFilter, onChangeFilter }: Props) => (
  <>
    {filters.entrySeq()
      .map(([attributeId, filterValues]) => {
        const attribute = attributes?.find(({ id }) => id === attributeId);

        return (
          <FilterGroup key={attributeId}>
            <FilterGroupTitle>
              {attribute.title}:
            </FilterGroupTitle>
            {filterValues.map((filter) => (
              <ActiveFilter filter={filter}
                            allActiveFilters={filters}
                            key={`${attribute.id}-${filter.value}`}
                            attribute={attribute}
                            filterValueRenderer={filterValueRenderers?.[attributeId]}
                            onChangeFilter={onChangeFilter}
                            onDeleteFilter={onDeleteFilter} />
            ))}
          </FilterGroup>
        );
      })
      .toArray()}
  </>
);

export default ActiveFilters;
