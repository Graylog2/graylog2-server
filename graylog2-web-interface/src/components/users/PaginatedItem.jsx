// @flow strict
import * as React from 'react';
import styled, { css, type StyledComponent } from 'styled-components';

import { type ThemeInterface } from 'theme';

import type { DescriptiveItem } from './PaginatedItemOverview';

type Props = { item: DescriptiveItem };

const Container: StyledComponent<{ isAdmin: boolean }, ThemeInterface, HTMLSpanElement> = styled.span(({ isAdmin, theme }) => css`
  display:block;
  padding: 10px;
  background-color: ${theme.colors.table.background};

  :nth-of-type(even) {
    background-color: ${theme.colors.table.backgroundAlt};
  };
`);

const Header = styled.h4`
  padding-bottom: 5px;
`;

const PaginatedItem = ({ item: { name, description } }: Props) => (
  <Container>
    <Header>{name}</Header>
    <span>{description}</span>
    <hl />
  </Container>
);

export default PaginatedItem;
