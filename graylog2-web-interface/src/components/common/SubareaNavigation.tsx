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

import { Button, ButtonToolbar } from 'components/bootstrap';
import { LinkContainer } from 'components/common/router';
import { IfPermitted } from 'components/common';

const Container = styled(ButtonToolbar)`
  margin-bottom: 10px;
`;

const StyledButton = styled(Button)(({ theme }) => css`
  &&&& {
    color: ${theme.colors.variant.darker.default};
    
    :hover, :focus {
      text-decoration: none;  
    }
    
    &.active {
      color: ${theme.colors.global.textDefault};
      border-bottom: 1px solid ${theme.colors.variant.primary};
    }
  }
`);

StyledButton.displayName = 'Button';

type Props = {
  items: Array<{
    title: string,
    path: string,
    permissions?: string | Array<string>
    exactPathMatch?: boolean,
  }>
}

const SubareaNavigation = ({ items }: Props) => (
  <Container>
    {items.map(({ path, title, permissions, exactPathMatch }) => {
      if (!path) {
        return null;
      }

      return (
        <IfPermitted permissions={permissions} key={path}>
          <LinkContainer to={path} relativeActive={!exactPathMatch}>
            <StyledButton bsStyle="link">{title}</StyledButton>
          </LinkContainer>
        </IfPermitted>
      );
    })}
  </Container>
);

export default SubareaNavigation;
