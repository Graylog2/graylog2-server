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

import { LinkContainer } from 'components/graylog/router';
import { MenuItem, NavItem } from 'components/graylog';

// We render a NavItem if topLevel is set to avoid errors when the NavigationLink is place in the navigation
// bar instead of a navigation drop-down menu.
const NavigationLink = ({ description, path, topLevel, ...rest }) => (
  <LinkContainer key={path} to={path} {...rest}>
    {topLevel ? <NavItem>{description}</NavItem> : <MenuItem>{description}</MenuItem>}
  </LinkContainer>
);

NavigationLink.propTypes = {
  description: PropTypes.string.isRequired,
  path: PropTypes.string.isRequired,
  topLevel: PropTypes.bool,
};

NavigationLink.defaultProps = {
  topLevel: false,
};

export default NavigationLink;
