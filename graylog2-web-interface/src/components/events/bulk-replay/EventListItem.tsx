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
import { useCallback } from 'react';
import styled, { css } from 'styled-components';

import IconButton from 'components/common/IconButton';
import ButtonGroup from 'components/bootstrap/ButtonGroup';
import type { Event } from 'components/events/events/types';

type EventListItemProps = {
  event: Event,
  done: boolean,
  selected: boolean,
  onClick: () => void,
  removeItem: (id: string) => void,
  markItemAsDone: (id: string) => void,
}

type StyledItemProps = {
  $selected: boolean;
}
const StyledItem = styled.li<StyledItemProps>(({ theme, $selected }) => css`
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 30px;
  background-color: ${$selected ? theme.colors.background.secondaryNav : 'transparent'};
  cursor: pointer;

  &:hover {
    background-color: ${theme.colors.background.body};
  }
`);

type SummaryProps = {
  $done: boolean;
}

const Summary = styled.span<SummaryProps>(({ theme, $done }) => css`
  margin: 10px;
  color: ${$done ? theme.colors.global.textSecondary : theme.colors.global.textDefault};
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  &:hover {
    text-decoration: underline;
  }
`);

const CompletedButton = styled(IconButton)<{ $done: boolean }>(({ theme, $done }) => css`
  color: ${$done ? theme.colors.variant.success : theme.colors.gray[60]};
`);

const EventListItem = ({ done, event, onClick, selected, removeItem, markItemAsDone }: EventListItemProps) => {
  const _removeItem = useCallback((e: React.MouseEvent<HTMLButtonElement>) => {
    e.stopPropagation();

    return removeItem(event.id);
  }, [event?.id, removeItem]);
  const _markItemAsDone = useCallback((e: React.MouseEvent<HTMLButtonElement>) => {
    e.stopPropagation();

    return markItemAsDone(event.id);
  }, [event?.id, markItemAsDone]);

  return (
    <StyledItem key={`event-replay-list-${event?.id}`} $selected={selected} onClick={onClick}>
      <Summary $done={done}>{event?.message ?? <i>Unknown</i>}</Summary>

      <ButtonGroup>
        <IconButton onClick={_removeItem} title={`Remove event "${event?.id}" from list`} name="delete" />
        <CompletedButton onClick={_markItemAsDone}
                         title={`Mark event "${event?.id}" as ${done ? 'not' : ''} investigated`}
                         name="verified"
                         iconType={done ? 'solid' : 'regular'}
                         $done={done} />
      </ButtonGroup>
    </StyledItem>
  );
};

export default EventListItem;
