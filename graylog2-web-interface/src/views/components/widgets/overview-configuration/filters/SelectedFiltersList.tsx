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
import * as React from 'react';
import type * as Immutable from 'immutable';
import styled from 'styled-components';
import { useRef } from 'react';

import { Input } from 'components/bootstrap';
import { IconButton } from 'components/common';
import type { FilterComponents, Filter } from 'views/components/widgets/overview-configuration/filters/types';
import FilterEditButton from 'views/components/widgets/overview-configuration/filters/FilterEditButton';
import UnknownAttributeTitle from 'views/components/widgets/events/UnknownAttributeTitle';

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
                   label={_columnTitle === 'unknown' ? <UnknownAttributeTitle /> : _columnTitle}
                   labelClassName="col-sm-3"
                   wrapperClassName="col-sm-9">
              {values.map((value, valueIndex) => {
                const _onEdit = (newValue: string) => {
                  onEdit(filterIndex, valueIndex, newValue);
                };

                return (
                  <FilterValue key={value}>
                    <ValueTitle>
                      {filterComponent ? filterComponent.renderValue?.(value) ?? value : ''}
                    </ValueTitle>
                    <ValueActions>
                      {filterComponent && (
                        <FilterEditButton filterComponent={filterComponent}
                                          onEdit={_onEdit}
                                          onDelete={() => onDelete(filterIndex, value)}
                                          selectedValues={values}
                                          columnTitle={columnTitle}
                                          containerWidth={container.current?.offsetWidth}
                                          column={column}
                                          value={value} />
                      )}
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
