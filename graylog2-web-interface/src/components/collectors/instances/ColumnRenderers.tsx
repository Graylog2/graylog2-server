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

import { Link, RelativeTime } from 'components/common';
import Routes from 'routing/Routes';
import type { ColumnRenderers } from 'components/common/EntityDataTable';

import InstanceStatusLabel from '../common/InstanceStatusLabel';
import SyncStateIndicator from '../common/SyncStateIndicator';
import collectorOsName from '../common/collectorOsName';
import type { CollectorInstanceView } from '../types';

const OsName = ({ instance }: { instance: CollectorInstanceView }) => {
  const label = collectorOsName(instance);

  return <span title={label}>{label}</span>;
};

type Props = {
  fleetNames: Record<string, string>;
};

const customColumnRenderers = ({ fleetNames }: Props): ColumnRenderers<CollectorInstanceView> => ({
  attributes: {
    status: {
      renderCell: (_status: string, instance: CollectorInstanceView) => (
        <InstanceStatusLabel status={instance.status} />
      ),
      staticWidth: 100,
    },
    has_pending_changes: {
      renderCell: (_hasPendingChanges: boolean, instance: CollectorInstanceView) => (
        <SyncStateIndicator pending={instance.has_pending_changes} />
      ),
      staticWidth: 60,
    },
    hostname: {
      renderCell: (_hostname: string, instance: CollectorInstanceView) => (
        <span>{instance.hostname || instance.instance_uid}</span>
      ),
      width: 0.3,
    },
    os: {
      renderCell: (_os: string, instance: CollectorInstanceView) => <OsName instance={instance} />,
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
