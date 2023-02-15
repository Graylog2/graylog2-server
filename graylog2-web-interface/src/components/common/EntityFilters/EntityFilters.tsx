import React from 'react';
import styled from 'styled-components';

import CreateFilterDropdown from 'components/common/EntityFilters/CreateFilterDropdown';
import type { Attributes } from 'stores/PaginationTypes';
import type { Filters } from 'components/common/EntityFilters/types';
import ActiveFilters from 'components/common/EntityFilters/ActiveFilters';

const Container = styled.div`
  display: inline-flex;
  height: 34px;
  align-items: center;
  margin-left: 5px;
`;

type Props = {
  attributes: Attributes,
  onCreateFilter: (attributeId: string, filter: { value: string, title: string }) => void,
  activeFilters: Filters,
  filterValueRenderer?: { [attributeId: string]: (value: unknown, title: string) => React.ReactNode };
}

const EntityFilters = ({ attributes = [], onCreateFilter, activeFilters, filterValueRenderer }: Props) => {
  const filterableAttributes = attributes.filter(({ filterable }) => filterable);

  if (!filterableAttributes.length) {
    return null;
  }

  return (
    <Container>
      Filters
      <CreateFilterDropdown filterableAttributes={filterableAttributes}
                            onSubmit={onCreateFilter}
                            filterValueRenderer={filterValueRenderer} />
      {activeFilters && (
        <ActiveFilters filters={activeFilters}
                       attributes={attributes}
                       filterValueRenderer={filterValueRenderer} />
      )}
    </Container>
  );
};

EntityFilters.defaultProps = {
  filterValueRenderer: undefined,
};

export default EntityFilters;
