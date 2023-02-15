import React from 'react';
import styled from 'styled-components';

import { Label } from 'components/bootstrap';
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

const FilterValue = styled(Label)`
  display: flex;
`;

const Spacer = styled.div`
  border-left: 1px solid currentColor;
  height: 1em;
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
              <FilterValue bsSize="xsmall">
                {filterValueRenderer[attributeId] ? filterValueRenderer[attributeId](value, title) : title}
                <Spacer />
                <Icon name="times" />
              </FilterValue>
            ),
            )}
          </FilterGroup>
        );
      })}

    </Container>
  );
};

export default ActiveFilters;
