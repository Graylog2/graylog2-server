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
import styled from 'styled-components';

import { Sanitize, Spinner } from 'components/common';
import type { NotificationType } from 'components/notifications/types';
import useNotificationBody from 'components/notifications/hooks/useNotificationBody';

const StyledPre = styled.pre`
  white-space: normal;
`;

const FALLBACK_MESSAGE = 'Could not load full notification body.';

const NotificationBody = ({ row }: { row: NotificationType }) => {
  const { data, isLoading, isError } = useNotificationBody(row.id);

  if (isLoading) return <Spinner />;
  if (isError || !data) return <span>{FALLBACK_MESSAGE}</span>;

  return <StyledPre><Sanitize html={data.description.trim()} /></StyledPre>;
};

const expandedSections = {
  body: {
    title: '',
    disableHeader: true,
    content: (row: NotificationType) => <NotificationBody row={row} />,
  },
};

export default expandedSections;
