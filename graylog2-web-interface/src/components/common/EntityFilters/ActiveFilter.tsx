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
import React, { useState } from 'react';
import styled from 'styled-components';

import { Button } from 'components/bootstrap';
import type { Attribute } from 'stores/PaginationTypes';
import { Icon } from 'components/common';
import type { Filter, Filters } from 'components/common/EntityFilters/types';
import OverlayDropdown from 'components/common/OverlayDropdown';
import FilterConfiguration from 'components/common/EntityFilters/FilterConfiguration';

const Container = styled.div`
  display: flex;
`;

const CenteredButton = styled(Button)`
  display: flex;
  align-items: center;
`;

type FilterValueDropdownProps = {
  attribute: Attribute,
  allActiveFilters: Filters | undefined,
  filter: Filter,
  filterValueRenderer: (value: Filter['value'], title: string) => React.ReactNode | undefined,
  onChangeFilter: (attributeId: string, prevValue: string, newFilter: Filter) => void,
}

const FilterValueDropdown = ({ attribute, allActiveFilters, onChangeFilter, filterValueRenderer, filter }: FilterValueDropdownProps) => {
  const [show, setShowDropdown] = useState(false);
  const { value, title } = filter;

  const _onToggle = () => {
    setShowDropdown((cur) => !cur);
  };

  const onSubmit = (newFilter: { title: string, value: string }) => {
    onChangeFilter(attribute.id, value, { value: newFilter.value, title: newFilter.title });
    _onToggle();
  };

  return (
    <OverlayDropdown show={show}
                     closeOnSelect={false}
                     toggleChild={(
                       <CenteredButton bsSize="xsmall" title="Change filter value">
                         {filterValueRenderer ? filterValueRenderer(value, title) : title}
                       </CenteredButton>
                     )}
                     placement="bottom"
                     onToggle={_onToggle}>
      <FilterConfiguration attribute={attribute}
                           filterValueRenderer={filterValueRenderer}
                           onSubmit={onSubmit}
                           filter={filter}
                           allActiveFilters={allActiveFilters} />
    </OverlayDropdown>
  );
};

type Props = {
  attribute: Attribute,
  filter: Filter,
  allActiveFilters: Filters | undefined
  filterValueRenderer: (value: string, title: string) => React.ReactNode | undefined,
  onChangeFilter: (attributeId: string, prevValue: string, newFilter: Filter) => void,
  onDeleteFilter: (attributeId: string, filterId: string) => void,
}

const ActiveFilter = ({
  attribute,
  allActiveFilters,
  filter,
  filterValueRenderer,
  onDeleteFilter,
  onChangeFilter,
}: Props) => {
  const { value, title } = filter;

  const onChangeBooleanValue = () => {
    if (attribute.type === 'BOOLEAN') {
      const oppositeFilterOption = attribute.filter_options.find(({ value: optionVal }) => optionVal !== value);
      onChangeFilter(attribute.id, value, { value: oppositeFilterOption.value, title: oppositeFilterOption.title });
    }
  };

  return (
    <Container className="btn-group" data-testid={`${attribute.id}-filter-${value}`}>
      {attribute.type === 'BOOLEAN' && (
        <CenteredButton bsSize="xsmall" onClick={onChangeBooleanValue} title="Change filter value">
          {filterValueRenderer ? filterValueRenderer(value, title) : title}
        </CenteredButton>
      )}
      {attribute.type !== 'BOOLEAN' && (
        <FilterValueDropdown onChangeFilter={onChangeFilter}
                             attribute={attribute}
                             filter={filter}
                             allActiveFilters={allActiveFilters}
                             filterValueRenderer={filterValueRenderer} />
      )}
      <CenteredButton bsSize="xsmall" onClick={() => onDeleteFilter(attribute.id, value)} title="Delete filter">
        <Icon name="times" />
      </CenteredButton>
    </Container>
  );
};

export default ActiveFilter;
