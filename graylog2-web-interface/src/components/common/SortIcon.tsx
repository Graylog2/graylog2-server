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

import styled, { css } from 'styled-components';
import React, { useCallback } from 'react';

import Icon from 'components/common/Icon';

const StyledSortIcon = styled.button(({ theme }) => css`
  border: 0;
  background: transparent;
  padding: 5px;
  cursor: pointer;
  position: relative;
  color: ${theme.colors.gray[70]};
  display: inline-flex;
  align-items: center;
  justify-content: center;
  vertical-align: middle;

  &.active {
    color: ${theme.colors.gray[20]};
  }
`);

const Bulb = styled.span(({ theme }) => css`
  position: absolute;
  top: 0;
  right: 0;
  font-size: ${theme.fonts.size.small};
  font-weight: 600;
`);

type Props<AscDirection extends string, DescDirection extends string> = {
  activeDirection: AscDirection | DescDirection | null,
  ascId?: string,
  descId?: string
  onChange: (activeDirection: AscDirection | DescDirection | null) => void,
  title?: string,
  order?: number,
  className?: string
}

const SortIcon = <AscDirection extends string, DescDirection extends string>({
  activeDirection,
  onChange,
  title = 'Sort',
  order,
  ascId = 'Ascending',
  descId = 'Descending',
  className = '',
}: Props<AscDirection, DescDirection>) => {
  const handleSortChange = useCallback(() => onChange(activeDirection), [activeDirection, onChange]);
  const isAscSort = activeDirection === ascId && activeDirection !== descId;

  const sortActive = !!activeDirection;

  return (
    <StyledSortIcon className={`${className} ${sortActive ? 'active' : ''}`}
                    title={title}
                    type="button"
                    aria-label={title}
                    onClick={handleSortChange}>
      <Icon name="sort" data-testid="sort-icon-svg" flip={isAscSort ? 'horizontal' : undefined} className={`sort-icon-${isAscSort ? 'asc' : 'desc'}`} />
      {order && <Bulb>{order}</Bulb>}
    </StyledSortIcon>
  );
};

export default SortIcon;
