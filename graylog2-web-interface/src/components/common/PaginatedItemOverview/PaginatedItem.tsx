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
import PropTypes from 'prop-types';

import IconButton from 'components/common/IconButton';

import type { DescriptiveItem } from './PaginatedItemOverview';

type Props = {
  item: DescriptiveItem,
  onDeleteItem?: (descriptiveItem: DescriptiveItem) => void,
};

const Container = styled.span(({ theme }) => css`
  display: flex;
  padding: 10px;
  background-color: ${theme.colors.table.background};

  :nth-of-type(even) {
    background-color: ${theme.colors.table.backgroundAlt};
  }
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
