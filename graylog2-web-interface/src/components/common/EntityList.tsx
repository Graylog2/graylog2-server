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

import { Alert } from 'components/bootstrap';

const StyledList = styled.ul`
  padding: 0;
  margin: 0;
`;

type Props = {
  bsNoItemsStyle?: 'info' | 'success' | 'warning',
  noItemsText?: string | React.ReactNode,
  items: Array<React.ReactNode>,
}

/**
 * Component used to represent list of entities in Graylog, where each entity will have a title, description,
 * action buttons, etc. You need to use this component alongside `EntityListItem` in order to get a similar
 * look and feel among different entities.
 */
const EntityList = ({ bsNoItemsStyle, items, noItemsText = 'No items available' }: Props) => {
  if (items.length === 0) {
    return (
      <Alert bsStyle={bsNoItemsStyle}>
        {noItemsText}
      </Alert>
    );
  }

  return (
    <StyledList>
      {items}
    </StyledList>
  );
};

export default EntityList;
