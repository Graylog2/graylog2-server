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
import styled, { css } from 'styled-components';

import { LinkContainer } from 'components/common/router';
import NavItemStateIndicator, {
  hoverIndicatorStyles,
  activeIndicatorStyles,
} from 'components/common/NavItemStateIndicator';
import { ButtonToolbar, Button } from 'components/bootstrap';

const Container = styled(ButtonToolbar)`
  margin-bottom: 10px;
`;

const StyledButton = styled(Button)(
  ({ theme }) => css`
  font-family: ${theme.fonts.family.navigation};
  font-size: ${theme.fonts.size.navigation};
  color: ${theme.colors.text.primary};
  
  &:hover,
  &:focus {
    background: inherit;
    text-decoration: none;
  }

  &:hover {
    color: inherit;
    ${hoverIndicatorStyles(theme)}
  }

  &.active {
    color: ${theme.colors.text.primary};

    ${activeIndicatorStyles(theme)}

    &:hover,
    &:focus-visible {
      ${activeIndicatorStyles(theme)}
    }
`,
);

StyledButton.displayName = 'Button';

type Props = {
  items: Array<{
    description: string;
    path: string;
    exactPathMatch?: boolean;
  }>;
};

const NavTabs = ({ items }: Props) => (
  <Container>
    {items.map(({ path, description, exactPathMatch }) => (
      <LinkContainer to={path} relativeActive={!exactPathMatch} key={path}>
        <StyledButton bsStyle="transparent">
          <NavItemStateIndicator>{description}</NavItemStateIndicator>
        </StyledButton>
      </LinkContainer>
    ))}
  </Container>
);

export default NavTabs;
