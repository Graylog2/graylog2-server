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
import { useTranslation } from 'react-i18next';

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
    padding: ${theme.spacings.xs} 0;
    background-color: ${$selected ? theme.colors.background.secondaryNav : 'transparent'};
    cursor: pointer;
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
    flex-grow: 1;
    overflow-wrap: anywhere;
  `,
);

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
  const { t } = useTranslation('events');
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
      title={isDropdown ? t('alerts_bulk_replay.show_selected') : undefined}>
      <CompletedButton
        onClick={_markItemAsDone}
        title={
          done
            ? t('alerts_bulk_replay.mark_not_reviewed', { id: event?.id })
            : t('alerts_bulk_replay.mark_reviewed', { id: event?.id })
        }
        name="verified"
        iconType={done ? 'solid' : 'regular'}
        $done={done}
      />
      <Summary $done={done}>{event?.message ?? <i>{t('alerts_bulk_replay.unknown_message')}</i>}</Summary>
      {isDropdown ? (
        <Icon name="arrow_drop_down" />
      ) : (
        <IconButton
          onClick={_removeItem}
          title={t('alerts_bulk_replay.remove_event', { id: event?.id })}
          name="delete"
        />
      )}
    </StyledItem>
  );
};

export default EventListItem;
