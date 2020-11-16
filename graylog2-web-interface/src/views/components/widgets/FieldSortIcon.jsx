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
// @flow strict
import React from 'react';
import styled, { css } from 'styled-components';
import type { StyledComponent } from 'styled-components';
import PropTypes from 'prop-types';

import type { ThemeInterface } from 'theme';
import MessagesWidgetConfig, { defaultSortDirection } from 'views/logic/widgets/MessagesWidgetConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import CustomPropTypes from 'views/components/CustomPropTypes';
import { Icon } from 'components/common';

type Props = {
  config: MessagesWidgetConfig,
  fieldName: string,
  onSortChange: (SortConfig[]) => Promise<void>,
  setLoadingState: (loading: boolean) => void,
};

type DirectionStrategy = {
  handleSortChange: (changeSort: (direction: Direction) => void) => void,
  icon: string,
  sortActive: boolean,
  tooltip: (fieldName: string) => string,
};

const SortIcon: StyledComponent<{sortActive: boolean}, ThemeInterface, HTMLButtonElement> = styled.button(({ sortActive, theme }) => {
  const color = sortActive ? theme.colors.gray[20] : theme.colors.gray[70];

  return css`
    border: 0;
    background: transparent;
    color: ${color};
    padding: 5px;
    cursor: pointer;
  `;
});

const _tooltip = (fieldName: string, newDirection: Direction) => {
  return `Sort ${fieldName} ${newDirection.direction}`;
};

const _changeSort = (nextDirection: Direction, config: MessagesWidgetConfig, fieldName: string, onSortChange: (SortConfig[]) => Promise<void>, setLoadingState: (loading: boolean) => void) => {
  const newSort = [new SortConfig(SortConfig.PIVOT_TYPE, fieldName, nextDirection)];

  setLoadingState(true);

  onSortChange(newSort).then(() => {
    setLoadingState(false);
  });
};

const _isFieldSortActive = (config: MessagesWidgetConfig, fieldName: string) => {
  return config.sort && config.sort.length > 0 && config.sort[0].field === fieldName;
};

const DirectionStrategyAsc: DirectionStrategy = {
  icon: 'sort-amount-down',
  tooltip: (fieldName: string) => _tooltip(fieldName, Direction.Descending),
  handleSortChange: (changeSort) => changeSort(Direction.Descending),
  sortActive: true,
};

const DirectionStrategyDesc: DirectionStrategy = {
  icon: 'sort-amount-up',
  tooltip: (fieldName: string) => _tooltip(fieldName, Direction.Ascending),
  handleSortChange: (changeSort) => changeSort(Direction.Ascending),
  sortActive: true,
};

const DirectionStrategyNoSort: DirectionStrategy = {
  icon: Direction.Descending.equals(defaultSortDirection) ? DirectionStrategyDesc.icon : DirectionStrategyAsc.icon,
  tooltip: (fieldName: string) => _tooltip(fieldName, defaultSortDirection),
  handleSortChange: (changeSort) => changeSort(defaultSortDirection),
  sortActive: false,
};

const _directionStrategy = (config: MessagesWidgetConfig, fieldName: string) => {
  const fieldSortDirection = _isFieldSortActive(config, fieldName) ? config.sort[0].direction.direction : null;

  switch (fieldSortDirection) {
    case Direction.Ascending.direction:
      return DirectionStrategyAsc;
    case Direction.Descending.direction:
      return DirectionStrategyDesc;
    default:
      return DirectionStrategyNoSort;
  }
};

const FieldSortIcon = ({ fieldName, config, onSortChange, setLoadingState }: Props) => {
  const changeSort = (nextDirection: Direction) => _changeSort(nextDirection, config, fieldName, onSortChange, setLoadingState);
  const { sortActive, tooltip, handleSortChange, icon }: DirectionStrategy = _directionStrategy(config, fieldName);

  return (
    <SortIcon sortActive={sortActive}
              title={tooltip(fieldName)}
              type="button"
              aria-label={tooltip(fieldName)}
              onClick={() => handleSortChange(changeSort)}
              data-testid="messages-sort-icon">
      <Icon name={icon} />
    </SortIcon>
  );
};

FieldSortIcon.propTypes = {
  config: CustomPropTypes.instanceOf(MessagesWidgetConfig).isRequired,
  fieldName: PropTypes.string.isRequired,
  onSortChange: PropTypes.func.isRequired,
  setLoadingState: PropTypes.func.isRequired,
};

export default FieldSortIcon;
