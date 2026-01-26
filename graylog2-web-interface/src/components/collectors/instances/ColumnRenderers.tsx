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

import { RelativeTime } from 'components/common';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import type { ColumnRenderers } from 'components/common/EntityDataTable';

import type { CollectorInstanceView } from '../types';

const OsIcon = ({ os }: { os: string | null }) => {
  if (os === 'linux') return <span title="Linux">🐧</span>;
  if (os === 'windows') return <span title="Windows">🪟</span>;
  if (os === 'darwin') return <span title="macOS">🍎</span>;

  return <span title="Unknown">❓</span>;
};

type Props = {
  fleetNames: Record<string, string>;
};

const customColumnRenderers = ({ fleetNames }: Props): ColumnRenderers<CollectorInstanceView> => ({
  attributes: {
    status: {
      renderCell: (_status: string, instance: CollectorInstanceView) => (
        <Badge color={instance.status === 'online' ? 'green' : 'gray'} size="sm">
          {instance.status === 'online' ? 'Online' : 'Offline'}
        </Badge>
      ),
      staticWidth: 100,
    },
    hostname: {
      renderCell: (_hostname: string, instance: CollectorInstanceView) => (
        <span>{instance.hostname || instance.agent_id}</span>
      ),
      width: 0.3,
    },
    os: {
      renderCell: (_os: string, instance: CollectorInstanceView) => (
        <OsIcon os={instance.os} />
      ),
      staticWidth: 60,
    },
    fleet_id: {
      renderCell: (_fleetId: string, instance: CollectorInstanceView) => (
        <Link to={Routes.SYSTEM.COLLECTORS.FLEET(instance.fleet_id)}>
          {fleetNames[instance.fleet_id] || instance.fleet_id}
        </Link>
      ),
      width: 0.2,
    },
    last_seen: {
      renderCell: (_lastSeen: string, instance: CollectorInstanceView) => (
        <RelativeTime dateTime={instance.last_seen} />
      ),
      width: 0.2,
    },
    version: {
      renderCell: (version: string) => <span>{version}</span>,
      width: 0.1,
    },
  },
});

export default customColumnRenderers;
