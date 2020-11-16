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
// eslint-disable-next-line no-restricted-imports
import { ListGroup as BootstrapListGroup } from 'react-bootstrap';

const ListGroup = ({ className, ...props }) => {
  return <BootstrapListGroup bsClass={className} {...props} />;
};

ListGroup.propTypes = {
  className: PropTypes.string,
};

ListGroup.defaultProps = {
  className: undefined,
};

/** @component */
export default ListGroup;
