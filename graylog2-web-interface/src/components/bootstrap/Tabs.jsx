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
// eslint-disable-next-line no-restricted-imports
import { Tabs as BootstrapTabs } from 'react-bootstrap';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import * as React from 'react';

import navTabsStyles from './styles/nav-tabs';

const StyledTabs = styled(BootstrapTabs)`
  ${navTabsStyles}
`;

const Tabs = ({ children, ...restProps }) => {
  return <StyledTabs {...restProps}>{children}</StyledTabs>;
};

Tabs.propTypes = {
  children: PropTypes.node,
};

Tabs.defaultProps = {
  children: undefined,
};

export default Tabs;
