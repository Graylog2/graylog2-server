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
  onUpdateFilters: (filters: Filters) => void,
  activeFilters: Filters,
  filterValueRenderer?: { [attributeId: string]: (value: unknown, title: string) => React.ReactNode };
}

const EntityFilters = ({ attributes = [], activeFilters = {}, filterValueRenderer, onUpdateFilters }: Props) => {
  const filterableAttributes = attributes.filter(({ filterable }) => filterable);

  if (!filterableAttributes.length) {
    return null;
  }

  const onCreateFilter = (attributeId, filter: { value: string, title; string, id: string}) => {
    onUpdateFilters({
      ...activeFilters,
      [attributeId]: [
        ...activeFilters[attributeId] ?? [],
        filter,
      ],
    });
  };

  const onDeleteFilter = (attributeId: string, filterId: string) => {
    const attributeFilters = activeFilters[attributeId];
    const newAttributeFilterValues = attributeFilters.filter(({ id }) => id !== filterId);
    let newFilters = { ...activeFilters };

    if (newAttributeFilterValues.length) {
      newFilters = {
        ...activeFilters,
        [attributeId]: newAttributeFilterValues,
      };
    } else {
      delete newFilters[attributeId];
    }

    onUpdateFilters(newFilters);
  };

  const onChangeFilter = (attributeId: string, filterId: string, newValue: string, newTitle: string) => {
    const filterGroup = activeFilters[attributeId];
    const targetFilterIndex = filterGroup.findIndex(({ id }) => id === filterId);
    const targetFilter = filterGroup[targetFilterIndex];

    const updatedFilterGroup = [...filterGroup];
    updatedFilterGroup[targetFilterIndex] = { id: targetFilter.id, value: newValue, title: newTitle };

    onUpdateFilters({
      ...activeFilters,
      [attributeId]: updatedFilterGroup,
    });
  };

  return (
    <Container>
      Filters
      <CreateFilterDropdown filterableAttributes={filterableAttributes}
                            onSubmit={onCreateFilter}
                            filterValueRenderer={filterValueRenderer} />
      {activeFilters && (
        <ActiveFilters filters={activeFilters}
                       attributes={attributes}
                       onChangeFilter={onChangeFilter}
                       onDeleteFilter={onDeleteFilter}
                       filterValueRenderer={filterValueRenderer} />
      )}
    </Container>
  );
};

EntityFilters.defaultProps = {
  filterValueRenderer: undefined,
};

export default EntityFilters;
