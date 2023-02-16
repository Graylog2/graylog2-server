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

import { Button } from 'components/bootstrap';
import type { Attribute } from 'stores/PaginationTypes';
import { Icon } from 'components/common';

const Container = styled.div`
  display: flex;

  :not(:last-child) {
    margin-right: 3px;
  }
`;

const CenteredButton = styled(Button)`
  display: flex;
  align-items: center;
`;

type Props = {
  attribute: Attribute,
  filter: { title: string, value: string, id: string },
  filterValueRenderer: (value: unknown, title: string) => React.ReactNode | undefined,
  onChangeFilter: (attributeId: string, filterId: string, newValue: string, newTitle) => void,
  onDeleteFilter: (attributeId: string, filterId: string) => void,
}

const ActiveFilter = ({
  attribute,
  filter: { value, title, id },
  filterValueRenderer,
  onDeleteFilter,
  onChangeFilter,
}: Props) => {
  const onFilterClick = (attributeId: string, curValue: string, filterId: string) => {
    if (attribute.type === 'boolean') {
      const oppositeFilterOption = attribute.filter_options.find(({ value: optionVal }) => optionVal !== curValue);
      onChangeFilter(attributeId, filterId, oppositeFilterOption.value, oppositeFilterOption.title);
    }
  };

  return (
    <Container className="btn-group" data-testid={`filter-${id}`}>
      <CenteredButton bsSize="xsmall" onClick={() => onFilterClick(attribute.id, value, id)} title="Change filter value">
        {filterValueRenderer ? filterValueRenderer(value, title) : title}
      </CenteredButton>
      <CenteredButton bsSize="xsmall" onClick={() => onDeleteFilter(attribute.id, id)} title="Delete filter">
        <Icon name="times" />
      </CenteredButton>
    </Container>
  );
};

export default ActiveFilter;
