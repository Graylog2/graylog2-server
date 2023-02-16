import React from 'react';
import styled from 'styled-components';

import type { Filters } from 'components/common/EntityFilters/types';
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
  onChangeFilter: (attributeId: string, filterId: string, newValue: string, newTitle) => void,
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
                          filterValueRenderer={filterValueRenderers[attributeId]}
                          onChangeFilter={onChangeFilter}
                          onDeleteFilter={onDeleteFilter} />
          ))}
        </FilterGroup>
      );
    })}

  </Container>
);

export default ActiveFilters;
