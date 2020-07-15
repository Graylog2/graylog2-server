// @flow strict
import * as React from 'react';
import styled, { css, type StyledComponent } from 'styled-components';

import { type ThemeInterface } from 'theme';
import Role from 'logic/roles/Role';

type Props = { role: Role };

const Container: StyledComponent<{ isAdmin: boolean }, ThemeInterface, HTMLSpanElement> = styled.span(({ isAdmin, theme }) => css`
  display:block;
  padding: 10px;
  background-color: ${isAdmin ? theme.colors.table.variantHover.info : theme.colors.table.background};

  :nth-of-type(even) {
    background-color: ${isAdmin ? theme.colors.table.variant.info : theme.colors.table.backgroundAlt};
  };
`);

const Header = styled.h4`
  padding-bottom: 5px;
`;

const RoleItem = ({ role: { name, description } }: Props) => (
  <Container isAdmin={name === 'Admin'}>
    <Header>{name}</Header>
    <span>{description}</span>
    <hl />
  </Container>
);

export default RoleItem;
