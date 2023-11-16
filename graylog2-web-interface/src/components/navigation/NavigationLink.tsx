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
import styled, {css} from 'styled-components';

import Menu from 'components/bootstrap/Menu';
import { LinkContainer } from 'components/common/router';
import { NavItem } from 'components/bootstrap';

// We render a NavItem if topLevel is set to avoid errors when the NavigationLink is place in the navigation
// bar instead of a navigation drop-down menu.
type Props = {
  description: string,
  path: string,
  topLevel: boolean,
}

const StyledMenuItem = styled(Menu.Item)(({ theme }) => css`
  color: ${theme.colors.global.textDefault};
  font-family: ${theme.fonts.family.navigation};
  font-size: ${theme.fonts.size.navigation};
`);

const NavigationLink = ({ description, path, topLevel, ...rest }: Props) => (
  <LinkContainer key={path} to={path} {...rest}>
    {topLevel ? <NavItem>{description}</NavItem> : <StyledMenuItem>{description}</StyledMenuItem>}
  </LinkContainer>
);

NavigationLink.propTypes = {
  description: PropTypes.oneOfType([PropTypes.string, PropTypes.object]).isRequired,
  path: PropTypes.string.isRequired,
  topLevel: PropTypes.bool,
};

NavigationLink.defaultProps = {
  topLevel: false,
};

export default NavigationLink;
