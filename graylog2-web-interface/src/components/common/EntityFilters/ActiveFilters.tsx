import React from 'react';
import styled from 'styled-components';

import { Button } from 'components/bootstrap';
import type { Filters } from 'components/common/EntityFilters/types';
import type { Attributes } from 'stores/PaginationTypes';
import { Icon } from 'components/common';

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

const Filter = styled.div`
  display: flex;
`;

const CenteredButton = styled(Button)`
  display: flex;
  align-items: center;
`;

type Props = {
  filters: Filters,
  attributes: Attributes,
  filterValueRenderer: { [attributeId: string]: (value: unknown, title: string) => React.ReactNode } | undefined,
  onDeleteFilter: (attributeId: string, filterId: string) => void,
  onChangeFilter: (attributeId: string, filterId: string, newValue: string, newTitle) => void,
}

const ActiveFilters = ({ attributes = [], filters, filterValueRenderer, onDeleteFilter, onChangeFilter }: Props) => {
  const onFilterClick = (attributeId: string, curValue: string, filterId: string) => {
    const relatedAttribute = attributes.find(({ id }) => id === attributeId);

    if (relatedAttribute.type === 'boolean') {
      const oppositeFilterOption = relatedAttribute.filter_options.find(({ value }) => value !== curValue);
      console.log({ oppositeFilterOption });
      onChangeFilter(attributeId, filterId, oppositeFilterOption.value, oppositeFilterOption.title);
    }
  };

  return (
    <Container>
      {Object.entries(filters).map(([attributeId, filterValues]) => {
        const relatedAttribute = attributes.find(({ id }) => id === attributeId);

        return (
          <FilterGroup key={attributeId}>
            <FilterGroupTitle>
              {relatedAttribute.title}:
            </FilterGroupTitle>
            {filterValues.map(({ title, value, id }) => (
              <Filter className="btn-group" key={id}>
                <CenteredButton bsSize="xsmall" onClick={() => onFilterClick(attributeId, value, id)} title="Change value">
                  {filterValueRenderer[attributeId] ? filterValueRenderer[attributeId](value, title) : title}
                </CenteredButton>
                <CenteredButton bsSize="xsmall" onClick={() => onDeleteFilter(attributeId, id)} title="Delete filter">
                  <Icon name="times" />
                </CenteredButton>
              </Filter>
            ))}
          </FilterGroup>
        );
      })}

    </Container>
  );
};

export default ActiveFilters;
