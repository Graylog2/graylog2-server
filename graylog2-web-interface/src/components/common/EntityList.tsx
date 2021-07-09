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
import PropTypes from 'prop-types';
import React from 'react';
import styled from 'styled-components';

import { Alert } from 'components/graylog';

import Icon from './Icon';

const StyledList = styled.ul`
  padding: 0;
  margin: 0;
`;

type Props = {
  bsNoItemsStyle: 'info' | 'success' | 'warning',
  noItemsText: string | React.ReactElement,
  items: Array<React.ReactElement>,
}

/**
 * Component used to represent list of entities in Graylog, where each entity will have a title, description,
 * action buttons, etc. You need to use this component alongside `EntityListItem` in order to get a similar
 * look and feel among different entities.
 */
const EntityList = ({ bsNoItemsStyle, items, noItemsText }: Props) => {
  if (items.length === 0) {
    return (
      <Alert bsStyle={bsNoItemsStyle}>
        <Icon name="info-circle" />&nbsp;
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

EntityList.defaultProps = {
  bsNoItemsStyle: 'info',
  noItemsText: 'No items available',
};

EntityList.propTypes = {
  /** bsStyle to use when there are no items in the list. */
  bsNoItemsStyle: PropTypes.oneOf(['info', 'success', 'warning']),
  /** Text to show when there are no items in the list. */
  noItemsText: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.element,
  ]),
  /** Array of `EntityListItem` that will be shown.  */
  items: PropTypes.array.isRequired,
};

export default EntityList;
