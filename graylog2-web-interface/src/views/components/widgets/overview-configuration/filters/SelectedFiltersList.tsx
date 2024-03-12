import * as React from 'react';
import type * as Immutable from 'immutable';
import styled from 'styled-components';
import { useRef } from 'react';

import { Input } from 'components/bootstrap';
import { IconButton } from 'components/common';
import type { FilterComponents, Filter } from 'views/components/widgets/overview-configuration/filters/types';
import FilterEditButton from 'views/components/widgets/overview-configuration/filters/FilterEditButton';

const Container = styled.div`
  display: flex;
  flex-direction: column;
  width: 100%;
`;

const FilterContainer = styled.div`
  display: flex;
  flex-direction: column;
  width: 100%;
`;

const FilterValue = styled.div`
  display: flex;
  justify-content: space-between;
`;

const ValueTitle = styled.div`
  padding-top: 5px;
`;

const ValueActions = styled.div`
  margin-top: 2px;
`;

type Props = {
  columnTitle: (column: string) => string,
  filterComponents: FilterComponents,
  onDelete: (filterIndex: number, value: string) => void,
  onEdit: (columnIndex: number, valueIndex: number, value: string) => void,
  selectedFilters: Immutable.OrderedSet<Filter>,
}

const SelectedFiltersList = ({ selectedFilters, columnTitle, filterComponents, onDelete, onEdit }: Props) => {
  const container = useRef(null);

  return (
    <Container ref={container}>
      {selectedFilters.toArray().map(({ field: column, value: values }, filterIndex) => {
        const _columnTitle = columnTitle(column);
        const filterComponent = filterComponents.find(({ attribute }) => attribute === column);

        return (
          <FilterContainer key={column}>
            <Input id={`${column}-filter-configuration`}
                   label={_columnTitle}
                   labelClassName="col-sm-3"
                   wrapperClassName="col-sm-9">
              {values.map((value, valueIndex) => {
                const _onEdit = (newValue: string) => {
                  onEdit(filterIndex, valueIndex, newValue);
                };

                return (
                  <FilterValue key={value}>
                    <ValueTitle>
                      {filterComponent?.renderValue(value) ?? value}
                    </ValueTitle>
                    <ValueActions>
                      <FilterEditButton filterComponent={filterComponent}
                                        onEdit={_onEdit}
                                        selectedValues={values}
                                        columnTitle={columnTitle}
                                        containerWidth={container.current?.offsetWidth}
                                        column={column}
                                        value={value} />
                      <IconButton name="delete"
                                  onClick={() => onDelete(filterIndex, value)}
                                  title={`Delete ${_columnTitle} filter`} />
                    </ValueActions>
                  </FilterValue>
                );
              })}
            </Input>
          </FilterContainer>
        );
      })}
    </Container>
  );
};

export default SelectedFiltersList;
