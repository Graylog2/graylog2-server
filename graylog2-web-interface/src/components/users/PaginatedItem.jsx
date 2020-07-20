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

type DeleteProps = {
  item: DescriptiveItem,
  onDeleteItem?: (DescriptiveItem) => void,
};

// eslint-disable-next-line react/prop-types
const DeleteButton = ({ onDeleteItem, item }: DeleteProps) => {
  if (typeof onDeleteItem === 'function') {
    return (
      <IconButton onClick={() => onDeleteItem(item)} name="remove" />
    );
  }

  return null;
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

const StyledDeleteButton = styled(DeleteButton)(() => css`
  flex: 1;
`);

const PaginatedItem = ({ item: { name, description }, onDeleteItem, item }: Props) => (
  <Container>
    <Header>{name}</Header>
    <Description>{description}</Description>
    <StyledDeleteButton onDeleteItem={onDeleteItem} item={item} />
  </Container>
);

PaginatedItem.defaultProps = {
  onDeleteItem: undefined,
};

export default PaginatedItem;
