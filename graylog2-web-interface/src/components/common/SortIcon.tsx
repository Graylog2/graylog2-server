import styled, { css } from 'styled-components';
import React, { useCallback, useMemo } from 'react';

import Icon from 'components/common/Icon';
import type { DirectionJson } from 'views/logic/aggregationbuilder/Direction';

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

type Directions = DirectionJson | 'DESC' | 'ASC' | 'asc' | 'desc' | undefined | null;
type Props = {
  direction: Directions,
  onChange: (direction: Directions) => void,
  ariaLabel?: string,
  title?: string,
  bulbText?: string | number | undefined,
  dataTestId?: string | undefined
}

const SortIcon = ({ direction, onChange, ariaLabel, title, bulbText, dataTestId }: Props) => {
  const handleSortChange = useCallback(() => onChange(direction), [direction, onChange]);
  const iconName = useMemo(() => {
    if (new Set(['Ascending', 'ASC', 'asc']).has(direction)) return 'arrow-up-short-wide';

    return 'arrow-down-wide-short';
  }, [direction]);
  const sortActive = !!direction;

  return (
    <StyledSortIcon className={sortActive ? 'active' : ''}
                    title={title}
                    type="button"
                    aria-label={ariaLabel}
                    onClick={handleSortChange}
                    data-testid={dataTestId}>
      <Icon name={iconName} />
      {bulbText && <Bulb>{bulbText}</Bulb>}
    </StyledSortIcon>
  );
};

SortIcon.defaultProps = {
  ariaLabel: 'Sort',
  title: 'Sort',
  bulbText: undefined,
  dataTestId: undefined,
};
