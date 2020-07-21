// @flow strict
import * as React from 'react';
import styled, { css, type StyledComponent } from 'styled-components';

import { type ThemeInterface } from 'theme';
import { IconButton } from 'components/common';

import type { DescriptiveItem } from './PaginatedItemOverview';

type Props = {
  item: DescriptiveItem,
  onDeleteItem?: (DescriptiveItem) => void,
};

const Container: StyledComponent<{}, ThemeInterface, HTMLSpanElement> = styled.span(({ theme }) => css`
  display: flex;
  padding: 10px;
  background-color: ${theme.colors.table.background};

  :nth-of-type(even) {
    background-color: ${theme.colors.table.backgroundAlt};
  };
`);

const Header = styled.h4`
  padding-bottom: 5px;
  flex: 1;
`;

const Description = styled.span`
  flex: 1;
`;

const StyledDeleteButton = styled(IconButton)(() => css`
  flex: 1;
`);

const PaginatedItem = ({ item: { name, description }, onDeleteItem, item }: Props) => {
  const deleteButton = typeof onDeleteItem === 'function'
    ? <StyledDeleteButton onClick={() => onDeleteItem(item)} name="remove" />
    : null;

  return (
    <Container>
      <Header>{name}</Header>
      <Description>{description}</Description>
      { deleteButton }
    </Container>
  );
};

PaginatedItem.defaultProps = {
  onDeleteItem: undefined,
};

export default PaginatedItem;
