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
import { Badge } from '@mantine/core';

import type { ColumnRenderers } from 'components/common/EntityDataTable';

import type { Source } from '../types';

const sourceTypeLabels: Record<string, string> = {
  file: 'File',
  journald: 'Journald',
  windows_event_log: 'Windows Event Log',
  tcp: 'TCP',
  udp: 'UDP',
};

const customColumnRenderers = (): ColumnRenderers<Source> => ({
  attributes: {
    name: {
      renderCell: (name: string) => <strong>{name}</strong>,
      width: 0.25,
    },
    type: {
      renderCell: (type: string) => (
        <Badge variant="light" size="sm">
          {sourceTypeLabels[type] || type}
        </Badge>
      ),
      staticWidth: 140,
    },
    enabled: {
      renderCell: (_enabled: boolean, source: Source) => (
        <Badge color={source.enabled ? 'green' : 'gray'} size="sm">
          {source.enabled ? 'Enabled' : 'Disabled'}
        </Badge>
      ),
      staticWidth: 100,
    },
    description: {
      renderCell: (description: string) => <span>{description || '—'}</span>,
      width: 0.4,
    },
  },
});

export default customColumnRenderers;
