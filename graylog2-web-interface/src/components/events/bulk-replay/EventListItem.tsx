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

import IconButton from 'components/common/IconButton';
import type { Event } from 'components/events/events/types';
import { Icon } from 'components/common';

type EventListItemProps = {
  event: Event;
  done: boolean;
  selected: boolean;
  onClick?: () => void;
  removeItem: (id: string) => void;
  markItemAsDone: (id: string) => void;
  className?: string;
  isDropdown?: boolean;
};

type StyledItemProps = {
  $selected: boolean;
};
const StyledItem = styled.div<StyledItemProps>(
  ({ theme, $selected }) => css`
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 30px;
    background-color: ${$selected ? theme.colors.background.secondaryNav : 'transparent'};
    cursor: pointer;
    max-width: 270px;
    position: relative;
    gap: ${theme.spacings.xs};

    &:hover {
      background-color: ${theme.colors.background.body};
    }
  `,
);

type SummaryProps = {
  $done: boolean;
};

const Summary = styled.span<SummaryProps>(
  ({ theme, $done }) => css`
    color: ${$done ? theme.colors.text.secondary : theme.colors.text.primary};
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    flex-grow: 1;
  `,
);
const Ellipsis = styled.span`
  text-overflow: ellipsis;
`;

const CompletedButton = styled(IconButton)<{ $done: boolean }>(
  ({ theme, $done }) => css`
    color: ${$done ? theme.colors.variant.success : theme.colors.gray[60]};
  `,
);

const EventListItem = ({
  done,
  event,
  onClick = undefined,
  selected,
  removeItem,
  markItemAsDone,
  className = '',
  isDropdown = false,
}: EventListItemProps) => {
  const _removeItem = (e: React.MouseEvent<HTMLButtonElement>) => {
    e.stopPropagation();

    return removeItem(event.id);
  };
  const _markItemAsDone = (e: React.MouseEvent<HTMLButtonElement>) => {
    e.stopPropagation();

    return markItemAsDone(event.id);
  };

  return (
    <StyledItem
      $selected={selected}
      onClick={onClick}
      className={className}
      title={isDropdown ? 'Show Selected Events' : undefined}>
      <CompletedButton
        onClick={_markItemAsDone}
        title={`Mark event "${event?.id}" as ${done ? 'not' : ''} reviewed`}
        name="verified"
        iconType={done ? 'solid' : 'regular'}
        $done={done}
      />
      <Summary $done={done}>
        <Ellipsis>{event?.message ?? <i>Unknown</i>}</Ellipsis>
      </Summary>
      {isDropdown ? (
        <Icon name="arrow_drop_down" />
      ) : (
        <IconButton onClick={_removeItem} title={`Remove event "${event?.id}" from list`} name="delete" />
      )}
    </StyledItem>
  );
};

export default EventListItem;
