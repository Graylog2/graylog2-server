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

import { Badge } from 'components/bootstrap';
import { Icon } from 'components/common';
import type { NotificationType } from 'components/notifications/types';
import useNotificationToggleRead from 'components/notifications/hooks/useNotificationToggleRead';

const ClickableBadge = styled(Badge)(
  () => css`
    cursor: pointer;
    display: inline-flex;
    align-items: center;
    gap: 4px;
  `,
);

type Props = { row: NotificationType };

const StatusCell = ({ row }: Props) => {
  const { mutate: toggleRead, isPending } = useNotificationToggleRead();

  return (
    <ClickableBadge
      bsStyle={row.is_read ? 'default' : 'info'}
      title={row.is_read ? 'Mark as unread' : 'Mark as read'}
      onClick={isPending ? undefined : () => toggleRead({ id: row.id, currentIsRead: row.is_read })}>
      <Icon name={row.is_read ? 'drafts' : 'mail'} size="sm" />
      {row.is_read ? 'Read' : 'Unread'}
    </ClickableBadge>
  );
};

export default StatusCell;
