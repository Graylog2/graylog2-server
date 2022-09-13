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
import React from 'react';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import { OrderedMap } from 'immutable';

import Direction from 'views/logic/aggregationbuilder/Direction';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import CustomPropTypes from 'views/components/CustomPropTypes';
import type { IconName } from 'components/common/Icon';
import Icon from 'components/common/Icon';
import type { Widgets } from 'views/stores/WidgetStore';

type Props = {
  sortConfigMap: OrderedMap<string, SortConfig>,
  fieldName: string,
  onSortChange: (newSortConfig: SortConfig[]) => Promise<Widgets>,
  setLoadingState: (loading: boolean) => void,
  type: 'pivot' | 'series' | undefined
};

type DirectionStrategy = {
  handleSortChange: (changeSort: (direction: Direction) => void) => void,
  icon: IconName,
  sortActive: boolean,
  tooltip: (fieldName: string) => string,
};

const SortIcon = styled.button<{ sortActive: boolean, $index: number }>(({ sortActive, $index, theme }) => {
  const color = sortActive ? theme.colors.gray[20] : theme.colors.gray[70];

  return css`
    border: 0;
    background: transparent;
    color: ${color};
    padding: 5px;
    cursor: pointer;
    position: relative;
    &:after {
      content: '${$index}';
      position: absolute;
      top: 0;
      right: 0;
      font-size: 0.75rem;
      font-weight: 600;
    }
  `;
});

const _tooltip = (fieldName: string, newDirection: Direction) => {
  return `Sort ${fieldName} ${newDirection.direction}`;
};

const _changeSort = (nextDirection: Direction, sortConfigMap: OrderedMap<string, SortConfig>, fieldName: string, onSortChange: (newSortConfig: SortConfig[]) => Promise<Widgets>, setLoadingState: (loading: boolean) => void, type, activeSort) => {
  let newSortConfigSet;

  if (activeSort) {
    newSortConfigSet = sortConfigMap.set(fieldName, new SortConfig(type, fieldName, nextDirection));
  } else {
    newSortConfigSet = sortConfigMap.delete(fieldName);
  }

  /*
  const newSort1 = _config.sort.map((sort) => {
    if (sort.field === fieldName) {
      return new SortConfig(sort.type, fieldName, nextDirection);
    }

    return sort;
  });
   */
  // const newSort = [new SortConfig(SortConfig.PIVOT_TYPE, fieldName, nextDirection)];

  setLoadingState(true);
  console.log({ newSort1: newSortConfigSet.toList().toArray() });

  onSortChange(newSortConfigSet.toList().toArray()).then(() => {
    setLoadingState(false);
  });
};

const _isFieldSortActive = (sortConfigMap: OrderedMap<string, SortConfig>, fieldName: string) => {
  console.log({ sortConfigMap, fieldName, item: sortConfigMap.get(fieldName) });

  return sortConfigMap.get(fieldName);
};

const DirectionStrategyAsc: DirectionStrategy = {
  icon: 'sort-amount-down',
  tooltip: (fieldName: string) => _tooltip(fieldName, Direction.Descending),
  handleSortChange: (changeSort) => changeSort(Direction.Descending, true),
  sortActive: true,
};

const DirectionStrategyDesc: DirectionStrategy = {
  icon: 'sort-amount-up',
  tooltip: (fieldName: string) => _tooltip(fieldName, Direction.Ascending),
  handleSortChange: (changeSort) => changeSort(Direction.Descending, false),
  sortActive: true,
};

const DirectionStrategyNoSort: DirectionStrategy = {
  icon: DirectionStrategyAsc.icon,
  tooltip: (fieldName: string) => _tooltip(fieldName, Direction.Ascending),
  handleSortChange: (changeSort) => changeSort(Direction.Ascending, true),
  sortActive: false,
};

const _directionStrategy = (sortConfigMap: OrderedMap<string, SortConfig>, fieldName: string) => {
  const fieldSortDirection = _isFieldSortActive(sortConfigMap, fieldName)?.direction?.direction;

  switch (fieldSortDirection) {
    case Direction.Ascending.direction:
      return DirectionStrategyAsc;
    case Direction.Descending.direction:
      return DirectionStrategyDesc;
    default:
      return DirectionStrategyNoSort;
  }
};

const FieldSortIcon = ({ fieldName, type, sortConfigMap, onSortChange, setLoadingState }: Props) => {
  const changeSort = (nextDirection: Direction, activeSort: boolean) => _changeSort(nextDirection, sortConfigMap, fieldName, onSortChange, setLoadingState, type, activeSort);
  const { sortActive, tooltip, handleSortChange, icon }: DirectionStrategy = _directionStrategy(sortConfigMap, fieldName);

  return (
    <SortIcon sortActive={sortActive}
              title={tooltip(fieldName)}
              type="button"
              aria-label={tooltip(fieldName)}
              onClick={() => handleSortChange(changeSort)}
              data-testid="messages-sort-icon"
              $index={sortConfigMap._map.get(fieldName) !== undefined ? sortConfigMap._map.get(fieldName) + 1 : ''}>
      <Icon name={icon} />
    </SortIcon>
  );
};

FieldSortIcon.propTypes = {
  sortConfigMap: CustomPropTypes.instanceOf(OrderedMap).isRequired,
  fieldName: PropTypes.string.isRequired,
  onSortChange: PropTypes.func.isRequired,
  setLoadingState: PropTypes.func.isRequired,
};

export default FieldSortIcon;
