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
import React, { useCallback, useMemo } from 'react';

import Icon from 'components/common/Icon';

const StyledSortIcon = styled.button(({ theme }) => {
  return css`
    border: 0;
    background: transparent;
    padding: 5px;
    cursor: pointer;
    position: relative;
    color: ${theme.colors.gray[70]};
    &.active {
      color: ${theme.colors.gray[20]};
    }
  `;
});

const Bulb = styled.span`
  position: absolute;
  top: 0;
  right: 0;
  font-size: 0.75rem;
  font-weight: 600;
`;

type Props<AscDirection extends string, DescDirection extends string> = {
  activeDirection: AscDirection | DescDirection | null,
  ascId?: string,
  descId?: string
  onChange: (activeDirection: AscDirection | DescDirection | null) => void,
  ariaLabel?: string,
  title?: string,
  order?: number,
  dataTestId?: string | undefined
  className?: string
}

const SortIcon = <AscDirection extends string, DescDirection extends string>({
  activeDirection,
  onChange,
  ariaLabel,
  title,
  order,
  dataTestId,
  ascId,
  descId,
  className,
}: Props<AscDirection, DescDirection>) => {
  const handleSortChange = useCallback(() => onChange(activeDirection), [activeDirection, onChange]);
  const iconName = useMemo(() => {
    if (activeDirection === ascId) return 'arrow-up-short-wide';
    if (activeDirection === descId) return 'arrow-down-wide-short';

    return 'arrow-down-wide-short';
  }, [activeDirection, ascId, descId]);
  const sortActive = !!activeDirection;

  return (
    <StyledSortIcon className={sortActive ? 'active' : ''}
                    title={title}
                    type="button"
                    aria-label={ariaLabel}
                    onClick={handleSortChange}
                    data-testid={dataTestId}>
      <Icon name={iconName} className={className} data-testid="sort-icon-svg" />
      {order && <Bulb>{order}</Bulb>}
    </StyledSortIcon>
  );
};

SortIcon.defaultProps = {
  ariaLabel: 'Sort',
  title: 'Sort',
  order: undefined,
  dataTestId: undefined,
  ascId: 'Ascending',
  descId: 'Descending',
  className: '',
};

export default SortIcon;
