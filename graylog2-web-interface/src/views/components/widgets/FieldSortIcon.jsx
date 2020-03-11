// @flow strict
import React from 'react';
import styled, { css, type StyledComponent } from 'styled-components';
import PropTypes from 'prop-types';

import MessagesWidgetConfig, { defaultSortDirection } from 'views/logic/widgets/MessagesWidgetConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import CustomPropTypes from 'views/components/CustomPropTypes';

import { Icon } from 'components/common';

type Props = {
  config: MessagesWidgetConfig,
  fieldName: string,
  onConfigChange: (MessagesWidgetConfig) => Promise<void>,
  setLoadingState: (loading: boolean) => void,
}

type CurrentState = {
  onSortChange: (changeSort: (direction: Direction) => void) => void,
  tooltip: (fieldName: string) => string,
  sortActive: boolean,
  icon: string
}

const SortIcon: StyledComponent<{sortActive: boolean}, {}, HTMLButtonElement> = styled.button(({ sortActive }) => {
  const color = sortActive ? '#333' : '#bdbdbd';

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

const _changeSort = (nextDirection: Direction, config: MessagesWidgetConfig, fieldName: string, onConfigChange: (MessagesWidgetConfig) => Promise<void>, setLoadingState: (loading: boolean) => void) => {
  const nextSort = [new SortConfig(SortConfig.PIVOT_TYPE, fieldName, nextDirection)];
  const newConfig = config.toBuilder().sort(nextSort).build();
  setLoadingState(true);
  onConfigChange(newConfig).then(() => {
    setLoadingState(false);
  });
};

const _isFieldSortActive = (config: MessagesWidgetConfig, fieldName: string) => {
  return config.sort && config.sort.length > 0 && config.sort[0].field === fieldName;
};

const StateAsc: CurrentState = {
  icon: 'sort-amount-asc',
  tooltip: (fieldName: string) => _tooltip(fieldName, Direction.Descending),
  onSortChange: changeSort => changeSort(Direction.Descending),
  sortActive: true,
};

const StateDesc: CurrentState = {
  icon: 'sort-amount-desc',
  tooltip: (fieldName: string) => _tooltip(fieldName, Direction.Ascending),
  onSortChange: changeSort => changeSort(Direction.Ascending),
  sortActive: true,
};

const StateNoSort: CurrentState = {
  icon: Direction.Descending.equals(defaultSortDirection) ? StateDesc.icon : StateAsc.icon,
  tooltip: (fieldName: string) => _tooltip(fieldName, defaultSortDirection),
  onSortChange: changeSort => changeSort(defaultSortDirection),
  sortActive: false,
};

const _currentState = (config: MessagesWidgetConfig, fieldName: string) => {
  const fieldSortDirection = _isFieldSortActive(config, fieldName) ? config.sort[0].direction.direction : null;
  switch (fieldSortDirection) {
    case Direction.Ascending.direction:
      return StateAsc;
    case Direction.Descending.direction:
      return StateDesc;
    default:
      return StateNoSort;
  }
};

const FieldSortIcon = ({ fieldName, config, onConfigChange, setLoadingState }: Props) => {
  const changeSort = (nextDirection: Direction) => _changeSort(nextDirection, config, fieldName, onConfigChange, setLoadingState);
  const { sortActive, tooltip, onSortChange, icon }: CurrentState = _currentState(config, fieldName);
  return (
    <SortIcon sortActive={sortActive}
              title={tooltip(fieldName)}
              aria-label={tooltip(fieldName)}
              onClick={() => onSortChange(changeSort)}
              data-testid="messages-sort-icon">
      <Icon name={icon} />
    </SortIcon>
  );
};

FieldSortIcon.propTypes = {
  config: CustomPropTypes.instanceOf(MessagesWidgetConfig).isRequired,
  fieldName: PropTypes.string.isRequired,
  onConfigChange: PropTypes.func.isRequired,
  setLoadingState: PropTypes.func.isRequired,
};

export default FieldSortIcon;
