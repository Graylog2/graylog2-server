// @flow strict
import * as React from 'react';
import styled, { css, type StyledComponent } from 'styled-components';
import PropTypes from 'prop-types';

import { type ThemeInterface } from 'theme';
import IconButton from 'components/common/IconButton';

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

const Header = styled.div`
  flex: 1;
  margin-right: 5px;
`;

const Description = styled.span`
  flex: 2;
`;

const StyledDeleteButton = styled(IconButton)`
  flex: 0;
`;

const PaginatedItem = ({ item: { name, description }, onDeleteItem, item }: Props) => {
  const deleteButton = typeof onDeleteItem === 'function'
    ? <StyledDeleteButton onClick={() => onDeleteItem(item)} name="times" title={`Remove ${name}`} />
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

PaginatedItem.propTypes = {
  onDeleteItem: PropTypes.func,
};

export default PaginatedItem;
