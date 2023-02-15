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
  filters: Filters
  attributes: Attributes
  filterValueRenderer: { [attributeId: string]: (value: unknown, title: string) => React.ReactNode } | undefined;
}

const ActiveFilters = ({ attributes = [], filters, filterValueRenderer }: Props) => {
  return (
    <Container>
      {Object.entries(filters).map(([attributeId, filterValues]) => {
        const relatedAttribute = attributes.find(({ id }) => id === attributeId);

        return (
          <FilterGroup>
            <FilterGroupTitle>
              {relatedAttribute.title}:
            </FilterGroupTitle>
            {filterValues.map(({ title, value }) => (
              <Filter className="btn-group">
                <CenteredButton bsSize="xsmall">
                  {filterValueRenderer[attributeId] ? filterValueRenderer[attributeId](value, title) : title}
                </CenteredButton>
                <CenteredButton bsSize="xsmall">
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
