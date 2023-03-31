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
import styled from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { NavDropdown } from 'react-bootstrap';

import NavItemStateIndicator from 'components/common/NavItemStateIndicator';

import menuItemStyles from './styles/menuItem';

const StyledNavDropdown = styled(NavDropdown)`
  ${menuItemStyles}
`;

type Props = {
  title: string,
  inactiveTitle?: string,
  badge?: React.ComponentType<{ text: string }>,
  [key: string]: any,
};

function GLNavDropdown({ title, inactiveTitle, badge: Badge, ...rest }: Props) {
  const isActive = inactiveTitle ? inactiveTitle !== title : undefined;

  return (
    <StyledNavDropdown {...rest}
                       id={rest.id}
                       title={<NavItemStateIndicator>{Badge ? <Badge text={title} /> : title}</NavItemStateIndicator>}
                       active={isActive} />
  );
}

GLNavDropdown.defaultProps = {
  inactiveTitle: undefined,
  badge: undefined,
};

export default GLNavDropdown;
