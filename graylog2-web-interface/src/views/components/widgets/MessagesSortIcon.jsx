// @flow strict
import React from 'react';
import styled, { css, type StyledComponent } from 'styled-components';
import { isEmpty } from 'lodash';

import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';

import { Icon } from 'components/common';

type Props = {
  config: MessagesWidgetConfig,
  fieldName: string,
  onConfigChange: (MessagesWidgetConfig) => Promise<void>,
  setLoadingState: (loading: boolean) => void,
}

const SortIcon: StyledComponent<{fieldSortActive: boolean}, {}, HTMLButtonElement> = styled.button(({ fieldSortActive }) => {
  const color = fieldSortActive ? '#333' : '#bdbdbd';

  return css`
    border: 0;
    background: transparent;
    color: ${color};

    padding: 5px;

    cursor: pointer;
  `;
});

const _changeSort = (config: MessagesWidgetConfig, fieldName: string, fieldSortDirection: ?Direction, onConfigChange: (MessagesWidgetConfig) => Promise<void>, setLoadingState: (loading: boolean) => void) => {
  const directionName = fieldSortDirection && fieldSortDirection.direction;
  let newSort;

  setLoadingState(true);

  switch (directionName) {
    case Direction.Ascending.direction:
      newSort = config.sort.map(sort => sort.toBuilder().direction(Direction.Descending).build());
      break;
    case Direction.Descending.direction:
      newSort = config.sort.map(sort => sort.toBuilder().direction(Direction.Ascending).build());
      break;
    default:
      newSort = [new SortConfig(SortConfig.PIVOT_TYPE, fieldName, Direction.Descending)];
      break;
  }

  const newConfig = config.toBuilder().sort(newSort).build();
  onConfigChange(newConfig).then(() => {
    setLoadingState(false);
  });
};


const _fieldSortDirection = (config: MessagesWidgetConfig, fieldName: string) => {
  const currentSort = !isEmpty(config.sort) ? config.sort[0] : null;
  if (currentSort && currentSort.field === fieldName) {
    return currentSort.direction;
  }
  return undefined;
};

const _fieldSortDescription = (fielName: string, fieldSortDirection?: Direction) => {
  let newDirection;
  if (fieldSortDirection && fieldSortDirection.direction === Direction.Ascending) {
    newDirection = Direction.Descending.direction;
  } else {
    newDirection = Direction.Ascending.direction;
  }
  return `Sort ${fielName} ${newDirection}`;
};

const _sortIcon = (fieldSortDirection?: string) => {
  if (fieldSortDirection && fieldSortDirection === Direction.Ascending) {
    return 'sort-amount-asc';
  }
  return 'sort-amount-desc';
};


const MessagesSortIcon = ({ fieldName, config, onConfigChange, setLoadingState }: Props) => {
  const fieldSortDirection = _fieldSortDirection(config, fieldName);
  const fieldSortDescription = _fieldSortDescription(fieldName, fieldSortDirection);
  return (
    <SortIcon fieldSortActive={!!fieldSortDirection}
              title={fieldSortDescription}
              aria-label={fieldSortDescription}
              onClick={() => _changeSort(config, fieldName, fieldSortDirection, onConfigChange, setLoadingState)}
              data-testid="messages-sort-icon">
      <Icon name={_sortIcon(fieldSortDirection)} />
    </SortIcon>
  );
};


export default MessagesSortIcon;
