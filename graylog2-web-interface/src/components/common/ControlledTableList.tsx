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
import PropTypes from 'prop-types';

import { ListGroup } from 'components/bootstrap';

import ControlledTableListHeader from './ControlledTableListHeader';
import ControlledTableListItem from './ControlledTableListItem';

const ControlledTableList = ({ children }: { children: Array<React.ReactNode>}) => {
  let effectiveChildren;

  if (children.length === 0) {
    effectiveChildren = <ControlledTableListItem>No items to display</ControlledTableListItem>;
  } else {
    effectiveChildren = children;
  }

  return (
    <div>
      <ListGroup>
        {effectiveChildren}
      </ListGroup>
    </div>
  );
};

ControlledTableList.Header = ControlledTableListHeader;
ControlledTableList.Item = ControlledTableListItem;

ControlledTableList.propTypes = {
  children: PropTypes.node.isRequired,
};

export default ControlledTableList;
