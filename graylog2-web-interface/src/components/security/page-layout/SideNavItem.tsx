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
import type { IconName } from '@fortawesome/fontawesome-common-types';
import { NavLink } from 'react-router-dom';
import styled, { css } from 'styled-components';

import Icon from 'components/common/Icon';

const StyledLink = styled(NavLink)(({ theme }) => css`
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
  gap: 0.6rem;
  transition: all 0.33s ease-in-out;
  padding: 5px 7px;
  margin: 16px 10px;
  border-radius: 5px;
  color: ${theme.colors.gray[30]};

  & > svg {
    width: 20px;
  }

  &:hover {
    background: ${theme.colors.global.contentBackground};
    text-decoration: none;
    color: ${theme.colors.global.textDefault};
  }

  &.active {
    background: ${theme.colors.global.contentBackground};
    color: ${theme.colors.brand.primary};
  }
`);

type Props = React.PropsWithChildren<{
  linkTarget: string,
  linkEnd?: boolean,
  iconName: IconName,
}>

const SideNavItem = ({ children, linkTarget, linkEnd, iconName }: Props) => (
  <li>
    <StyledLink to={linkTarget} end={linkEnd}>
      <Icon name={iconName} size="xs" />
      {children}
    </StyledLink>
  </li>
);

SideNavItem.defaultProps = {
  linkEnd: false,
};

export default SideNavItem;
